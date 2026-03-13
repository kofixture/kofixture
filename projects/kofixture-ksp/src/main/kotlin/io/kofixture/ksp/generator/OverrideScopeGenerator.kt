@file:Suppress("TooManyFunctions")

package io.kofixture.ksp.generator

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import java.io.Writer

internal class OverrideScopeGenerator(
    private val moduleGen: FixtureModuleGenerator,
    private val hasArb: Boolean,
) {
    private lateinit var classByFqn: Map<String, KSClassDeclaration>
    private val generatedScopeClasses = mutableSetOf<String>()

    fun writeExtensions(
        classes: List<KSClassDeclaration>,
        writer: Writer,
    ) {
        classByFqn = classes.mapNotNull { decl -> decl.qualifiedName?.asString()?.let { it to decl } }.toMap()
        val targets =
            classes.filter {
                it.classKind == ClassKind.CLASS &&
                    it.typeParameters.isEmpty() &&
                    Modifier.SEALED !in it.modifiers &&
                    (it.primaryConstructor?.parameters?.isNotEmpty() == true)
            }
        val hasComplexParams =
            targets.any { klass ->
                klass.primaryConstructor?.parameters?.any { isComplexType(it.type.resolve()) } == true
            }
        if (hasComplexParams) {
            writeFixtureDsl(writer)
        }
        generatedScopeClasses.clear()
        for (klass in targets) {
            writeClassExtensions(klass, writer)
        }
    }

    private fun writeClassExtensions(
        klass: KSClassDeclaration,
        writer: Writer,
    ) {
        val fqn = klass.qualifiedName?.asString() ?: return
        val params = klass.primaryConstructor?.parameters ?: return
        writer.write("\n")
        params.forEach { param -> writeParamExtensions(klass, fqn, param, writer) }
    }

    private fun writeParamExtensions(
        owner: KSClassDeclaration,
        fqn: String,
        param: KSValueParameter,
        writer: Writer,
    ) {
        val name = param.name?.asString() ?: return
        if (shouldSkipParam(owner, param, name)) return
        val type = param.type.resolve()
        val typeName = moduleGen.renderKSType(type)
        writeValueSetter(fqn, name, typeName, writer)
        val typeFqn = complexTypeFqn(type)
        if (typeFqn != null) {
            val scopeName = scopeClassName(fqn, name)
            writeFieldScopeClass(scopeName, fqn, typeFqn, writer, generatedScopeClasses)
            writeComplexGeneratorOverload(fqn, name, typeFqn, scopeName, writer)
            if (hasArb) {
                writeComplexArbOverload(fqn, name, typeFqn, scopeName, writer)
            }
            writeComplexUnitOverload(fqn, name, typeFqn, scopeName, writer)
            val childDecl = classByFqn[typeFqn]
            if (childDecl != null) {
                writeNestedChildProperties(scopeName, childDecl, writer)
            }
        } else {
            writeGeneratorOverload(fqn, name, fqn, typeName, writer)
            if (hasArb) {
                writeArbLambdaOverload(fqn, name, fqn, typeName, writer)
            }
        }
    }

    private fun shouldSkipParam(
        owner: KSClassDeclaration,
        param: KSValueParameter,
        name: String,
    ): Boolean {
        val property =
            owner.declarations
                .filterIsInstance<KSPropertyDeclaration>()
                .firstOrNull { it.simpleName.asString() == name }
        val isPrivateProperty = property != null && Modifier.PRIVATE in property.modifiers
        val parentProperty = param.parent as? KSPropertyDeclaration
        val isPrivateParent = parentProperty != null && Modifier.PRIVATE in parentProperty.modifiers
        val isPrivateCtorParam = isPrivateConstructorParam(owner, name)
        return isPrivateProperty || isPrivateParent || isPrivateCtorParam
    }

    private fun complexTypeFqn(type: KSType): String? {
        if (!isComplexType(type)) return null
        return (type.declaration as? KSClassDeclaration)?.qualifiedName?.asString()
    }

    private fun writeFixtureDsl(writer: Writer) {
        writer.write("@DslMarker\n")
        writer.write("annotation class FixtureDsl\n")
        writer.write("\n")
    }

    private fun writeComplexGeneratorOverload(
        fqn: String,
        name: String,
        typeFqn: String,
        scopeName: String,
        writer: Writer,
    ) {
        writer.write("@OptIn(ExperimentalTypeInference::class)\n")
        writer.write("@OverloadResolutionByLambdaReturnType\n")
        writer.write("@JvmName(\"${fqn.replace('.', '_')}__${name}__gen\")\n")
        writer.write("fun OverrideScope<$fqn>.$name(block: $scopeName.() -> Generator<$typeFqn>) {\n")
        writer.write("    val scope = $scopeName(this, OverrideScope<$typeFqn>(registry), typeOf<$typeFqn>())\n")
        writer.write("    addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            key = NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("            gen = scope.block(),\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun writeComplexArbOverload(
        fqn: String,
        name: String,
        typeFqn: String,
        scopeName: String,
        writer: Writer,
    ) {
        writer.write("@OptIn(ExperimentalTypeInference::class)\n")
        writer.write("@OverloadResolutionByLambdaReturnType\n")
        writer.write("@JvmName(\"${fqn.replace('.', '_')}__${name}__arb\")\n")
        writer.write("fun OverrideScope<$fqn>.$name(block: $scopeName.() -> Arb<$typeFqn>) {\n")
        writer.write("    val scope = $scopeName(this, OverrideScope<$typeFqn>(registry), typeOf<$typeFqn>())\n")
        writer.write("    addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            key = NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("            gen = ArbGenerator(scope.block()),\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun writeComplexUnitOverload(
        fqn: String,
        name: String,
        typeFqn: String,
        scopeName: String,
        writer: Writer,
    ) {
        writer.write("@OptIn(ExperimentalTypeInference::class)\n")
        writer.write("@OverloadResolutionByLambdaReturnType\n")
        writer.write("@JvmName(\"${fqn.replace('.', '_')}__${name}__unit\")\n")
        writer.write("fun OverrideScope<$fqn>.$name(block: $scopeName.() -> Unit) {\n")
        writer.write("    val scope = $scopeName(this, OverrideScope<$typeFqn>(registry), typeOf<$typeFqn>())\n")
        writer.write("    scope.block()\n")
        writer.write("    val gen = registry.generatorFor<$typeFqn>(typeOf<$typeFqn>(), null, ActiveOverrides.from(scope.child))\n")
        writer.write("    addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            key = NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("            gen = gen,\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun writeNestedChildProperties(
        scopeName: String,
        childDecl: KSClassDeclaration,
        writer: Writer,
    ) {
        val params = childDecl.primaryConstructor?.parameters ?: return
        if (params.isEmpty()) return
        writer.write("\n")
        params.forEach { param ->
            val name = param.name?.asString() ?: return@forEach
            val typeName = moduleGen.renderKSType(param.type.resolve())
            writeFieldScopeValueSetter(scopeName, name, typeName, writer)
            writeFieldScopeValueOverload(scopeName, name, typeName, writer)
            writeFieldScopeGeneratorOverload(scopeName, name, typeName, writer)
            if (hasArb) {
                writeFieldScopeArbOverload(scopeName, name, typeName, writer)
            }
        }
    }

    private fun writeFieldScopeValueSetter(
        scopeName: String,
        name: String,
        typeName: String,
        writer: Writer,
    ) {
        writer.write("var $scopeName.$name: $typeName?\n")
        writer.write("    get() = null\n")
        writer.write("    set(value) {\n")
        writer.write("        val v = value ?: return\n")
        writer.write("        setChildConst(\"$name\", v)\n")
        writer.write("    }\n")
        writer.write("\n")
    }

    private fun writeFieldScopeValueOverload(
        scopeName: String,
        name: String,
        typeName: String,
        writer: Writer,
    ) {
        writer.write("@OptIn(ExperimentalTypeInference::class)\n")
        writer.write("@OverloadResolutionByLambdaReturnType\n")
        writer.write("@JvmName(\"${scopeName}__${name}__value\")\n")
        writer.write("fun $scopeName.$name(block: PropOverrideScope<$typeName>.() -> $typeName) {\n")
        writer.write("    child.addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            NamedOverrideKey(childType, \"$name\"),\n")
        writer.write("            Generator { _ -> PropOverrideScope<$typeName>(child.registry).block() },\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun writeFieldScopeGeneratorOverload(
        scopeName: String,
        name: String,
        typeName: String,
        writer: Writer,
    ) {
        writer.write("@OptIn(ExperimentalTypeInference::class)\n")
        writer.write("@OverloadResolutionByLambdaReturnType\n")
        writer.write("@JvmName(\"${scopeName}__${name}__gen\")\n")
        writer.write("fun $scopeName.$name(block: PropOverrideScope<$typeName>.() -> Generator<$typeName>) {\n")
        writer.write("    child.addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            NamedOverrideKey(childType, \"$name\"),\n")
        writer.write("            PropOverrideScope<$typeName>(child.registry).block(),\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun writeFieldScopeArbOverload(
        scopeName: String,
        name: String,
        typeName: String,
        writer: Writer,
    ) {
        writer.write("@OptIn(ExperimentalTypeInference::class)\n")
        writer.write("@OverloadResolutionByLambdaReturnType\n")
        writer.write("@JvmName(\"${scopeName}__${name}__arb\")\n")
        writer.write("fun $scopeName.$name(block: PropOverrideScope<$typeName>.() -> Arb<$typeName>) {\n")
        writer.write("    child.addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            NamedOverrideKey(childType, \"$name\"),\n")
        writer.write("            ArbGenerator(PropOverrideScope<$typeName>(child.registry).block()),\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun scopeClassName(
        parentFqn: String,
        fieldName: String,
    ): String {
        val safeField = fieldName.replace(Regex("[^A-Za-z0-9_]"), "_")
        return "${parentFqn.replace('.', '_')}__${safeField}__FieldScope"
    }

    private fun writeFieldScopeClass(
        scopeName: String,
        parentFqn: String,
        childFqn: String,
        writer: Writer,
        generatedScopeClasses: MutableSet<String>,
    ) {
        if (!generatedScopeClasses.add(scopeName)) return
        writer.write("@FixtureDsl\n")
        writer.write("class $scopeName(\n")
        writer.write("    internal val parent: OverrideScope<$parentFqn>,\n")
        writer.write("    internal val child: OverrideScope<$childFqn>,\n")
        writer.write("    internal val childType: KType,\n")
        writer.write(")\n")
        writer.write("\n")
        writer.write("internal fun <V> $scopeName.setChildConst(\n")
        writer.write("    name: String,\n")
        writer.write("    value: V,\n")
        writer.write(") {\n")
        writer.write("    child.addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            NamedOverrideKey(childType, name),\n")
        writer.write("            Generator { _ -> value },\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun isPrivateConstructorParam(
        owner: KSClassDeclaration,
        name: String,
    ): Boolean {
        val filePath = owner.containingFile?.filePath
        val file = filePath?.let { java.io.File(it) }
        val text = if (file?.exists() == true) file.readText() else null
        val pattern = Regex("""\bprivate\s+(val|var)\s+$name\b""")
        val paramsBlock =
            if (text != null && pattern.containsMatchIn(text)) {
                extractConstructorParamsBlock(text, owner.simpleName.asString())
            } else {
                null
            }
        return paramsBlock?.let { pattern.containsMatchIn(it) } ?: false
    }

    private fun extractConstructorParamsBlock(
        text: String,
        className: String,
    ): String? {
        val classMatch = Regex("""\bclass\s+$className\b""").find(text)
        val startIdx = classMatch?.let { text.indexOf('(', it.range.last) } ?: -1
        var depth = 0
        var endIdx = -1
        if (startIdx >= 0) {
            endIdx = findMatchingParen(text, startIdx)
        }
        return if (startIdx == -1 || endIdx == -1) null else text.substring(startIdx + 1, endIdx)
    }

    private fun findMatchingParen(
        text: String,
        startIdx: Int,
    ): Int {
        var depth = 0
        for (i in startIdx until text.length) {
            when (text[i]) {
                '(' -> {
                    depth += 1
                }

                ')' -> {
                    depth -= 1
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun writeValueSetter(
        fqn: String,
        name: String,
        typeName: String,
        writer: Writer,
    ) {
        val jvmBase = "${fqn.replace('.', '_')}__$name"
        writer.write("@get:JvmName(\"${jvmBase}__get\")\n")
        writer.write("@set:JvmName(\"${jvmBase}__set\")\n")
        writer.write("var OverrideScope<$fqn>.$name: $typeName?\n")
        writer.write("    get() = null\n")
        writer.write("    set(value) {\n")
        writer.write("        val v = value ?: return\n")
        writer.write("        addOverride(\n")
        writer.write("            FixtureOverride.Named(\n")
        writer.write("                NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("                Generator { _ -> v },\n")
        writer.write("            ),\n")
        writer.write("        )\n")
        writer.write("    }\n")
        writer.write("\n")
    }

    private fun writeGeneratorOverload(
        fqn: String,
        name: String,
        blockReceiverFqn: String,
        typeName: String,
        writer: Writer,
    ) {
        writer.write("@OptIn(ExperimentalTypeInference::class)\n")
        writer.write("@OverloadResolutionByLambdaReturnType\n")
        writer.write("@JvmName(\"${fqn.replace('.', '_')}__${name}__gen\")\n")
        writer.write("fun OverrideScope<$fqn>.$name(block: OverrideScope<$blockReceiverFqn>.() -> Generator<$typeName>) {\n")
        writer.write("    val scope = OverrideScope<$blockReceiverFqn>(registry)\n")
        writer.write("    addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("            scope.block(),\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun writeArbLambdaOverload(
        fqn: String,
        name: String,
        blockReceiverFqn: String,
        typeName: String,
        writer: Writer,
    ) {
        writer.write("@OptIn(ExperimentalTypeInference::class)\n")
        writer.write("@OverloadResolutionByLambdaReturnType\n")
        writer.write("@JvmName(\"${fqn.replace('.', '_')}__${name}__arb\")\n")
        writer.write("fun OverrideScope<$fqn>.$name(block: OverrideScope<$blockReceiverFqn>.() -> Arb<$typeName>) {\n")
        writer.write("    val scope = OverrideScope<$blockReceiverFqn>(registry)\n")
        writer.write("    addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("            ArbGenerator(scope.block()),\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun isComplexType(type: KSType): Boolean {
        val decl = type.declaration as? KSClassDeclaration ?: return false
        val isClassOrSealedInterface =
            decl.classKind == ClassKind.CLASS ||
                (decl.classKind == ClassKind.INTERFACE && Modifier.SEALED in decl.modifiers)
        val pkg = decl.packageName.asString()
        return isClassOrSealedInterface && !pkg.startsWith("kotlin") && !pkg.startsWith("java")
    }
}
