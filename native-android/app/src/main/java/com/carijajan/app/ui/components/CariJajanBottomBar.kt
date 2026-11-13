package com.carijajan.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carijajan.app.ui.theme.BrandMaroon
import com.carijajan.app.ui.theme.BrandOrange
import com.carijajan.app.ui.theme.BrandOrangeDark
import com.carijajan.app.ui.theme.BrandOrangeLight

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
        shadowElevation = 20.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF5EBE6))
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            tonalElevation = 0.dp
        ) {
            // 1. Peta
            val petaSelected = tabAktif == "peta"
            val petaScale by animateFloatAsState(
                targetValue = if (petaSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "petaScale"
            )
            NavigationBarItem(
                selected = petaSelected,
                onClick = { onTabSelected("peta") },
                icon = {
                    Box(modifier = Modifier.scale(petaScale)) {
                        Icon(
                            imageVector = if (petaSelected) Icons.Default.Map else Icons.Outlined.Map,
                            contentDescription = "Peta"
                        )
                    }
                },
                label = { Text("Peta", fontSize = 11.sp, fontWeight = if (petaSelected) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandOrange,
                    selectedTextColor = BrandOrange,
                    indicatorColor = BrandOrangeLight,
                    unselectedIconColor = Color(0xFF78716C),
                    unselectedTextColor = Color(0xFF78716C)
                )
            )

            // 2. Pilih Lokasi & Kota/Daerah
            val lokasiSelected = tabAktif == "lokasi"
            val lokasiScale by animateFloatAsState(
                targetValue = if (lokasiSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "lokasiScale"
            )
            NavigationBarItem(
                selected = lokasiSelected,
                onClick = { onTabSelected("lokasi") },
                icon = {
                    Box(modifier = Modifier.scale(lokasiScale)) {
                        Icon(
                            imageVector = if (lokasiSelected) Icons.Default.LocationOn else Icons.Outlined.LocationOn,
                            contentDescription = "Pilih Lokasi"
                        )
                    }
                },
                label = { Text("Lokasi", fontSize = 11.sp, fontWeight = if (lokasiSelected) FontWeight.Bold else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandOrange,
                    selectedTextColor = BrandOrange,
                    indicatorColor = BrandOrangeLight,
                    unselectedIconColor = Color(0xFF78716C),
                    unselectedTextColor = Color(0xFF78716C)
                )
            )

            // 3. Tambah Warung (Elevated Action Center)
            val tambahSelected = tabAktif == "tambah"
            val tambahScale by animateFloatAsState(
                targetValue = if (tambahSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "tambahScale"
            )
            NavigationBarItem(
                selected = tambahSelected,
                onClick = { onTabSelected("tambah") },
                icon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(tambahScale)
                            .size(38.dp)
                            .shadow(6.dp, CircleShape, spotColor = BrandOrange)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(BrandOrange, BrandOrangeDark)
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Warung",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        "Tambah",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tambahSelected) BrandOrange else Color(0xFF57534E)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = BrandOrange,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color(0xFF78716C)
                )
            )

            // 4. List Warung Terdekat
            val daftarSelected = tabAktif == "daftar"
            val daftarScale by animateFloatAsState(
                targetValue = if (daftarSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "daftarScale"
            )
            NavigationBarItem(
                selected = daftarSelected,
                onClick = { onTabSelected("daftar") },
                icon = {
                    Box(modifier = Modifier.scale(daftarScale)) {
                        Icon(
                            imageVector = if (daftarSelected) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = "Daftar Warung"
                        )
                    }
                },
                label = {
                    Text(
                        "Daftar",
                        fontSize = 11.sp,
                        fontWeight = if (daftarSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandOrange,
                    selectedTextColor = BrandOrange,
                    indicatorColor = BrandOrangeLight,
                    unselectedIconColor = Color(0xFF78716C),
                    unselectedTextColor = Color(0xFF78716C)
                )
            )

            // 5. Akun
            val akunSelected = tabAktif == "akun"
            val akunScale by animateFloatAsState(
                targetValue = if (akunSelected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "akunScale"
            )
            NavigationBarItem(
                selected = akunSelected,
                onClick = { onTabSelected("akun") },
                icon = {
                    Box(modifier = Modifier.scale(akunScale)) {
                        Icon(
                            imageVector = if (akunSelected || sudahLogin) Icons.Default.Person else Icons.Outlined.Person,
                            contentDescription = "Akun"
                        )
                    }
                },
                label = {
                    Text(
                        "Akun",
                        fontSize = 11.sp,
                        fontWeight = if (akunSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandOrange,
                    selectedTextColor = BrandOrange,
                    indicatorColor = BrandOrangeLight,
                    unselectedIconColor = Color(0xFF78716C),
                    unselectedTextColor = Color(0xFF78716C)
                )
            )
        }
    }
}

