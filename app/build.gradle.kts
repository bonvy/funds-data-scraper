plugins {
    id("buildlogic.spring-boot-application-conventions")
}

dependencies {
    implementation(project(":core-scraping"))

    // Versions come from the Spring Boot BOM applied in the common conventions.
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

application {
    // Define the main class for the application.
    mainClass = "it.francesco.bonvecchio.scraper.ScraperApplicationKt"
}
