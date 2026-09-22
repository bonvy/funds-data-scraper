package it.francesco.bonvecchio.clients.models

import org.jsoup.nodes.Document

sealed interface ScrapeClientResult {

    sealed interface ScrapeStaticSiteClientResult : ScrapeClientResult {
        data class Success(val data : Document) : ScrapeClientResult
        data class Error(val exception: Exception) : ScrapeClientResult
    }

}