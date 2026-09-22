package it.francesco.bonvecchio.scraper.service

import it.francesco.bonvecchio.async.services.AsyncService
import it.francesco.bonvecchio.clients.BaseScrapeClient
import it.francesco.bonvecchio.clients.models.ScrapeClientResult
import it.francesco.bonvecchio.scraper.models.ScrapeRequest
import it.francesco.bonvecchio.scraper.models.ScrapeResult
import it.francesco.bonvecchio.scraper.parser.ParserStaticSiteDefinition
import it.francesco.bonvecchio.scraper.parser.models.ParserInput
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ScraperServiceVirtualThread(
    private val asyncService: AsyncService,
    private val parserStaticStie: Map<String, ParserStaticSiteDefinition<Any>>,
    @Qualifier("StaticSiteClient") private val staticSiteClient: BaseScrapeClient
) : ScraperService {

    private val scapeQueue: MutableSet<ScrapeRequest> = mutableSetOf();

    override fun scrape(request: ScrapeRequest): Unit {

        when (request) {
            is ScrapeRequest.StaticSiteRequest -> {}
        }

    }

    private fun <R> scrapeStaticSite(request: ScrapeRequest.StaticSiteRequest) : ScrapeResult {
        val parser = parserStaticStie[request.siteId.domain] ?: return ScrapeResult.ParserNotFound("Error while parsing ${request.siteId.domain}, parser not found");
        val response = staticSiteClient.fetch(request)
        return when (response){
            is ScrapeClientResult.ScrapeStaticSiteClientResult.Success -> parser.parse(
                ParserInput.StaticSiteParserInput(response.data)
            );
            is ScrapeClientResult.ScrapeStaticSiteClientResult.Error -> ScrapeResult.Error(response.exception.message ?: "Error while parsing ${request.siteId.domain}, parser not found");
        } as ScrapeResult

    }

}
