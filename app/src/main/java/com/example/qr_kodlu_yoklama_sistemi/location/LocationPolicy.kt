package com.example.qr_kodlu_yoklama_sistemi.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat

data class DemoLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

object LocationPolicy {

    private const val DEMO_ACCEPTANCE_RADIUS_METERS = 2000f

    val demoLocations = listOf(
        DemoLocation("11. Noter Fikirtepe Kadıköy", 40.9932183, 29.0516214),
        DemoLocation("Recep Tayyip Erdoğan Külliyesi", 40.9517048, 29.1384771)
    )

    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun isEmulatorDevice(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val product = Build.PRODUCT.lowercase()

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("emulator") ||
            model.contains("android sdk built for x86") ||
            manufacturer.contains("genymotion") ||
            brand.startsWith("generic") ||
            product.contains("sdk_gphone") ||
            product.contains("emulator")
    }

    fun createLocation(name: String, latitude: Double, longitude: Double): Location {
        return Location(name).apply {
            this.latitude = latitude
            this.longitude = longitude
        }
    }

    fun defaultDemoLocation(): Location {
        val demoLocation = demoLocations.first()
        return createLocation(demoLocation.name, demoLocation.latitude, demoLocation.longitude)
    }

    fun resolveForDevice(location: Location?): Location? {
        if (!isEmulatorDevice()) return location

        if (location != null && isWithinDemoZone(location)) {
            return location
        }

        return defaultDemoLocation()
    }

    fun isWithinDemoZone(location: Location): Boolean {
        return demoLocations.any { demoLocation ->
            val results = FloatArray(1)
            Location.distanceBetween(
                demoLocation.latitude,
                demoLocation.longitude,
                location.latitude,
                location.longitude,
                results
            )
            results[0] <= DEMO_ACCEPTANCE_RADIUS_METERS
        }
    }

    fun describe(location: Location?): String {
        if (location == null) return "-"

        val matchedDemo = demoLocations.firstOrNull { demoLocation ->
            val results = FloatArray(1)
            Location.distanceBetween(
                demoLocation.latitude,
                demoLocation.longitude,
                location.latitude,
                location.longitude,
                results
            )
            results[0] <= DEMO_ACCEPTANCE_RADIUS_METERS
        }

        return matchedDemo?.name ?: String.format(
            java.util.Locale.getDefault(),
            "%.6f, %.6f",
            location.latitude,
            location.longitude
        )
    }
}