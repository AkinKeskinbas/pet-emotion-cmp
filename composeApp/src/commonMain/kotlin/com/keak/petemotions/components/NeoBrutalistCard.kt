package com.keak.petemotions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NeoBrutalistCardView(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color.Black,
    shadowColor: Color = Color.Black,
    borderWith: Dp = 3.dp,
    shadowOffset: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.offset(x = shadowOffset, y = shadowOffset)
            .background(shadowColor, RoundedCornerShape(0.dp))
    ) {
        Box(
            modifier = Modifier.matchParentSize().offset(-shadowOffset, -shadowOffset)
                .background(backgroundColor, RoundedCornerShape(0.dp))
                .border(borderWith, borderColor, RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}