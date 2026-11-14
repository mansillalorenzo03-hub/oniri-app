package com.kanso.focus.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.focus.data.FocusSessionManager
import com.kanso.focus.ui.theme.BorderZinc
import com.kanso.focus.ui.theme.KansoMinimalTheme
import com.kanso.focus.ui.theme.MutedGray
import com.kanso.focus.ui.theme.PureBlack
import com.kanso.focus.ui.theme.PureWhite
import com.kanso.focus.ui.theme.SurfaceZinc

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FocusSessionManager.init(applicationContext)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Bloquea el botón atrás durante la sesión de enfoque
            }
        })

        setContent {
            KansoMinimalTheme {
                val remainingSeconds by FocusSessionManager.remainingSeconds.collectAsState()
                val activeProfile by FocusSessionManager.activeProfile.collectAsState()
                val isStrict by FocusSessionManager.isStrictMode.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureBlack)
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KANSO · MODO $activeProfile".uppercase(),
                        color = MutedGray,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (remainingSeconds > 0L) {
                            FocusSessionManager.formatRemainingTime(remainingSeconds)
                        } else {
                            "ENFOQUE"
                        },
                        color = PureWhite,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isStrict) {
                            "Esta aplicación está restringida hasta que finalice el temporizador."
                        } else {
                            "Esta aplicación está restringida en tu sesión activa de concentración."
                        },
                        color = MutedGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    OutlinedButton(
                        onClick = { goHome() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderZinc),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceZinc,
                            contentColor = PureWhite
                        )
                    ) {
                        Text(
                            text = "Volver al inicio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
