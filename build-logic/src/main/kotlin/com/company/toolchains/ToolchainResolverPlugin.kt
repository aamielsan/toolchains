package com.company.toolchains

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.jvm.toolchain.JavaToolchainResolverRegistry
import org.gradle.kotlin.dsl.jvm
import javax.inject.Inject

abstract class ToolchainResolverPlugin : Plugin<Settings> {
     @get:Inject
    abstract val toolchainResolverRegistry: JavaToolchainResolverRegistry

    override fun apply(settings: Settings) {
        settings.pluginManager.apply("jvm-toolchain-management")
        toolchainResolverRegistry.register(AmazonCorrettoJavaToolchainResolver::class.java)
        settings.toolchainManagement {
            jvm {
                javaRepositories {
                    repository("default") {
                        resolverClass.set(AmazonCorrettoJavaToolchainResolver::class.java)
                    }
                }
            }
        }

    }
}