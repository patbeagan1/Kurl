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

## Advanced Examples

### Authentication and Security

#### Basic Authentication
```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Basic authentication
    val response = curl(client) {
        u("https://httpbin.org/basic-auth/user/pass")
        H("Authorization: Basic dXNlcjpwYXNz") // base64 encoded "user:pass"
    }.execute()
    
    println("Auth Response: ${response.bodyAsText()}")
    client.close()
}
```

#### Bearer Token Authentication
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Bearer token authentication
    val response = curl(client) {
        u("https://api.example.com/protected")
        H("Authorization: Bearer your-jwt-token-here")
        H("Accept: application/json")
    }.execute()
    
    println("Protected Resource: ${response.bodyAsText()}")
    client.close()
}
```

### File Operations

#### File Upload
```kotlin
import java.io.File

fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Upload a file using form data
    val response = curl(client) {
        X(HttpMethod.Post)
        u("https://httpbin.org/post")
        form("file", File("path/to/your/file.txt"))
        form("description", "My uploaded file")
    }.execute()
    
    println("Upload Response: ${response.bodyAsText()}")
    client.close()
}
```

#### Download and Save to File
```kotlin
import java.io.File

fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Download content and save to file
    val response = curl(client) {
        u("https://httpbin.org/json")
        o(File("downloaded.json")) // Save response to file
    }.execute()
    
    println("Downloaded to file. Status: ${response.status}")
    client.close()
}
```

### Advanced Request Configuration

#### Custom Timeouts and Retries
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }
    }
    
    val response = curl(client) {
        u("https://httpbin.org/delay/5")
        connectTimeout(15000) // Override connection timeout
        maxTime(20000) // Set maximum time for entire request
        verbose() // Enable detailed logging
    }.execute()
    
    println("Response after delay: ${response.status}")
    client.close()
}
```

#### SSL and Security Options
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Skip SSL verification (use with caution!)
    val response = curl(client) {
        u("https://self-signed.badssl.com/")
        insecure() // Skip SSL certificate verification
        verbose()
    }.execute()
    
    println("SSL Response: ${response.status}")
    client.close()
}
```

### Complex API Interactions

#### RESTful API with Multiple Operations
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // GET - Fetch user data
    val getUserResponse = curl(client) {
        u("https://jsonplaceholder.typicode.com/users/1")
        H("Accept: application/json")
    }.execute()
    
    println("User Data: ${getUserResponse.bodyAsText()}")
    
    // POST - Create new post
    val createPostResponse = curl(client) {
        X(HttpMethod.Post)
        u("https://jsonplaceholder.typicode.com/posts")
        H("Content-Type: application/json")
        d("""{
            "title": "My New Post",
            "body": "This is the content of my post",
            "userId": 1
        }""")
    }.execute()
    
    println("Created Post: ${createPostResponse.bodyAsText()}")
    
    // PUT - Update existing post
    val updatePostResponse = curl(client) {
        X(HttpMethod.Put)
        u("https://jsonplaceholder.typicode.com/posts/1")
        H("Content-Type: application/json")
        d("""{
            "id": 1,
            "title": "Updated Post Title",
            "body": "Updated content",
            "userId": 1
        }""")
    }.execute()
    
    println("Updated Post: ${updatePostResponse.bodyAsText()}")
    
    client.close()
}
```

#### Working with Query Parameters
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Complex query parameters
    val response = curl(client) {
        u("https://httpbin.org/get")
        p("page", "1")
        p("limit", "10")
        p("sort", "created_at")
        p("order", "desc")
        p("filter", "active")
        p("search", "kotlin dsl")
    }.execute()
    
    println("Query Response: ${response.bodyAsText()}")
    client.close()
}
```

### Real-World Use Cases

#### GitHub API Integration
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Get GitHub repository information
    val response = curl(client) {
        u("https://api.github.com/repos/ktorio/ktor")
        H("Accept: application/vnd.github.v3+json")
        H("User-Agent: Kurl-Example/1.0")
    }.execute()
    
    println("Ktor Repository: ${response.bodyAsText()}")
    client.close()
}
```

#### Weather API Integration
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Get weather data (example with OpenWeatherMap API)
    val response = curl(client) {
        u("https://api.openweathermap.org/data/2.5/weather")
        p("q", "London")
        p("appid", "your-api-key")
        p("units", "metric")
        H("Accept: application/json")
    }.execute()
    
    println("Weather Data: ${response.bodyAsText()}")
    client.close()
}
```

#### Webhook Testing
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Test webhook endpoint
    val response = curl(client) {
        X(HttpMethod.Post)
        u("https://webhook.site/your-unique-url")
        H("Content-Type: application/json")
        H("X-Webhook-Source: Kurl-Test")
        d("""{
            "event": "test",
            "timestamp": "${System.currentTimeMillis()}",
            "data": {
                "message": "Hello from Kurl!",
                "version": "1.0"
            }
        }""")
        verbose()
    }.execute()
    
    println("Webhook Response: ${response.status}")
    client.close()
}
```

### Performance Optimization

#### Connection Pooling and Reuse
```kotlin
fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
        }
    }
    
    // Reuse the same client for multiple requests
    val urls = listOf(
        "https://httpbin.org/get",
        "https://httpbin.org/headers",
        "https://httpbin.org/user-agent"
    )
    
    urls.forEach { url ->
        val response = curl(client) {
            u(url)
            H("Accept: application/json")
        }.execute()
        
        println("Response from $url: ${response.status}")
    }
    
    client.close()
}
```

#### Batch Processing with Coroutines
```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

fun main() = runBlocking {
    val client = HttpClient(CIO)
    
    // Process multiple requests concurrently
    val requests = listOf(
        "https://httpbin.org/delay/1",
        "https://httpbin.org/delay/2",
        "https://httpbin.org/delay/3"
    )
    
    val responses = requests.map { url ->
        async {
            curl(client) {
                u(url)
                H("Accept: application/json")
            }.execute()
        }
    }.awaitAll()
    
    responses.forEachIndexed { index, response ->
        println("Request ${index + 1} completed with status: ${response.status}")
    }
    
    client.close()
}
```

## Development

The core logic resides within the `CurlDslScope` class, which defines the DSL methods and maps them to Ktor client configurations. The `curl` function acts as the entry point, taking an `HttpClient` instance and a lambda with `CurlDslScope` as its receiver to build and execute requests.

### Architecture Overview

- **`Curl.kt`**: Entry point functions for both DSL and command string parsing
- **`CurlDslScope.kt`**: Core DSL implementation with fluent API methods
- **`CurlParser.kt`**: Parses raw curl command strings into flag-value pairs
- **`CurlApplyFlag.kt`**: Maps curl flags to DSL method calls

### Contributing

When contributing to Kurl, please ensure:
1. All tests pass (`mvn test`)
2. New features include comprehensive tests
3. Documentation is updated for new functionality
4. Code follows Kotlin style guidelines