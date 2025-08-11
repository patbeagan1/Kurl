package com.github.patbeagan1.kurl

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class CurlDslTest {
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
    fun `test simple GET request to httpbin`() = runBlocking {
        // GET request to httpbin.org
        val response = curl(client) {
            u("https://httpbin.org/get")
            H("Accept: application/json")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"url\": \"https://httpbin.org/get\""),
            "Response should contain the request URL"
        )
    }

    @Test
    fun `test GET request with query parameters`() = runBlocking {
        // GET with query parameters
        val response = curl(client) {
            u("https://httpbin.org/get")
            p("param1", "value1")
            p("param2", "value2")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"param1\": \"value1\""),
            "Response should contain the first query parameter"
        )
        assertTrue(
            body.contains("\"param2\": \"value2\""),
            "Response should contain the second query parameter"
        )
    }

    @Test
    fun `test POST request with JSON body`() = runBlocking {
        // POST with JSON body
        val response = curl(client) {
            X(HttpMethod.Post)
            u("https://httpbin.org/post")
            H("Content-Type: application/json")
            d("""{"name":"test","value":"data"}""")
        }.execute()

        println(response.request)
        println(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"name\": \"test\""),
            "Response should contain the posted JSON data"
        )
    }

    @Test
    fun `test request with custom headers`() = runBlocking {
        // Request with custom headers
        val response = curl(client) {
            u("https://httpbin.org/headers")
            H("X-Custom-Header: Custom Value")
            H("X-Another-Header: Another Value")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"X-Custom-Header\": \"Custom Value\""),
            "Response should contain the custom header"
        )
        assertTrue(
            body.contains("\"X-Another-Header\": \"Another Value\""),
            "Response should contain the second custom header"
        )
    }

    @Test
    fun `test User-Agent header`() = runBlocking {
        // Set User-Agent header
        val userAgent = "CurlDSL/1.0 Test Suite"
        val response = curl(client) {
            u("https://httpbin.org/user-agent")
            H("User-Agent: $userAgent")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains(userAgent),
            "Response should contain the custom User-Agent"
        )
    }

    @Test
    fun `test request with timeout`() = runBlocking {
        // Request with timeout
        val response = curl(client) {
            u("https://httpbin.org/delay/1")  // 1 second delay
            connectTimeout(5000)  // 5 second timeout
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test redirect following`() = runBlocking {
        // Test redirect following
        val response = curl(client) {
            u("https://httpbin.org/redirect/1")  // 1 redirect
            followRedirects()
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"url\": \"https://httpbin.org/get\""),
            "Response should be from the final redirect destination"
        )
    }

    @Test
    fun `test HEAD request`() = runBlocking {
        // HEAD request
        val response = curl(client) {
            X(HttpMethod.Head)
            u("https://httpbin.org/get")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("", response.bodyAsText(), "HEAD request should have empty body")
    }

    @Test
    fun `test PUT request`() = runBlocking {
        // PUT request
        val response = curl(client) {
            X(HttpMethod.Put)
            u("https://httpbin.org/put")
            H("Content-Type: application/json")
            d("""{"updated":"value"}""")
        }.execute()

        println(response.request)
        println(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"updated\": \"value\""),
            "Response should contain the PUT data"
        )
    }

    @Test
    fun `test request to well-known site`() = runBlocking {
        // Request to a well-known site
        val response = curl(client) {
            u("https://example.com")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("<title>Example Domain</title>"),
            "Response should contain expected title"
        )
    }

    @Test
    fun `test JSON API request`() = runBlocking {
        // Public JSON API request
        val response = curl(client) {
            u("https://jsonplaceholder.typicode.com/posts/1")
            H("Accept: application/json")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"id\": 1"),
            "Response should contain the requested post ID"
        )
        assertTrue(
            body.contains("\"title\""),
            "Response should contain a title field"
        )
    }

    @Test
    fun `test form submission`() = runBlocking {
        // Form submission
        val response = curl(client) {
            X(HttpMethod.Post)
            u("https://httpbin.org/post")
            H("Content-Type: application/x-www-form-urlencoded")
            form("field1", "value1")
            form("field2", "value2")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"field1\": \"value1\""),
            "Response should contain the first form field"
        )
        assertTrue(
            body.contains("\"field2\": \"value2\""),
            "Response should contain the second form field"
        )
    }

    @Test
    fun `test curl string command - GET request`() = runBlocking {
        // Execute a curl command string
        val curlCommand = "curl -X GET https://httpbin.org/get?param=test -H 'Accept: application/json'"

        val response = curl(client, curlCommand).execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"url\": \"https://httpbin.org/get?param=test\""),
            "Response should contain the request URL with query parameter"
        )
    }

    @Test
    fun `test curl string command - POST request`() = runBlocking {
        // Execute a curl command string with POST
        val curlCommand = """
            curl -X POST https://httpbin.org/post 
            -H "Content-Type: application/json" 
            -d '{"name":"test","value":"data"}'
        """.trimIndent()

        val response = curl(client, curlCommand).execute()

        println(response.request)
        println(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"name\": \"test\""),
            "Response should contain the posted JSON data"
        )
    }

    @Test
    fun `test curl string command - with multiple flags`() = runBlocking {
        // Execute a curl command with multiple flags
        val curlCommand = """
            curl -v -L -X GET https://httpbin.org/redirect/1 
            -H "Accept: application/json" 
            -H "User-Agent: CurlDSL-Test"
        """.trimIndent()

        val response = curl(client, curlCommand).execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"url\": \"https://httpbin.org/get\""),
            "Response should be from the final redirect destination"
        )
    }

    @Test
    fun `test curl string command - query parameters`() = runBlocking {
        // Execute a curl command with explicit query parameters
        val curlCommand = """
            curl "https://httpbin.org/get" -G 
            -p "param1=value1" 
            -p "param2=value2" 
            -p "param3=value with spaces"
        """.trimIndent()

        val response = curl(client, curlCommand).execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"param1\": \"value1\""),
            "Response should contain the first query parameter"
        )
        assertTrue(
            body.contains("\"param2\": \"value2\""),
            "Response should contain the second query parameter"
        )
    }

    @Test
    fun `test request to Wikipedia API`() = runBlocking {
        // Request to Wikipedia API
        val response = curl(client) {
            u("https://en.wikipedia.org/api/rest_v1/page/summary/Earth")
            H("Accept: application/json")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Earth"),
            "Response should contain information about Earth"
        )
        assertTrue(
            body.contains("title"),
            "Response should contain a title field"
        )
    }

    @Test
    fun `test request to GitHub API`() = runBlocking {
        // Request to GitHub API (public endpoint)
        val response = curl(client) {
            u("https://api.github.com/zen")
        }.execute()

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertFalse(body.isEmpty(), "Response should not be empty")
    }

    @Test
    fun `test complex curl command with multiple features`() = runBlocking {
        // A complex curl command using multiple features
        val curlCommand = """
            curl -v -L -X POST "https://httpbin.org/post" 
            -H "Content-Type: application/json" 
            -H "Accept: application/json" 
            -H "User-Agent: CurlDSL-Complex-Test" 
            -d '{"test":"complex","nested":{"value":42}}' 
            --connect-timeout 5
        """.trimIndent()

        val response = curl(client, curlCommand).execute()

        println(response.request)
        println(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"test\": \"complex\""),
            "Response should contain the posted data"
        )
        assertTrue(
            body.contains("\"value\": 42"),
            "Response should contain the nested JSON data"
        )
    }
}
