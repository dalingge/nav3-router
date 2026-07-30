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
    private val logger: KSPLogger, // KSP 日志记录器
    private val options: Map<String, String> // KSP 环境变量选项
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.yiqun.nav.annotation.Screen")
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        if (symbols.isEmpty()) {
            return emptyList()
        }

        logger.info("==================== NavSymbolProcessor Start ====================")
        logger.info("Found ${symbols.size} @Screen annotated composable functions.")

        //  自动模块名识别逻辑：优先读取 Gradle arg，若无则自动根据 Package 解析
        val moduleNameFromPkg = symbols.firstOrNull()?.packageName?.asString()
            ?.split(".")
            ?.dropLast(1)
            ?.lastOrNull() ?: ""

        val rawModuleName = options["NAV_MODULE_NAME"] ?: moduleNameFromPkg
        val capitalizedModuleName = rawModuleName.replaceFirstChar { it.uppercase() }

        val initFuncName = if (capitalizedModuleName.isNotEmpty()) "init${capitalizedModuleName}" else "initNavRegistry"
        val fileName = if (capitalizedModuleName.isNotEmpty()) "${capitalizedModuleName}NavRegistryInit" else "NavRegistryInit"

        logger.info("Target Module -> '$capitalizedModuleName' | Function -> NavCenter.$initFuncName() | File -> $fileName.kt")

        // 收集增量源文件依赖，防止 build 目录清理后丢失
        val sourceFiles = symbols.mapNotNull { it.containingFile }.toTypedArray()
        val dependencies = Dependencies(aggregating = true, *sourceFiles)

        val registryInitBlock = CodeBlock.builder()

        symbols.forEach { function ->
            val annotation = function.annotations.firstOrNull { it.shortName.asString() == "Screen" } ?: return@forEach

            val route = annotation.arguments.firstOrNull { it.name?.asString() == "route" }?.value as? String ?: ""
            val needLogin = annotation.arguments.firstOrNull { it.name?.asString() == "needLogin" }?.value as? Boolean ?: false

            if (route.isEmpty()) {
                logger.error("Route path cannot be empty on function: ${function.simpleName.asString()}", function)
                return@forEach
            }

            val functionName = function.simpleName.asString()
            val packageName = function.packageName.asString()
            val destClassName = "${functionName}Destination"

            logger.info("Processing Screen: route = '$route' -> $packageName.$destClassName")

            // 过滤提取路由参数（排除带有默认值的 ViewModel 和 Navigator）
            val routeParams = function.parameters.filter { param ->
                val typeName = param.type.toTypeName().toString()
                !param.hasDefault && typeName != "com.yiqun.nav.runtime.Navigator"
            }

            // 生成 Destination 强类型数据类
            generateDestinationClass(packageName, destClassName, route, routeParams, dependencies)

            // 拼接代码块
            buildRegistryStatement(registryInitBlock, packageName, destClassName, route, needLogin, function, routeParams, annotation)
        }

        // 生成模块注册表初始化文件
        generateRegistryFile(fileName, initFuncName, registryInitBlock.build(), dependencies)

        logger.info("==================== NavSymbolProcessor Finish ====================")
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
            // 无参生成普通 class (避免 Kotlin data class 无参编译错误)
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
                if (typeStr == "kotlin.String" || typeStr == "kotlin.Int" || typeStr == "kotlin.Boolean" || typeStr == "kotlin.Long") {
                    toUrlCode.append("$name=\$$name&")
                } else {
                    toUrlCode.append("$name=\${java.net.URLEncoder.encode(kotlinx.serialization.json.Json.encodeToString($name), \"UTF-8\")}&")
                }
            }

            val finalToUrlStatement = toUrlCode.toString().dropLast(1) + "\""

            classBuilder.addProperty(
                PropertySpec.builder("route", String::class, KModifier.OVERRIDE).initializer("%S", route).build()
            )

            // 使用 %L 输出原生 Kotlin 运行时求值表达式
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

        logger.logging("Generated Destination Class: $packageName.$className")
    }

    private fun buildRegistryStatement(
        block: CodeBlock.Builder,
        pkg: String,
        destClass: String,
        route: String,
        needLogin: Boolean,
        function: KSFunctionDeclaration,
        routeParams: List<KSValueParameter>,
        annotation: KSAnnotation
    ) {
        val destFQCN = ClassName(pkg, destClass)

        block.addStatement("com.yiqun.nav.runtime.NavRegistry.register(")
        block.indent()
        block.addStatement("com.yiqun.nav.runtime.RouteMeta(")
        block.indent()

        block.addStatement("route = %S,", route)
        block.addStatement("needLogin = $needLogin,")

        // Factory 构建
        block.addStatement("factory = { params ->")
        block.indent()

        if (routeParams.isEmpty()) {
            block.addStatement("%T()", destFQCN)
        } else {
            val factoryArgs = routeParams.joinToString(", ") { p ->
                val pName = p.name!!.asString()
                when (p.type.toTypeName().toString()) {
                    "kotlin.Int" -> "$pName = params[\"$pName\"]?.toIntOrNull() ?: 0"
                    "kotlin.Boolean" -> "$pName = params[\"$pName\"]?.toBooleanStrictOrNull() ?: false"
                    "kotlin.Long" -> "$pName = params[\"$pName\"]?.toLongOrNull() ?: 0L"
                    "kotlin.String" -> "$pName = params[\"$pName\"] ?: \"\""
                    else -> "$pName = kotlinx.serialization.json.Json.decodeFromString(params[\"$pName\"] ?: \"{}\")"
                }
            }
            block.addStatement("%T($factoryArgs)", destFQCN)
        }

        block.unindent()
        block.addStatement("},")

        // Content 调用
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

        // 解析局部拦截器
        val interceptorsArg = annotation.arguments.firstOrNull { it.name?.asString() == "interceptors" }
        @Suppress("UNCHECKED_CAST")
        val interceptorTypes = (interceptorsArg?.value as? List<*>)?.mapNotNull { it as? KSType } ?: emptyList()

        if (interceptorTypes.isEmpty()) {
            block.addStatement("interceptors = emptyList(),")
        } else {
            val instances = interceptorTypes.joinToString(", ") {
                "${it.declaration.qualifiedName?.asString()}()"
            }
            block.addStatement("interceptors = listOf($instances),")
        }

        // 解析局部转场动画
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
        dependencies: Dependencies
    ) {
        val navCenterClassName = ClassName("com.yiqun.nav.runtime", "NavCenter")

        FileSpec.builder("com.yiqun.nav.generated", fileName)
            .addFunction(
                FunSpec.builder(funcName)
                    .receiver(navCenterClassName) // 扩展函数：NavCenter.initXxx()
                    .returns(navCenterClassName)  // 支持链式调用：return this
                    .addCode(initBlock)
                    .addStatement("return this")
                    .build()
            )
            .build()
            .writeTo(codeGenerator, dependencies)

        logger.info("Successfully generated Init Registry: com.yiqun.nav.generated.$fileName.kt -> fun NavCenter.$funcName()")
    }
}