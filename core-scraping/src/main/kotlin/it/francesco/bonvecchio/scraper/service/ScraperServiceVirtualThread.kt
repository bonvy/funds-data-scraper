package it.francesco.bonvecchio.scraper.service

import it.francesco.bonvecchio.async.services.AsyncService
import it.francesco.bonvecchio.clients.BaseScrapeClient
import it.francesco.bonvecchio.clients.models.ScrapeClientResult
import it.francesco.bonvecchio.scraper.models.PostOperationResult
import it.francesco.bonvecchio.scraper.models.ScrapeRequest
import it.francesco.bonvecchio.scraper.models.ScrapeResult
import it.francesco.bonvecchio.scraper.parser.ParserStaticSiteDefinition
import it.francesco.bonvecchio.scraper.parser.models.ParserInput
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger


@Service
class ScraperServiceVirtualThread(
    private val asyncService: AsyncService,
    private val parserStaticSite: Map<String, ParserStaticSiteDefinition<Any>>,
    @Qualifier("StaticSiteClient") private val staticSiteClient: BaseScrapeClient,
    private val logger: Logger = LoggerFactory.getLogger(ScraperServiceVirtualThread::class.java)
) : ScraperService {
    companion object {
        private const val MAX_PARALLEL_EXECUTION = 200
    }

    private var currentScrapeProcess: AtomicInteger = AtomicInteger(0)
    private val scapeQueue: LinkedBlockingQueue<ScrapeRequest> = LinkedBlockingQueue()
    private val postScrapeOperationMap: MutableMap<ScrapeRequest, (Any) -> PostOperationResult> = mutableMapOf();

    override fun scrape(request: ScrapeRequest, postScrapeOperation: (Any) -> PostOperationResult): Unit {

        postScrapeOperationMap[request] = postScrapeOperation
        scapeQueue.add(request)
    }

    @PostConstruct
    private fun startWorker() {
        asyncService.run {
            while (!Thread.currentThread().isInterrupted) {

                if (currentScrapeProcess.get() >= MAX_PARALLEL_EXECUTION) {
                    Thread.sleep(1000)
                    continue;
                }

                val request = scapeQueue.take()
                currentScrapeProcess.incrementAndGet()
                startScraping(request).thenAccept { result ->
                    run {
                        if (result == null) {
                            return@run
                        }
                        when (result) {
                            is ScrapeResult.Success<*> -> {
                                val postScrapeOperation = postScrapeOperationMap.remove(request)
                                postScrapeOperation?.let {

                                    logger.info("Starting post operation for {}", request)

                                    try {
                                        when (it.invoke(result)) {
                                            is PostOperationResult.Success -> logger.info(
                                                "Post operation succeeded: {}",
                                                request
                                            )

                                            is PostOperationResult.Failure -> logger.error(
                                                "Post operation failed: {}",
                                                request
                                            )
                                        }
                                    } catch (e: Exception) {
                                        logger.error("Post operation failed with unhandled exception request: $result, exception: $e")
                                    } finally {
                                        currentScrapeProcess.decrementAndGet()
                                    }

                                    return@run
                                }

                                logger.warn(
                                    "It was not possible to find the right post operation function for {}",
                                    request
                                )
                            }

                            is ScrapeResult.Error -> {}
                            is ScrapeResult.ParserNotFound -> {}
                        }

                    }
                }.whenComplete { _: Void?, ex: Throwable? ->
                    if (ex != null) logger.error(
                        "Scraping error",
                        ex
                    ) else logger.info("Scraping complete")
                }

            }
        }
    }

    private fun startScraping(request: ScrapeRequest): CompletableFuture<ScrapeResult> {
        return this.asyncService.execute {
            when (request) {
                is ScrapeRequest.StaticSiteRequest -> {
                    scrapeStaticSite(request)
                }
            }
        }

    }

    private fun scrapeStaticSite(request: ScrapeRequest.StaticSiteRequest): ScrapeResult {
        val parser = parserStaticSite[request.siteId.domain]
            ?: return ScrapeResult.ParserNotFound("Error while parsing ${request.siteId.domain}, parser not found");
        val response = staticSiteClient.fetch(request)
        return when (response) {
            is ScrapeClientResult.ScrapeStaticSiteClientResult.Success -> parser.parse(
                ParserInput.StaticSiteParserInput(response.data)
            )

            is ScrapeClientResult.ScrapeStaticSiteClientResult.Error -> ScrapeResult.Error(
                response.exception.message ?: "Error while parsing ${request.siteId.domain}, parser not found"
            );
        } as ScrapeResult

    }

}
