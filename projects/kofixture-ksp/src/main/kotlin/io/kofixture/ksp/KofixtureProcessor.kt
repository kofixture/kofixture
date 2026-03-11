package io.kofixture.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import io.kofixture.ksp.generator.FixtureModuleGenerator
import io.kofixture.ksp.generator.OverrideScopeGenerator

private const val ANNOTATION_FQN = "io.kofixture.core.Kofixture"

class KofixtureProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver
                .getSymbolsWithAnnotation(ANNOTATION_FQN)
                .filterIsInstance<KSClassDeclaration>()

        val (valid, deferred) = symbols.partition { it.validate() }

        valid.forEach { decl ->
            if (decl.classKind != ClassKind.OBJECT) {
                logger.error("@Kofixture must be applied to an object declaration", decl)
                return@forEach
            }
            processObject(decl, resolver)
        }

        return deferred
    }

    private fun processObject(
        objectDecl: KSClassDeclaration,
        resolver: Resolver,
    ) {
        val annotation =
            objectDecl.annotations
                .firstOrNull { it.shortName.asString() == "Kofixture" } ?: return

        val packages =
            (
                annotation.arguments
                    .firstOrNull { it.name?.asString() == "packages" }
                    ?.value as? List<*>
            )?.filterIsInstance<String>() ?: emptyList()

        val explicitClasses =
            (
                annotation.arguments
                    .firstOrNull { it.name?.asString() == "classes" }
                    ?.value as? List<*>
            )?.filterIsInstance<KSType>()
                ?.mapNotNull { it.declaration as? KSClassDeclaration } ?: emptyList()

        val moduleName =
            (
                annotation.arguments
                    .firstOrNull { it.name?.asString() == "moduleName" }
                    ?.value as? String
            )?.takeIf { it.isNotBlank() }
                ?: objectDecl.simpleName.asString().replaceFirstChar { it.lowercase() }

        val classes =
            ClassCollector(resolver)
                .collect(packages, explicitClasses)
                .filter { it.qualifiedName?.asString() != objectDecl.qualifiedName?.asString() }

        val hasArb =
            resolver.getClassDeclarationByName(
                resolver.getKSNameFromString("io.kotest.property.Arb"),
            ) != null

        val packageName = objectDecl.packageName.asString()
        val objectName = objectDecl.simpleName.asString()
        val originatingFiles = listOfNotNull(objectDecl.containingFile).toTypedArray()

        @Suppress("SpreadOperator")
        val file =
            codeGenerator.createNewFile(
                Dependencies(aggregating = true, *originatingFiles),
                packageName,
                "${objectName}Generated",
            )
        writeGeneratedFile(file, packageName, moduleName, classes, hasArb)
        logger.info("Generated ${objectName}Generated.kt")
    }

    private fun writeGeneratedFile(
        file: java.io.OutputStream,
        packageName: String,
        moduleName: String,
        classes: List<KSClassDeclaration>,
        hasArb: Boolean,
    ) {
        val hasSealed = classes.any { Modifier.SEALED in it.modifiers }
        val classesWithParams =
            classes.filter {
                it.classKind == ClassKind.CLASS &&
                    Modifier.SEALED !in it.modifiers &&
                    (it.primaryConstructor?.parameters?.isNotEmpty() == true)
            }
        val moduleGen = FixtureModuleGenerator(logger)
        file.bufferedWriter().use { writer ->
            writer.write("@file:Suppress(\"UNCHECKED_CAST\")\n\n")
            writer.write("package $packageName\n\n")
            writer.write("import io.kofixture.core.FixtureModule\n")
            writer.write("import io.kofixture.core.Generator\n")
            writer.write("import io.kofixture.core.fixtureModule\n")
            writer.write("import io.kofixture.core.register\n")
            if (hasSealed) {
                writer.write("import io.kofixture.core.generatorFor\n")
            }
            if (hasSealed || classesWithParams.isNotEmpty()) {
                writer.write("import kotlin.reflect.typeOf\n")
            }
            if (classesWithParams.isNotEmpty()) {
                writer.write("import io.kofixture.core.OverrideScope\n")
                writer.write("import io.kofixture.core.FixtureOverride\n")
                writer.write("import io.kofixture.core.NamedOverrideKey\n")
                writer.write("import kotlin.random.Random\n")
            }
            if (classesWithParams.isNotEmpty() && hasArb) {
                writer.write("import io.kotest.property.Arb\n")
                writer.write("import io.kofixture.kotest.arb.ArbGenerator\n")
            }
            writer.write("\n")
            moduleGen.writeModule(moduleName, classes, writer)
            OverrideScopeGenerator(moduleGen, hasArb).writeExtensions(classes, writer)
        }
    }
}
