package com.example.core.model

enum class ControlType(val displayName: String) {
    DPAD("D-Pad"),
    LEFT_STICK("Left Analog Stick"),
    RIGHT_STICK("Right Analog Stick"),
    ACTION_CLUSTER("Action Buttons (A/B/X/Y)"),
    L1_BUTTON("L1 Bumper"),
    R1_BUTTON("R1 Bumper"),
    L2_TRIGGER("L2 Trigger"),
    R2_TRIGGER("R2 Trigger"),
    START_BUTTON("Start Button"),
    SELECT_BUTTON("Select Button"),
    LS_BUTTON("LS Button (L3 Click)"),
    RS_BUTTON("RS Button (R3 Click)"),
    XBOX_GUIDE_BUTTON("Xbox Guide Button"),
    SETTINGS_BUTTON("Settings Button")
}

enum class ControlShape(val displayName: String) {
    CIRCLE("Circle"),
    ROUNDED_RECT("Rounded"),
    DIAMOND("Diamond")
}

data class ControlElementConfig(
    val id: String,
    val type: ControlType,
    val xPercent: Float,      // 0.0f..1.0f relative screen offset
    val yPercent: Float,      // 0.0f..1.0f relative screen offset
    val sizeDp: Float = 140f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 0.95f,
    val shape: ControlShape = ControlShape.CIRCLE,
    val colorHex: String = "#00F0FF",
    val isVisible: Boolean = true
)
