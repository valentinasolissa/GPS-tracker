package com.example.gps_trackerreceiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import android.util.Log
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

private const val HOME_LAT = 49.1865
private const val HOME_LON = -122.8772
private const val RADIUS_THRESHOLD_METERS = 10000.0 // 10km in meters

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val supabaseUrl = "https://pimqzcefuqisvrcrkxbi.supabase.co/"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBpbXF6Y2VmdXFpc3ZyY3JreGJpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzczODA3ODAsImV4cCI6MjA5Mjk1Njc4MH0.J96-QR8edxUCSrxxyco3TI2f77D9BO8Y4vyf2v-eA8E"

    private val retrofit = Retrofit.Builder()
        .baseUrl(supabaseUrl)
        .addConverterFactory(GsonConverterFactory.create()) // This is the bridge for GSON
        .build()

    private val supabaseApi = retrofit.create(SupabaseApi::class.java) //creates interface object

    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    //Variables for map and pin
    private lateinit var markedMap: GoogleMap
    private var marker: Marker? = null
    private fun sendPushNotification(message: String) {
        val builder = NotificationCompat.Builder(this, "PI_NOTIFY")
            .setSmallIcon(R.drawable.ic_notification) // Ensure you have this icon
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }
    fun fetchLatestLocation() {
        Log.d("TRACKER_DEBUG", "1. Function started")
        // Launching on a background thread (IO) so the UI doesn't freeze
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("TRACKER_DEBUG", "2. Attempting Network Call for VALENTINA-PI-4B")
                val response = supabaseApi.getLatestLocation(
                    apiKey = supabaseKey,
                    bearer = "Bearer $supabaseKey",
                    deviceId = "eq.VALENTINA-PI-4B"
                )

                val latestRow = response.firstOrNull()
                if (latestRow != null) {
                    Log.d("TRACKER_DEBUG", "3. SUCCESS: Found raw payload: ${latestRow.rawPayload}")
                    // 1. Decode the Base64 String back to Binary Bytes
                    val binaryBytes = Base64.decode(latestRow.rawPayload, Base64.DEFAULT)

                    // 2. USE THE PROTOBUF: Turn bytes into an object
                    val currentLocation = DeviceStatus.parseFrom(binaryBytes)
                    Log.d("TRACKER_DEBUG", "4. Protobuf Decoded! Lat: ${currentLocation.latitude}")

                    // 3. Switch back to the Main thread to update the screen
                    withContext(Dispatchers.Main) {
                        println("Parsed Protobuf! Lat: ${currentLocation.latitude}, Lon: ${currentLocation.longitude}")
                        Log.d("TRACKER_DEBUG", "Successfully parsed location for: ${currentLocation.deviceId}")
                        tvLatitude.text = "Latitude: ${currentLocation.latitude}"
                        tvLongitude.text = "Longitude: ${currentLocation.longitude}"
                        //Update the map when data arrives
                        updateMapLocation(currentLocation.latitude, currentLocation.longitude)
                        Log.d("TRACKER_DEBUG", "UI Updated!")
                        if (isFarFromHome(currentLocation.latitude, currentLocation.longitude)) {
                            sendPushNotification("VALENTINA-PI-4B has left the 10km radius!")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TRACKER_DEBUG", "NETWORK FAIL: ${e.message}")
                Log.e("TRACKER_DEBUG", "CAUSE: ${e.cause}")
                e.printStackTrace()
            }
        }
    }

    private fun updateMapLocation(lat: Double, lng: Double) {
        //Check if map is initialized yet
        if(!::markedMap.isInitialized) return

        val currentPosition = LatLng(lat, lng)

        //Remove the old marker (marker?. ensures marker is not null)
        marker?.remove()

        //Add new marker and move camera
        marker = markedMap.addMarker(MarkerOptions().position(currentPosition).title("Pi Location"))
        markedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
    }

    private fun isFarFromHome(currentLat: Double, currentLon: Double): Boolean {
        val homeLocation = Location("Home").apply {
            latitude = HOME_LAT
            longitude = HOME_LON
        }
        val piLocation = Location("Pi").apply {
            latitude = currentLat
            longitude = currentLon
        }
        val distance = homeLocation.distanceTo(piLocation)
        return distance > RADIUS_THRESHOLD_METERS
    }

    private fun createNotificationChannel() {
        val name = "Pi Alerts"
        val descriptionText = "Notifications for Pi movement"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("PI_NOTIFY", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Link the variables to the XML (UI) IDs
        tvLatitude = findViewById(R.id.tv_latitude)
        tvLongitude = findViewById(R.id.tv_longitude)

        //Initialize Map Fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fetchLatestLocation()
    }

    //This runs when the map is ready
    override fun onMapReady(googleMap: GoogleMap) {
        markedMap = googleMap

    }
}