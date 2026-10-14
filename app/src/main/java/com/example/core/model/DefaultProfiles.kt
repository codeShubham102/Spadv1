package com.example.core.model

object DefaultProfiles {

    val XBOX = GamepadProfile(
        id = "builtin_xbox",
        name = "Xbox Controller",
        targetGame = "Xbox Cloud Gaming & Local TV",
        isBuiltIn = true,
        backgroundTheme = "dark_minimal",
        rgbConfig = RgbConfig(
            isEnabled = true,
            mode = RgbEffectMode.STATIC,
            primaryColorHex = "#107C10",
            secondaryColorHex = "#52B043",
            brightness = 0.90f
        ),
        controls = listOf(
            ControlElementConfig(
                id = "left_stick",
                type = ControlType.LEFT_STICK,
                xPercent = 0.18f,
                yPercent = 0.40f,
                sizeDp = 135f,
                shape = ControlShape.CIRCLE,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "dpad",
                type = ControlType.DPAD,
                xPercent = 0.28f,
                yPercent = 0.74f,
                sizeDp = 135f,
                shape = ControlShape.CIRCLE,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "ls_btn",
                type = ControlType.LS_BUTTON,
                xPercent = 0.34f,
                yPercent = 0.42f,
                sizeDp = 44f,
                shape = ControlShape.CIRCLE,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "action_cluster",
                type = ControlType.ACTION_CLUSTER,
                xPercent = 0.82f,
                yPercent = 0.40f,
                sizeDp = 145f,
                shape = ControlShape.CIRCLE,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "right_stick",
                type = ControlType.RIGHT_STICK,
                xPercent = 0.72f,
                yPercent = 0.74f,
                sizeDp = 135f,
                shape = ControlShape.CIRCLE,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "rs_btn",
                type = ControlType.RS_BUTTON,
                xPercent = 0.66f,
                yPercent = 0.54f,
                sizeDp = 44f,
                shape = ControlShape.CIRCLE,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "l1_bumper",
                type = ControlType.L1_BUTTON,
                xPercent = 0.22f,
                yPercent = 0.10f,
                sizeDp = 78f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "r1_bumper",
                type = ControlType.R1_BUTTON,
                xPercent = 0.78f,
                yPercent = 0.10f,
                sizeDp = 78f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "l2_trigger",
                type = ControlType.L2_TRIGGER,
                xPercent = 0.08f,
                yPercent = 0.10f,
                sizeDp = 78f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "r2_trigger",
                type = ControlType.R2_TRIGGER,
                xPercent = 0.92f,
                yPercent = 0.10f,
                sizeDp = 78f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "select_btn",
                type = ControlType.SELECT_BUTTON,
                xPercent = 0.42f,
                yPercent = 0.26f,
                sizeDp = 46f,
                shape = ControlShape.CIRCLE,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "start_btn",
                type = ControlType.START_BUTTON,
                xPercent = 0.58f,
                yPercent = 0.26f,
                sizeDp = 46f,
                shape = ControlShape.CIRCLE,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "xbox_guide_btn",
                type = ControlType.XBOX_GUIDE_BUTTON,
                xPercent = 0.50f,
                yPercent = 0.13f,
                sizeDp = 48f,
                shape = ControlShape.CIRCLE,
                colorHex = "#107C10"
            ),
            ControlElementConfig(
                id = "settings_btn",
                type = ControlType.SETTINGS_BUTTON,
                xPercent = 0.50f,
                yPercent = 0.48f,
                sizeDp = 48f,
                shape = ControlShape.CIRCLE,
                colorHex = "#107C10"
            )
        )
    )

