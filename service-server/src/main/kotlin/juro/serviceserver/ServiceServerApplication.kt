package juro.serviceserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@SpringBootApplication
class ServiceServerApplication

fun main(args: Array<String>) {
    runApplication<ServiceServerApplication>(*args)
}

@RestController
class ServiceUserController(
    private val domainUserClient: DomainUserClient
) {
    @GetMapping("/user")
    fun getUser(): Mono<User> {
        return domainUserClient.getUserMono()
    }
}

data class User(
    val id: Long,
    val name: String,
)

@Component
class DomainUserClient(builder: WebClient.Builder) {

    private val webClient = builder.baseUrl("http://localhost:8081").build()

    fun getUserMono(): Mono<User> {
        return webClient.get().uri("/user").retrieve().bodyToMono<User>()
    }

}
