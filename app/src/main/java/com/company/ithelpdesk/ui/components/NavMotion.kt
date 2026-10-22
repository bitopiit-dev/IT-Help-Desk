// © 2026 Khondaker Arifur Rahman. All Rights Reserved.
// BGL-IT Help — see COPYRIGHT.md. Unauthorized copying, modification or redistribution is prohibited.

package com.company.ithelpdesk.ui.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Screen-to-screen motion for both apps: a short fade with a small push, so opening a ticket
 * shows where you went and Back (including the predictive back gesture) shows where you return.
 */
object NavMotion {
    private const val IN_MS = 240
    private const val OUT_MS = 180

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(IN_MS, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(IN_MS, easing = FastOutSlowInEasing)) { it / 10 }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(OUT_MS))
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(IN_MS))
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(OUT_MS)) + slideOutHorizontally(tween(IN_MS, easing = FastOutSlowInEasing)) { it / 10 }
    }
}
