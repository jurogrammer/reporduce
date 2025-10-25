package juro.client

import kotlinx.coroutines.*
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

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
            runConcurrentRequestsAndCancelEarly(concurrentSize, cancelAfterMillis)

            delay(100L)
            cancelAfterMillis = 14L
        }
    }

    private suspend fun runConcurrentRequestsAndCancelEarly(concurrentSize: Int, delayMillis: Long) {
        coroutineScope {
            val job = launch {
                repeat(concurrentSize) {
                    launch { callUserApi() }
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
