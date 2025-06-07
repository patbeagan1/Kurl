package com.github.patbeagan1.kurl

import io.ktor.http.*
import java.io.File
import java.util.*

internal fun CurlDslScope.applyFlag(
    flag: String,
    value: String,
) {
    try {
        when (flag) {
            // Positional arguments (numbered) are typically URLs or resources
            "1" -> { /* Skip the curl command itself */
            }

            "2", "3", "4", "5" -> {
                // Assume any positional argument could be a URL
                // This supports curl https://example.com parameter1 parameter2
                if (value.contains("://") || value.startsWith("www.")) {
                    val cleanValue = removeQuotes(value)
                    u(cleanValue)
                }
            }

            // Method
            "-X", "--request" -> {
                val cleanValue = removeQuotes(value)
                X(HttpMethod.Companion.parse(cleanValue))
            }

            // Headers
            "-H", "--header" -> {
                val cleanValue = removeQuotes(value)
                H(cleanValue)
            }

            // URL
            "-u", "--url" -> {
                val cleanValue = removeQuotes(value)
                u(cleanValue)
            }

            // Data / Body
            "-d", "--data", "--data-ascii" -> {
                val cleanValue = removeQuotes(value)
                d(cleanValue)
            }

            "--data-raw" -> {
                val cleanValue = removeQuotes(value)
                d(cleanValue)
            }

            "--data-binary" -> {
                val cleanValue = removeQuotes(value)
                // Handle file upload with @ prefix
                if (cleanValue.startsWith("@")) {
                    val filePath = cleanValue.substring(1)
                    try {
                        d(File(filePath).readText())
                    } catch (e: Exception) {
                        d(cleanValue) // Fall back to raw value if file can't be read
                    }
                } else {
                    d(cleanValue)
                }
            }

            "--data-urlencode" -> {
                val cleanValue = removeQuotes(value)
                val keyValue = parseKeyValuePair(cleanValue)
                if (keyValue != null) {
                    // If it's a key-value pair, add as URL-encoded form parameter
                    val (k, v) = keyValue
                    form(k, v)
                } else {
                    // Otherwise treat as raw data
                    d(cleanValue)
                }
            }

            // Form data
            "-F", "--form", "--form-string" -> {
                val cleanValue = removeQuotes(value)
                val parts = cleanValue.split("=", limit = 2)
                if (parts.size == 2) {
                    val formKey = parts[0]
                    var formValue = parts[1]

                    // Handle file upload with @ prefix
                    if (formValue.startsWith("@") && flag != "--form-string") {
                        val filePath = formValue.substring(1)
                        // Extract content type if specified
                        val semicolonPos = filePath.indexOf(";")
                        val actualPath = if (semicolonPos > 0) filePath.substring(0, semicolonPos) else filePath

                        try {
                            form(formKey, File(actualPath))
                        } catch (e: Exception) {
                            form(formKey, formValue) // Fall back to raw value
                        }
                    } else {
                        form(formKey, formValue)
                    }
                }
            }

            // SSL/Security options
            "-k", "--insecure" -> insecure()
            "--http1.1" -> httpVersion("1.1")
            "--http2" -> httpVersion("2")

            // Output/Verbosity
            "-v", "--verbose" -> verbose()
            "-s", "--silent" -> { /* Ignore silent mode */
            }

            // Timeouts
            "--max-time", "-m" -> {
                val timeValue = removeQuotes(value).toLongOrNull()
                timeValue?.let { ms -> maxTime(ms * 1000) }
            }

            "--connect-timeout" -> {
                val timeValue = removeQuotes(value).toLongOrNull()
                timeValue?.let { ms -> connectTimeout(ms * 1000) }
            }

            // Cookies
            "-b", "--cookie" -> {
                val cleanValue = removeQuotes(value)
                val cookieFile = File(cleanValue)
                if (cookieFile.exists() && cookieFile.isFile) {
                    cookieFile(cookieFile)
                } else {
                    cookie(cleanValue)
                }
            }

            // Save cookies
            "-c", "--cookie-jar" -> {
                val cleanValue = removeQuotes(value)
                c(File(cleanValue))
            }

            // Output
            "-o", "--output" -> {
                val cleanValue = removeQuotes(value)
                o(File(cleanValue))
            }

            // Follow redirects
            "-L", "--location", "--location-trusted" -> followRedirects()

            // Compression
            "--compressed" -> compressed()

            // User credentials
            "-u", "--user" -> {
                val cleanValue = removeQuotes(value)
                val parts = cleanValue.split(":", limit = 2)
                if (parts.size == 2) {
                    val authValue = "Basic " + Base64.getEncoder()
                        .encodeToString("${parts[0]}:${parts[1]}".toByteArray())
                    H("Authorization: $authValue")
                }
            }

            // Request customization
            "-A", "--user-agent" -> {
                val cleanValue = removeQuotes(value)
                H("User-Agent: $cleanValue")
            }

            "-e", "--referer" -> {
                val cleanValue = removeQuotes(value)
                H("Referer: $cleanValue")
            }

            // HEAD request
            "-I", "--head" -> X(HttpMethod.Companion.Head)

            // GET with query parameters
            "-G", "--get" -> X(HttpMethod.Companion.Get)

            // Query parameters
            "-p", "--param" -> {
                val cleanValue = removeQuotes(value)
                val keyValue = parseKeyValuePair(cleanValue)
                if (keyValue != null) {
                    val (k, v) = keyValue
                    p(k, v)
                }
            }

            // Single-letter short flags without values
            "-s", "-S", "-i", "-j", "-0" -> {
                // These flags are recognized but don't map to specific DSL actions
                // -s: silent, -S: show errors, -i: include headers in output
                // -j: junk session cookies, -0: HTTP 1.0
            }

            else -> {
                // Handle positional arguments or unrecognized flags
                if (flag.toIntOrNull() != null) {
                    if (value.contains("://") || value.startsWith("www.")) {
                        val cleanValue = removeQuotes(value)
                        u(cleanValue)
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Log error or handle invalid flag/value pairs
        error("Error processing curl flag '$flag' with value '$value': ${e.message}")
    }
}


/**
 * Tries to extract key-value pairs from form data or query parameters
 * @param value The form data string (e.g. "name=John Doe")
 * @return A Pair of key and value, or null if parsing fails
 */
private fun parseKeyValuePair(value: String): Pair<String, String>? {
    val parts = value.split("=", limit = 2)
    return if (parts.size == 2) {
        parts[0] to parts[1]
    } else {
        null
    }
}

/**
 * Removes surrounding quotes (single or double) from a string if present
 * @param value The string to process
 * @return The string without surrounding quotes
 */
private fun removeQuotes(value: String): String {
    return when {
        (value.startsWith("\"") && value.endsWith("\"")) -> value.substring(1, value.length - 1)
        (value.startsWith("'") && value.endsWith("'")) -> value.substring(1, value.length - 1)
        else -> value
    }
}