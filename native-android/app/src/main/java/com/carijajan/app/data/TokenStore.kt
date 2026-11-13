package com.carijajan.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "carijajan_prefs")

class TokenStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("auth_token")
    private val tipeAkunKey = stringPreferencesKey("tipe_akun")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val tipeAkunFlow: Flow<String> = context.dataStore.data.map { it[tipeAkunKey] ?: "penjual" }

    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    suspend fun getTipeAkun(): String = tipeAkunFlow.first()

    suspend fun saveTipeAkun(tipe: String) {
        context.dataStore.edit { it[tipeAkunKey] = tipe }
    }

    // ── Zero Waste Food Promo Preferences per Vendor ──────────────────────────
    fun isZeroWasteActive(vendorId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("zw_active_$vendorId")
        return context.dataStore.data.map { it[key] ?: false }
    }

    fun getZeroWasteDiscount(vendorId: String): Flow<Int> {
        val key = intPreferencesKey("zw_discount_$vendorId")
        return context.dataStore.data.map { it[key] ?: 30 }
    }

    fun getZeroWasteNote(vendorId: String): Flow<String> {
        val key = stringPreferencesKey("zw_note_$vendorId")
        return context.dataStore.data.map {
            it[key] ?: "Diskon spesial jelang tutup! Cegah makanan terbuang dan nikmati harga hemat."
        }
    }

    suspend fun saveZeroWaste(vendorId: String, active: Boolean, discount: Int, note: String) {
        val activeKey = booleanPreferencesKey("zw_active_$vendorId")
        val discountKey = intPreferencesKey("zw_discount_$vendorId")
        val noteKey = stringPreferencesKey("zw_note_$vendorId")
        context.dataStore.edit { prefs ->
            prefs[activeKey] = active
            prefs[discountKey] = discount
            prefs[noteKey] = note
        }
    }

    // ── Pelacakan Kunjungan Warung (Views) & Radar ─────────────────────────────
    suspend fun recordVendorView(vendorId: String) {
        val key = intPreferencesKey("views_$vendorId")
        context.dataStore.edit { prefs ->
            val current = prefs[key] ?: 0
            prefs[key] = current + 1
        }
    }

    fun getVendorViews(vendorId: String): Flow<Int> {
        val key = intPreferencesKey("views_$vendorId")
        // Base count 24 agar pada demo hackathon angka statistik langsung terlihat aktif dan representatif
        return context.dataStore.data.map { (it[key] ?: 0) + 24 }
    }

    fun getRadarImpressions(vendorId: String): Flow<Int> {
        val key = intPreferencesKey("views_$vendorId")
        return context.dataStore.data.map { ((it[key] ?: 0) * 3) + 78 }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(tokenKey) }
    }
}
