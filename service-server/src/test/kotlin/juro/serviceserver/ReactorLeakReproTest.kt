package juro.serviceserver

import io.netty.util.ResourceLeakDetector
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.netty.DisposableServer
import reactor.netty.http.client.HttpClient
import reactor.netty.http.server.HttpServer
import reactor.netty.resources.LoopResources

class ReactorLeakReproTest {

    private lateinit var reactorHttpServer: DisposableServer
    private lateinit var reactorHttpClient: HttpClient

    @BeforeEach
    fun setUp() {
        startReactorHttpServer()
        reactorHttpClient = HttpClient.create()
            .runOn(LoopResources.create("reactor-client"))
    }

    @AfterEach
    fun tearDown() {
        reactorHttpServer.disposeNow()
    }

    @Test
    fun `reproduce reactor leak with reactor http client and server`() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID)
        val cancelAfterMillis = 10L
        val concurrentSize = 400

        repeat(100) { round ->
            runReactorHttpRequestsAndCancelEarly(concurrentSize, cancelAfterMillis)
        }
    }


    private fun runReactorHttpRequestsAndCancelEarly(concurrentSize: Int, delayMillis: Long) {
        val disposable = Flux.range(0, concurrentSize)
            .flatMap { callReactorHttpApi() }
            .subscribeOn(Schedulers.parallel())
            .subscribe()

        Thread.sleep(delayMillis)
        disposable.dispose()

        Thread.sleep(100)
    }

    private fun callReactorHttpApi(): Mono<String> {
        return reactorHttpClient
            .get()
            .uri("http://localhost:8082/user")
            .responseSingle { response, content -> content.asString() }
            .onErrorResume { Mono.empty() }
    }

    private fun startReactorHttpServer() {
        val serverLoopResources = LoopResources.create("reactor-server")
        reactorHttpServer = HttpServer.create()
            .port(8082)
            .runOn(serverLoopResources)
            .route { routes ->
                routes.get("/user") { request, response ->
                    response
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just("""{"id":1,"name":"John"}"""))
                }
            }
            .bindNow()

        println("Reactor HTTP server started on port 8082")
    }

}
