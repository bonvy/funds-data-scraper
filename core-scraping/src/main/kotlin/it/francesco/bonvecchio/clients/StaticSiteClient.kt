package it.francesco.bonvecchio.clients

import it.francesco.bonvecchio.clients.models.ScrapeClientResult
import it.francesco.bonvecchio.scraper.models.ScrapeRequest
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service

/**
 * Fetches a static HTML page with Jsoup. On success the result carries the parsed
 * [org.jsoup.nodes.Document]; every failure (bad request, unsupported method, network error,
 * non-2xx status, non-HTML content, timeout) is returned as [ScrapeClientResult.Error], never thrown.
 */
@Service("StaticSiteClient")
internal class StaticSiteClient : BaseScrapeClient {

    override fun fetch(request: ScrapeRequest): ScrapeClientResult {
        val staticReq = request as? ScrapeRequest.StaticSiteRequest
            ?: return ScrapeClientResult.ScrapeStaticSiteClientResult.Error(IllegalArgumentException("Expected a StaticSiteRequest, got ${request::class.simpleName}"))

        val method = staticReq.method.toJsoupMethod()
            ?: return ScrapeClientResult.ScrapeStaticSiteClientResult.Error(IllegalArgumentException("Unsupported HTTP method ${staticReq.method}"))

        if (staticReq.timeout < 0) {
            return ScrapeClientResult.ScrapeStaticSiteClientResult.Error(IllegalArgumentException("Timeout must be >= 0 ms, got ${staticReq.timeout}"))
        }

        return try {
            val connection = Jsoup.connect(staticReq.url.toString())
                .method(method)
                .userAgent(staticReq.userAgent)
                // Milliseconds; 0 means no timeout.
                .timeout(staticReq.timeout)
                .cookies(staticReq.cookie.associate { it.name to it.value })

            // With GET/HEAD/DELETE Jsoup appends the data to the query string, otherwise it sends it as a form body.
            staticReq.data?.let { connection.data(it) }

            val response = connection.execute()
            ScrapeClientResult.ScrapeStaticSiteClientResult.Success(response.parse())
        } catch (e: Exception) {
            ScrapeClientResult.ScrapeStaticSiteClientResult.Error(e)
        }
    }

    private fun HttpMethod.toJsoupMethod(): Connection.Method? = when (this) {
        HttpMethod.GET -> Connection.Method.GET
        HttpMethod.POST -> Connection.Method.POST
        HttpMethod.PUT -> Connection.Method.PUT
        HttpMethod.PATCH -> Connection.Method.PATCH
        HttpMethod.DELETE -> Connection.Method.DELETE
        HttpMethod.HEAD -> Connection.Method.HEAD
        HttpMethod.OPTIONS -> Connection.Method.OPTIONS
        HttpMethod.TRACE -> Connection.Method.TRACE
        // HttpMethod is a class, not an enum: HttpMethod.valueOf("FOO") yields a custom method Jsoup cannot send.
        else -> null
    }
}
