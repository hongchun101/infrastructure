package com.github.infrastructure.app

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(
    packages = ["com.github.infrastructure.app"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class ArchitectureTest {
    @ArchTest
    val controllers_should_reside_in_controller_packages: ArchRule = classes()
        .that().haveSimpleNameEndingWith("Controller")
        .should().resideInAPackage("..controller")
        .because("controller classes must live in a '.controller' subpackage")

    @ArchTest
    val services_should_reside_in_service_packages: ArchRule = classes()
        .that().haveSimpleNameEndingWith("Service")
        .and().doNotHaveFullyQualifiedName(
            "com.github.infrastructure.app.audit.event.AlertEventBridge",
        )
        .should().resideInAPackage("..service")
        .because("service classes must live in a '.service' subpackage")

    @ArchTest
    val repositories_should_reside_in_repository_packages: ArchRule = classes()
        .that().haveSimpleNameEndingWith("Repository")
        .and().doNotHaveSimpleName("PackageMarker")
        .should().resideInAPackage("..repository")
        .because("repository classes must live in a '.repository' subpackage")

    @ArchTest
    val controllers_should_not_depend_on_repositories: ArchRule = noClasses()
        .that().resideInAPackage("..controller")
        .should().dependOnClassesThat().resideInAPackage("..repository")
        .because("controllers must go through services, never call repositories directly")

    @ArchTest
    val controllers_should_not_depend_on_entity_data_classes_or_interfaces: ArchRule = classes()
        .that().resideInAPackage("..controller")
        .should(notHaveEntityTypeInMethodSignature())
        .because("controllers expose DTOs in their public signatures; enums like AnnouncementStatus or Severity are fine, entity interfaces and data classes are not")

    private fun notHaveEntityTypeInMethodSignature(): ArchCondition<JavaClass> =
        object : ArchCondition<JavaClass>(
            "not declare a method whose parameter or return type is a non-enum ..entity class",
        ) {
            override fun check(item: JavaClass, events: ConditionEvents) {
                val offenders = item.methods
                    .flatMap { method ->
                        buildList {
                            addAll(method.rawParameterTypes)
                            add(method.rawReturnType)
                        }
                    }
                    .filter { it.packageName.contains(".entity") }
                    .filter { !it.isAssignableTo(Enum::class.java) }
                offenders.distinctBy { it.name }.forEach { entityType ->
                    events.add(
                        SimpleConditionEvent.violated(
                            item,
                            "${item.name} exposes ${entityType.name} in a public signature (entity types must not leak to controllers; expose a DTO instead)",
                        ),
                    )
                }
            }

            private val JavaClass.isEnum: Boolean
                get() = modifiers.contains(JavaModifier.ENUM)
        }
}
