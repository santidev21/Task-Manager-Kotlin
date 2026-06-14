package com.example.taskmanager

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    // Sets up the login screen and redirects signed-in users to the main area.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser != null) {
            openMainScreen()
            return
        }

        setContentView(R.layout.activity_auth)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)
        val btnGoToRegister = findViewById<MaterialButton>(R.id.btnGoToRegister)

        btnSignIn.setOnClickListener {
            tilEmail.error = null
            tilPassword.error = null

            val email = etEmail.text?.toString().orEmpty().trim()
            val password = etPassword.text?.toString().orEmpty().trim()

            if (!validateEmail(email, tilEmail) or !validatePassword(password, tilPassword)) {
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { openMainScreen() }
                .addOnFailureListener { exception ->
                    showMessage(resolveLoginError(exception))
                }
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Validates the email format and shows the error inline when it is invalid.
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

    // Translates Firebase login errors into messages that are easier to understand.
    private fun resolveLoginError(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> getString(R.string.auth_error_user_not_found)
            is FirebaseAuthInvalidCredentialsException -> getString(R.string.auth_error_invalid_credentials)
            is FirebaseAuthException -> when (exception.errorCode) {
                "ERROR_USER_NOT_FOUND" -> getString(R.string.auth_error_user_not_found)
                "ERROR_WRONG_PASSWORD" -> getString(R.string.auth_error_wrong_password)
                "ERROR_INVALID_EMAIL" -> getString(R.string.auth_error_invalid_email)
                "ERROR_INVALID_LOGIN_CREDENTIALS" -> getString(R.string.auth_error_invalid_credentials)
                else -> getString(R.string.auth_error_failed)
            }
            else -> getString(R.string.auth_error_failed)
        }
    }

    // Checks that the password field is not empty.
    private fun validatePassword(
        password: String,
        tilPassword: TextInputLayout
    ): Boolean {
        return if (password.isBlank()) {
            tilPassword.error = getString(R.string.auth_error_password_required)
            false
        } else {
            true
        }
    }

    // Opens the authenticated area and clears the back stack.
    private fun openMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // Displays a short feedback message at the bottom of the screen.
    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
