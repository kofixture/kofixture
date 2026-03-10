package io.kofixture.kotest.arb

import io.kofixture.core.Generator
import io.kofixture.core.buildRegistry
import io.kofixture.core.register
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.arbitrary

private data class Project(val name: String, val description: String)

class OverrideScopeTest : FreeSpec({

    val projectRegistry =
        buildRegistry {
            register<String> { Generator { _ -> "default" } }
            register<Project> {
                Generator { r ->
                    Project(
                        sample(Project::name, r),
                        sample(Project::description, r),
                    )
                }
            }
        }

    "override<T> { Arb<T> }" - {

        "type-based Arb override replaces all occurrences of that type" {
            val result =
                projectRegistry.sample<Project> {
                    override<String> { arbitrary { "override" } }
                }
            result shouldBe Project("override", "override")
        }

        "type-based Arb override coexists with value override" {
            val resultValue =
                projectRegistry.sample<Project> {
                    override<String> { "value-override" }
                }
            val resultArb =
                projectRegistry.sample<Project> {
                    override<String> { arbitrary { "arb-override" } }
                }
            resultValue shouldBe Project("value-override", "value-override")
            resultArb shouldBe Project("arb-override", "arb-override")
        }
    }

    "override(property) { Arb<P> }" - {

        "property-based Arb override replaces only that property" {
            val result =
                projectRegistry.sample<Project> {
                    override(Project::name) { arbitrary { "name-only" } }
                }
            result.name shouldBe "name-only"
            result.description shouldBe "default"
        }

        "property-based Arb override coexists with value override" {
            val resultValue =
                projectRegistry.sample<Project> {
                    override(Project::name) { "value-name" }
                }
            val resultArb =
                projectRegistry.sample<Project> {
                    override(Project::name) { arbitrary { "arb-name" } }
                }
            resultValue.name shouldBe "value-name"
            resultArb.name shouldBe "arb-name"
        }
    }
})
