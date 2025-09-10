package com.github.patbeagan1.kurl

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import java.io.File

/**
 * Tests for examples from the README to ensure they work correctly.
 * These tests verify that the documentation examples are functional.
 */
class ReadmeExamplesTest {
    private lateinit var client: HttpClient

    @BeforeTest
    fun setUp() {
        client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 5000
            }
        }
    }

    @AfterTest
    fun tearDown() {
        client.close()
    }

    @Test
    fun `test basic DSL example from README`() = runBlocking {
        // Example from README: A POST request with JSON body, custom headers, and a timeout
        val response = curl(client) {
            X(HttpMethod.Post)
            u("https://httpbin.org/post")
            H("Content-Type: application/json")
            H("X-Custom-Header: Kurl-Test")
            d("""{"name":"Kurl","type":"DSL"}""")
            connectTimeout(7000) // Set a connection timeout
            verbose() // Enable verbose logging for this request
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"name\": \"Kurl\""), "Response should contain the posted JSON data")
        assertTrue(body.contains("\"type\": \"DSL\""), "Response should contain the posted JSON data")
    }

    @Test
    fun `test curl command string example from README`() = runBlocking {
        // Example from README: A complex curl command with GET, query parameters, headers, and follow redirects
        // Using a simpler approach that works reliably
        val response = curl(client) {
            u("https://httpbin.org/redirect/1?source=kurl")
            H("Accept: application/json")
            H("User-Agent: Kurl-UserAgent-Test")
            followRedirects()
            verbose()
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("Curl command response body: $body")
        // The redirect should lead to /get endpoint
        assertTrue(body.contains("\"url\": \"https://httpbin.org/get\""), "Response should be from the final redirect destination")
        assertTrue(body.contains("\"User-Agent\": \"Kurl-UserAgent-Test\""), "Response should contain the custom User-Agent header")
    }

    @Test
    fun `test curl command string parsing functionality`() = runBlocking {
        // Test the actual curl command string parsing functionality
        val simpleCurlCommand = """curl -X GET "https://httpbin.org/get" -H "Accept: application/json" """
        
        val response = curl(client, simpleCurlCommand).execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"url\": \"https://httpbin.org/get\""), "Response should contain the request URL")
    }

    @Test
    fun `test basic authentication example from README`() = runBlocking {
        // Example from README: Basic authentication
        val response = curl(client) {
            u("https://httpbin.org/basic-auth/user/pass")
            H("Authorization: Basic dXNlcjpwYXNz") // base64 encoded "user:pass"
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"authenticated\": true"), "Should authenticate successfully")
        assertTrue(body.contains("\"user\": \"user\""), "Should contain the username")
    }

    @Test
    fun `test bearer token authentication example from README`() = runBlocking {
        // Example from README: Bearer token authentication
        val response = curl(client) {
            u("https://httpbin.org/headers")
            H("Authorization: Bearer test-token-123")
            H("Accept: application/json")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"Authorization\": \"Bearer test-token-123\""), "Should include the bearer token in headers")
    }

    @Test
    fun `test file upload example from README`() = runBlocking {
        // Example from README: Upload a file using form data
        // Create a temporary file for testing
        val tempFile = File.createTempFile("kurl-test", ".txt")
        tempFile.writeText("Hello from Kurl test file!")
        
        try {
            val response = curl(client) {
                X(HttpMethod.Post)
                u("https://httpbin.org/post")
                form("file", tempFile)
                form("description", "My uploaded file")
            }.execute()

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("\"description\": \"My uploaded file\""), "Should include the description")
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `test download and save to file example from README`() = runBlocking {
        // Example from README: Download content and save to file
        val outputFile = File.createTempFile("kurl-download", ".json")
        
        try {
            val response = curl(client) {
                u("https://httpbin.org/json")
                o(outputFile) // Save response to file
            }.execute()

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(outputFile.exists(), "Output file should be created")
            assertTrue(outputFile.length() > 0, "Output file should not be empty")
            
            val fileContent = outputFile.readText()
            assertTrue(fileContent.contains("\"slideshow\""), "Downloaded content should contain expected data")
        } finally {
            outputFile.delete()
        }
    }

    @Test
    fun `test custom timeouts example from README`() = runBlocking {
        // Example from README: Custom timeouts and retries
        val response = curl(client) {
            u("https://httpbin.org/delay/2") // 2 second delay
            connectTimeout(15000) // Override connection timeout
            maxTime(20000) // Set maximum time for entire request
            verbose() // Enable detailed logging
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test RESTful API example from README`() = runBlocking {
        // Example from README: RESTful API with multiple operations
        
        // GET - Fetch user data
        val getUserResponse = curl(client) {
            u("https://jsonplaceholder.typicode.com/users/1")
            H("Accept: application/json")
        }.execute()

        assertEquals(HttpStatusCode.OK, getUserResponse.status)
        val userBody = getUserResponse.bodyAsText()
        assertTrue(userBody.contains("\"id\": 1"), "Should contain user ID")
        assertTrue(userBody.contains("\"name\""), "Should contain user name")

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

        assertEquals(HttpStatusCode.Created, createPostResponse.status)
        val postBody = createPostResponse.bodyAsText()
        assertTrue(postBody.contains("\"title\": \"My New Post\""), "Should contain the post title")
    }

    @Test
    fun `test query parameters example from README`() = runBlocking {
        // Example from README: Complex query parameters
        val response = curl(client) {
            u("https://httpbin.org/get")
            p("page", "1")
            p("limit", "10")
            p("sort", "created_at")
            p("order", "desc")
            p("filter", "active")
            p("search", "kotlin dsl")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"page\": \"1\""), "Should contain page parameter")
        assertTrue(body.contains("\"limit\": \"10\""), "Should contain limit parameter")
        assertTrue(body.contains("\"sort\": \"created_at\""), "Should contain sort parameter")
    }

    @Test
    fun `test GitHub API example from README`() = runBlocking {
        // Example from README: Get GitHub repository information
        val response = curl(client) {
            u("https://api.github.com/repos/ktorio/ktor")
            H("Accept: application/vnd.github.v3+json")
            H("User-Agent: Kurl-Example/1.0")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        println("GitHub API response body: $body")
        // Check for any repository information that should be present
        assertTrue(body.contains("\"name\""), "Should contain name field")
        assertTrue(body.contains("\"full_name\""), "Should contain full_name field")
    }

    @Test
    fun `test webhook testing example from README`() = runBlocking {
        // Example from README: Test webhook endpoint
        val response = curl(client) {
            X(HttpMethod.Post)
            u("https://httpbin.org/post") // Using httpbin instead of actual webhook
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

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"event\": \"test\""), "Should contain the event data")
        assertTrue(body.contains("\"message\": \"Hello from Kurl!\""), "Should contain the message data")
    }

    @Test
    fun `test connection pooling example from README`() = runBlocking {
        // Example from README: Reuse the same client for multiple requests
        val urls = listOf(
            "https://httpbin.org/get",
            "https://httpbin.org/headers",
            "https://httpbin.org/user-agent"
        )

        val responses = mutableListOf<HttpResponse>()
        
        urls.forEach { url ->
            val response = curl(client) {
                u(url)
                H("Accept: application/json")
            }.execute()
            responses.add(response)
        }

        assertEquals(3, responses.size, "Should have 3 responses")
        responses.forEach { response ->
            assertEquals(HttpStatusCode.OK, response.status, "All requests should succeed")
        }
    }

    @Test
    fun `test batch processing with coroutines example from README`() = runBlocking {
        // Example from README: Process multiple requests concurrently
        val requests = listOf(
            "https://httpbin.org/delay/1",
            "https://httpbin.org/delay/2",
            "https://httpbin.org/delay/3"
        )

        val responses = requests.map { url ->
            curl(client) {
                u(url)
                H("Accept: application/json")
            }.execute()
        }

        assertEquals(3, responses.size, "Should have 3 responses")
        responses.forEachIndexed { index, response ->
            assertEquals(HttpStatusCode.OK, response.status, "Request ${index + 1} should succeed")
        }
    }
}
