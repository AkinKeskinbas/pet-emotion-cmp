package com.keak.petemotions.response


import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val status: String? = null,
    val data: T? = null
)

fun <T> BaseResponse<T>.isSuccess(): Boolean {
    return status == "success"
}