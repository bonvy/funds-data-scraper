package it.francesco.bonvecchio.scraper.parser

interface ParserSiteDefinition <I, R> {

    fun parse(document: I): R
}