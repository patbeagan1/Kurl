package com.github.patbeagan1.kurl

import kotlin.test.Test
import kotlin.test.assertEquals

const val OPENAI_API_KEY = "YOUR_OPENAI_API_KEY"

class CurlParserTest {

    @Test
    fun testParseCurlToFlagValuePairs() {
        // Given
        val curlCommand = """
            curl --request POST \
            --url https://api.openai.com/v1/audio/transcriptions \
            --header "Authorization: Bearer $OPENAI_API_KEY" \
            --header 'Content-Type: multipart/form-data' \
            --form file=@/path/to/file/speech.mp3 \
            --form model=gpt-4o-transcribe \
            --form response_format=text
        """.trimIndent()

        // Expected output based on the provided sample
        val expected = listOf(
            "1" to "curl",
            "--request" to "POST",
            "--url" to "https://api.openai.com/v1/audio/transcriptions",
            "--header" to "\"Authorization: Bearer $OPENAI_API_KEY\"",
            "--header" to "'Content-Type: multipart/form-data'",
            "--form" to "file=@/path/to/file/speech.mp3",
            "--form" to "model=gpt-4o-transcribe",
            "--form" to "response_format=text"
        )

        // When
        val parser = CurlParser()
        val actual = parser.parseCurlToFlagValuePairs(curlCommand)

        // Then
        assertEquals(expected.size, actual.size, "The number of parsed pairs should match")

        // Print actual results for debugging
        println("Actual parsed results:")
        actual.forEach { (flag, value) -> println("[$flag] = $value") }

        // Assert each expected pair is in the actual result
        expected.forEachIndexed { index, (expectedFlag, expectedValue) ->
            val (actualFlag, actualValue) = actual[index]
            assertEquals(expectedFlag, actualFlag, "Flag at index $index should match")
            assertEquals(expectedValue, actualValue, "Value for flag '$expectedFlag' should match")
        }
    }

    @Test
    fun testParseCurlToFlagValuePairsWithComplexCommand() {
        // Given
        val curlCommand = """
            curl -X POST "https://api.example.com/v1/resource" \
            -H "Authorization: Bearer token123" \
            -H "Content-Type: application/json" \
            -d '{"key1":"value1","key2":"value2"}' \
            -v \
            --compressed
        """.trimIndent()

        // Expected output
        val expected = listOf(
            "1" to "curl",
            "-X" to "POST",
            "2" to "\"https://api.example.com/v1/resource\"",
            "-H" to "\"Authorization: Bearer token123\"",
            "-H" to "\"Content-Type: application/json\"",
            "-d" to """'{"key1":"value1","key2":"value2"}'""",
            "-v" to "",
            "--compressed" to ""
        )

        // When
        val parser = CurlParser()
        val actual = parser.parseCurlToFlagValuePairs(curlCommand)

        // Then
        assertEquals(expected.size, actual.size, "The number of parsed pairs should match")

        // Assert each expected pair is in the actual result
        expected.forEachIndexed { index, (expectedFlag, expectedValue) ->
            val (actualFlag, actualValue) = actual[index]
            assertEquals(expectedFlag, actualFlag, "Flag at index $index should match")
            assertEquals(expectedValue, actualValue, "Value for flag '$expectedFlag' should match")
        }
    }
}