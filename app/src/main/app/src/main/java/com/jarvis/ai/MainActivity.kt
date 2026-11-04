
package com.jarvis.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        setContent {
            JarvisScreen(
                onSpeak = { speak(it) },
                onCommand = { handleCommand(it) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    private fun handleCommand(command: String) {
        val text = command.lowercase(Locale.ROOT)

        when {
            "youtube" in text -> openApp(
                "com.google.android.youtube",
                "YouTube"
            )
            "whatsapp" in text -> openApp(
                "com.whatsapp",
                "WhatsApp"
            )
            "time" in text -> speak(
                "The time is ${
                    java.text.SimpleDateFormat(
                        "hh:mm a",
                        Locale.US
                    ).format(java.util.Date())
                }"
            )
            "hello" in text || "hi jarvis" in text ->
                speak("Hello. I am JARVIS. How can I help you?")
            "your name" in text ->
                speak("I am JARVIS AI version 2.0.")
            else -> speak("I heard $command. This command is not configured yet.")
        }
    }

    private fun openApp(packageName: String, appName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            startActivity(launch)
            speak("Opening $appName")
        } else {
            speak("$appName is not installed.")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun JarvisScreen(
    onSpeak: (String) -> Unit,
    onCommand: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var status by remember { mutableStateOf("SYSTEM ONLINE") }
    var lastCommand by remember { mutableStateOf("Awaiting command") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val words = result.data?.getStringArrayListExtra(
            RecognizerIntent.EXTRA_RESULTS
        )
        val command = words?.firstOrNull()

        if (!command.isNullOrBlank()) {
            lastCommand = command
            status = "COMMAND RECEIVED"
            onCommand(command)
        } else {
            status = "NO VOICE DETECTED"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            status = "LISTENING..."
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
            }
            launcher.launch(intent)
        } else {
            status = "MICROPHONE PERMISSION DENIED"
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF020817),
                            Color(0xFF071B35),
                            Color(0xFF020817)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "J.A.R.V.I.S",
                    color = Color(0xFF00D9FF),
                    fontSize = 34.sp
                )

                Text(
                    "PERSONAL AI ASSISTANT",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            status = "LISTENING..."
                            val intent = Intent(
                                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                            ).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE,
                                    "en-IN"
                                )
                            }
                            launcher.launch(intent)
                        } else {
                            permissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    },
                    modifier = Modifier.size(190.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF063B62)
                    )
                ) {
                    Text("TAP TO\nSPEAK", color = Color.White, fontSize = 21.sp)
                }

                Text(status, color = Color(0xFF00E5FF))
                Text(
                    "You said: $lastCommand",
                    color = Color.White,
                    fontSize = 14.sp
                )

                Text(
                    "Say: Hello JARVIS • Open YouTube • Open WhatsApp • Time",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
