package com.example.taskmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val taskManager by lazy { TaskManager() }
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var googleMap: GoogleMap? = null
    private var tasksListener: ValueEventListener? = null
    private var latestTasks: List<Task> = emptyList()
    private var currentLocation: LatLng? = null

    private lateinit var tvMapStatus: TextView

    // Requests location permission and updates the map state based on the result.
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            showCurrentLocation()
        } else {
            currentLocation = null
            renderMap(getString(R.string.map_permission_denied))
        }
    }

    // Sets up the map screen and redirects unauthenticated users away.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            openAuthScreen()
            return
        }

        setContentView(R.layout.activity_map)

        tvMapStatus = findViewById(R.id.tvMapStatus)

        findViewById<Button>(R.id.btnBackMap).setOnClickListener {
            finish()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Starts the Firebase observer so task markers stay in sync.
    override fun onStart() {
        super.onStart()
        observeTasks()
    }

    // Removes the Firebase observer when the activity is no longer visible.
    override fun onStop() {
        removeTasksListener()
        super.onStop()
    }

    // Receives the map instance and triggers the first location check.
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true

        if (hasLocationPermission()) {
            showCurrentLocation()
        } else {
            renderMap(getString(R.string.map_permission_required))
            requestLocationPermission()
        }
    }

    // Subscribes to Firebase task updates so markers can be redrawn.
    private fun observeTasks() {
        removeTasksListener()
        tasksListener = taskManager.observeTasks(
            onTasksLoaded = { tasks ->
                latestTasks = tasks
                renderMap()
            },
            onError = { exception ->
                showMessage(exception.message ?: getString(R.string.error_loading_tasks))
            }
        )
    }

    // Detaches the current Firebase listener to avoid duplicate updates.
    private fun removeTasksListener() {
        tasksListener?.let {
            taskManager.removeTasksListener(it)
            tasksListener = null
        }
    }

    // Launches the runtime prompt for location access.
    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Checks whether the app can safely access the device location.
    private fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    // Reads the last known position and refreshes the map camera and markers.
    private fun showCurrentLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        try {
            googleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) {
        }

        currentLocation = null

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                currentLocation = location?.let {
                    LatLng(it.latitude, it.longitude)
                }
                renderMap(
                    if (location != null) {
                        getString(R.string.map_marker_title)
                    } else {
                        getString(R.string.map_location_unavailable)
                    }
                )
            }
            .addOnFailureListener {
                currentLocation = null
                renderMap(getString(R.string.map_location_unavailable))
            }
    }

    // Resolves encrypted coordinates to a LatLng or null.
    private fun resolveCoordinates(task: Task): LatLng? {
        if (task.encryptedCoords.isNotBlank()) {
            try {
                val parts = CryptoUtil.decrypt(task.encryptedCoords).split(",").map { it.toDouble() }
                if (parts.size == 2 && (parts[0] != 0.0 || parts[1] != 0.0)) {
                    return LatLng(parts[0], parts[1])
                }
            } catch (_: Exception) { }
        }
        return if (task.latitude != 0.0 || task.longitude != 0.0) {
            LatLng(task.latitude, task.longitude)
        } else null
    }

    // Clears and redraws the map with the current location and saved task markers.
    private fun renderMap(statusMessage: String? = null) {
        val map = googleMap ?: return

        map.clear()

        currentLocation?.let { latLng ->
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.map_marker_title))
            )
        }

        val tasksWithCoords = latestTasks.mapNotNull { task ->
            resolveCoordinates(task)?.let { coords -> task to coords }
        }

        tasksWithCoords.forEach { (task, coords) ->
            val decryptedName = try { CryptoUtil.decrypt(task.name) } catch (_: Exception) { task.name }
            val decryptedLocation = if (task.locationName.isNotBlank()) {
                try { CryptoUtil.decrypt(task.locationName) } catch (_: Exception) { task.locationName }
            } else ""
            val decryptedDescription = try { CryptoUtil.decrypt(task.description) } catch (_: Exception) { task.description }
            val snippet = when {
                decryptedLocation.isNotBlank() -> decryptedLocation
                decryptedDescription.isNotBlank() -> decryptedDescription
                else -> getString(R.string.task_location_label)
            }

            map.addMarker(
                MarkerOptions()
                    .position(coords)
                    .title(decryptedName)
                    .snippet(snippet)
            )
        }

        val target = currentLocation
            ?: tasksWithCoords.firstOrNull()?.second
            ?: LatLng(4.7110, -74.0721)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 15f))

        tvMapStatus.text = statusMessage ?: when {
            tasksWithCoords.isNotEmpty() -> getString(R.string.map_tasks_loaded)
            latestTasks.isNotEmpty() -> getString(R.string.map_tasks_no_location)
            currentLocation != null -> getString(R.string.map_marker_title)
            else -> getString(R.string.map_location_unavailable)
        }
    }

    // Returns to the authentication screen if the session is missing.
    private fun openAuthScreen() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    // Displays a short message at the bottom of the screen.
    private fun showMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
