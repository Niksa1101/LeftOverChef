package com.leftoverchef.app.auth

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object JwtManager {

    // Local demo sessions only; production authentication belongs on a server.
    private const val SECRET = "leftoverchef-public-demo-key-not-for-production"
    private const val EXPIRY_MS = 7L * 24 * 60 * 60 * 1000

    // Kreiramo JWT token sa user ID-em, username-om, email-om i vremenom isteka
    fun createToken(userId: Int, username: String, email: String): String {
        // Token nosi minimalne podatke potrebne da znamo ko je prijavljen korisnik
        val header = base64Encode("""{"alg":"HS256","typ":"JWT"}""")
        val now = System.currentTimeMillis()
        val exp = now + EXPIRY_MS
        val payload = base64Encode(
            """{"sub":$userId,"username":"$username","email":"$email","iat":$now,"exp":$exp}"""
        )
        val signingInput = "$header.$payload"
        val signature = sign(signingInput)
        return "$signingInput.$signature"
    }

    // Proveravamo da li token ima dobar potpis i da li jos nije istekao
    fun isTokenValid(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            val signingInput = "${parts[0]}.${parts[1]}"
            if (sign(signingInput) != parts[2]) return false

            val exp = extractLong(parts[1], "exp") ?: return false
            System.currentTimeMillis() < exp
        } catch (e: Exception) {
            false
        }
    }

    // Izdvajamo username prijavljenog korisnika iz sacuvanog JWT-a
    fun getUsernameFromToken(token: String): String? {
        val payload = token.split(".").getOrNull(1) ?: return null
        return extractString(payload, "username")
    }

    // Izdvajamo email prijavljenog korisnika iz sacuvanog JWT-a
    fun getEmailFromToken(token: String): String? {
        val payload = token.split(".").getOrNull(1) ?: return null
        return extractString(payload, "email")
    }

    // Izdvajamo ID prijavljenog korisnika iz sacuvanog JWT-a
    fun getUserIdFromToken(token: String): Int? {
        val payload = token.split(".").getOrNull(1) ?: return null
        return extractLong(payload, "sub")?.toInt()
    }

    // Hesiramo lozinku pre cuvanja u bazi i pre poredjenja pri loginu
    fun hashPassword(password: String): String {
        // Ista hash funkcija mora da se koristi i kod registracije i kod logina
        val salt = "LeftoverChef_PwSalt_2024"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return base64Encode(hash)
    }

    private fun base64Encode(text: String): String =
        Base64.encodeToString(
            text.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

    private fun base64Encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun decodePayload(encodedPayload: String): String =
        String(
            Base64.decode(encodedPayload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
            Charsets.UTF_8
        )

    private fun extractString(encodedPayload: String, key: String): String? {
        val json = decodePayload(encodedPayload)
        return Regex(""""$key"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
    }

    private fun extractLong(encodedPayload: String, key: String): Long? {
        val json = decodePayload(encodedPayload)
        return Regex(""""$key"\s*:\s*"?(\d+)"?""").find(json)?.groupValues?.get(1)?.toLong()
    }
}
