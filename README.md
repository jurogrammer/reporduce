# Reactor Netty Resource Leak Reproduction

**Versions:** Java 21, Kotlin 2.1.10, Spring Boot 3.4.3, Reactor Netty

This project reproduces Reactor Netty resource leaks when HTTP requests are cancelled early. The test makes 400 concurrent requests per round (100 rounds total) and cancels them after 10ms to trigger memory leaks.

## Quick Run

```bash
./gradlew -p service-server clean test -Dorg.gradle.jvmargs="-Xms64m -Xmx128m" --info
```

## What You'll See

- **Success**: No LEAK warnings
- **Leak Detected**: `LEAK: ByteBuf.release() was not called before it's garbage-collected`

The test automatically enables paranoid leak detection to catch resource leaks.