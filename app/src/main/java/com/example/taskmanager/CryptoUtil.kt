package com.example.taskmanager

import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {

    private const val KEY_ALGORITHM = "AES"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 10000
    private val SALT = "TaskManager_AES_2026".toByteArray()

    private var secretKey: SecretKey? = null

    private fun getKey(): SecretKey {
        secretKey?.let { return it }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User is not authenticated")

        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(userId.toCharArray(), SALT, PBKDF2_ITERATIONS, 256)
        val tmp = factory.generateSecret(spec)
        val key = SecretKeySpec(tmp.encoded, KEY_ALGORITHM)
        secretKey = key
        return key
    }

    fun encrypt(plainText: String): String {
        val key = getKey()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(GCM_IV_LENGTH + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, 0, combined, GCM_IV_LENGTH,
            encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String): String {
        val key = getKey()
        val combined = Base64.decode(encryptedText, Base64.DEFAULT)

        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
