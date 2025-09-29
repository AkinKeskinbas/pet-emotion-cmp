package com.keak.petemotions.network

import com.keak.petemotions.response.BaseResponse
import com.keak.petemotions.response.TokenResponse


interface AishouApiService {
    suspend fun getToken(): ApiResult<BaseResponse<TokenResponse>>

}