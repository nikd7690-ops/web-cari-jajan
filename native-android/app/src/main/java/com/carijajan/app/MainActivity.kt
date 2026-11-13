package com.carijajan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carijajan.app.ui.screen.BerandaScreen
import com.carijajan.app.ui.screen.DaftarScreen
import com.carijajan.app.ui.screen.DashboardScreen
import com.carijajan.app.ui.screen.EditWarungScreen
import com.carijajan.app.ui.screen.MasukScreen
import com.carijajan.app.ui.screen.WarungDetailScreen
import com.carijajan.app.ui.theme.CariJajanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CariJajanApp

        setContent {
            CariJajanTheme {
                val navController = rememberNavController()
                val container = app.container

                // Pulihkan sesi saat startup
                var sessionChecked by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    container.authRepository.restoreSession()
                    sessionChecked = true
                }

                if (!sessionChecked) return@CariJajanTheme

                NavHost(
                    navController = navController,
                    startDestination = "beranda"
                ) {
                    composable("beranda") {
                        BerandaScreen(
                            container = container,
                            onLogin = { navController.navigate("masuk") },
                            onDashboard = { navController.navigate("dashboard") },
                            onAdd = {
                                if (container.authRepository.user.value != null) {
                                    navController.navigate("edit-warung/new")
                                } else {
                                    navController.navigate("masuk")
                                }
                            },
                            onVendor = { id -> navController.navigate("vendor/$id") }
                        )
                    }

                    composable("masuk") {
                        MasukScreen(
                            container = container,
                            onSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("masuk") { inclusive = true }
                                }
                            },
                            onRegister = { navController.navigate("daftar") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("daftar") {
                        DaftarScreen(
                            container = container,
                            onSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("daftar") { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            container = container,
                            onBack = { navController.popBackStack() },
                            onAdd = { navController.navigate("edit-warung/new") },
                            onEdit = { id -> navController.navigate("edit-warung/$id") },
                            onVendor = { id -> navController.navigate("vendor/$id") },
                            onLogout = {
                                navController.navigate("beranda") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("edit-warung/{vendorId}") { back ->
                        val vendorId = back.arguments?.getString("vendorId") ?: "new"
                        EditWarungScreen(
                            container = container,
                            vendorId = if (vendorId == "new") null else vendorId,
                            onSuccess = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("vendor/{id}") { back ->
                        val id = back.arguments?.getString("id") ?: return@composable
                        WarungDetailScreen(
                            container = container,
                            vendorId = id,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
