package it.francesco.bonvecchio.scraper.models

sealed interface ScrapeResult {

    data class ParserNotFound(val errorMessage: String): ScrapeResult
    data class Error(val errorMessage: String): ScrapeResult
    data class Success<R>(val data: R): ScrapeResult

}