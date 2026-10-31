package com.privatejarvis.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.net.URLEncoder
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var status: TextView
    private lateinit var commandInput: EditText

    private var tts: TextToSpeech? = null
    private var kannadaMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(30, 50, 30, 20)
            setBackgroundColor(Color.rgb(5, 12, 28))
        }

        val title = TextView(this).apply {
            text = "J A R V I S"
            textSize = 32f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        layout.addView(title)

        status = TextView(this).apply {
            text = "Jarvis Ready"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(10, 35, 10, 35)
        }

        layout.addView(status)

        val languageButton = Button(this).apply {
            text = "Kannada / English"

            setOnClickListener {
                kannadaMode = !kannadaMode

                status.text = if (kannadaMode) {
                    "ಕನ್ನಡ ಮೋಡ್"
                } else {
                    "English Mode"
                }
            }
        }

        layout.addView(languageButton)

        commandInput = EditText(this).apply {
            hint = "Enter command"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }

        layout.addView(commandInput)

        val executeButton = Button(this).apply {
            text = "Execute Command"

            setOnClickListener {
                executeCommand(commandInput.text.toString())
            }
        }

        layout.addView(executeButton)

        val voiceButton = Button(this).apply {
            text = "🎤 Speak to Jarvis"

            setOnClickListener {
                startVoiceRecognition()
            }
        }

        layout.addView(voiceButton)

        setContentView(layout)
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
        }
    }

    private fun speak(message: String) {
        status.text = message

        tts?.language = if (kannadaMode) {
            Locale("kn", "IN")
        } else {
            Locale.ENGLISH
        }

        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis"
        )
    }

    private fun startVoiceRecognition() {
        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (kannadaMode) "kn-IN" else "en-IN"
            )
        }

        try {
            startActivityForResult(intent, 100)
        } catch (e: Exception) {
            speak("Voice recognition unavailable")
        }
    }

    @Deprecated("Legacy activity result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val command = data
                ?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )
                ?.firstOrNull()
                .orEmpty()

            commandInput.setText(command)
            executeCommand(command)
        }
    }

    private fun executeCommand(command: String) {
        val text = command.trim()
        val lower = text.lowercase(Locale.ROOT)

        when {
            lower.startsWith("youtube") ||
            lower.contains("ಯೂಟ್ಯೂಬ್") -> {

                val search = text.replace(
                    Regex("(?i)youtube|ಯೂಟ್ಯೂಬ್"),
                    ""
                ).trim()

                val url =
                    "https://www.youtube.com/results?search_query=" +
                    URLEncoder.encode(search, "UTF-8")

                openIntent(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    )
                )

                speak("Opening YouTube")
            }

            lower.startsWith("whatsapp") ||
            lower.contains("ವಾಟ್ಸಾಪ್") -> {

                val details = text.replace(
                    Regex("(?i)whatsapp|ವಾಟ್ಸಾಪ್"),
                    ""
                ).trim().split(" ", limit = 2)

                val phone = details.firstOrNull()
                    .orEmpty()
                    .filter { it.isDigit() }

                val message = details.getOrNull(1).orEmpty()

                if (phone.length !in 10..15) {
                    speak("Enter phone number with country code")
                    return
                }

                val url = "https://wa.me/$phone?text=" +
                    URLEncoder.encode(message, "UTF-8")

                openIntent(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    )
                )

                speak("Review message before sending")
            }

            lower.startsWith("call") ||
            lower.startsWith("dial") ||
            lower.contains("ಕರೆ") -> {

                val phone = text.filter {
                    it.isDigit() || it == '+'
                }

                if (phone.length < 10) {
                    speak("Please enter a valid phone number")
                    return
                }

                openIntent(
                    Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:$phone")
                    )
                )

                speak("Review number and tap Call")
            }

            else -> {
                speak(
                    if (kannadaMode) {
                        "ಮಚ್ಚಾ, YouTube, WhatsApp ಅಥವಾ Call ಹೇಳು"
                    } else {
                        "Try YouTube, WhatsApp or Call"
                    }
                )
            }
        }
    }

    private fun openIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            speak("Unable to open application")
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
