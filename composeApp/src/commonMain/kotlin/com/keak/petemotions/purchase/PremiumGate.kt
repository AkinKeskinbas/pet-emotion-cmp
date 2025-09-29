package com.keak.petemotions.purchase

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject

@Composable
fun PremiumGate(
    premium: @Composable () -> Unit,
    free: @Composable () -> Unit,
    loading: @Composable () -> Unit = { CircularProgressIndicator() },
    error: @Composable (String) -> Unit = { Text("Hata: $it") }
) {
    val presenter: PremiumPresenter = koinInject()
    val state by presenter.viewState.collectAsState()
    when (val s = state) {
        PremiumState.Loading -> loading()
        is PremiumState.Error -> error(s.message)
        PremiumState.Free -> free()
        PremiumState.Premium -> premium()
    }
}