package com.example.data.repository

import com.example.core.model.ControlElementConfig
import com.example.core.model.ControlShape
import com.example.core.model.ControlType
import com.example.core.model.DefaultProfiles
import com.example.core.model.GamepadProfile
import com.example.core.model.HapticConfig
import com.example.core.model.RgbConfig
import com.example.core.model.RgbEffectMode
import com.example.core.model.StickConfig
import com.example.data.local.ProfileDao
import com.example.data.local.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface ProfileRepository {
    fun getAllProfilesFlow(): Flow<List<GamepadProfile>>
    suspend fun getProfileById(id: String): GamepadProfile?
    suspend fun saveProfile(profile: GamepadProfile)
    suspend fun deleteProfile(id: String)
    suspend fun duplicateProfile(id: String): GamepadProfile?
    suspend fun seedDefaultsIfEmpty()
    suspend fun resetAllToDefaults()
    fun exportProfileJson(profile: GamepadProfile): String
    fun importProfileJson(jsonString: String): Result<GamepadProfile>
}

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao
) : ProfileRepository {

    override fun getAllProfilesFlow(): Flow<List<GamepadProfile>> {
        return profileDao.getAllProfilesFlow().map { entities ->
            entities.mapNotNull { parseProfileJson(it.profileJson) }
        }
    }

    override suspend fun getProfileById(id: String): GamepadProfile? {
        val entity = profileDao.getProfileById(id) ?: return null
        return parseProfileJson(entity.profileJson)
    }

    override suspend fun saveProfile(profile: GamepadProfile) {
        val json = serializeProfile(profile)
        val entity = ProfileEntity(
            id = profile.id,
            name = profile.name,
            targetGame = profile.targetGame,
            profileJson = json,
            isBuiltIn = profile.isBuiltIn,
            updatedAt = System.currentTimeMillis()
        )
        profileDao.insertProfile(entity)
    }

    override suspend fun deleteProfile(id: String) {
        profileDao.deleteById(id)
    }

    override suspend fun duplicateProfile(id: String): GamepadProfile? {
        val original = getProfileById(id) ?: return null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            isBuiltIn = false
        )
        saveProfile(copy)
        return copy
    }

    override suspend fun seedDefaultsIfEmpty() {
        val count = profileDao.getProfileCount()
        if (count == 0) {
            for (defaultProfile in DefaultProfiles.ALL_DEFAULTS) {
                saveProfile(defaultProfile)
            }
        } else {
            if (getProfileById(DefaultProfiles.XBOX.id) == null) {
                saveProfile(DefaultProfiles.XBOX)
            }
        }
    }

    override suspend fun resetAllToDefaults() {
        for (defaultProfile in DefaultProfiles.ALL_DEFAULTS) {
            saveProfile(defaultProfile)
        }
    }

    override fun exportProfileJson(profile: GamepadProfile): String {
        return serializeProfile(profile)
    }

    override fun importProfileJson(jsonString: String): Result<GamepadProfile> {
        return try {
            // Guard against oversized malicious files (> 100 KB)
            if (jsonString.length > 100 * 1024) {
                return Result.failure(IllegalArgumentException("Profile file is too large (max 100KB)"))
            }
            val profile = parseProfileJson(jsonString)
                ?: return Result.failure(IllegalArgumentException("Invalid profile format"))

            // Assign a fresh ID and mark as user-created
            val sanitized = profile.copy(
                id = UUID.randomUUID().toString(),
                name = if (profile.name.isBlank()) "Imported Profile" else profile.name,
                isBuiltIn = false
            )
            Result.success(sanitized)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun serializeProfile(profile: GamepadProfile): String {
            val root = JSONObject()
            root.put("id", profile.id)
            root.put("name", profile.name)
            root.put("targetGame", profile.targetGame)
            root.put("isBuiltIn", profile.isBuiltIn)
            root.put("backgroundTheme", profile.backgroundTheme)
            root.put("backgroundBrightness", profile.backgroundBrightness.toDouble())
            root.put("backgroundOpacity", profile.backgroundOpacity.toDouble())

            // Left Stick
            val ls = JSONObject()
            ls.put("deadzonePercent", profile.leftStickConfig.deadzonePercent.toDouble())
            ls.put("sensitivity", profile.leftStickConfig.sensitivity.toDouble())
            ls.put("isFloating", profile.leftStickConfig.isFloating)
            ls.put("sizeDp", profile.leftStickConfig.sizeDp.toDouble())
            root.put("leftStickConfig", ls)

            // Right Stick
            val rs = JSONObject()
            rs.put("deadzonePercent", profile.rightStickConfig.deadzonePercent.toDouble())
            rs.put("sensitivity", profile.rightStickConfig.sensitivity.toDouble())
            rs.put("isFloating", profile.rightStickConfig.isFloating)
            rs.put("sizeDp", profile.rightStickConfig.sizeDp.toDouble())
            root.put("rightStickConfig", rs)

            // RGB
            val rgb = JSONObject()
            rgb.put("isEnabled", profile.rgbConfig.isEnabled)
            rgb.put("mode", profile.rgbConfig.mode.name)
            rgb.put("primaryColorHex", profile.rgbConfig.primaryColorHex)
            rgb.put("secondaryColorHex", profile.rgbConfig.secondaryColorHex)
            rgb.put("brightness", profile.rgbConfig.brightness.toDouble())
            rgb.put("animationSpeed", profile.rgbConfig.animationSpeed.toDouble())
            rgb.put("glowIntensity", profile.rgbConfig.glowIntensity.toDouble())
            root.put("rgbConfig", rgb)

            // Haptics
            val haptic = JSONObject()
            haptic.put("isEnabled", profile.hapticConfig.isEnabled)
            haptic.put("strengthPercent", profile.hapticConfig.strengthPercent.toDouble())
            root.put("hapticConfig", haptic)

            // Controls
            val controlsArray = JSONArray()
            for (c in profile.controls) {
                val co = JSONObject()
                co.put("id", c.id)
                co.put("type", c.type.name)
                co.put("xPercent", c.xPercent.toDouble())
                co.put("yPercent", c.yPercent.toDouble())
                co.put("sizeDp", c.sizeDp.toDouble())
                co.put("rotationDeg", c.rotationDeg.toDouble())
                co.put("opacity", c.opacity.toDouble())
                co.put("shape", c.shape.name)
                co.put("colorHex", c.colorHex)
                co.put("isVisible", c.isVisible)
                controlsArray.put(co)
            }
            root.put("controls", controlsArray)

            return root.toString(2)
        }

        fun parseProfileJson(jsonString: String): GamepadProfile? {
            return try {
                val root = JSONObject(jsonString)
                val id = root.optString("id", UUID.randomUUID().toString())
                val name = root.optString("name", "Untitled Profile")
                val targetGame = root.optString("targetGame", "")
                val isBuiltIn = root.optBoolean("isBuiltIn", false)
                val bgTheme = root.optString("backgroundTheme", "cyber_grid")
                val bgBrightness = root.optDouble("backgroundBrightness", 0.85).toFloat()
                val bgOpacity = root.optDouble("backgroundOpacity", 0.70).toFloat()

                // Stick Configs
                val lsObj = root.optJSONObject("leftStickConfig")
                val leftStickConfig = if (lsObj != null) {
                    StickConfig(
                        deadzonePercent = lsObj.optDouble("deadzonePercent", 0.12).toFloat().coerceIn(0f, 0.4f),
                        sensitivity = lsObj.optDouble("sensitivity", 1.0).toFloat().coerceIn(0.5f, 2.5f),
                        isFloating = lsObj.optBoolean("isFloating", false),
                        sizeDp = lsObj.optDouble("sizeDp", 160.0).toFloat().coerceIn(100f, 240f)
                    )
                } else StickConfig()

                val rsObj = root.optJSONObject("rightStickConfig")
                val rightStickConfig = if (rsObj != null) {
                    StickConfig(
                        deadzonePercent = rsObj.optDouble("deadzonePercent", 0.12).toFloat().coerceIn(0f, 0.4f),
                        sensitivity = rsObj.optDouble("sensitivity", 1.0).toFloat().coerceIn(0.5f, 2.5f),
                        isFloating = rsObj.optBoolean("isFloating", false),
                        sizeDp = rsObj.optDouble("sizeDp", 160.0).toFloat().coerceIn(100f, 240f)
                    )
                } else StickConfig()

                // RGB
                val rgbObj = root.optJSONObject("rgbConfig")
                val rgbConfig = if (rgbObj != null) {
                    val modeStr = rgbObj.optString("mode", RgbEffectMode.RAINBOW_CYCLE.name)
                    val mode = try { RgbEffectMode.valueOf(modeStr) } catch (_: Exception) { RgbEffectMode.RAINBOW_CYCLE }
                    RgbConfig(
                        isEnabled = rgbObj.optBoolean("isEnabled", true),
                        mode = mode,
                        primaryColorHex = rgbObj.optString("primaryColorHex", "#00F0FF"),
                        secondaryColorHex = rgbObj.optString("secondaryColorHex", "#BD00FF"),
                        brightness = rgbObj.optDouble("brightness", 0.85).toFloat().coerceIn(0.1f, 1.0f),
                        animationSpeed = rgbObj.optDouble("animationSpeed", 1.0).toFloat().coerceIn(0.2f, 3.0f),
                        glowIntensity = rgbObj.optDouble("glowIntensity", 0.75).toFloat().coerceIn(0f, 1.0f)
                    )
                } else RgbConfig()

                // Haptic
                val hapticObj = root.optJSONObject("hapticConfig")
                val hapticConfig = if (hapticObj != null) {
                    HapticConfig(
                        isEnabled = hapticObj.optBoolean("isEnabled", true),
                        strengthPercent = hapticObj.optDouble("strengthPercent", 0.8).toFloat().coerceIn(0.1f, 1.0f)
                    )
                } else HapticConfig()

                // Controls
                val controlsArray = root.optJSONArray("controls")
                val controls = mutableListOf<ControlElementConfig>()
                if (controlsArray != null) {
                    val limit = controlsArray.length().coerceAtMost(30)
                    for (i in 0 until limit) {
                        val co = controlsArray.getJSONObject(i)
                        val cid = co.optString("id", "control_$i")
                        val typeStr = co.optString("type", ControlType.ACTION_CLUSTER.name)
                        val type = try { ControlType.valueOf(typeStr) } catch (_: Exception) { ControlType.ACTION_CLUSTER }
                        val shapeStr = co.optString("shape", ControlShape.CIRCLE.name)
                        val shape = try { ControlShape.valueOf(shapeStr) } catch (_: Exception) { ControlShape.CIRCLE }

                        controls.add(
                            ControlElementConfig(
                                id = cid,
                                type = type,
                                xPercent = co.optDouble("xPercent", 0.5).toFloat().coerceIn(0f, 1f),
                                yPercent = co.optDouble("yPercent", 0.5).toFloat().coerceIn(0f, 1f),
                                sizeDp = co.optDouble("sizeDp", 140.0).toFloat().coerceIn(40f, 300f),
                                rotationDeg = co.optDouble("rotationDeg", 0.0).toFloat().coerceIn(-180f, 180f),
                                opacity = co.optDouble("opacity", 0.9).toFloat().coerceIn(0.1f, 1.0f),
                                shape = shape,
                                colorHex = co.optString("colorHex", "#00F0FF"),
                                isVisible = co.optBoolean("isVisible", true)
                            )
                        )
                    }
                }

                val baseControls = if (controls.isNotEmpty()) controls else DefaultProfiles.XBOX.controls
                val controlsWithSettings = if (baseControls.none { it.type == ControlType.SETTINGS_BUTTON }) {
                    baseControls + ControlElementConfig(
                        id = "settings_btn",
                        type = ControlType.SETTINGS_BUTTON,
                        xPercent = 0.50f,
                        yPercent = 0.48f,
                        sizeDp = 48f,
                        shape = ControlShape.CIRCLE,
                        colorHex = if (id == DefaultProfiles.XBOX.id) "#107C10" else "#00F0FF"
                    )
                } else {
                    baseControls
                }
                // If settings_btn was saved at old top-edge position (< 0.10f), migrate it to central space
                val finalControls = controlsWithSettings.map { ctrl ->
                    if (ctrl.type == ControlType.SETTINGS_BUTTON && ctrl.yPercent < 0.10f) {
                        ctrl.copy(xPercent = 0.50f, yPercent = 0.48f, sizeDp = 48f)
                    } else {
                        ctrl
                    }
                }

                GamepadProfile(
                    id = id,
                    name = name,
                    targetGame = targetGame,
                    controls = finalControls,
                    leftStickConfig = leftStickConfig,
                    rightStickConfig = rightStickConfig,
                    rgbConfig = rgbConfig,
                    hapticConfig = hapticConfig,
                    backgroundTheme = bgTheme,
                    backgroundBrightness = bgBrightness,
                    backgroundOpacity = bgOpacity,
                    isBuiltIn = isBuiltIn
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
