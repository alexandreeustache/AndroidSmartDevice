package fr.isen.eustache.androidsmartdevice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class ScanActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val scannedDevices = mutableStateListOf<ScanResult>()
    private var isScanning = false
    private lateinit var handler: Handler
    private lateinit var stopScanRunnable: Runnable

    // Callback to handle the scan results
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onDeviceFound(result)  // Handle the device found
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLEScan", "Scan failed with error: $errorCode")
        }
    }

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Setting up activity result launchers
        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                checkPermissionsAndStartScan()  // Check permissions and start the scan
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                startBleScan()  // Start BLE scan if permissions are granted
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

        handler = Handler()
        stopScanRunnable = Runnable {
            stopBleScan()  // Stop the scan after the timeout
            Toast.makeText(this, "Scan stopped due to timeout", Toast.LENGTH_SHORT).show()
        }

        setContent {
            MaterialTheme {
                BLEScanScreen()  // Set the content of the activity
            }
        }
    }

    // Check if Bluetooth is enabled and start scanning
    private fun checkPermissionsAndStartScan() {
        if (!bluetoothAdapter.isEnabled) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        if (hasPermissions()) {
            startBleScan()  // Start scanning if permissions are granted
        } else {
            requestPermissionLauncher.launch(getRequiredPermissions())  // Request permissions if not granted
        }
    }

    // Get the list of required permissions based on Android version
    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }
        return permissions.toTypedArray()
    }

    // Check if all required permissions are granted
    private fun hasPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Start BLE scanning for nearby devices
    private fun startBleScan() {
        if (!hasPermissions()) {
            Log.e("BLEScan", "Permissions not granted")
            return
        }

        val bleScanner = bluetoothAdapter.bluetoothLeScanner
        if (bleScanner == null) {
            Log.e("BLEScan", "BluetoothLeScanner is null")
            return
        }

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                scannedDevices.clear()  // Clear previous scan results
                bleScanner.startScan(null, scanSettings, scanCallback)  // Start scanning for BLE devices
                isScanning = true
                handler.postDelayed(stopScanRunnable, 30000)  // Stop scan after 30 seconds
                Toast.makeText(this, "BLE Scan started", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("BLEScan", "Permission BLUETOOTH_SCAN not granted")
            }
        } catch (e: SecurityException) {
            Log.e("BLEScan", "Security exception during scan", e)
        }
    }

    // Stop BLE scanning
    private fun stopBleScan() {
        try {
            val bleScanner = bluetoothAdapter.bluetoothLeScanner
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bleScanner?.stopScan(scanCallback)  // Stop scanning
                isScanning = false
                Toast.makeText(this, "BLE Scan stopped", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("BLEScan", "Permission BLUETOOTH_CONNECT not granted. Cannot stop scan.")
            }
        } catch (securityException: SecurityException) {
            Log.e("BLEScan", "Security exception while stopping scan", securityException)
        }
    }

    // Handle device found during scanning
    private fun onDeviceFound(result: ScanResult) {
        if (scannedDevices.none { it.device.address == result.device.address }) {
            scannedDevices.add(result)  // Add the device to the list if it's not already there
        }
    }

    // Composable to display the BLE scanning screen
    @Composable
    fun BLEScanScreen() {
        var isScanningState by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Button(onClick = {
                if (isScanningState) {
                    stopBleScan()  // Stop scanning if already scanning
                } else {
                    checkPermissionsAndStartScan()  // Start scanning if not scanning
                }
                isScanningState = !isScanningState  // Toggle the scanning state
            }) {
                Text(if (isScanningState) "Stop Scan" else "Start Scan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(scannedDevices) { device ->  // Display the list of scanned devices
                    DeviceItem(device)
                }
            }
        }
    }

    // Composable to display individual device information
    @Composable
    fun DeviceItem(device: ScanResult) {
        val context = LocalContext.current
        // Vérification des permissions pour accéder aux informations Bluetooth
        val deviceName = if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            device.device.name ?: "Unknown Device"
        } else {
            "Unknown Device (Permission Denied)"
        }
        val deviceAddress = if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            device.device.address
        } else {
            "Permission Denied"
        }
        val deviceRSSI = device.rssi

        Column(modifier = Modifier.padding(8.dp)) {
            Text("Name: $deviceName")
            Text("Address: $deviceAddress")
            Text("RSSI: $deviceRSSI dBm")
        }
    }

    // Stop scanning if the activity is stopped
    override fun onStop() {
        super.onStop()
        if (isScanning) stopBleScan()
    }

    // Remove the scan timeout handler when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(stopScanRunnable)
    }
}
