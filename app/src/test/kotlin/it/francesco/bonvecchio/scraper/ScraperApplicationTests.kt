package it.francesco.bonvecchio.scraper

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ScraperApplicationTests {

    @Test
    fun contextLoads() {
        // Fails if any bean cannot be wired: missing component scan, a final class
        // Spring cannot proxy, an unresolvable @Value placeholder, ...
    }
}
