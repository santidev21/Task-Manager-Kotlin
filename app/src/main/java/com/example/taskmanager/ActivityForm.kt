package com.example.taskmanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ActivityForm : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val taskManager by lazy { TaskManager() }
    private var selectedLocation: LatLng? = null
    private var selectedLocationLabel: String = ""

    private val placeAutocompleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val data = result.data ?: return@registerForActivityResult
                val place = Autocomplete.getPlaceFromIntent(data)
                val latLng = place.location
                if (latLng != null) {
                    selectedLocation = latLng
                    selectedLocationLabel = place.displayName ?: place.formattedAddress.orEmpty()
                    findViewById<TextInputEditText>(R.id.etTaskLocation).setText(selectedLocationLabel)
                    findViewById<TextInputLayout>(R.id.tilTaskLocation).helperText = getString(
                        R.string.task_location_ready,
                        latLng.latitude,
                        latLng.longitude
                    )
                }
            }
            Activity.RESULT_CANCELED -> Unit
            else -> {
                val data = result.data
                val status = if (data != null) Autocomplete.getStatusFromIntent(data) else null
                showMessage(status?.statusMessage ?: getString(R.string.map_location_unavailable))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            openAuthScreen()
            return
        }

        ensurePlacesInitialized()
        setContentView(R.layout.activity_form)

        val tilTaskName = findViewById<TextInputLayout>(R.id.tilTaskName)
        val tilTaskDescription = findViewById<TextInputLayout>(R.id.tilTaskDescription)
        val tilTaskLocation = findViewById<TextInputLayout>(R.id.tilTaskLocation)
        val etTaskName = findViewById<TextInputEditText>(R.id.etTaskName)
        val etTaskDescription = findViewById<TextInputEditText>(R.id.etTaskDescription)
        val etTaskLocation = findViewById<TextInputEditText>(R.id.etTaskLocation)
        val btnSearchLocation = findViewById<MaterialButton>(R.id.btnSearchLocation)
        val btnSaveTask = findViewById<MaterialButton>(R.id.btnSaveTask)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        etTaskLocation.setOnClickListener { openPlaceAutocomplete() }
        btnSearchLocation.setOnClickListener { openPlaceAutocomplete() }

        btnSaveTask.setOnClickListener {
            tilTaskName.error = null
            tilTaskDescription.error = null
            tilTaskLocation.error = null

            val taskName = etTaskName.text?.toString().orEmpty().trim()
            val taskDescription = etTaskDescription.text?.toString().orEmpty().trim()

            if (taskName.isBlank()) {
                tilTaskName.error = getString(R.string.error_task_name_required)
                return@setOnClickListener
            }

            val task = Task(
                name = taskName,
                description = taskDescription,
                locationName = selectedLocationLabel,
                latitude = selectedLocation?.latitude ?: 0.0,
                longitude = selectedLocation?.longitude ?: 0.0
            )

            taskManager.saveTask(
                task = task,
                onSuccess = {
                    showMessage(getString(R.string.task_saved))
                    finish()
                },
                onError = { exception ->
                    showMessage(exception.message ?: getString(R.string.error_saving_task))
                }
            )
        }

        btnBack.setOnClickListener { finish() }

        tilTaskLocation.helperText = getString(R.string.task_location_hint)
        etTaskLocation.isFocusable = false
        etTaskLocation.isClickable = true
        etTaskLocation.isCursorVisible = false
    }

    private fun ensurePlacesInitialized() {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, BuildConfig.PLACES_API_KEY)
        }
    }

    private fun openPlaceAutocomplete() {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION
        )

        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.FULLSCREEN,
            fields
        ).build(this)

        placeAutocompleteLauncher.launch(intent)
    }

    private fun openAuthScreen() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
