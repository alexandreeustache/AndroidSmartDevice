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
import fr.isen.eustache.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class ScanActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val scannedDevices = mutableListOf<ScanResult>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onDeviceFound(result)  // Appel de la méthode onDeviceFound
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLEScan", "Scan failed with error: $errorCode")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                checkPermissionsAndStartScan()
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                checkPermissionsAndStartScan()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            AndroidSmartDeviceTheme {
                BLEScanScreen(
                    enableBluetoothLauncher,
                    requestPermissionLauncher,
                    bluetoothAdapter
                )
            }
        }
    }

    private fun checkPermissionsAndStartScan() {
        if (!bluetoothAdapter.isEnabled) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        if (hasPermissions()) {
            startBleScan { /* Handle scan result */ }
        } else {
            requestPermissionLauncher.launch(getAllPermissionsForBLE())
        }
    }

    private fun getAllPermissionsForBLE(): Array<String> {
        val basePermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> basePermissions + arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> basePermissions + Manifest.permission.ACCESS_BACKGROUND_LOCATION
            else -> basePermissions
        }
    }

    private fun hasPermissions(): Boolean =
        getAllPermissionsForBLE().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun startBleScan(onDeviceFound: (ScanResult) -> Unit) {
        if (!hasPermissions()) {
            Log.e("BLEScan", "Permissions not granted")
            return
        }

        try {
            val bleScanner = bluetoothAdapter.bluetoothLeScanner
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scannedDevices.clear()
            bleScanner?.startScan(null, scanSettings, object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    onDeviceFound(result)
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e("BLEScan", "Scan failed with error: $errorCode")
                }
            })
            Toast.makeText(this, "BLE Scan started", Toast.LENGTH_SHORT).show()
        } catch (securityException: SecurityException) {
            Log.e("BLEScan", "Security exception during scan", securityException)
            Toast.makeText(this, "Permission to scan denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopBleScan() {
        try {
            val bleScanner = bluetoothAdapter.bluetoothLeScanner
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bleScanner?.stopScan(scanCallback)  // Utilise scanCallback ici
                Toast.makeText(this, "BLE Scan stopped", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("BLEScan", "Permission BLUETOOTH_CONNECT not granted. Cannot stop scan.")
            }
        } catch (securityException: SecurityException) {
            Log.e("BLEScan", "Security exception while stopping scan", securityException)
        }
    }

    private fun onDeviceFound(result: ScanResult) {
        // Vérifier si la permission BLUETOOTH_CONNECT est accordée
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            // Si la permission est accordée, on peut accéder aux informations du périphérique
            val deviceName = result.device.name ?: "Unknown Device"
            val deviceAddress = result.device.address

            // Ajouter le périphérique à la liste s'il n'est pas déjà présent
            if (scannedDevices.none { it.device.address == result.device.address }) {
                scannedDevices.add(result)
            }

            // Afficher les informations du périphérique
            Log.d("BLEScan", "Device found: $deviceName - $deviceAddress")
        } else {
            // Si la permission n'est pas accordée, afficher un message ou gérer la situation autrement
            Log.e("BLEScan", "Permission BLUETOOTH_CONNECT not granted. Cannot access device information.")
        }
    }

    @Composable
    fun BLEScanScreen(
        enableBluetoothLauncher: ActivityResultLauncher<Intent>,
        requestPermissionLauncher: ActivityResultLauncher<Array<String>>,
        bluetoothAdapter: BluetoothAdapter
    ) {
        val context = LocalContext.current
        var isScanning by remember { mutableStateOf(false) }
        val scannedDevicesState = remember { mutableStateListOf<ScanResult>() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Button(onClick = {
                if (!bluetoothAdapter.isEnabled) {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    if (hasPermissions()) {
                        if (isScanning) {
                            stopBleScan()
                            isScanning = false
                        } else {
                            startBleScan { result ->
                                if (scannedDevicesState.none { it.device.address == result.device.address }) {
                                    scannedDevicesState.add(result)
                                }
                            }
                            isScanning = true
                        }
                    } else {
                        requestPermissionLauncher.launch(getAllPermissionsForBLE())
                    }
                }
            }) {
                Text(text = if (isScanning) "Stop Scan" else "Start Scan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                items(scannedDevicesState) { device ->
                    DeviceItem(device)
                }
            }
        }
    }

    @Composable
    fun DeviceItem(device: ScanResult) {
        val context = LocalContext.current
        val deviceName = if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            device.device.name ?: "Unknown Device"
        } else {
            "Unknown Device"
        }
        val deviceAddress = device.device.address

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Name: $deviceName",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Address: $deviceAddress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