    val BALANCED = GamepadProfile(
        id = "builtin_balanced",
        name = "Balanced Layout",
        targetGame = "Universal / TV Navigation",
        isBuiltIn = true,
        backgroundTheme = "cyber_grid",
        rgbConfig = RgbConfig(mode = RgbEffectMode.RAINBOW_CYCLE),
        controls = listOf(
            ControlElementConfig(
                id = "dpad",
                type = ControlType.DPAD,
                xPercent = 0.16f,
                yPercent = 0.52f,
                sizeDp = 145f,
                shape = ControlShape.CIRCLE,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "left_stick",
                type = ControlType.LEFT_STICK,
                xPercent = 0.32f,
                yPercent = 0.74f,
                sizeDp = 130f,
                shape = ControlShape.CIRCLE,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "action_cluster",
                type = ControlType.ACTION_CLUSTER,
                xPercent = 0.84f,
                yPercent = 0.52f,
                sizeDp = 145f,
                shape = ControlShape.CIRCLE,
                colorHex = "#BD00FF"
            ),
            ControlElementConfig(
                id = "right_stick",
                type = ControlType.RIGHT_STICK,
                xPercent = 0.68f,
                yPercent = 0.74f,
                sizeDp = 130f,
                shape = ControlShape.CIRCLE,
                colorHex = "#BD00FF"
            ),
            ControlElementConfig(
                id = "l1_bumper",
                type = ControlType.L1_BUTTON,
                xPercent = 0.22f,
                yPercent = 0.10f,
                sizeDp = 76f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "r1_bumper",
                type = ControlType.R1_BUTTON,
                xPercent = 0.78f,
                yPercent = 0.10f,
                sizeDp = 76f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#BD00FF"
            ),
            ControlElementConfig(
                id = "l2_trigger",
                type = ControlType.L2_TRIGGER,
                xPercent = 0.08f,
                yPercent = 0.10f,
                sizeDp = 76f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "r2_trigger",
                type = ControlType.R2_TRIGGER,
                xPercent = 0.92f,
                yPercent = 0.10f,
                sizeDp = 76f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#BD00FF"
            ),
            ControlElementConfig(
                id = "select_btn",
                type = ControlType.SELECT_BUTTON,
                xPercent = 0.44f,
                yPercent = 0.26f,
                sizeDp = 48f,
                shape = ControlShape.CIRCLE,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "start_btn",
                type = ControlType.START_BUTTON,
                xPercent = 0.56f,
                yPercent = 0.26f,
                sizeDp = 48f,
                shape = ControlShape.CIRCLE,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "ls_btn",
                type = ControlType.LS_BUTTON,
                xPercent = 0.32f,
                yPercent = 0.53f,
                sizeDp = 42f,
                shape = ControlShape.CIRCLE,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "rs_btn",
                type = ControlType.RS_BUTTON,
                xPercent = 0.68f,
                yPercent = 0.53f,
                sizeDp = 42f,
                shape = ControlShape.CIRCLE,
                colorHex = "#BD00FF"
            ),
            ControlElementConfig(
                id = "settings_btn",
                type = ControlType.SETTINGS_BUTTON,
                xPercent = 0.50f,
                yPercent = 0.48f,
                sizeDp = 48f,
                shape = ControlShape.CIRCLE,
                colorHex = "#00F0FF"
            )
        )
    )

    val RACING = GamepadProfile(
        id = "builtin_racing",
        name = "Racing Layout",
        targetGame = "Racing / Driving",
        isBuiltIn = true,
        backgroundTheme = "neon_wave",
        rgbConfig = RgbConfig(mode = RgbEffectMode.NEON_WAVE, primaryColorHex = "#FF007F", secondaryColorHex = "#FFE600"),
        controls = listOf(
            ControlElementConfig(
                id = "left_stick",
                type = ControlType.LEFT_STICK,
                xPercent = 0.22f,
                yPercent = 0.58f,
                sizeDp = 170f,
                shape = ControlShape.CIRCLE,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "l2_trigger",
                type = ControlType.L2_TRIGGER,
                xPercent = 0.10f,
                yPercent = 0.12f,
                sizeDp = 95f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#FF007F"
            ),
            ControlElementConfig(
                id = "r2_trigger",
                type = ControlType.R2_TRIGGER,
                xPercent = 0.90f,
                yPercent = 0.12f,
                sizeDp = 95f,
                shape = ControlShape.ROUNDED_RECT,
                colorHex = "#00FF66"
            ),
            ControlElementConfig(
                id = "action_cluster",
                type = ControlType.ACTION_CLUSTER,
                xPercent = 0.80f,
                yPercent = 0.60f,
                sizeDp = 135f,
                shape = ControlShape.CIRCLE,
                colorHex = "#FFE600"
            ),
            ControlElementConfig(
                id = "select_btn",
                type = ControlType.SELECT_BUTTON,
                xPercent = 0.44f,
                yPercent = 0.25f,
                sizeDp = 48f,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "start_btn",
                type = ControlType.START_BUTTON,
                xPercent = 0.56f,
                yPercent = 0.25f,
                sizeDp = 48f,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "settings_btn",
                type = ControlType.SETTINGS_BUTTON,
                xPercent = 0.50f,
                yPercent = 0.48f,
                sizeDp = 48f,
                shape = ControlShape.CIRCLE,
                colorHex = "#00F0FF"
            )
        )
    )

