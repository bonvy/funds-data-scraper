package it.francesco.bonvecchio.scraper.models

sealed interface PostOperationResult {
    class Success : PostOperationResult
    class Failure : PostOperationResult
}