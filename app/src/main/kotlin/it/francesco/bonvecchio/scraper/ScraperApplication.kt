package it.francesco.bonvecchio.scraper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

// Scanning starts at the shared root package so the beans living in :core-scraping
// (it.francesco.bonvecchio.scraper.service, it.francesco.bonvecchio.async.services) are picked up.
@SpringBootApplication(scanBasePackages = ["it.francesco.bonvecchio"])
@EnableScheduling
class ScraperApplication

fun main(args: Array<String>) {
    runApplication<ScraperApplication>(*args)
}
