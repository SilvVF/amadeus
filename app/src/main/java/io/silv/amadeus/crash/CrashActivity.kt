package io.silv.amadeus.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import io.silv.amadeus.MainActivity

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)

        setContent {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text(exception?.message ?: "")
                Button(onClick = {
                    finishAffinity()
                    startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                }) {
                    Text(text = "restart")
                }
            }
        }
    }
}
