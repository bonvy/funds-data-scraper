package it.francesco.bonvecchio.async.services

import java.util.concurrent.CompletableFuture

interface AsyncService {

    fun <T> execute(action: () -> T ): CompletableFuture<T>;
}