package com.thevinesh.squishyrings

import platform.CoreMotion.CMMotionManager
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

internal class IosHapticFeedback : HapticFeedback {
    private val squishGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val popGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)

    init {
        squishGenerator.prepare()
        popGenerator.prepare()
    }

    override fun squish() {
        squishGenerator.impactOccurred()
        squishGenerator.prepare()
    }

    override fun pop() {
        popGenerator.impactOccurred()
        popGenerator.prepare()
    }
}