package juro.client

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.util.concurrent.atomic.AtomicInteger

@SpringBootApplication
class ClientApplication

fun main(args: Array<String>) {
    runApplication<ClientApplication>(*args)
}

@Component
class MyApplicationRunner : ApplicationRunner {

    private val serviceServerWebClient = WebClient.create("http://localhost:8080")

    override fun run(args: ApplicationArguments): Unit = runBlocking {
        var cancelAfterMillis = 20L
        val concurrentSize = 300

        repeat(1_000_000) { round ->
            val successCount = AtomicInteger(0)
            runConcurrentRequestsAndCancelEarly(concurrentSize, successCount, cancelAfterMillis)

            delay(100L)
            cancelAfterMillis = adjustCancelAfterMillis(cancelAfterMillis, successCount, concurrentSize)
            println("[$round] success=${successCount.get()} delay=$cancelAfterMillis")
        }
    }

    private fun adjustCancelAfterMillis(delayMillis: Long, successCount: AtomicInteger, concurrentSize: Int): Long {
        var delayMillis1 = delayMillis
        delayMillis1 = when {
            successCount.get() == 0 -> delayMillis1 + 1
            successCount.get() == concurrentSize -> delayMillis1 - 1
            else -> delayMillis1
        }
        return delayMillis1
    }

    private suspend fun runConcurrentRequestsAndCancelEarly(concurrentSize: Int, successCount: AtomicInteger, delayMillis: Long) {
        coroutineScope {
            val job = launch {
                repeat(concurrentSize) {
                    launch { if (callUserApi()) successCount.incrementAndGet() }
                }
            }

            delay(delayMillis)
            job.cancelAndJoin()
        }
    }

    private suspend fun callUserApi(): Boolean {
        return runCatching {
            serviceServerWebClient.get().uri("/user")
                .retrieve()
                .awaitBody<String>()
        }.isSuccess
    }
}
