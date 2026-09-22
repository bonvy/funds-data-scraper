package it.francesco.bonvecchio.scraper.parser

import it.francesco.bonvecchio.scraper.parser.models.ParserInput

interface ParserStaticSiteDefinition<R>: ParserSiteDefinition<ParserInput.StaticSiteParserInput, R> {

    override fun parse(document: ParserInput.StaticSiteParserInput): R;
}