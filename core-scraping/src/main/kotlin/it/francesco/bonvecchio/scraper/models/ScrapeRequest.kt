package it.francesco.bonvecchio.scraper.models

import org.springframework.http.HttpCookie
import org.springframework.http.HttpMethod
import java.net.URL

sealed interface ScrapeRequest{
    data class StaticSiteRequest(val siteId: SiteId, val url: URL, val method: HttpMethod, val userAgent: String = "Mozilla",val timeout: Int ,val data: Map<String,String>? = null, val cookie: List<HttpCookie>) : ScrapeRequest
}
