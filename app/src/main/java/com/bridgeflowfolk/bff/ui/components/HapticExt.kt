package com.bridgeflowfolk.bff.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Feedback haptique léger (50 ms).
 * Encapsulé dans try/catch : getSystemService peut retourner null ou lever
 * une exception sur certains contextes Compose / appareils (Samsung, Xiaomi…).
 * Le feedback haptique est un bonus — il ne doit jamais crasher l'app.
 */
fun Context.hapticTick() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)?.vibrate(50)
        }
    } catch (e: Exception) {
        // Silencieux : haptique non critique, on ne crashe jamais pour ça
    }
}
