package com.carijajan.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CariJajanBottomBar(
    tabAktif: String, // "peta", "lokasi", "tambah", "daftar", "akun"
    sudahLogin: Boolean,
    modifier: Modifier = Modifier,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            tonalElevation = 0.dp
        ) {
            // 1. Peta
            NavigationBarItem(
                selected = (tabAktif == "peta"),
                onClick = { onTabSelected("peta") },
                icon = {
                    Icon(
                        imageVector = if (tabAktif == "peta") Icons.Default.Map else Icons.Outlined.Map,
                        contentDescription = "Peta"
                    )
                },
                label = { Text("Peta", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // 2. Pilih Lokasi & Kota/Daerah
            NavigationBarItem(
                selected = (tabAktif == "lokasi"),
                onClick = { onTabSelected("lokasi") },
                icon = {
                    Icon(
                        imageVector = if (tabAktif == "lokasi") Icons.Default.LocationOn else Icons.Outlined.LocationOn,
                        contentDescription = "Pilih Lokasi & Daerah"
                    )
                },
                label = { Text("Lokasi", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // 3. Tambah Warung
            NavigationBarItem(
                selected = (tabAktif == "tambah"),
                onClick = { onTabSelected("tambah") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = "Tambah Warung",
                        tint = if (tabAktif == "tambah") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        "Tambah",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // 4. List Warung Terdekat
            NavigationBarItem(
                selected = (tabAktif == "daftar"),
                onClick = { onTabSelected("daftar") },
                icon = {
                    Icon(
                        imageVector = if (tabAktif == "daftar") Icons.AutoMirrored.Filled.FormatListBulleted else Icons.AutoMirrored.Outlined.FormatListBulleted,
                        contentDescription = "List Warung Terdekat"
                    )
                },
                label = {
                    Text(
                        "Daftar",
                        fontSize = 11.sp,
                        fontWeight = if (tabAktif == "daftar") FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // 5. Akun
            NavigationBarItem(
                selected = (tabAktif == "akun"),
                onClick = { onTabSelected("akun") },
                icon = {
                    Icon(
                        imageVector = if (tabAktif == "akun" || sudahLogin) Icons.Default.Person else Icons.Outlined.Person,
                        contentDescription = "Akun"
                    )
                },
                label = {
                    Text(
                        "Akun",
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
