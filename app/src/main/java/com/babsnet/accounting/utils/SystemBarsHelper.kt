package com.babsnet.accounting.utils

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

object SystemBarsHelper {

    fun applySystemBarsPadding(activity: Activity, root: View) {

        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val initL = root.paddingLeft
        val initT = root.paddingTop
        val initR = root.paddingRight
        val initB = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = initL + bars.left,
                top = initT + bars.top,
                right = initR + bars.right,
                bottom = initB + bars.bottom
            )
            insets
        }

        ViewCompat.requestApplyInsets(root)
    }
}
