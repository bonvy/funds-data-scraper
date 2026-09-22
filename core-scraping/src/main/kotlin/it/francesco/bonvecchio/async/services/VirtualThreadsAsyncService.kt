package it.francesco.bonvecchio.async.services

import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class VirtualThreadsAsyncService : AsyncService {

    private val executor = Executors.newVirtualThreadPerTaskExecutor();

    override fun <T> execute(action: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(action, executor);
    }
}
