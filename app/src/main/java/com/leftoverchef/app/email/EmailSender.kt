package com.leftoverchef.app.email

import com.leftoverchef.app.data.api.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object EmailSender {

    // Saljemo reset kod korisniku preko SendGrid API-ja
    suspend fun sendResetCode(toEmail: String, toName: String, code: String): Boolean =
        withContext(Dispatchers.IO) {
            val key = ApiConfig.SENDGRID_API_KEY
            val json = buildSendGridBody(toEmail, toName, code)
            var conn: HttpURLConnection? = null
            try {
                conn = (URL("https://api.sendgrid.com/v3/mail/send").openConnection()
                        as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout    = 15_000
                    doOutput       = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", "Bearer $key")
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(json) }
                val status = conn.responseCode
                if (status !in 200..299) {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                    Log.e("EmailSender", "SendGrid $status: $error")
                }
                status in 200..299
            } catch (ignored: Exception) {
                Log.e("EmailSender", "Send failed", ignored)
                false
            } finally {
                conn?.disconnect()
            }
        }

    // Formiramo JSON payload sa podacima primaoca, posiljaoca i reset kodom za SendGrid
    private fun buildSendGridBody(toEmail: String, toName: String, code: String): String {
        val safeEmail = toEmail.replace("\"", "")
        val safeName  = toName.replace("\"", "")
        val textBody  = """
            |Your LeftoverChef password reset code is:
            |
            |  $code
            |
            |This code expires in 15 minutes.
            |
            |If you did not request a password reset, you can safely ignore this email.
        """.trimMargin()

        return """
            {
              "personalizations": [
                { "to": [{ "email": "$safeEmail", "name": "$safeName" }] }
              ],
              "from": {
                "email": "${ApiConfig.SENDGRID_FROM_EMAIL}",
                "name":  "${ApiConfig.SENDGRID_FROM_NAME}"
              },
              "subject": "Your LeftoverChef password reset code",
              "content": [
                { "type": "text/plain", "value": "${textBody.replace("\n", "\\n").replace("\"", "\\\"")}" }
              ]
            }
        """.trimIndent()
    }
}
