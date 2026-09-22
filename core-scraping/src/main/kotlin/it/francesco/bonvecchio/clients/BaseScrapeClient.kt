package it.francesco.bonvecchio.clients

import it.francesco.bonvecchio.clients.models.ScrapeClientResult
import it.francesco.bonvecchio.scraper.models.ScrapeRequest

interface BaseScrapeClient {

    fun fetch(request: ScrapeRequest) : ScrapeClientResult

}