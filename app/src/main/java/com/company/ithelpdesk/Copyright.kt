// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk

/** Copyright details shown in the app (About screen, login footer, reports). */
object Copyright {
    const val OWNER = "Khondaker Arifur Rahman"
    const val YEAR = "2026"
    const val EMAIL = "khondaker.arahman@gmail.com"
    const val SHORT = "© $YEAR $OWNER. All Rights Reserved."

    const val NOTICE = """This application, including its original source code, software components, user interface, design, and original content, is protected by applicable intellectual property laws.

Unauthorized copying, reproduction, modification, redistribution, or commercial use of the original materials is prohibited without prior written permission from the rights holder, except as permitted by applicable law.

All rights not expressly granted are reserved.

This notice does not claim ownership of third-party libraries, open-source components, or other materials belonging to their respective owners."""

    /** Third-party components used by the app and their licences. */
    val THIRD_PARTY: List<Pair<String, String>> = listOf(
        "Android Jetpack, Jetpack Compose, Material 3" to "Apache License 2.0 · Google",
        "Kotlin & Kotlin Coroutines" to "Apache License 2.0 · JetBrains",
        "Dagger Hilt" to "Apache License 2.0 · Google",
        "Firebase (Auth, Firestore, Cloud Messaging)" to "Google APIs Terms of Service · Google",
        "Google Code Scanner (ML Kit)" to "Google APIs Terms of Service · Google",
        "Coil" to "Apache License 2.0 · Coil contributors",
        "WorkManager" to "Apache License 2.0 · Google",
        "Sora typeface" to "SIL Open Font License 1.1 · Sora Project Authors",
        "Material Icons" to "Apache License 2.0 · Google",
    )
}
