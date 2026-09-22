package it.francesco.bonvecchio.scraper.scheduler

import it.francesco.bonvecchio.scraper.service.ScraperService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScrapeScheduler(
    private val scraperService: ScraperService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${scraper.schedule.cron}")
    fun scheduledScrape() {
        log.info("Avvio scraping schedulato")
        // Delegates to scraperService once the site parsers are registered.
    }
}
