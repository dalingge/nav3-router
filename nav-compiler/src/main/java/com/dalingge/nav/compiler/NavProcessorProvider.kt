package com.dalingge.nav.compiler

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/30  10:19
 */
@AutoService(SymbolProcessorProvider::class)
class NavProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = NavSymbolProcessor(environment.codeGenerator, environment.logger,environment.options)
}