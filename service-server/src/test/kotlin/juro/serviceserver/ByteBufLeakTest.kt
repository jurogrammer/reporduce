package juro.serviceserver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ByteBufLeakTest {
    private val mockDomainServer = MockWebServer().apply { start(8081) }

    @LocalServerPort
    private var port: Int? = 8080

    @Test
    fun `reproduce bytebuf leak on cancellation`(): Unit = runBlocking(Dispatchers.IO) {
        startFakeServer()

        var cancelAfterMillis = 20L
        val concurrentSize = 50

        repeat(1_000_000) { round ->
            val successCount = AtomicInteger(0)
            runConcurrentRequestsAndCancelEarly(concurrentSize, successCount, cancelAfterMillis)

            delay(100L)
            cancelAfterMillis = adjustCancelAfterMillis(cancelAfterMillis, successCount, concurrentSize)
            println("[$round] success=${successCount.get()} delay=$cancelAfterMillis")
        }

    }

    private fun startFakeServer() {
        val response = MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""{ "id": 1, "name": "John" }""")


        repeat(1_000_000) { mockDomainServer.enqueue(response) }
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
                repeat(concurrentSize) { launch { if (callUserApi()) successCount.incrementAndGet() } }
            }

            delay(delayMillis)
            job.cancelAndJoin()
        }
    }

    private val serviceServerClient = WebClient.builder().baseUrl("http://localhost:$port").build()

    private suspend fun callUserApi(): Boolean {
        return runCatching {
            serviceServerClient.get().uri("/user")
                .retrieve()
                .awaitBody<String>()
        }.isSuccess
    }
}
