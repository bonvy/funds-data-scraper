/*
 * Conventions for a runnable Spring Boot application module.
 *
 * The Spring Boot plugin does not apply dependency management on its own; managed versions come
 * from the BOM platform wired in `buildlogic.kotlin-common-conventions`. This plugin only adds the
 * bootJar / bootRun tasks on top of the plain application conventions.
 */

plugins {
    id("buildlogic.kotlin-application-conventions")
    id("org.springframework.boot")
}
