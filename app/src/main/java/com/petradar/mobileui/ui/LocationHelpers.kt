package com.petradar.mobileui.ui

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

/** Returns a [GeoPoint] for the given address string, or null if not found. */
internal suspend fun forwardGeocode(context: Context, address: String): GeoPoint? =
    withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val deferred = CompletableDeferred<GeoPoint?>()
                geocoder.getFromLocationName(address, 1) { addresses ->
                    val a = addresses.firstOrNull()
                    deferred.complete(if (a != null) GeoPoint(a.latitude, a.longitude) else null)
                }
                deferred.await()
            } else {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                val a = results?.firstOrNull()
                if (a != null) GeoPoint(a.latitude, a.longitude) else null
            }
        }.getOrNull()
    }

/** Returns a human-readable address for the given coordinates, or empty string if unavailable. */
internal suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String =
    withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val deferred = CompletableDeferred<String>()
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    deferred.complete(addresses.firstOrNull()?.getAddressLine(0) ?: "")
                }
                deferred.await()
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()?.getAddressLine(0) ?: ""
            }
        }.getOrDefault("")
    }

internal fun tryUseCurrentLocation(context: Context, callback: (point: GeoPoint?, errorMessage: String?) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        callback(null, "Se requiere el permiso de ubicación.")
        return
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        @Suppress("MissingPermission")
        runCatching {
            locationManager.getCurrentLocation(
                LocationManager.GPS_PROVIDER,
                null,
                context.mainExecutor
            ) { location ->
                if (location != null) {
                    callback(location.toGeoPoint(), null)
                } else {
                    callback(getLastKnownGeoPoint(context, locationManager), "No se pudo obtener tu ubicación actual.")
                }
            }
        }.getOrElse {
            callback(getLastKnownGeoPoint(context, locationManager), "No se pudo obtener tu ubicación actual.")
        }
        return
    }

    callback(getLastKnownGeoPoint(context, locationManager), "No se pudo obtener tu ubicación actual.")
}

@Suppress("MissingPermission")
private fun getLastKnownGeoPoint(context: Context, locationManager: LocationManager): GeoPoint? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        return null
    }

    if (locationManager.allProviders.isEmpty()) return null

    val bestLocation = locationManager
        .getProviders(true)
        .filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }

    return bestLocation?.toGeoPoint()
}

private fun Location.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
