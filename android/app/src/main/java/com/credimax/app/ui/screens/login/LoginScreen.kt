package com.credimax.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextField
import com.credimax.app.CredimaxApp
import com.credimax.app.data.LoginRequest
import com.credimax.app.data.parseApiError
import com.credimax.app.ui.components.CredimaxCard
import com.credimax.app.ui.components.PrimaryButton
import com.credimax.app.ui.theme.SlateMuted
import com.credimax.app.ui.theme.TealDeep
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val app = LocalContext.current.applicationContext as CredimaxApp
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("admin@credimax.pe") }
    var password by remember { mutableStateOf("admin123") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TealDeep),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Credimax", style = MaterialTheme.typography.headlineLarge)
                Text("Microcréditos semanales", color = SlateMuted)
            }
            CredimaxCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    PrimaryButton("Entrar", loading = loading, onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                val res = app.container.api.login(LoginRequest(email.trim(), password))
                                app.container.tokenStore.save(res.token)
                                onLoggedIn()
                            } catch (e: HttpException) {
                                error = parseApiError(app.container.gson, e.response()?.errorBody()?.string())
                            } catch (e: Exception) {
                                error = e.message ?: "No se pudo conectar al servidor"
                            } finally {
                                loading = false
                            }
                        }
                    })
                }
            }
        }
    }
}
