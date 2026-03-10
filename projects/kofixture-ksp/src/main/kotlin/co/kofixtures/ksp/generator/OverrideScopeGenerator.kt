package co.kofixtures.ksp.generator

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import java.io.Writer

internal class OverrideScopeGenerator(
    private val moduleGen: FixtureModuleGenerator,
    private val hasArb: Boolean,
) {
    fun writeExtensions(
        classes: List<KSClassDeclaration>,
        writer: Writer,
    ) {
        val targets =
            classes.filter {
                it.classKind == ClassKind.CLASS &&
                    Modifier.SEALED !in it.modifiers &&
                    (it.primaryConstructor?.parameters?.isNotEmpty() == true)
            }
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
        params.forEach { param -> writeParamExtensions(fqn, param, writer) }
    }

    private fun writeParamExtensions(
        fqn: String,
        param: KSValueParameter,
        writer: Writer,
    ) {
        val name = param.name?.asString() ?: return
        val type = param.type.resolve()
        val typeName = moduleGen.renderKSType(type)
        writeValueSetter(fqn, name, typeName, writer)
        val typeFqn = complexTypeFqn(type)
        if (typeFqn != null) {
            writeNestedScopeOverload(fqn, name, typeFqn, writer)
        } else {
            writeGeneratorOverload(fqn, name, typeName, writer)
        }
        if (hasArb) {
            writeArbOverload(fqn, name, typeName, writer)
        }
    }

    private fun complexTypeFqn(type: KSType): String? {
        if (!isComplexType(type)) return null
        return (type.declaration as? KSClassDeclaration)?.qualifiedName?.asString()
    }

    private fun writeValueSetter(
        fqn: String,
        name: String,
        typeName: String,
        writer: Writer,
    ) {
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
        typeName: String,
        writer: Writer,
    ) {
        writer.write("@JvmName(\"${fqn.replace('.', '_')}__${name}__gen\")\n")
        writer.write("fun OverrideScope<$fqn>.$name(block: (Random) -> $typeName) {\n")
        writer.write("    addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("            Generator(block),\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun writeNestedScopeOverload(
        fqn: String,
        name: String,
        typeFqn: String,
        writer: Writer,
    ) {
        writer.write("@JvmName(\"${fqn.replace('.', '_')}__${name}__scope\")\n")
        writer.write("fun OverrideScope<$fqn>.$name(block: OverrideScope<$typeFqn>.() -> Unit) {\n")
        writer.write("    addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            key = NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("            gen = registry.generator<$typeFqn>(block = block),\n")
        writer.write("        ),\n")
        writer.write("    )\n")
        writer.write("}\n")
        writer.write("\n")
    }

    private fun writeArbOverload(
        fqn: String,
        name: String,
        typeName: String,
        writer: Writer,
    ) {
        writer.write("@JvmName(\"${fqn.replace('.', '_')}__${name}__arb\")\n")
        writer.write("fun OverrideScope<$fqn>.$name(arb: Arb<$typeName>) {\n")
        writer.write("    addOverride(\n")
        writer.write("        FixtureOverride.Named(\n")
        writer.write("            NamedOverrideKey(typeOf<$fqn>(), \"$name\"),\n")
        writer.write("            ArbGenerator(arb),\n")
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
