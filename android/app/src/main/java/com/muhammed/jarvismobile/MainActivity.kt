package com.muhammed.jarvismobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JarvisScreen()
                }
            }
        }
    }
}

private data class ChatMessage(val role: String, val content: String)

@Composable
private fun JarvisScreen() {
    var endpoint by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("qwen2.5:3b") }
    var apiKey by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Bağlantı yapılandırılmadı") }
    var busy by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("JARVIS MOBILE", style = MaterialTheme.typography.headlineMedium)
        Text("OpenJarvis bağlantılı kişisel yapay zekâ", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API adresi (ör. http://192.168.1.10:11434/v1)") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.weight(1f),
                label = { Text("Model") },
                singleLine = true
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.weight(1f),
                label = { Text("API anahtarı (isteğe bağlı)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        }
        Text(
            "Güvenlik: API anahtarını yalnızca güvendiğiniz sunucuya gönderin. HTTP bağlantısı şifrelenmez.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        Text(status, style = MaterialTheme.typography.bodySmall)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (message.role == "user") "SİZ" else "JARVIS",
                            style = MaterialTheme.typography.labelMedium)
                        Text(message.content)
                    }
                }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mesajınızı yazın") },
            enabled = !busy
        )
        Button(
            onClick = {
                val prompt = draft.trim()
                val base = endpoint.trim().trimEnd('/')
                if (prompt.isEmpty() || base.isEmpty() || model.isBlank()) {
                    status = "API adresi, model ve mesaj gerekli."
                    return@Button
                }
                messages.add(ChatMessage("user", prompt))
                draft = ""
                busy = true
                status = "Yanıt bekleniyor..."
                scope.launch {
                    try {
                        val answer = withContext(Dispatchers.IO) {
                            requestChat(base, model.trim(), apiKey, messages.toList())
                        }
                        messages.add(ChatMessage("assistant", answer))
                        status = "Bağlantı başarılı"
                    } catch (error: Exception) {
                        status = "Bağlantı hatası: ${error.message ?: "Bilinmeyen hata"}"
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text(if (busy) "Yanıt bekleniyor..." else "Gönder")
        }
        TextButton(onClick = {
            messages.clear()
            status = "Sohbet temizlendi"
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Sohbeti temizle")
        }
    }
}

private fun requestChat(
    baseUrl: String,
    model: String,
    apiKey: String,
    history: List<ChatMessage>
): String {
    val url = URL("$baseUrl/chat/completions")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 90_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
    }
    try {
        val messageArray = JSONArray()
        history.forEach { item ->
            messageArray.put(JSONObject().put("role", item.role).put("content", item.content))
        }
        val payload = JSONObject()
            .put("model", model)
            .put("messages", messageArray)
            .put("stream", false)
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) error("HTTP $code: ${response.take(300)}")
        val root = JSONObject(response)
        return root.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    } finally {
        connection.disconnect()
    }
}
