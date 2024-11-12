package fr.isen.eustache.androidsmartdevice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import fr.isen.eustache.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSmartDeviceTheme {
                // Scaffold with padding to avoid content overlap with status bar
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BLEScanScreen(modifier = Modifier.padding(innerPadding), context = this)
                }
            }
        }
    }
}

@Composable
fun BLEScanScreen(modifier: Modifier = Modifier, context: Context) {
    // Content of the screen
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(text = "Scan de BLE", style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp))

        // Description
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cette application vous permet de scanner les périphériques BLE à proximité.",
            style = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
            color = Color.Gray
        )

        // Bluetooth Icon
        Spacer(modifier = Modifier.height(32.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_bluetooth), // Assurez-vous que l'icône Bluetooth est dans res/drawable
            contentDescription = "Bluetooth Icon",
            modifier = Modifier.size(64.dp),
            tint = Color.Blue
        )

        // Scan button
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { startScan(context) }) {
            Text(text = "Lancer le scan")
        }
    }
}

fun startScan(context: Context) {
    val bluetoothManager = ContextCompat.getSystemService(
        context, BluetoothManager::class.java
    ) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
        // Handle Bluetooth not enabled (You can show a message or request enabling Bluetooth)
    } else {
        // Start scanning logic here
    }
}

@Preview(showBackground = true)
@Composable
fun BLEScanScreenPreview() {
    AndroidSmartDeviceTheme {
        BLEScanScreen(context = LocalContext.current)
    }
}
