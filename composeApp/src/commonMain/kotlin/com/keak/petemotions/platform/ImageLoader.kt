package com.keak.petemotions.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun loadImageFromBytes(bytes: ByteArray): ImageBitmap?

expect fun createPlaceholderImage(): ImageBitmap