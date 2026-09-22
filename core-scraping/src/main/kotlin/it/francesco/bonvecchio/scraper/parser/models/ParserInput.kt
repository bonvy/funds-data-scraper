package it.francesco.bonvecchio.scraper.parser.models

import org.jsoup.nodes.Document

sealed interface ParserInput {

    data class StaticSiteParserInput(val document: Document): ParserInput;
}