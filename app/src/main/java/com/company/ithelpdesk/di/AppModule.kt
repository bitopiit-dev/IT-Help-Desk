// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.di

import com.company.ithelpdesk.data.AuthRepositoryImpl
import com.company.ithelpdesk.data.NotificationRepositoryImpl
import com.company.ithelpdesk.data.SettingsRepositoryImpl
import com.company.ithelpdesk.data.TicketRepositoryImpl
import com.company.ithelpdesk.data.UserRepositoryImpl
import com.company.ithelpdesk.domain.repository.AuthRepository
import com.company.ithelpdesk.domain.repository.NotificationRepository
import com.company.ithelpdesk.domain.repository.SettingsRepository
import com.company.ithelpdesk.domain.repository.TicketRepository
import com.company.ithelpdesk.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Production Firebase only (no emulator, no LAN IP), so the app works on any network:
 * office Wi-Fi, home broadband or mobile data. Offline persistence is on by default.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun auth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun authRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun userRepository(impl: UserRepositoryImpl): UserRepository
    @Binds abstract fun ticketRepository(impl: TicketRepositoryImpl): TicketRepository
    @Binds abstract fun settingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun notificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
