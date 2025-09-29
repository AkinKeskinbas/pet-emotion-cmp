package com.keak.petemotions.network

import com.keak.petemotions.response.BaseResponse
import com.keak.petemotions.response.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AishouApiImpl : AishouApiService {

    private val client: HttpClient by lazy {
        val json = Json { ignoreUnknownKeys = true }
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 60000  // 60 seconds for the entire request
                connectTimeoutMillis = 20000  // 20 seconds to establish connection
                socketTimeoutMillis = 60000   // 60 seconds for socket timeout
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
                logger = object : Logger {
                    override fun log(message: String) {
                        println("KtorClient: $message")
                    }
                }
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }

            install(ContentNegotiation) {
                json(json, contentType = ContentType.Any)
            }

            defaultRequest {
                url(ApiList.BASE_URL)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
            }
        }
    }

    override suspend fun getToken(): ApiResult<BaseResponse<TokenResponse>> {
        return handleApi {
            client.get(ApiList.GET_TOKEN)
        }
    }
}