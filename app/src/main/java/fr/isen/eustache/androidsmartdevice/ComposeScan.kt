package fr.isen.eustache.androidsmartdevice

import android.content.Intent
import android.content.pm.PackageManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScan(
    enableBluetoothLauncher: ActivityResultLauncher<Intent>,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>,
    getAllPermissionsForBLE: () -> Array<String> // Ajout de la fonction getAllPermissionsForBLE
) {
    val context = LocalContext.current
    val bluetoothManager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
    val bluetoothAdapter = bluetoothManager?.adapter

    var bluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }
    var bluetoothAvailable by remember { mutableStateOf(bluetoothAdapter != null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Bluetooth Devices") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!bluetoothAvailable) {
                    Text("Bluetooth is not available on this device.", color = MaterialTheme.colorScheme.error)
                } else if (!bluetoothEnabled) {
                    Text("Bluetooth is turned off. Please enable it to scan for devices.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { enableBluetooth(bluetoothAdapter, enableBluetoothLauncher) }) {
                        Text("Enable Bluetooth")
                    }
                } else {
                    Text("Bluetooth is enabled. Ready to scan.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (arePermissionsGranted(context, getAllPermissionsForBLE)) {
                            Toast.makeText(context, "Starting scan...", Toast.LENGTH_SHORT).show()
                            // Add scanning logic here
                        } else {
                            showPermissionRationaleOrRequest(context, getAllPermissionsForBLE(), requestPermissionLauncher)
                        }
                    }) {
                        Text("Start Scan")
                    }
                }
            }
        }
    )
}

private fun enableBluetooth(bluetoothAdapter: BluetoothAdapter?, enableBluetoothLauncher: ActivityResultLauncher<Intent>) {
    bluetoothAdapter?.let {
        if (!it.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }
}

private fun arePermissionsGranted(context: Context, getAllPermissionsForBLE: () -> Array<String>): Boolean {
    return getAllPermissionsForBLE().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

private fun showPermissionRationaleOrRequest(
    context: Context,
    permissionsToRequest: Array<String>,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>
) {
    if (permissionsToRequest.isNotEmpty()) {
        requestPermissionLauncher.launch(permissionsToRequest)
    }
}
