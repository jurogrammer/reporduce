# Reproduce Project

This is a multi-module Spring Boot project designed to reproduce and debug request-related issues using Netty and concurrent service interactions.

## 📦 Modules

```
client → service-server → domain-server
```

- **domain-server**: Downstream service
- **service-server**: Middle service that calls domain-server
- **client**: Triggers the chain by calling service-server

---

## 🚀 How to Run

1. **Give execute permission to the script** (first time only):

```bash
chmod +x run-all.sh
```

2. **Run the script**:

```bash
./run-all.sh
```

---

### What the script does

1. Starts `domain-server` in the background.
2. Starts `service-server` in the background with Netty leak detection enabled:
   - `-Dio.netty.leakDetectionLevel=PARANOID`
   - `-Dio.netty.leakDetection.targetRecords=40`
   - `-Xmx100m`
3. Waits for 5 seconds to allow services to initialize.
4. Executes `client` in the foreground.
5. Shuts down background servers automatically after client finishes.

---

## 📂 Logs

Each module writes its logs to the `logs/` directory:

```
logs/
├── domain-server.log
├── service-server.log
└── client.log
```

---

## 💻 Requirements

- **Java 21** (automatically handled by Gradle toolchains)
- **Gradle Wrapper** (included)

---

## 🧼 Cleanup

- Servers shut down automatically after the client completes.
- Pressing `Ctrl + C` will also clean up background services.
