package juro.domainserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class DomainServerApplication

fun main(args: Array<String>) {
    runApplication<DomainServerApplication>(*args)
}

@RestController
class DomainUserController {
    @GetMapping("/user")
    suspend fun getUser(): User {
        return User(id = 1L, name = "John Doe")
    }
}


data class User(
    val id: Long,
    val name: String,
)
