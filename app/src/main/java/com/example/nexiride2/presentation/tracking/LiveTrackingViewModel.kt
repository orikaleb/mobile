package com.example.nexiride2.presentation.tracking

import android.annotation.SuppressLint
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LiveTrackingViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation = _userLocation.asStateFlow()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationCallback != null) return
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    _userLocation.value = LatLng(loc.latitude, loc.longitude)
                }
            }
        }
        locationCallback = cb
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
        } catch (_: SecurityException) {
            locationCallback = null
        }
    }

    override fun onCleared() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        super.onCleared()
    }
}
