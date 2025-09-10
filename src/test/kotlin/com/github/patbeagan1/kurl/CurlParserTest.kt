package com.github.patbeagan1.kurl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.system.measureTimeMillis

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

    @Test
    fun testDirectParsingMethod() {
        // Given
        val curlCommand = """
            curl -X POST "https://api.example.com/v1/resource" \
            -H "Authorization: Bearer token123" \
            -H "Content-Type: application/json" \
            -d '{"key1":"value1","key2":"value2"}' \
            -v \
            --compressed
        """.trimIndent()

        // When
        val parser = CurlParser()
        val standardResult = parser.parseCurlToFlagValuePairs(curlCommand)
        val directResult = parser.parseCurlToFlagValuePairsDirect(curlCommand)

        // Then
        assertEquals(standardResult.size, directResult.size, "Both methods should produce the same number of results")
        
        // Compare results
        standardResult.forEachIndexed { index, (expectedFlag, expectedValue) ->
            val (actualFlag, actualValue) = directResult[index]
            assertEquals(expectedFlag, actualFlag, "Flag at index $index should match between methods")
            assertEquals(expectedValue, actualValue, "Value at index $index should match between methods")
        }
    }

    @Test
    fun testCombinedShortFlags() {
        // Given
        val curlCommand = "curl -sSLk https://example.com"
        
        // When
        val parser = CurlParser()
        val result = parser.parseCurlToFlagValuePairs(curlCommand)
        
        // Then
        assertTrue(result.any { it.first == "-s" && it.second == "" }, "Should contain -s flag")
        assertTrue(result.any { it.first == "-S" && it.second == "" }, "Should contain -S flag")
        assertTrue(result.any { it.first == "-L" && it.second == "" }, "Should contain -L flag")
        assertTrue(result.any { it.first == "-k" && it.second == "" }, "Should contain -k flag")
    }

    @Test
    fun testQuotedValues() {
        // Given
        val curlCommand = """curl -H "Content-Type: application/json" -H 'User-Agent: MyApp/1.0' https://example.com"""
        
        // When
        val parser = CurlParser()
        val result = parser.parseCurlToFlagValuePairs(curlCommand)
        
        // Then
        assertTrue(result.any { it.first == "-H" && it.second == "\"Content-Type: application/json\"" }, 
            "Should preserve double-quoted values")
        assertTrue(result.any { it.first == "-H" && it.second == "'User-Agent: MyApp/1.0'" }, 
            "Should preserve single-quoted values")
    }

    @Test
    fun testPerformanceComparison() {
        // Given - a complex curl command
        val complexCurlCommand = """
            curl -X POST "https://api.example.com/v1/complex/endpoint" \
            -H "Authorization: Bearer very-long-token-here" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -H "User-Agent: MyApp/1.0" \
            -d '{"complex": {"nested": {"data": "with lots of content"}}, "array": [1,2,3,4,5]}' \
            -v \
            --compressed \
            --connect-timeout 30 \
            --max-time 60 \
            -L \
            -k
        """.trimIndent()

        val parser = CurlParser()
        val iterations = 1000

        // Measure standard parsing
        val standardTime = measureTimeMillis {
            repeat(iterations) {
                parser.parseCurlToFlagValuePairs(complexCurlCommand)
            }
        }

        // Measure direct parsing
        val directTime = measureTimeMillis {
            repeat(iterations) {
                parser.parseCurlToFlagValuePairsDirect(complexCurlCommand)
            }
        }

        println("Standard parsing time for $iterations iterations: ${standardTime}ms")
        println("Direct parsing time for $iterations iterations: ${directTime}ms")
        println("Performance improvement: ${((standardTime - directTime).toDouble() / standardTime * 100).toInt()}%")

        // Verify both methods produce the same results
        val standardResult = parser.parseCurlToFlagValuePairs(complexCurlCommand)
        val directResult = parser.parseCurlToFlagValuePairsDirect(complexCurlCommand)
        
        assertEquals(standardResult.size, directResult.size, "Both methods should produce the same number of results")
        
        // Log the performance comparison (performance can vary due to JVM warmup, etc.)
        if (directTime < standardTime) {
            val improvement = ((standardTime - directTime).toDouble() / standardTime * 100).toInt()
            println("Direct parsing is $improvement% faster than standard parsing")
        } else if (standardTime < directTime) {
            val improvement = ((directTime - standardTime).toDouble() / directTime * 100).toInt()
            println("Standard parsing is $improvement% faster than direct parsing")
        } else {
            println("Both parsing methods have similar performance")
        }
        
        // Both methods should produce correct results regardless of performance
        assertTrue(standardResult.isNotEmpty(), "Standard parsing should produce results")
        assertTrue(directResult.isNotEmpty(), "Direct parsing should produce results")
    }

    @Test
    fun testLargeCommandPerformance() {
        // Given - a very large curl command with many flags
        val largeCurlCommand = buildString {
            append("curl -X POST \"https://api.example.com/v1/large/endpoint\" ")
            repeat(50) { i ->
                append("-H \"Header$i: Value$i\" ")
            }
            append("-d '{\"data\": \"")
            repeat(1000) { append("x") }
            append("\"}' ")
            append("-v --compressed --connect-timeout 30 --max-time 60")
        }

        val parser = CurlParser()
        val iterations = 100

        // Measure performance
        val time = measureTimeMillis {
            repeat(iterations) {
                parser.parseCurlToFlagValuePairs(largeCurlCommand)
            }
        }

        println("Large command parsing time for $iterations iterations: ${time}ms")
        println("Average time per parse: ${time / iterations}ms")

        // Verify the result is correct
        val result = parser.parseCurlToFlagValuePairs(largeCurlCommand)
        assertTrue(result.size > 50, "Should parse many flags from large command")
        assertTrue(result.any { it.first == "-X" && it.second == "POST" }, "Should parse method correctly")
    }

    @Test
    fun testEdgeCases() {
        val parser = CurlParser()
        
        // Test empty command
        val emptyResult = parser.parseCurlToFlagValuePairs("")
        assertTrue(emptyResult.isEmpty(), "Empty command should produce empty result")
        
        // Test command with only curl
        val curlOnlyResult = parser.parseCurlToFlagValuePairs("curl")
        assertEquals(1, curlOnlyResult.size, "Should handle curl-only command")
        assertEquals("1" to "curl", curlOnlyResult[0], "Should parse curl as positional argument")
        
        // Test command with only flags
        val flagsOnlyResult = parser.parseCurlToFlagValuePairs("curl -v -L")
        assertEquals(3, flagsOnlyResult.size, "Should handle flags-only command")
        assertTrue(flagsOnlyResult.any { it.first == "-v" }, "Should parse -v flag")
        assertTrue(flagsOnlyResult.any { it.first == "-L" }, "Should parse -L flag")
    }
}