package com.keak.petemotions.components
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NeoBrutalistCircleButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFFF66B2),
    borderColor: Color = Color.Black,
    shadowColor: Color = Color.Black,
    borderWith: Dp = 3.dp,
    shadowOffset: Dp = 6.dp,
    onMainClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .offset(x = shadowOffset, y = shadowOffset)
            .background(shadowColor, shape = CircleShape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = -shadowOffset, y = -shadowOffset)
                .background(backgroundColor, shape = CircleShape)
                .border(borderWith, borderColor, shape = CircleShape)
                .clickable { onMainClick() },
            contentAlignment = Alignment.Center
        ) {
            content()

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(28.dp)
                    .background(Color.Black, shape = CircleShape)
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
