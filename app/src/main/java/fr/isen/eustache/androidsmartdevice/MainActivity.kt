package fr.isen.eustache.androidsmartdevice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.isen.eustache.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme
import androidx.compose.ui.platform.LocalContext


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

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = "home") {
        composable("home") { BLEHomeScreen(navController) }
        composable("scan") {
            // Utiliser Intent pour naviguer vers ScanActivity (qui est une activité Android normale)
            ScanActivityLaunch(navController)
        }
    }
}

@Composable
fun BLEHomeScreen(navController: NavHostController) {
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

@Composable
fun ScanActivityLaunch(navController: NavHostController) {
    // Lancer ScanActivity via Intent, car ScanActivity est une activité Android
    val context = LocalContext.current
    val intent = Intent(context, ScanActivity::class.java)
    context.startActivity(intent)
}
