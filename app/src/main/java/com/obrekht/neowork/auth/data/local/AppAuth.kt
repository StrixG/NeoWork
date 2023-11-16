package com.obrekht.neowork.auth.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.obrekht.neowork.auth.model.AuthState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_ID = longPreferencesKey("id")
private val KEY_TOKEN = stringPreferencesKey("token")

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val loggedInState: StateFlow<Boolean> =
        state.map { it.id > 0L }.stateIn(
            scope,
            SharingStarted.Lazily,
            false
        )

    init {
        scope.launch {
            context.dataStore.edit {
                val id = it[KEY_ID] ?: 0L
                val token = it[KEY_TOKEN]
                if (id == 0L || token == null) {
                    it.clear()
                    _state.value = AuthState()
                } else {
                    _state.value = AuthState(id, token)
                }
            }
        }
    }

    fun setAuth(id: Long, token: String) {
        _state.value = AuthState(id, token)
        scope.launch {
            context.dataStore.edit {
                it[KEY_ID] = id
                it[KEY_TOKEN] = token
            }
        }
    }

    fun removeAuth() {
        _state.value = AuthState()
        scope.launch {
            context.dataStore.edit { it.clear() }
        }
    }
}
