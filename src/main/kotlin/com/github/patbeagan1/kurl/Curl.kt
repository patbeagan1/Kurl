package com.github.patbeagan1.kurl

import io.ktor.client.*

fun curl(client: HttpClient, block: CurlDslScope.() -> Unit): CurlDslScope {
    val dsl = CurlDslScope(client)
    dsl.block()
    return dsl
}

fun curl(client: HttpClient, cmd: String): CurlDslScope {
    val parser = CurlParser()
    val flagValuePairs = parser.parseCurlToFlagValuePairs(cmd)

    return curl(client) {
        flagValuePairs.forEach { (flag, value) ->
            applyFlag(flag, value)
        }
    }
}