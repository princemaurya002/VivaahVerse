package com.princemaurya.vivaahaverse.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.princemaurya.vivaahaverse.data.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val AUTH_DATASTORE_NAME = "auth_preferences"

private val Context.dataStore by preferencesDataStore(name = AUTH_DATASTORE_NAME)

class AuthPreferences private constructor(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val NAME = stringPreferencesKey("name")
    }

    val authSessionFlow: Flow<AuthSession?> =
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val token = preferences[Keys.TOKEN]
                val userId = preferences[Keys.USER_ID]
                val email = preferences[Keys.EMAIL]
                if (token != null && userId != null && email != null) {
                    AuthSession(
                        token = token,
                        userId = userId,
                        email = email,
                        name = preferences[Keys.NAME]
                    )
                } else {
                    null
                }
            }

    suspend fun saveSession(session: AuthSession) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TOKEN] = session.token
            preferences[Keys.USER_ID] = session.userId
            preferences[Keys.EMAIL] = session.email
            session.name?.let { preferences[Keys.NAME] = it } ?: preferences.remove(Keys.NAME)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthPreferences? = null

        fun getInstance(context: Context): AuthPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

