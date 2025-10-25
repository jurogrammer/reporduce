package juro.serviceserver

import io.netty.util.ResourceLeakDetector
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.resources.LoopResources
import reactor.netty.http.client.HttpClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ReactorLeakReproTest {

    private lateinit var serviceServerClient: WebClient
    private lateinit var domainServer: Process
    private lateinit var loopResources: LoopResources

    @LocalServerPort
    private var port: Int = 8080

    @BeforeEach
    fun setUp() {
        startDomainServer()
        loopResources = LoopResources.create("service-cl")
        serviceServerClient = WebClient.builder()
            .baseUrl("http://localhost:$port")
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .runOn(loopResources)
                )
            )
            .build()
    }

    @AfterEach
    fun tearDown() {
        domainServer.destroy()
    }

    @Test
    fun `reproduce reactor leak with mass cancellation`() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID)
        val cancelAfterMillis = 14L
        val concurrentSize = 400

        repeat(100) { round -> // Reduced iterations for testing
            runConcurrentRequestsAndCancelEarly(concurrentSize, cancelAfterMillis)
        }
    }

    private fun runConcurrentRequestsAndCancelEarly(concurrentSize: Int, delayMillis: Long) {
        val disposable = Flux.range(0, concurrentSize)
            .flatMap { callUserApi() }
            .subscribeOn(Schedulers.parallel())
            .subscribe()
        
        // Wait for the specified delay
        Thread.sleep(delayMillis)
        
        // Cancel the requests
        disposable.dispose()
        
        // Additional delay
        Thread.sleep(1000)
    }

    private fun callUserApi(): Mono<String> {
        return serviceServerClient.get()
            .uri("/user")
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorResume { Mono.empty() }
    }


    private fun startDomainServer() {
        // Java HttpServer 사용
        Thread {
            try {
                val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(8081), 0)
                server.createContext("/user") { exchange ->
                    val response = """{"id":1,"name":"John"}"""
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                    exchange.responseBody.use { it.write(response.toByteArray()) }
                }
                server.start()
                println("Domain server started on port 8081")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        // 서버 시작 대기
        Thread.sleep(1000)
    }

}
