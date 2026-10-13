package cz.financer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Červená průhledná páska z levého dolního do pravého horního rohu se žlutým textem „Vyprodáno“.
 * Průhlednost 25 % = neprůhlednost 75 % (alpha 0xBF).
 */
@Composable
fun SoldOutRibbon(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val bandLength = maxWidth * 1.45f
        val bandThickness = maxWidth * 0.2f
        Box(
            Modifier
                .align(Alignment.Center)
                .requiredWidth(bandLength)
                .height(bandThickness)
                .graphicsLayer { rotationZ = -45f }
                .background(Color(0xBFD32F2F)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Vyprodáno",
                color = Color(0xFFFFEB3B),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}
