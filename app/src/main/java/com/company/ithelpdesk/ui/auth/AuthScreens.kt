// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.ithelpdesk.domain.model.UserProfile
import com.company.ithelpdesk.ui.components.DropdownField
import com.company.ithelpdesk.ui.components.GradientButton
import com.company.ithelpdesk.ui.components.PulsingLogo
import com.company.ithelpdesk.ui.theme.LocalBrand

private enum class AuthPage { LOGIN, REGISTER }

/** Sign-in and sign-up for everyone. Admins use the same login; their role decides which app opens. */
@Composable
fun AuthFlow(viewModel: AuthViewModel = hiltViewModel()) {
    var page by rememberSaveable { mutableStateOf(AuthPage.LOGIN) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(LocalBrand.current.heroBrush)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 28.dp)) {
                LogoMark()
                Spacer(Modifier.height(20.dp))
                Text(
                    if (page == AuthPage.LOGIN) "Hey there 👋" else "Join the crew 🚀",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    if (page == AuthPage.LOGIN) "Sign in to BGL-IT Help and get your IT sorted, fast."
                    else "Sign up with any email. An admin approves you, then you're in.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            // White sheet fills the rest of the screen, down to the very bottom.
            Surface(
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState())) {
                        AnimatedContent(targetState = page, label = "auth") { p ->
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                when (p) {
                                    AuthPage.LOGIN -> LoginForm(viewModel, state, onRegister = { viewModel.clearMessages(); page = AuthPage.REGISTER })
                                    AuthPage.REGISTER -> RegisterForm(viewModel, state, onLogin = { viewModel.clearMessages(); page = AuthPage.LOGIN })
                                }
                            }
                        }
                    }
                    CopyrightWatermark()
                }
            }
        }
    }
}

/** Owner's name pinned to the very bottom as a bold, faded watermark. */
@Composable
private fun CopyrightWatermark() {
    Text(
        "© ${com.company.ithelpdesk.Copyright.YEAR} ${com.company.ithelpdesk.Copyright.OWNER}",
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 14.dp),
    )
}

