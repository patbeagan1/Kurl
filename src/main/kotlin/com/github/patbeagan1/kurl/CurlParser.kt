package com.github.patbeagan1.kurl

class CurlParser {
    fun tokenizeCurlCommand(curlCommand: String): List<String> {
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false
        var i = 0

        // Preprocess to remove line continuation characters
        val cleanedInput = curlCommand.replace("\\\n", "").trim()

        while (i < cleanedInput.length) {
            val c = cleanedInput[i]

            when (c) {
                '"' -> {
                    if (!inSingleQuote) {
                        inDoubleQuote = !inDoubleQuote
                        currentToken.append(c)
                    } else {
                        currentToken.append(c)
                    }
                }

                '\'' -> {
                    if (!inDoubleQuote) {
                        inSingleQuote = !inSingleQuote
                        currentToken.append(c)
                    } else {
                        currentToken.append(c)
                    }
                }

                ' ', '\t', '\n' -> {
                    if (inSingleQuote || inDoubleQuote) {
                        currentToken.append(c)
                    } else {
                        if (currentToken.isNotEmpty()) {
                            tokens.add(currentToken.toString())
                            currentToken.clear()
                        }
                    }
                }

                else -> {
                    currentToken.append(c)
                }
            }
            i++
        }

        if (currentToken.isNotEmpty()) {
            tokens.add(currentToken.toString())
        }

        return tokens
    }

    fun parseCurlToFlagValuePairs(curlCommand: String): List<Pair<String, String>> {
        val tokens = tokenizeCurlCommand(curlCommand)
        val result = mutableListOf<Pair<String, String>>()

        var i = 0
        var positionalIndex = 1

        while (i < tokens.size) {
            val token = tokens[i]

            if (token.startsWith("-")) {
                // Handle combined short flags like -sSLk
                if (token.startsWith("-") && !token.startsWith("--") && token.length > 2) {
                    // Split combined flags
                    token.substring(1).forEach { flag ->
                        result.add("-$flag" to "")
                    }
                    i++
                } else {
                    // Regular flag handling
                    val flag = token
                    val value = tokens.getOrNull(i + 1)

                    // Check if the next token is a value (not a flag)
                    if (value != null && !value.startsWith("-")) {
                        result.add(flag to value)
                        i += 2
                    } else {
                        result.add(flag to "") // handle flags without a value
                        i++
                    }
                }
            } else {
                // It's a positional argument
                result.add(positionalIndex.toString() to token)
                positionalIndex++
                i++
            }
        }

        return result
    }
}