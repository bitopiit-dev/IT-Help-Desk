package com.company.ithelpdesk.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RocketLaunch
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 28.dp)) {
                LogoMark()
                Spacer(Modifier.height(20.dp))
                Text(
                    if (page == AuthPage.LOGIN) "Hey there 👋" else "Join the crew 🚀",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    if (page == AuthPage.LOGIN) "Sign in to BGLITDesk and get your IT sorted, fast."
                    else "Sign up with any email. An admin approves you, then you're in.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Surface(
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = (LocalConfiguration.current.screenHeightDp * 0.72f).dp),
            ) {
                AnimatedContent(targetState = page, label = "auth") { p ->
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (p) {
                            AuthPage.LOGIN -> LoginForm(viewModel, state, onRegister = { viewModel.clearMessages(); page = AuthPage.REGISTER })
                            AuthPage.REGISTER -> RegisterForm(viewModel, state, onLogin = { viewModel.clearMessages(); page = AuthPage.LOGIN })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) { Text("BG", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
        Spacer(Modifier.size(12.dp))
        Column {
            Text("BGLITDesk", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("IT help, minus the hassle", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
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
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Text("Sign in", style = MaterialTheme.typography.headlineSmall)
    AppTextField(email, { email = it }, "Email", Icons.Outlined.Email, KeyboardType.Email)
    PasswordField(password, { password = it }, "Password")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { vm.resetPassword(email) }) { Text("Forgot password?") }
    }
    Messages(state)
    GradientButton(
        "Let's go",
        onClick = { vm.signIn(email, password) },
        loading = state.loading,
        icon = Icons.AutoMirrored.Filled.Login,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
        Text("New here? Create an account")
    }
    Text(
        "Works on any network — office Wi-Fi, home or mobile data.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
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
