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

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        const val EXTRA_TASK_LOCATION_NAME = "extra_task_location_name"
        const val EXTRA_TASK_LATITUDE = "extra_task_latitude"
        const val EXTRA_TASK_LONGITUDE = "extra_task_longitude"
        const val EXTRA_TASK_COMPLETED = "extra_task_completed"
        const val EXTRA_TASK_CREATED_AT = "extra_task_created_at"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val taskManager by lazy { TaskManager() }
    private var selectedLocation: LatLng? = null
    private var selectedLocationLabel: String = ""
    private var editingTaskId: String = ""
    private var editingCompleted: Boolean = false
    private var editingCreatedAt: Long = 0L

    // Handles the Places autocomplete result and stores the selected coordinates.
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
                } else {
                    showMessage(getString(R.string.map_location_unavailable))
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

    // Loads the screen in create or edit mode depending on the intent extras.
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
        val tvFormTitle = findViewById<android.widget.TextView>(R.id.tvFormTitle)
        val tvFormSubtitle = findViewById<android.widget.TextView>(R.id.tvFormSubtitle)

        restoreTaskState(etTaskName, etTaskDescription, etTaskLocation, tilTaskLocation)

        val isEditing = editingTaskId.isNotBlank()
        if (isEditing) {
            tvFormTitle.text = getString(R.string.edit_task)
            tvFormSubtitle.text = getString(R.string.edit_task_subtitle)
            btnSaveTask.text = getString(R.string.update_task)
        }

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
                id = editingTaskId,
                name = taskName,
                description = taskDescription,
                completed = editingCompleted,
                locationName = selectedLocationLabel,
                latitude = selectedLocation?.latitude ?: 0.0,
                longitude = selectedLocation?.longitude ?: 0.0,
                createdAt = if (isEditing && editingCreatedAt != 0L) editingCreatedAt else System.currentTimeMillis()
            )

            if (isEditing) {
                taskManager.updateTask(
                    task = task,
                    onSuccess = {
                        showMessage(getString(R.string.task_updated))
                        finish()
                    },
                    onError = { exception ->
                        showMessage(exception.message ?: getString(R.string.error_updating_task))
                    }
                )
            } else {
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
        }

        btnBack.setOnClickListener { finish() }

        if (selectedLocation == null) {
            tilTaskLocation.helperText = getString(R.string.task_location_hint)
        }
        etTaskLocation.isFocusable = false
        etTaskLocation.isClickable = true
        etTaskLocation.isCursorVisible = false
    }

    // Restores the task data when the form is opened for editing.
    private fun restoreTaskState(
        etTaskName: TextInputEditText,
        etTaskDescription: TextInputEditText,
        etTaskLocation: TextInputEditText,
        tilTaskLocation: TextInputLayout
    ) {
        editingTaskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        editingCompleted = intent.getBooleanExtra(EXTRA_TASK_COMPLETED, false)
        editingCreatedAt = intent.getLongExtra(EXTRA_TASK_CREATED_AT, 0L)

        val taskName = intent.getStringExtra(EXTRA_TASK_NAME).orEmpty()
        val taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION).orEmpty()
        val taskLocationName = intent.getStringExtra(EXTRA_TASK_LOCATION_NAME).orEmpty()
        val taskLatitude = intent.getDoubleExtra(EXTRA_TASK_LATITUDE, 0.0)
        val taskLongitude = intent.getDoubleExtra(EXTRA_TASK_LONGITUDE, 0.0)

        etTaskName.setText(taskName)
        etTaskDescription.setText(taskDescription)

        if (taskLocationName.isNotBlank() && (taskLatitude != 0.0 || taskLongitude != 0.0)) {
            selectedLocation = LatLng(taskLatitude, taskLongitude)
            selectedLocationLabel = taskLocationName
            etTaskLocation.setText(taskLocationName)
            tilTaskLocation.helperText = getString(
                R.string.task_location_ready,
                taskLatitude,
                taskLongitude
            )
        }
    }

    // Initializes the Places SDK once per process.
    private fun ensurePlacesInitialized() {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, BuildConfig.PLACES_API_KEY)
        }
    }

    // Opens the full-screen Places picker so the user selects a valid location.
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

    // Returns to the authentication screen if the session is gone.
    private fun openAuthScreen() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    // Displays a short message at the bottom of the screen.
    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