@Composable
private fun LogoMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.company.ithelpdesk.R.drawable.logo_emblem),
                contentDescription = "BGL-IT Help logo",
                modifier = Modifier.size(42.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text("BGL-IT Help", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("IT Support", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Messages(state: AuthViewModel.UiState) {
    state.error?.let {
        Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium) {
            Text("⚠️  $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp).fillMaxWidth())
        }
    }
    state.info?.let {
        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium) {
            Text("📬  $it", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun LoginForm(vm: AuthViewModel, state: AuthViewModel.UiState, onRegister: () -> Unit) {
    var email by rememberSaveable { mutableStateOf(vm.savedEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(vm.savedRememberMe) }
    var showHistoryMenu by remember { mutableStateOf(false) }
    val history = vm.loginHistory

    Text("Sign in", style = MaterialTheme.typography.headlineSmall)

    Box(modifier = Modifier.fillMaxWidth()) {
        AppTextField(
            value = email,
            onChange = { email = it },
            label = "Email",
            icon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            trailingIcon = if (history.isNotEmpty()) {
                {
                    IconButton(onClick = { showHistoryMenu = !showHistoryMenu }) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Login history",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else null,
        )
        if (history.isNotEmpty()) {
            DropdownMenu(
                expanded = showHistoryMenu,
                onDismissRequest = { showHistoryMenu = false },
                modifier = Modifier.fillMaxWidth(0.85f),
            ) {
                Text(
                    "Login History",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                history.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            email = item
                            showHistoryMenu = false
                        },
                    )
                }
            }
        }
    }

    PasswordField(password, { password = it }, "Password")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { rememberMe = !rememberMe }
                .padding(vertical = 2.dp),
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Text(
                text = "Remember me",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TextButton(onClick = { vm.resetPassword(email) }) {
            Text("Forgot password?")
        }
    }

    Messages(state)

    GradientButton(
        "Let's go",
        onClick = { vm.signIn(email, password, rememberMe) },
        loading = state.loading,
        icon = Icons.AutoMirrored.Filled.Login,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(4.dp))

    OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
        Text("New here? Create an account")
    }
}

@Composable
private fun RegisterForm(vm: AuthViewModel, state: AuthViewModel.UiState, onLogin: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val form = remember { ProfileFormState() }
    val departments by vm.departments.collectAsStateWithLifecycle()

    Text("Create account", style = MaterialTheme.typography.headlineSmall)
    AppTextField(email, { email = it }, "Email (any provider)", Icons.Outlined.Email, KeyboardType.Email)
    PasswordField(password, { password = it }, "Password (6+ characters)")
    PasswordField(confirm, { confirm = it }, "Confirm password")
    Text("About you", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    ProfileFields(form, departments.map { it.name })
    Messages(state)
    GradientButton(
        "Request access",
        onClick = { vm.register(email, password, confirm, form) },
        loading = state.loading,
        icon = Icons.Outlined.RocketLaunch,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Already have an account? Sign in") }
}

/** Name, ID, department, phone and location. Department is a list if admins set one up, otherwise free text. */
@Composable
fun ProfileFields(form: ProfileFormState, departments: List<String>) {
    AppTextField(form.name, { form.name = it }, "Full name", Icons.Outlined.Person)
    AppTextField(form.employeeId, { form.employeeId = it }, "Employee ID (optional)", Icons.Outlined.Badge)
    if (departments.isNotEmpty()) {
        DropdownField("Department", departments, form.department.ifBlank { null }, { it }, { form.department = it }, Modifier.fillMaxWidth())
    } else {
        AppTextField(form.department, { form.department = it }, "Department", Icons.Outlined.Badge)
    }
    AppTextField(form.phone, { form.phone = it }, "Phone", Icons.Outlined.Phone, KeyboardType.Phone)
    AppTextField(form.location, { form.location = it }, "Location / desk (optional)", Icons.Outlined.Place)
}

@Composable
fun AppTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                )
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Signed in but no profile yet (e.g. the profile write failed during sign-up). */
@Composable
fun CompleteProfileScreen(email: String, onSignOut: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val form = remember { ProfileFormState() }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Almost there ✨", style = MaterialTheme.typography.headlineMedium)
        Text("Signed in as $email. Tell us a bit about you so IT knows who's asking.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProfileFields(form, departments.map { it.name })
        Messages(state)
        GradientButton("Send request", onClick = { viewModel.completeProfile(form) }, loading = state.loading,
            modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}

@Composable
fun PendingApprovalScreen(profile: UserProfile, onSignOut: () -> Unit) {
    StatusScreen(
        emoji = "⏳",
        title = "You're on the list!",
        body = "Thanks, ${profile.firstName}. An admin needs to approve your request for ${profile.email}. " +
            "This screen opens the app by itself the moment you're approved — no need to sign in again.",
        onSignOut = onSignOut,
        showPulse = true,
    )
}

@Composable
fun BlockedScreen(onSignOut: () -> Unit) {
    StatusScreen(
        emoji = "🚫",
        title = "Access not available",
        body = "Your request was declined or your account has been turned off. If you think this is a mistake, contact the IT team.",
        onSignOut = onSignOut,
        showPulse = false,
    )
}

@Composable
private fun StatusScreen(emoji: String, title: String, body: String, onSignOut: () -> Unit, showPulse: Boolean) {
    Box(Modifier.fillMaxSize().background(LocalBrand.current.heroBrush)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showPulse) PulsingLogo(88.dp)
            Spacer(Modifier.height(20.dp))
            Text(emoji, fontSize = 52.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            OutlinedButton(onClick = onSignOut, shape = RoundedCornerShape(18.dp)) { Text("Sign out", color = Color.White) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginFormPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var email by remember { mutableStateOf("user@bgl.com") }
                var password by remember { mutableStateOf("password") }
                var rememberMe by remember { mutableStateOf(true) }

                Text("Sign in", style = MaterialTheme.typography.headlineSmall)
                AppTextField(
                    value = email,
                    onChange = { email = it },
                    label = "Email",
                    icon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                )
                PasswordField(password, { password = it }, "Password")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 2.dp),
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TextButton(onClick = { }) {
                        Text("Forgot password?")
                    }
                }
                GradientButton(
                    "Let's go",
                    onClick = { },
                    loading = false,
                    icon = Icons.AutoMirrored.Filled.Login,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
