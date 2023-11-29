package io.silv.manga.composables

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewOverlay(
    base: String,
) {

    val webviewState = rememberWebViewState(url = base)


    Box(modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = webviewState,
            onCreated = { webView ->
                webView.settings.javaScriptEnabled = true
                webView.settings.blockNetworkImage = false
                webView.settings.javaScriptCanOpenWindowsAutomatically = true
                webView.settings.blockNetworkLoads = false
                webView.settings.loadsImagesAutomatically = true
                webView.settings.userAgentString = "Mozilla"
                webView.settings.domStorageEnabled = true },
        )
        if (webviewState.isLoading) {
            CircularProgressIndicator()
        }
    }
}