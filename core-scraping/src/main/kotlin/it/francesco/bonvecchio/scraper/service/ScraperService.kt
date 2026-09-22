package it.francesco.bonvecchio.scraper.service

import it.francesco.bonvecchio.scraper.models.ScrapeRequest

interface ScraperService {

    fun scrape(request: ScrapeRequest): Unit;

}