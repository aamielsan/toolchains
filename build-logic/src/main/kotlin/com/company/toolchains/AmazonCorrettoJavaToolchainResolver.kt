package com.company.toolchains

import com.company.internals.Failure
import com.company.internals.Result
import com.company.internals.Success
import com.company.internals.flatMap
import com.company.internals.map
import com.company.internals.recover
import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.toolchain.internal.DefaultJvmVendorSpec
import org.gradle.platform.Architecture
import org.gradle.platform.OperatingSystem
import java.net.URI
import java.util.*

/**
 * JavaToolchainResolver for Amazon Corretto flavored JDKs.
 * Normally a JavaToolchainResolver should be able to resolve JDKs based on different vendors, OS, and architecture,
 * and a more generalise resolver can be implemented, but for current our use case resolving AmazonCorretto JDKs
 * should suffice
 */
abstract class AmazonCorrettoJavaToolchainResolver : JavaToolchainResolver {
    private object UnsupportedRequest

    override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> =
        validate(request)
            .flatMap(::resolveDownloadUri)
            .map(JavaToolchainDownload::fromUri)
            .map { Optional.of(it) }
            .recover { Optional.empty() }

    private fun validate(request: JavaToolchainRequest): Result<JavaToolchainRequest, UnsupportedRequest> {
        val supportedVendors = listOf(
            JvmVendorSpec.AMAZON,
            DefaultJvmVendorSpec.any(),
        )

        return when (request.javaToolchainSpec.vendor.get()) {
            in supportedVendors -> Success(request)
            else -> Failure(UnsupportedRequest)
        }
    }

    private fun resolveDownloadUri(request: JavaToolchainRequest): Result<URI, UnsupportedRequest> {
        val spec = request.javaToolchainSpec
        val version = spec.languageVersion.get()
        val platform = request.buildPlatform

        val architecture =
            when (platform.architecture) {
                Architecture.X86_64 -> "x64"
                Architecture.AARCH64 -> "aarch64"
                else -> return Failure(UnsupportedRequest)
            }

        val operatingSystem =
            when (platform.operatingSystem) {
                // Since JavaToolchainResolver can't detect if we're running alpine Linux and
                // since we're only running Linux on CI builders, assume that we're using alpine image
                // https://docs.gradle.org/current/userguide/toolchains.html#sec:limitations
                // TODO: Add a warning log that we're defaulting to alpine
                OperatingSystem.LINUX -> "alpine"
                OperatingSystem.WINDOWS -> "windows"
                OperatingSystem.MAC_OS -> "macos"
                else -> return Failure(UnsupportedRequest)
            }

        val extension =
            when (platform.operatingSystem) {
                OperatingSystem.WINDOWS -> "zip"
                OperatingSystem.LINUX,
                OperatingSystem.MAC_OS -> "tar.gz"
                else -> return Failure(UnsupportedRequest)
            }

        val downloadUri = URI.create(
            "https://corretto.aws/downloads/latest/amazon-corretto-${version.asInt()}-${architecture}-${operatingSystem}-jdk.${extension}"
        )

        return Success(downloadUri)
    }
}