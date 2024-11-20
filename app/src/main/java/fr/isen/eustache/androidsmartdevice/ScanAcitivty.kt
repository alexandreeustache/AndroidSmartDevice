package fr.isen.eustache.androidsmartdevice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.compose.ui.platform.LocalContext

class ScanActivity : AppCompatActivity() {

    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the launcher for enabling Bluetooth
        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Register the launcher for requesting permissions
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                // Now you can proceed to start BLE scan
                startBLEScan()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

        // Set the composable UI
        setContent {
            ScanActivityContent(
                enableBluetoothLauncher = enableBluetoothLauncher,
                requestPermissionLauncher = requestPermissionLauncher
            )
        }
    }

    private fun getAllPermissionsForBLE(): Array<String> {
        var allPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allPermissions = allPermissions.plus(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            allPermissions = allPermissions.plus(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
        return allPermissions
    }

    private fun arePermissionsGranted(): Boolean {
        return getAllPermissionsForBLE().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBLEScan() {
        // Logic to start BLE scan goes here
        Toast.makeText(this, "Starting BLE scan...", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ScanActivityContent(
    enableBluetoothLauncher: ActivityResultLauncher<Intent>,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>
) {
    val context = LocalContext.current // Use LocalContext to access the context

    // Display the "Play" button
    Button(onClick = {
        // Check if permissions are granted, if not, request them
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            // If permissions are granted, start scanning
            Toast.makeText(context, "Starting BLE scan...", Toast.LENGTH_SHORT).show()
        } else {
            // If permissions are not granted, request them
            requestPermissionLauncher.launch(permissions)
        }
    }) {
        Text("Play")
    }
}
