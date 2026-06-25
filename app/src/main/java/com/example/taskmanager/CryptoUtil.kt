package com.example.taskmanager

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {

    private const val KEY_ALGORITHM = "AES"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val PREFS_NAME = "secure_prefs"

    private var secretKey: SecretKey? = null

    // Retrieves or generates a secure AES key from EncryptedSharedPreferences.
    private fun getKey(context: Context): SecretKey {
        secretKey?.let { return it }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User is not authenticated")

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val prefKey = "aes_key_$userId"
        val existingKeyBase64 = sharedPreferences.getString(prefKey, null)

        val key = if (existingKeyBase64 != null) {
            val keyBytes = Base64.decode(existingKeyBase64, Base64.NO_WRAP)
            SecretKeySpec(keyBytes, KEY_ALGORITHM)
        } else {
            val keyGen = KeyGenerator.getInstance(KEY_ALGORITHM)
            keyGen.init(256, SecureRandom())
            val newKey = keyGen.generateKey()
            val newKeyBase64 = Base64.encodeToString(newKey.encoded, Base64.NO_WRAP)
            sharedPreferences.edit().putString(prefKey, newKeyBase64).apply()
            newKey
        }

        secretKey = key
        return key
    }

    // Encrypts plain text using AES-GCM and returns a Base64 encoded string.
    fun encrypt(context: Context, plainText: String): String {
        val key = getKey(context)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(GCM_IV_LENGTH + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, 0, combined, GCM_IV_LENGTH, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // Decrypts a Base64 encoded AES-GCM string back to plain text.
    fun decrypt(context: Context, encryptedText: String): String {
        val key = getKey(context)
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)

        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
