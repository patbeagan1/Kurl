# Kurl: A cURL-like DSL for Ktor

Kurl is a Kotlin DSL (Domain Specific Language) built on top of the **Ktor HTTP client**, designed to offer a familiar `curl`-like syntax for making HTTP requests. It simplifies the process of constructing and executing HTTP requests in Kotlin by providing a fluent and expressive API.

---

## Features

Kurl provides a variety of features that mirror common `curl` commands, allowing you to easily handle:

* **Diverse HTTP interactions**: Perform `GET`, `POST`, `PUT`, `HEAD` requests, set URLs, and include query parameters.
* **Customizable requests**: Add **headers** (like `User-Agent` or `Content-Type`), send **request bodies** (JSON or form-urlencoded), and manage **cookies**.
* **Request control**: Configure **timeouts**, enable **redirect following**, output responses to **files**, and use **verbose mode** for detailed logging.
* **`curl` command string parsing**: Directly interpret and execute HTTP requests from a raw `curl` command string, making it simple to test and integrate existing `curl` commands.

---

## Installation

To use Kurl, include the necessary Ktor client dependencies in your project.

```kotlin
// In your build.gradle.kts (Kotlin DSL)
dependencies {
    implementation("io.ktor:ktor-client-core:2.3.11") // Or the latest version
    implementation("io.ktor:ktor-client-cio:2.3.11")  // Or your preferred engine
    implementation("io.github.patbeagan1:kurl:<VERSION>")
}
```

---

## Usage

Kurl offers two main ways to define your HTTP requests: using the **DSL builder** or by **parsing a `curl` command string**.

### Using the DSL Builder

The DSL builder provides a fluent and type-safe way to construct your requests, combining various options in one go.

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 5000
        }
    }

    // Example: A POST request with JSON body, custom headers, and a timeout
    val response = curl(client) {
        X(HttpMethod.Post)
        u("https://httpbin.org/post")
        H("Content-Type: application/json")
        H("X-Custom-Header: Kurl-Test")
        d("""{"name":"Kurl","type":"DSL"}""")
        connectTimeout(7000) // Set a connection timeout
        verbose() // Enable verbose logging for this request
    }.execute()

    println("Response Status: ${response.status}")
    println("Response Body: ${response.bodyAsText()}")

    client.close()
}
```

### Parsing a `curl` Command String

You can directly interpret standard `curl` command strings, which is ideal for testing or integrating existing commands.

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 5000
        }
    }

    // Example: A complex curl command with GET, query parameters, headers, and follow redirects
    val complexCurlCommand = """
        curl -v -L "https://httpbin.org/redirect/1?source=kurl"
        -H "Accept: application/json"
        -H "User-Agent: Kurl-UserAgent-Test"
    """.trimIndent()

    val response = curl(client, complexCurlCommand).execute()

    println("Command Response Status: ${response.status}")
    println("Command Response Body: ${response.bodyAsText()}")

    client.close()
}
```

---

<details>
    <summary>Stability</summary>
    The curl command-line tool and its underlying library, libcurl, are designed for stability and backward compatibility. The developers strive to avoid breaking changes to the API. In fact, the last time the libcurl API changed in a non-compatible way was for version 7.16.0 in 2006, and the project aims to never do it again.
While the core API remains stable, curl is under continuous development, with new features, options, and bug fixes being added regularly. These changes are typically additive and do not break existing functionality.
Regarding the latest changes, curl releases new versions frequently. For example, curl 8.14.1 was released on June 4, 2025 (just a few days ago, relative to today's date), and curl 8.14.0 was released on May 28, 2025. These releases primarily include bug fixes and minor improvements.
You can find a detailed list of changes for each release on the official curl website's change log: https://curl.se/changes.html
</details>

## Development

The core logic resides within the `CurlDslScope` class, which defines the DSL methods and maps them to Ktor client configurations. The `curl` function acts as the entry point, taking an `HttpClient` instance and a lambda with `CurlDslScope` as its receiver to build and execute requests.