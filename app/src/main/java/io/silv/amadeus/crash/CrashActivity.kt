package io.silv.amadeus.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.silv.amadeus.MainActivity
import io.silv.amadeus.R
import io.silv.ui.theme.AmadeusTheme

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)

        exception?.printStackTrace()

        setContent {
            AmadeusTheme {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.kurisu_chibi),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Something went wrong")
                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = {
                            finishAffinity()
                            startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                        }
                    ) {
                        Text(text = "Go back to home screen")
                    }
                }
            }
        }
    }
}
