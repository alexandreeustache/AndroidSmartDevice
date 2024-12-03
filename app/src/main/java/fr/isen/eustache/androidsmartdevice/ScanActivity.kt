package fr.isen.eustache.androidsmartdevice

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fr.isen.eustache.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class ScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSmartDeviceTheme {
                BLEScanScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BLEScanScreen() {
    val context = LocalContext.current
    val scanInProgress = remember { mutableStateOf(false) }
    val devicesFound = remember { mutableStateOf<Set<ScanResult>>(emptySet()) }

    // Vérifie si la permission de localisation est accordée
    val locationPermissionGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Vérifie si Bluetooth est activé
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
        Log.e("BLEScan", "Bluetooth n'est pas activé")
        Toast.makeText(context, "Bluetooth n'est pas activé", Toast.LENGTH_SHORT).show()
    }

    val scanner = bluetoothAdapter?.bluetoothLeScanner
    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (locationPermissionGranted) {
                    try {
                        // Vérifie si l'appareil est déjà dans la liste
                        if (!devicesFound.value.any { it.device.address == result.device.address }) {
                            // Ajoute le périphérique trouvé à la liste
                            devicesFound.value = devicesFound.value + result
                            Log.d("BLEScan", "Appareil trouvé: ${result.device.name ?: "Inconnu"} avec RSSI: ${result.rssi}")
                        }
                    } catch (e: SecurityException) {
                        Log.e("BLEScan", "Erreur de permission", e)
                        requestLocationPermission(context)
                    }
                } else {
                    Log.e("BLEScan", "Permission de localisation non accordée")
                    requestLocationPermission(context)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLEScan", "Échec du scan avec le code d'erreur: $errorCode")
                Toast.makeText(context, "Scan failed with error code: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Demander la permission si nécessaire
    if (!locationPermissionGranted) {
        requestLocationPermission(context)
    }

    // Gère l'état du scan
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Scan BLE") },
                actions = {
                    IconButton(onClick = {
                        Log.d("BLEScan", "Bouton cliqué, scanInProgress: ${scanInProgress.value}")
                        if (locationPermissionGranted) {
                            try {
                                if (scanInProgress.value) {
                                    // Arrêter le scan
                                    scanner?.stopScan(scanCallback)
                                    scanInProgress.value = false
                                    Log.d("BLEScan", "Scan arrêté")
                                } else {
                                    // Démarrer le scan
                                    scanner?.startScan(scanCallback)
                                    scanInProgress.value = true
                                    Log.d("BLEScan", "Scan démarré")
                                }
                            } catch (e: SecurityException) {
                                Log.e("BLEScan", "Erreur de permission", e)
                                requestLocationPermission(context)
                            }
                        } else {
                            Log.e("BLEScan", "Permission de localisation non accordée")
                            requestLocationPermission(context)
                        }
                    }) {
                        val icon = if (scanInProgress.value) {
                            R.drawable.ic_pause
                        } else {
                            R.drawable.ic_play
                        }

                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = if (scanInProgress.value) "Pause" else "Play"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val text = if (scanInProgress.value) "Scan BLE en cours..." else "Lancer le Scan BLE"
            Text(text = text, fontSize = 20.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Affichage des appareils trouvés avec LazyColumn pour le défilement
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(devicesFound.value.toList()) { result ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "${result.rssi}",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 8.dp),
                            color = Color.Gray
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Device: ${result.device.name ?: "Inconnu"}", fontSize = 16.sp)
                            Text(text = "Adresse: ${result.device.address}", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}


fun requestLocationPermission(context: Context) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }
}

