package com.princemaurya.vivaahaverse.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserSession(
    val token: String? = null,
    val name: String? = null,
    val email: String? = null
)

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_session")

class UserPreferences(context: Context) {

    private val dataStore = context.userPreferencesDataStore

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val NAME_KEY = stringPreferencesKey("name")
        private val EMAIL_KEY = stringPreferencesKey("email")
    }

    val sessionFlow: Flow<UserSession> = dataStore.data.map { prefs ->
        UserSession(
            token = prefs[TOKEN_KEY],
            name = prefs[NAME_KEY],
            email = prefs[EMAIL_KEY]
        )
    }

    suspend fun saveSession(token: String, name: String?, email: String) {
        dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[NAME_KEY] = name as String
            prefs[EMAIL_KEY] = email
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(NAME_KEY)
            prefs.remove(EMAIL_KEY)
        }
    }
}

