package com.example.taskmanager

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    // Sets up the registration screen and bypasses it for signed-in users.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser != null) {
            openMainScreen()
            return
        }

        setContentView(R.layout.activity_register)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilRegisterEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilRegisterPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val etEmail = findViewById<TextInputEditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etRegisterPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnCreateAccount = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val btnBackToLogin = findViewById<MaterialButton>(R.id.btnBackToLogin)

        btnCreateAccount.setOnClickListener {
            tilEmail.error = null
            tilPassword.error = null
            tilConfirmPassword.error = null

            val email = etEmail.text?.toString().orEmpty().trim()
            val password = etPassword.text?.toString().orEmpty().trim()
            val confirmPassword = etConfirmPassword.text?.toString().orEmpty().trim()

            if (!validateEmail(email, tilEmail) or !validatePassword(password, tilPassword) or !validateConfirmation(password, confirmPassword, tilConfirmPassword)) {
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { openMainScreen() }
                .addOnFailureListener { exception ->
                    showMessage(resolveRegisterError(exception))
                }
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    // Converts Firebase registration failures into readable messages.
    private fun resolveRegisterError(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthUserCollisionException -> getString(R.string.register_error_email_in_use)
            is FirebaseAuthWeakPasswordException -> getString(R.string.register_error_password_short)
            is FirebaseAuthException -> when (exception.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> getString(R.string.register_error_email_in_use)
                "ERROR_WEAK_PASSWORD" -> getString(R.string.register_error_password_short)
                "ERROR_INVALID_EMAIL" -> getString(R.string.auth_error_invalid_email)
                else -> getString(R.string.register_error_failed)
            }
            else -> getString(R.string.register_error_failed)
        }
    }

    // Validates that the email has a proper format and is not empty.
    private fun validateEmail(
        email: String,
        tilEmail: TextInputLayout
    ): Boolean {
        return when {
            email.isBlank() -> {
                tilEmail.error = getString(R.string.auth_error_email_required)
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = getString(R.string.auth_error_invalid_email)
                false
            }
            else -> true
        }
    }

    // Ensures the password meets the minimum Firebase length requirement.
    private fun validatePassword(
        password: String,
        tilPassword: TextInputLayout
    ): Boolean {
        return when {
            password.isBlank() -> {
                tilPassword.error = getString(R.string.auth_error_password_required)
                false
            }
            password.length < 6 -> {
                tilPassword.error = getString(R.string.register_error_password_short)
                false
            }
            else -> true
        }
    }

    // Verifies that both password fields match before account creation.
    private fun validateConfirmation(
        password: String,
        confirmation: String,
        tilConfirmPassword: TextInputLayout
    ): Boolean {
        return if (password != confirmation) {
            tilConfirmPassword.error = getString(R.string.register_error_passwords_mismatch)
            false
        } else {
            true
        }
    }

    // Opens the main screen after a successful registration.
    private fun openMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // Displays a short feedback message at the bottom of the screen.
    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
