package com.papererp.api.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Architectural Governance & Boundary Tests (ArchUnit)")
class ArchUnitTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.papererp");
    }

    @Test
    @DisplayName("Controllers must not depend directly on Repositories (Only via Domain Services)")
    void controllersShouldNotAccessRepositoriesDirectly() {
        noClasses().that().resideInAPackage("..api.controller..")
                .should().dependOnClassesThat().resideInAPackage("..core.repository..")
                .because("Controllers must stay thin and access persistence exclusively through module-owned services")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Domain entities must only reside in domain packages")
    void entitiesMustResideInDomainPackage() {
        classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage("..core.domain..")
                .because("JPA Entities must reside strictly within domain boundaries")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Common module must not depend on core or api modules")
    void commonShouldNotDependOnCoreOrApi() {
        noClasses().that().resideInAPackage("..common..")
                .should().dependOnClassesThat().resideInAnyPackage("..core..", "..api..")
                .because("Common module must be decoupled and shared across all modules")
                .check(importedClasses);
    }
}
