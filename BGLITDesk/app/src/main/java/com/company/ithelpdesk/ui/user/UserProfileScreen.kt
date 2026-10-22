package com.company.ithelpdesk.ui.user

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.ithelpdesk.ui.shared.ProfileContent
import com.company.ithelpdesk.ui.shared.ProfileViewModel

@Composable
fun UserProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    ProfileContent(viewModel)
}
