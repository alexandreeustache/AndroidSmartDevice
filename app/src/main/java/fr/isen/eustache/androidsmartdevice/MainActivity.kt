package fr.isen.eustache.androidsmartdevice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.tooling.preview.Preview // Assure-toi d'avoir cet import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.isen.eustache.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

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
}

@OptIn(ExperimentalMaterial3Api::class) // Ajoute cette annotation pour les API Material3 expérimentales
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = "home") {
        composable("home") { BLEHomeScreen(navController) }
        composable("scan") { BLEScanScreen() }
    }
}

@Composable
fun BLEHomeScreen(navController: NavController) {
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
        // Bouton personnalisé
        Button(
            onClick = { navController.navigate("scan") },
            modifier = Modifier
                .size(width = 200.dp, height = 60.dp), // Ajuste la taille du bouton
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0D47A1), // Bleu foncé
                contentColor = Color.White // Texte en blanc
            )
        ) {
            Text(text = "Commencer", fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BLEScanScreen() {
    // Créer un état mutable pour suivre l'état du scan
    val scanInProgress = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Scan BLE") },
                actions = {
                    IconButton(onClick = {
                        // Change l'état en fonction de l'état actuel
                        scanInProgress.value = !scanInProgress.value
                    }) {
                        // Affiche le bon logo selon l'état du scan
                        val icon = if (scanInProgress.value) {
                            R.drawable.ic_pause // Assure-toi d'avoir cet icône dans les ressources
                        } else {
                            R.drawable.ic_play // Assure-toi d'avoir cet icône dans les ressources
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
            // Change le texte selon l'état du scan
            val text = if (scanInProgress.value) "Scan BLE en cours..." else "Lancer le Scan BLE"
            Text(text = text, fontSize = 20.sp)

            // Optionnel : afficher d'autres informations ou UI ici
        }
    }
}

fun startScan(context: Context) {
    val bluetoothManager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
        // Gérer le Bluetooth désactivé
    } else {
        // Logique de scan
    }
}

@Preview(showBackground = true)
@Composable
fun BLEHomeScreenPreview() {
    AndroidSmartDeviceTheme {
        BLEHomeScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun BLEScanScreenPreview() {
    AndroidSmartDeviceTheme {
        BLEScanScreen()
    }
}
