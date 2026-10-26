package com.leftoverchef.app.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AuthManager {

    private const val PREFS_FILE = "leftoverchef_auth"
    private const val KEY_TOKEN  = "jwt_token"

    @Volatile private var cachedPrefs: SharedPreferences? = null

    // Vraca EncryptedSharedPreferences instancu u kojoj se cuva JWT token.

    private fun getPrefs(context: Context): SharedPreferences =
        cachedPrefs ?: synchronized(this) {
            // EncryptedSharedPreferences cuva token bez obicnog plaintext zapisa
            cachedPrefs ?: EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_FILE,
                MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { cachedPrefs = it }
        }

    //Cuvа JWT token nakon uspesne prijave ili registracije korisnika

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).commit()
    }

    fun getToken(context: Context): String? =
        getPrefs(context).getString(KEY_TOKEN, null)

    fun clearToken(context: Context) {
        getPrefs(context).edit().remove(KEY_TOKEN).commit()
    }

    // Proveravamo da li korisnik ima aktivnu lokalnu sesiju

    fun isLoggedIn(context: Context): Boolean {
        val token = getToken(context) ?: return false
        // Sesija vazi samo ako token postoji i nije istekao
        return JwtManager.isTokenValid(token)
    }

    // Izdvajamo username i ID prijavljenog korisnika iz trenutno sacuvanog JWT-a

    fun getLoggedInUsername(context: Context): String? {
        val token = getToken(context) ?: return null
        return JwtManager.getUsernameFromToken(token)
    }

    fun getLoggedInUserId(context: Context): Int? {
        val token = getToken(context) ?: return null
        return JwtManager.getUserIdFromToken(token)
    }
}
