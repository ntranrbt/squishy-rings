package com.thevinesh.squishyrings

/** Haptic feedback for the squishy button, injected via a composition local. */
interface HapticFeedback {
    /** Press-down: solid "thock". */
    fun squish()
    /** Release: lighter "pop". */
    fun pop()
}

object NoOpHapticFeedback : HapticFeedback {
    override fun squish() = Unit
    override fun pop() = Unit
}