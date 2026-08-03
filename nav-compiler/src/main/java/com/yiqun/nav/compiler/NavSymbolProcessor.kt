package com.yiqun.nav.compiler

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/28  14:02
 */

class NavSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.yiqun.nav.annotation.Screen")
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        if (symbols.isEmpty()) return emptyList()

        //  1. 编译期查重检测 (Duplicate Route Detection)
        val routeMap = mutableMapOf<String, KSFunctionDeclaration>()
        symbols.forEach { function ->
            val annotation = function.annotations.firstOrNull { it.shortName.asString() == "Screen" } ?: return@forEach
            val route = annotation.arguments.firstOrNull { it.name?.asString() == "route" }?.value as? String ?: ""

            if (routeMap.containsKey(route)) {
                val conflictFunc = routeMap[route]!!
                logger.error(
                    "❌ [Nav3-Router 编译错误] 路由冲突！路径 '$route' 同时被 ${conflictFunc.qualifiedName?.asString()} 和 ${function.qualifiedName?.asString()} 占用。路由路径必须全局唯一！",
                    function
                )
            } else {
                routeMap[route] = function
            }
        }

        val moduleNameFromPkg = symbols.firstOrNull()?.packageName?.asString()
            ?.split(".")
            ?.dropLast(1)
            ?.lastOrNull() ?: ""

        val rawModuleName = options["NAV_MODULE_NAME"] ?: moduleNameFromPkg
        val capitalizedModuleName = rawModuleName.replaceFirstChar { it.uppercase() }

        val initFuncName = if (capitalizedModuleName.isNotEmpty()) "init${capitalizedModuleName}" else "initNavRegistry"
        val fileName = if (capitalizedModuleName.isNotEmpty()) "${capitalizedModuleName}NavRegistryInit" else "NavRegistryInit"

        val sourceFiles = symbols.mapNotNull { it.containingFile }.toTypedArray()
        val dependencies = Dependencies(aggregating = true, *sourceFiles)

        val registryInitBlock = CodeBlock.builder()

        symbols.forEach { function ->
            val annotation = function.annotations.firstOrNull { it.shortName.asString() == "Screen" } ?: return@forEach
            val route = annotation.arguments.firstOrNull { it.name?.asString() == "route" }?.value as? String ?: ""
            val needLogin = annotation.arguments.firstOrNull { it.name?.asString() == "needLogin" }?.value as? Boolean ?: false

            val functionName = function.simpleName.asString()
            val packageName = function.packageName.asString()
            val destClassName = "${functionName}Destination"

            val routeParams = function.parameters.filter { param ->
                val typeName = param.type.toTypeName().toString()
                !param.hasDefault && typeName != "com.yiqun.nav.runtime.Navigator"
            }

            generateDestinationClass(packageName, destClassName, route, routeParams, dependencies)
            buildRegistryStatement(registryInitBlock, packageName, destClassName, route, needLogin, function, routeParams, annotation, logger)
        }

        generateRegistryFile(fileName, initFuncName, registryInitBlock.build(), dependencies, logger)
        return emptyList()
    }

    private fun generateDestinationClass(
        packageName: String,
        className: String,
        route: String,
        routeParams: List<KSValueParameter>,
        dependencies: Dependencies
    ) {
        val classBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName("com.yiqun.nav.runtime", "NavDestination"))

        val primaryConstructor = FunSpec.constructorBuilder()

        if (routeParams.isEmpty()) {
            classBuilder.addProperty(
                PropertySpec.builder("route", String::class, KModifier.OVERRIDE).initializer("%S", route).build()
            )
            classBuilder.addFunction(
                FunSpec.builder("toUrl").addModifiers(KModifier.OVERRIDE).returns(String::class)
                    .addStatement("return %S", route)
                    .build()
            )
        } else {
            classBuilder.addModifiers(KModifier.DATA)

            val toUrlCode = StringBuilder("return \"$route?")
            routeParams.forEach { param ->
                val name = param.name!!.asString()
                val type = param.type.toTypeName()
                primaryConstructor.addParameter(name, type)
                classBuilder.addProperty(PropertySpec.builder(name, type).initializer(name).build())

                val typeStr = type.toString()
                when (typeStr) {
                    "kotlin.Int", "kotlin.Boolean", "kotlin.Long" ->
                        toUrlCode.append("$name=\$$name&")
                    "kotlin.String" ->
                        toUrlCode.append("$name=\${java.net.URLEncoder.encode($name, \"UTF-8\")}&")
                    else ->
                        toUrlCode.append("$name=\${java.net.URLEncoder.encode(kotlinx.serialization.json.Json.encodeToString($name), \"UTF-8\")}&")
                }

            }

            val finalToUrlStatement = toUrlCode.toString().dropLast(1) + "\""

            classBuilder.addProperty(
                PropertySpec.builder("route", String::class, KModifier.OVERRIDE).initializer("%S", route).build()
            )

            classBuilder.addFunction(
                FunSpec.builder("toUrl").addModifiers(KModifier.OVERRIDE).returns(String::class)
                    .addStatement("%L", finalToUrlStatement)
                    .build()
            )

            classBuilder.primaryConstructor(primaryConstructor.build())
        }

        FileSpec.builder(packageName, className)
            .addType(classBuilder.build())
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun buildRegistryStatement(
        block: CodeBlock.Builder,
        pkg: String,
        destClass: String,
        route: String,
        needLogin: Boolean,
        function: KSFunctionDeclaration,
        routeParams: List<KSValueParameter>,
        annotation: KSAnnotation,
        logger: KSPLogger
    ) {
        val destFQCN = ClassName(pkg, destClass)

        block.addStatement("com.yiqun.nav.runtime.NavRegistry.register(")
        block.indent()
        block.addStatement("com.yiqun.nav.runtime.RouteMeta(")
        block.indent()

        block.addStatement("route = %S,", route)
        block.addStatement("needLogin = $needLogin,")

        //  必传参数防御解析 (Required Arguments Check)
        block.addStatement("factory = { params ->")
        block.indent()

        if (routeParams.isEmpty()) {
            block.addStatement("%T()", destFQCN)
        } else {
            val factoryArgs = routeParams.joinToString(", ") { p ->
                val pName = p.name!!.asString()
                val isRequired = p.annotations.any { it.shortName.asString() == "Required" }

                when (p.type.toTypeName().toString()) {
                    "kotlin.Int" -> "$pName = params[\"$pName\"]?.toIntOrNull() ?: 0"
                    "kotlin.Boolean" -> "$pName = params[\"$pName\"]?.toBooleanStrictOrNull() ?: false"
                    "kotlin.Long" -> "$pName = params[\"$pName\"]?.toLongOrNull() ?: 0L"
                    "kotlin.String" -> {
                        if (isRequired) {
                            // 必传参数若为空则抛出显式 IllegalStateException
                            "$pName = params[\"$pName\"].takeIf { !it.isNullOrEmpty() } ?: throw IllegalStateException(\"Missing required route parameter: '$pName' for route '$route'\")"
                        } else {
                            "$pName = params[\"$pName\"] ?: \"\""
                        }
                    }
                    else -> "$pName = kotlinx.serialization.json.Json.decodeFromString(params[\"$pName\"] ?: \"{}\")"
                }
            }
            block.addStatement("try {")
            block.indent()
            block.addStatement("%T($factoryArgs)", destFQCN)
            block.unindent()
            block.addStatement("} catch(e: Exception) {")
            block.indent()
            block.addStatement("throw IllegalArgumentException(\"Failed to parse arguments for route '$route': \${e.message}\", e)")
            block.unindent()
            block.addStatement("}")
        }

        block.unindent()
        block.addStatement("},")

        // Content
        block.addStatement("content = { dest ->")
        block.indent()
        if (routeParams.isEmpty()) {
            block.addStatement("%M()", MemberName(pkg, function.simpleName.asString()))
        } else {
            block.addStatement("val d = dest as %T", destFQCN)
            val composableCallArgs = routeParams.joinToString(", ") { p ->
                val name = p.name!!.asString()
                "$name = d.$name"
            }
            block.addStatement("%M($composableCallArgs)", MemberName(pkg, function.simpleName.asString()))
        }
        block.unindent()
        block.addStatement("},")

        // Interceptors
        val interceptorsArg = annotation.arguments.firstOrNull { it.name?.asString() == "interceptors" }
        @Suppress("UNCHECKED_CAST")
        val interceptorTypes = (interceptorsArg?.value as? List<KSType>) ?: emptyList()

        if (interceptorTypes.isEmpty()) {
            block.addStatement("interceptors = emptyList(),")
        } else {
            val instances = interceptorTypes.joinToString(", ") {
                // 动态实例化运行时拦截器
                "${it.declaration.qualifiedName?.asString()}()"
            }
            block.addStatement("interceptors = listOf($instances),")
        }

        // Transition
        val transitionArg = annotation.arguments.firstOrNull { it.name?.asString() == "enterTransition" }
        val transitionType = transitionArg?.value as? KSType
        val transitionClassName = transitionType?.declaration?.qualifiedName?.asString()

        if (transitionClassName == null || transitionClassName.contains("UnspecifiedTransition")) {
            block.addStatement("transition = null")
        } else {
            block.addStatement("transition = %L()", transitionClassName)
        }

        block.unindent()
        block.addStatement(")")
        block.unindent()
        block.addStatement(")")
    }

    private fun generateRegistryFile(
        fileName: String,
        funcName: String,
        initBlock: CodeBlock,
        dependencies: Dependencies,
        logger: KSPLogger
    ) {
        val navCenterClassName = ClassName("com.yiqun.nav.runtime", "NavCenter")

        FileSpec.builder("com.yiqun.nav.generated", fileName)
            .addFunction(
                FunSpec.builder(funcName)
                    .receiver(navCenterClassName)
                    .returns(navCenterClassName)
                    .addCode(initBlock)
                    .addStatement("return this")
                    .build()
            )
            .build()
            .writeTo(codeGenerator, dependencies)

        logger.info("Successfully generated: com.yiqun.nav.generated.$fileName.kt")
    }
}
