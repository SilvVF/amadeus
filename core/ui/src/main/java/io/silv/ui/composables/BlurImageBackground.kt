package io.silv.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun BlurImageBackground(modifier: Modifier, url: String, content: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    Box(
        modifier = modifier
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .blur(10.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(screenWidth.dp)
                    .height(screenHeight.dp)
            )
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.8f)
            )
        )
        content()
    }
}