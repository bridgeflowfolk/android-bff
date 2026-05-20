package com.bridgeflowfolk.bff.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Feedback haptique léger (50 ms).
 * Confirme l'action sans être intrusif — cohérent avec les guidelines Material3.
 * Compatible Android 8+ (API 26+, minSdk du projet).
 */
fun Context.hapticTick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = getSystemService(VibratorManager::class.java)
        vm?.defaultVibrator?.vibrate(
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)?.vibrate(50)
    }
}
