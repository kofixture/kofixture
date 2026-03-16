package io.kofixture.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal class ClassCollector(private val resolver: Resolver) {
    @OptIn(KspExperimental::class)
    fun collect(
        packages: List<String>,
        explicitDecls: List<KSClassDeclaration>,
    ): List<KSClassDeclaration> {
        val seen = LinkedHashSet<String>()
        val result = mutableListOf<KSClassDeclaration>()

        fun add(decl: KSClassDeclaration) {
            val fqn = decl.qualifiedName?.asString() ?: return
            if (seen.add(fqn) && isProcessable(decl)) result.add(decl)
        }

        fun addWithNested(decl: KSClassDeclaration) {
            fun walk(node: KSClassDeclaration) {
                add(node)
                node.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .forEach { walk(it) }
            }
            walk(decl)
        }

        packages.forEach { pkg ->
            resolver
                .getDeclarationsFromPackage(pkg)
                .filterIsInstance<KSClassDeclaration>()
                .forEach { addWithNested(it) }
        }

        explicitDecls.forEach { decl -> addWithNested(decl) }

        return result
    }

    private fun isProcessable(decl: KSClassDeclaration): Boolean {
        val isExcluded = isNestedSealedSubtype(decl) || isKotlinxSerializerClass(decl)
        return !isExcluded &&
            when (decl.classKind) {
                ClassKind.OBJECT -> {
                    true
                }

                ClassKind.ENUM_CLASS -> {
                    true
                }

                ClassKind.CLASS -> {
                    // sealed class: ClassKind.CLASS + Modifier.SEALED
                    // abstract class: ClassKind.CLASS + Modifier.ABSTRACT
                    // data class / regular class: ClassKind.CLASS, neither sealed nor abstract (sealed is still allowed here)
                    Modifier.ABSTRACT !in decl.modifiers &&
                        decl.primaryConstructor?.modifiers?.contains(Modifier.PRIVATE) != true
                }

                ClassKind.INTERFACE -> {
                    Modifier.SEALED in decl.modifiers
                }

                else -> {
                    false
                }
            }
    }

    private fun isKotlinxSerializerClass(decl: KSClassDeclaration): Boolean {
        val simple = decl.simpleName.asString()
        val hasSerializerSuffix = simple.endsWith("\$serializer")
        val fqn = decl.qualifiedName?.asString()
        val hasSerializerInFqn = fqn?.contains(".\$serializer") == true
        val hasAnnotation = hasSerializerAnnotation(decl)
        val isSerializerType = implementsKSerializer(decl)
        return hasSerializerSuffix || hasSerializerInFqn || hasAnnotation || isSerializerType
    }

    private fun hasSerializerAnnotation(decl: KSClassDeclaration): Boolean = decl.annotations.any { annotation ->
        val annFqn =
            annotation.annotationType
                .resolve()
                .declaration.qualifiedName
                ?.asString()
        annFqn == "kotlinx.serialization.Serializer"
    }

    private fun implementsKSerializer(decl: KSClassDeclaration): Boolean {
        val targets =
            setOf(
                "kotlinx.serialization.KSerializer",
                "kotlinx.serialization.internal.GeneratedSerializer",
            )
        val visited = mutableSetOf<String>()

        fun walk(node: KSClassDeclaration): Boolean {
            val nodeFqn = node.qualifiedName?.asString()
            val shouldWalk = nodeFqn != null && visited.add(nodeFqn)
            val matchesTarget =
                if (shouldWalk) {
                    node.superTypes.any { superTypeRef ->
                        val superDecl = superTypeRef.resolve().declaration as? KSClassDeclaration
                        val superFqn = superDecl?.qualifiedName?.asString()
                        superFqn in targets || (superDecl != null && walk(superDecl))
                    }
                } else {
                    false
                }
            return matchesTarget
        }
        return walk(decl)
    }

    private fun isNestedSealedSubtype(decl: KSClassDeclaration): Boolean {
        val parent = decl.parentDeclaration as? KSClassDeclaration ?: return false
        return Modifier.SEALED in parent.modifiers
    }
}
