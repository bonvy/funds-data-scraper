package it.francesco.bonvecchio.scraper.models

import java.net.IDN
import java.net.URI
import java.net.URL

/**
 * Identifies a scraped site by its domain (e.g. `morningstar.it`).
 *
 * The value is normalized on construction — trimmed, lowercased, trailing dot and leading
 * `www.` removed, internationalized names converted to their ASCII (punycode) form — so two
 * ids for the same domain are always equal.
 */
@JvmInline
value class SiteId private constructor(val domain: String) {

    override fun toString(): String = domain

    companion object {
        private val LABEL = Regex("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$")

        fun of(domain: String): SiteId {
            val ascii = IDN.toASCII(domain.trim().removeSuffix("."), IDN.ALLOW_UNASSIGNED).lowercase()
            // "www.example.it" and "example.it" are the same site; "www.it" is left alone since "it" is not a domain.
            val normalized = ascii.removePrefix("www.").takeIf { it.contains('.') } ?: ascii
            require(normalized.length in 1..253) { "Invalid domain '$domain': length must be 1..253" }
            val labels = normalized.split('.')
            require(labels.size >= 2) { "Invalid domain '$domain': a top-level domain is required" }
            require(labels.all { LABEL.matches(it) }) { "Invalid domain '$domain'" }
            return SiteId(normalized)
        }

        fun of(url: URL): SiteId = of(requireNotNull(url.host?.takeIf { it.isNotEmpty() }) { "URL '$url' has no host" })

        fun of(uri: URI): SiteId = of(requireNotNull(uri.host) { "URI '$uri' has no host" })
    }
}
