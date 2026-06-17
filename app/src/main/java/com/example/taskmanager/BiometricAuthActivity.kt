package com.example.taskmanager

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executor

class BiometricAuthActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var btnAuthenticate: MaterialButton
    private var biometricAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_auth)

        btnAuthenticate = findViewById(R.id.btnAuthenticate)
        val tvStatus = findViewById<TextView>(R.id.tvBiometricStatus)

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    openMainScreen()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        showMessage(errString.toString())
                    }
                }
            })

        when (checkBiometricAvailability()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricAvailable = true
                tvStatus.text = getString(R.string.biometric_available)
                btnAuthenticate.isEnabled = true
                btnAuthenticate.setOnClickListener { showBiometricPrompt() }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                tvStatus.text = getString(R.string.biometric_enroll_required)
                btnAuthenticate.isEnabled = true
                btnAuthenticate.text = getString(R.string.biometric_enroll_button)
                btnAuthenticate.setOnClickListener {
                    val enrollIntent = Intent(
                        android.provider.Settings.ACTION_BIOMETRIC_ENROLL
                    ).apply {
                        putExtra(
                            android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                        )
                    }
                    startActivity(enrollIntent)
                }
            }
            else -> {
                tvStatus.text = getString(R.string.biometric_unavailable)
                btnAuthenticate.isEnabled = false
                btnAuthenticate.text = getString(R.string.biometric_unavailable)
                openMainScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (biometricAvailable) {
            showBiometricPrompt()
        }
    }

    private fun checkBiometricAvailability(): Int {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun openMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
