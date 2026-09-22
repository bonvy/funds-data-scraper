package it.francesco.bonvecchio.scraper.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI

class SiteIdTest {

    @Test
    fun `normalizes the domain`() {
        assertEquals("morningstar.it", SiteId.of("  MorningStar.IT. ").domain)
    }

    @Test
    fun `same domain gives equal ids`() {
        assertEquals(SiteId.of("morningstar.it"), SiteId.of(URI("https://morningstar.it/fondi?id=1").toURL()))
    }

    @Test
    fun `www prefix is ignored`() {
        assertEquals(SiteId.of("morningstar.it"), SiteId.of("WWW.morningstar.it"))
        assertEquals(SiteId.of("morningstar.it"), SiteId.of(URI("https://www.morningstar.it/fondi").toURL()))
        assertEquals("www.it", SiteId.of("www.it").domain)
    }

    @Test
    fun `converts internationalized domains to punycode`() {
        assertEquals("xn--mnchen-3ya.de", SiteId.of("münchen.de").domain)
    }

    @Test
    fun `rejects invalid domains`() {
        listOf("", "localhost", "-bad.it", "bad_.it", "a..it", "https://morningstar.it").forEach {
            assertThrows<IllegalArgumentException>(it) { SiteId.of(it) }
        }
    }
}
