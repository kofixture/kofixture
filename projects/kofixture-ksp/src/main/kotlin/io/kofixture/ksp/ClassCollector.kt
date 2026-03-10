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

        packages.forEach { pkg ->
            resolver
                .getDeclarationsFromPackage(pkg)
                .filterIsInstance<KSClassDeclaration>()
                .forEach { add(it) }
        }

        explicitDecls.forEach { decl -> add(decl) }

        return result
    }

    private fun isProcessable(decl: KSClassDeclaration): Boolean {
        // Skip nested sealed subtypes (they are handled via DFS from their parent)
        if (isNestedSealedSubtype(decl)) return false

        return when (decl.classKind) {
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
                Modifier.ABSTRACT !in decl.modifiers
            }

            ClassKind.INTERFACE -> {
                Modifier.SEALED in decl.modifiers
            }

            else -> {
                false
            }
        }
    }

    private fun isNestedSealedSubtype(decl: KSClassDeclaration): Boolean {
        val parent = decl.parentDeclaration as? KSClassDeclaration ?: return false
        return Modifier.SEALED in parent.modifiers
    }
}
