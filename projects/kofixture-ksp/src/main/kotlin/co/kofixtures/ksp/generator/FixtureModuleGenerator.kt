package co.kofixtures.ksp.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance
import java.io.Writer

internal class FixtureModuleGenerator(private val logger: KSPLogger) {
    fun writeModule(
        valName: String,
        classes: List<KSClassDeclaration>,
        writer: Writer,
    ) {
        val ordered = orderClasses(classes)
        writer.write("val $valName: FixtureModule = fixtureModule {\n")
        for (klass in ordered) {
            writeClassEntry(klass, writer)
        }
        writer.write("}\n")
    }

    private fun orderClasses(classes: List<KSClassDeclaration>): List<KSClassDeclaration> {
        val result = mutableListOf<KSClassDeclaration>()
        val processed = mutableSetOf<String>()
        classes.forEach { klass -> addOrdered(klass, result, processed) }
        return result
    }

    private fun addOrdered(
        klass: KSClassDeclaration,
        result: MutableList<KSClassDeclaration>,
        processed: MutableSet<String>,
    ) {
        val qn = klass.qualifiedName?.asString() ?: return
        if (qn in processed) return
        if (Modifier.SEALED in klass.modifiers) {
            collectSealedDfs(klass, result, processed)
        } else {
            result.add(klass)
            processed.add(qn)
        }
    }

    private fun collectSealedDfs(
        klass: KSClassDeclaration,
        result: MutableList<KSClassDeclaration>,
        processed: MutableSet<String>,
    ) {
        for (subtype in klass.getSealedSubclasses()) {
            collectSealedDfs(subtype, result, processed)
        }
        val qn = klass.qualifiedName?.asString() ?: return
        if (qn !in processed && Modifier.ABSTRACT !in klass.modifiers) {
            result.add(klass)
            processed.add(qn)
        }
    }

    private fun writeClassEntry(
        klass: KSClassDeclaration,
        writer: Writer,
    ) {
        when {
            klass.classKind == ClassKind.OBJECT -> writeObject(klass, writer)
            klass.classKind == ClassKind.ENUM_CLASS -> writeEnum(klass, writer)
            Modifier.SEALED in klass.modifiers -> writeSealed(klass, writer)
            else -> writeDataOrClass(klass, writer)
        }
    }

    private fun writeObject(
        klass: KSClassDeclaration,
        writer: Writer,
    ) {
        val fqn = klass.qualifiedName?.asString() ?: return
        writer.write("    register<$fqn> { Generator { _ -> $fqn } }\n")
    }

    private fun writeEnum(
        klass: KSClassDeclaration,
        writer: Writer,
    ) {
        val fqn = klass.qualifiedName?.asString() ?: return
        writer.write(
            "    register<$fqn> { Generator { random -> $fqn.entries[random.nextInt($fqn.entries.size)] } }\n",
        )
    }

    private fun writeSealed(
        klass: KSClassDeclaration,
        writer: Writer,
    ) {
        val fqn = klass.qualifiedName?.asString() ?: return
        val subtypes = klass.getSealedSubclasses().toList()
        if (subtypes.isEmpty()) {
            logger.warn("Sealed class $fqn has no subtypes — skipping register")
            return
        }
        writer.write("    register<$fqn> {\n")
        writer.write("        val generators = listOf<Generator<$fqn>>(\n")
        for (sub in subtypes) {
            val subFqn = sub.qualifiedName?.asString() ?: continue
            writer.write("            registry.generatorFor(typeOf<$subFqn>(), null, activeOverrides),\n")
        }
        writer.write("        )\n")
        writer.write("        Generator { random -> generators[random.nextInt(generators.size)].next(random) }\n")
        writer.write("    }\n")
    }

    private fun writeDataOrClass(
        klass: KSClassDeclaration,
        writer: Writer,
    ) {
        val fqn = klass.qualifiedName?.asString() ?: return
        val params = klass.primaryConstructor?.parameters ?: emptyList()
        if (params.isEmpty()) {
            writer.write("    register<$fqn> { Generator { _ -> $fqn() } }\n")
            return
        }
        writer.write("    register<$fqn> {\n")
        writer.write("        Generator { random ->\n")
        writer.write("            $fqn(\n")
        for (param in params) {
            val name = param.name?.asString() ?: continue
            writer.write("                $name = sample($fqn::$name, random),\n")
        }
        writer.write("            )\n")
        writer.write("        }\n")
        writer.write("    }\n")
    }

    internal fun renderKSType(type: KSType): String {
        val decl = type.declaration
        val fqn = decl.qualifiedName?.asString() ?: return decl.simpleName.asString()
        val pkg = fqn.substringBeforeLast('.', missingDelimiterValue = "")
        val name = if (pkg.startsWith("kotlin") || pkg.startsWith("java")) decl.simpleName.asString() else fqn
        val nullable = if (type.isMarkedNullable) "?" else ""
        val args = type.arguments
        return if (args.isEmpty()) {
            "$name$nullable"
        } else {
            "$name<${args.joinToString(", ") { arg ->
                when (arg.variance) {
                    Variance.STAR -> "*"
                    else -> arg.type?.resolve()?.let { renderKSType(it) } ?: "*"
                }
            }}>$nullable"
        }
    }
}
