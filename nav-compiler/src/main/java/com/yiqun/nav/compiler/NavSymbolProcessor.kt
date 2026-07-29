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
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.yiqun.nav.annotation.Screen")
            .filterIsInstance<KSFunctionDeclaration>()

        if (!symbols.iterator().hasNext()) return emptyList()

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

            generateDestinationClass(packageName, destClassName, route, routeParams)
            buildRegistryStatement(registryInitBlock, packageName, destClassName, route, needLogin, function, routeParams, annotation)
        }

        generateRegistryFile(registryInitBlock.build())
        return emptyList()
    }

    private fun generateDestinationClass(
        packageName: String,
        className: String,
        route: String,
        routeParams: List<KSValueParameter>
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

            // 构建原生的 return "app/detail?user=${...}" 动态字符串语句
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

            // 去掉末尾多余的 & 符号，并闭合字符串双引号
            val finalToUrlStatement = toUrlCode.toString().dropLast(1) + "\""

            classBuilder.addProperty(
                PropertySpec.builder("route", String::class, KModifier.OVERRIDE).initializer("%S", route).build()
            )

            //  核心修复：将 %S 改为 %L，让 KotlinPoet 输出原生求值代码表达式
            classBuilder.addFunction(
                FunSpec.builder("toUrl").addModifiers(KModifier.OVERRIDE).returns(String::class)
                    .addStatement("%L", finalToUrlStatement)
                    .build()
            )

            classBuilder.primaryConstructor(primaryConstructor.build())
        }

        FileSpec.builder(packageName, className).addType(classBuilder.build()).build().writeTo(codeGenerator, false)
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

        // Factory 构建 (已修正：params[key] 原生已解码，无需二次 URLDecoder)
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

        val transitionArg = annotation.arguments.firstOrNull { it.name?.asString() == "enterTransition" }
        val transitionType = transitionArg?.value as? KSType
        val transitionClassName = transitionType?.declaration?.qualifiedName?.asString()
        // 如果未配置或配置的是 UnspecifiedTransition，则传 null（降级使用全局动画）
        if (transitionClassName == null || transitionClassName.contains("UnspecifiedTransition")) {
            block.addStatement("transition = null")
        } else {
            block.addStatement("transition = %L(),", transitionClassName)
        }

        block.unindent()
        block.addStatement(")")
        block.unindent()
        block.addStatement(")")
    }

    private fun generateRegistryFile(initBlock: CodeBlock) {
        FileSpec.builder("com.yiqun.nav.generated", "NavRegistryInit")
            .addFunction(FunSpec.builder("initNavRegistry").addCode(initBlock).build())
            .build().writeTo(codeGenerator, false)
    }
}

class NavProcessorProvider : SymbolProcessorProvider {
    override fun create(env: SymbolProcessorEnvironment) = NavSymbolProcessor(env.codeGenerator, env.logger)
}