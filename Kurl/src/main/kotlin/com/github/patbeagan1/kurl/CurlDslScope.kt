package com.github.patbeagan1.kurl

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import java.io.File

class CurlDslScope(private val client: HttpClient) {
    private var method: HttpMethod = HttpMethod.Companion.Get
    private var url: String = ""
    private val headerMap: MutableMap<String, String> = mutableMapOf()
    private val queryParams: MutableMap<String, String> = mutableMapOf()
    private val formData: MutableMap<String, Any> = mutableMapOf()
    private var body: Any? = null
    private var sslIgnore: Boolean = false
    private var verbose: Boolean = false
    private var maxTime: Long? = null
    private var connectTimeout: Long? = null
    private var cookie: String? = null
    private var cookieFile: File? = null
    private var outputFile: File? = null
    private var followRedirects: Boolean = false
    private var httpVersion: HttpProtocolVersion? = null
    private var compressed: Boolean = false

    fun url(value: String) {
        url = value
    }

    fun u(value: String) = url(value) // Alias for URL

    fun method(value: HttpMethod) {
        method = value
    }

    fun X(value: HttpMethod) = method(value) // Alias for HTTP method

    fun header(key: String, value: String) {
        headerMap[key] = value
    }

    /**
     * See [header]
     */
    fun H(header: String) {
        val (key, value) = header.split(":", limit = 2).map { it.trim() }
        headerMap[key] = value
    } // Alias for header short form

    fun param(key: String, value: String) {
        queryParams[key] = value
    }

    /**
     * See [param]
     */
    fun p(key: String, value: String) = param(key, value) // Alias for query param

    fun form(key: String, value: Any) {
        formData[key] = value
    } // Alias for form data (mirrors `-F` in curl)

    fun d(value: String) {
        body = value
    } // Alias for body short form

    fun body(value: Any) {
        body = value
    }

    fun insecure() {
        sslIgnore = true
    } // Alias for `-k` in curl

    fun verbose() {
        verbose = true
    } // Alias for `-v` in curl

    fun maxTime(milliseconds: Long) {
        maxTime = milliseconds
    } // Alias for `--max-time`

    fun connectTimeout(milliseconds: Long) {
        connectTimeout = milliseconds
    } // Alias for `--connect-timeout`

    fun cookie(value: String) {
        cookie = value
    } // Alias for `-b` cookie string

    fun cookieFile(file: File) {
        cookieFile = file
    } // Alias for `-b` cookie file

    fun c(file: File) = saveCookies(file)
    fun saveCookies(file: File) {
        cookieFile = file
    } // Alias for `-c`

    fun o(file: File) = output(file)
    fun output(file: File) {
        outputFile = file
    } // Alias for `-o`

    fun followRedirects() {
        this.followRedirects = true
    } // Alias for `-L`

    fun compressed() {
        this.compressed = true
    } // Alias for `--compressed`

    fun httpVersion(version: String) {
        httpVersion = when (version) {
            "1.1" -> HttpProtocolVersion.Companion.HTTP_1_1
            "2" -> HttpProtocolVersion.Companion.HTTP_2_0
            else -> throw IllegalArgumentException("Unsupported HTTP version: $version")
        }
    } // Alias for `--http1.1` and `--http2`

    suspend fun execute(): HttpResponse {
        if (verbose) {
            listOf(
                "Executing curl:",
                "URL: $url",
                "Method: $method",
                "Headers: $headerMap",
                "Query Params: $queryParams",
                "Form Data: $formData",
                "Body: $body",
                "Timeouts: maxTime=${maxTime ?: "Default"}, connectTimeout=${connectTimeout ?: "Default"}",
                "SSL Ignore: $sslIgnore",
                "Follow Redirects: $followRedirects",
                "HTTP Version: $httpVersion",
                "Compressed: $compressed",
                "\n"
            )
                .joinToString(" :: ")
                .let { println(it) }
        }

        val response = client.request(url, fun(builder: HttpRequestBuilder) {
            builder.method = this.method
            headerMap.forEach { (key, value) -> builder.header(key, value) }
            queryParams.forEach { (key, value) -> builder.parameter(key, value) }
            formData.forEach { (key, value) -> builder.parameter(key, value) }
            body?.let { builder.setBody(it) }

//            if (sslIgnore) trustAllCertificates()
            if (maxTime != null || connectTimeout != null) {
                builder.timeout {
                    this.requestTimeoutMillis = this@CurlDslScope.maxTime ?: 10_000
                    this.connectTimeoutMillis = this@CurlDslScope.connectTimeout ?: 10_000
                }
            }
            if (followRedirects) followRedirects = true
//            if (httpVersion != null) {version = httpVersion
//            if (compressed) acceptEncoding(ContentEncoding.Gzip)
            cookie?.let { builder.header(HttpHeaders.Cookie, it) }
            cookieFile?.let { builder.header(HttpHeaders.Cookie, it.readText()) }
        })

        outputFile?.writeText(response.bodyAsText())

        return response
    }
}