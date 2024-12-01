package fr.isen.eustache.androidsmartdevice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.IconButton
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fr.isen.eustache.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme
import android.content.Context
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSmartDeviceTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Si la permission de localisation est accordée, démarrer le scan
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission de localisation accordée !", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission de localisation requise pour le scan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = "home") {
        composable("home") { BLEHomeScreen(navController) }
        composable("scan") { BLEScanScreen() }
    }
}

@Composable
fun BLEHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val locationPermissionGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Scan de BLE",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(130.dp))
        Text(
            text = "Cette application vous permet de scanner les périphériques BLE à proximité.",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(70.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_bluetooth),
            contentDescription = "Bluetooth Icon",
            modifier = Modifier.size(128.dp),
            tint = Color.Blue
        )

        Spacer(modifier = Modifier.height(70.dp))

        Button(
            onClick = { navController.navigate("scan") },
            modifier = Modifier
                .size(width = 200.dp, height = 60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0D47A1),
                contentColor = Color.White
            )
        ) {
            Text(text = "Commencer", fontSize = 18.sp)
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
        Toast.makeText(context, "Bluetooth n'est pas activé", Toast.LENGTH_SHORT).show()
    }

    val scanner = bluetoothAdapter?.bluetoothLeScanner
    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (locationPermissionGranted) {
                    try {
                        // Ajoutez le périphérique trouvé à la liste
                        devicesFound.value = devicesFound.value + result
                        Log.d("BLEScan", "Device found: ${result.device.name ?: "Unknown"} - ${result.device.address}")
                    } catch (e: SecurityException) {
                        Log.e("BLEScan", "Permission manquante pour accéder aux informations du périphérique", e)
                        // Demander la permission à l'utilisateur si nécessaire
                        requestLocationPermission(context)
                    }
                } else {
                    Log.e("BLEScan", "La permission de localisation est requise pour accéder aux périphériques BLE.")
                    // Demander la permission si nécessaire
                    requestLocationPermission(context)
                }
            }


            override fun onScanFailed(errorCode: Int) {
                Log.e("BLEScan", "Scan failed with error code: $errorCode")
            }
        }
    }


    // Demande la permission si nécessaire
    LaunchedEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) {
            requestLocationPermission(context)
        }
    }

    // Gère l'état du scan
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Scan BLE") },
                actions = {
                    IconButton(onClick = {
                        if (locationPermissionGranted) {
                            try {
                                if (scanInProgress.value) {
                                    // Arrêter le scan
                                    scanner?.stopScan(scanCallback) // Utilisation directe de scanCallback sans .value
                                    scanInProgress.value = false
                                } else {
                                    // Démarrer le scan
                                    scanner?.startScan(scanCallback) // Utilisation directe de scanCallback sans .value
                                    scanInProgress.value = true
                                }
                            } catch (e: SecurityException) {
                                Log.e("BLEScan", "Permission manquante pour démarrer le scan BLE", e)
                                requestLocationPermission(context)
                            }
                        } else {
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
                    Text(text = "Device: ${result.device.name ?: "Inconnu"} - ${result.device.address}")
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