    val ACTION = GamepadProfile(
        id = "builtin_action",
        name = "Action Layout",
        targetGame = "Shooters & Fighting",
        isBuiltIn = true,
        backgroundTheme = "cyber_grid",
        rgbConfig = RgbConfig(mode = RgbEffectMode.COLOR_PULSE, primaryColorHex = "#FF0055", secondaryColorHex = "#00F0FF"),
        controls = listOf(
            ControlElementConfig(
                id = "left_stick",
                type = ControlType.LEFT_STICK,
                xPercent = 0.18f,
                yPercent = 0.56f,
                sizeDp = 150f,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "right_stick",
                type = ControlType.RIGHT_STICK,
                xPercent = 0.70f,
                yPercent = 0.72f,
                sizeDp = 135f,
                colorHex = "#FF0055"
            ),
            ControlElementConfig(
                id = "action_cluster",
                type = ControlType.ACTION_CLUSTER,
                xPercent = 0.85f,
                yPercent = 0.42f,
                sizeDp = 145f,
                colorHex = "#FFE600"
            ),
            ControlElementConfig(
                id = "l1_bumper",
                type = ControlType.L1_BUTTON,
                xPercent = 0.18f,
                yPercent = 0.10f,
                sizeDp = 76f,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "r1_bumper",
                type = ControlType.R1_BUTTON,
                xPercent = 0.82f,
                yPercent = 0.10f,
                sizeDp = 76f,
                colorHex = "#FF0055"
            ),
            ControlElementConfig(
                id = "start_btn",
                type = ControlType.START_BUTTON,
                xPercent = 0.50f,
                yPercent = 0.24f,
                sizeDp = 48f,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "ls_btn",
                type = ControlType.LS_BUTTON,
                xPercent = 0.32f,
                yPercent = 0.53f,
                sizeDp = 42f,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "rs_btn",
                type = ControlType.RS_BUTTON,
                xPercent = 0.70f,
                yPercent = 0.53f,
                sizeDp = 42f,
                colorHex = "#FF0055"
            ),
            ControlElementConfig(
                id = "settings_btn",
                type = ControlType.SETTINGS_BUTTON,
                xPercent = 0.50f,
                yPercent = 0.48f,
                sizeDp = 48f,
                shape = ControlShape.CIRCLE,
                colorHex = "#00F0FF"
            )
        )
    )

    val MINIMAL = GamepadProfile(
        id = "builtin_minimal",
        name = "Minimal Layout",
        targetGame = "Retro & Simple TV Apps",
        isBuiltIn = true,
        backgroundTheme = "dark_minimal",
        rgbConfig = RgbConfig(mode = RgbEffectMode.STATIC, primaryColorHex = "#00F0FF"),
        controls = listOf(
            ControlElementConfig(
                id = "dpad",
                type = ControlType.DPAD,
                xPercent = 0.18f,
                yPercent = 0.52f,
                sizeDp = 150f,
                colorHex = "#00F0FF"
            ),
            ControlElementConfig(
                id = "action_cluster",
                type = ControlType.ACTION_CLUSTER,
                xPercent = 0.82f,
                yPercent = 0.52f,
                sizeDp = 145f,
                colorHex = "#BD00FF"
            ),
            ControlElementConfig(
                id = "select_btn",
                type = ControlType.SELECT_BUTTON,
                xPercent = 0.44f,
                yPercent = 0.28f,
                sizeDp = 48f,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "start_btn",
                type = ControlType.START_BUTTON,
                xPercent = 0.56f,
                yPercent = 0.28f,
                sizeDp = 48f,
                colorHex = "#94A3B8"
            ),
            ControlElementConfig(
                id = "settings_btn",
                type = ControlType.SETTINGS_BUTTON,
                xPercent = 0.50f,
                yPercent = 0.48f,
                sizeDp = 48f,
                shape = ControlShape.CIRCLE,
                colorHex = "#00F0FF"
            )
        )
    )

    val ALL_DEFAULTS = listOf(XBOX, BALANCED, RACING, ACTION, MINIMAL)
}
