package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.engine.DeadzoneCalculator
import com.example.core.model.DefaultProfiles
import com.example.core.model.GamepadButton
import com.example.core.model.PacketConstants
import com.example.core.model.SpadInputState
import com.example.core.model.SpadPacketSerializer
import com.example.core.model.StickConfig
import com.example.core.model.StickPosition
import com.example.core.model.TvKeyCommand
import com.example.data.network.samsung.TizenKeyMapper
import com.example.data.repository.ProfileRepositoryImpl
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun read_string_from_context() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("S!pad", appName)
    }

    @Test
    fun deadzone_calculator_filters_small_movement() {
        val config = StickConfig(deadzonePercent = 0.15f, sensitivity = 1.0f)

        // Movement inside deadzone
        val zeroResult = DeadzoneCalculator.processStickInput(0.08f, 0.05f, config)
        assertEquals(0f, zeroResult.x, 0.001f)
        assertEquals(0f, zeroResult.y, 0.001f)

        // Movement outside deadzone
        val activeResult = DeadzoneCalculator.processStickInput(0.80f, 0.0f, config)
        assertTrue(activeResult.x > 0.5f)
        assertEquals(0f, activeResult.y, 0.001f)
    }

    @Test
    fun trigger_deadzone_processing() {
        val deadTrigger = DeadzoneCalculator.processTrigger(0.03f, 0.05f)
        assertEquals(0f, deadTrigger, 0.001f)

        val fullTrigger = DeadzoneCalculator.processTrigger(1.0f, 0.05f)
        assertEquals(1.0f, fullTrigger, 0.001f)
    }

    @Test
    fun packet_serialization_json() {
        val state = SpadInputState(
            sequenceNumber = 42L,
            pressedButtons = setOf(GamepadButton.A, GamepadButton.R1),
            leftStick = StickPosition(0.5f, -0.7f),
            rightTrigger = 0.9f
        )

        val jsonString = SpadPacketSerializer.toJson(state)
        val json = JSONObject(jsonString)

        assertEquals(PacketConstants.PROTOCOL_VERSION.toInt(), json.getInt("version"))
        assertEquals(42L, json.getLong("sequence"))

        val buttons = json.getJSONObject("buttons")
        assertTrue(buttons.getBoolean("a"))
        assertFalse(buttons.getBoolean("b"))
        assertTrue(buttons.getBoolean("r1"))

        val analog = json.getJSONObject("analog")
        val leftStick = analog.getJSONObject("leftStick")
        assertEquals(0.5, leftStick.getDouble("x"), 0.01)
        assertEquals(-0.7, leftStick.getDouble("y"), 0.01)
    }

    @Test
    fun packet_serialization_binary_24_bytes() {
        val state = SpadInputState(
            sequenceNumber = 100L,
            pressedButtons = setOf(GamepadButton.A, GamepadButton.START),
            leftStick = StickPosition(1.0f, -1.0f),
            leftTrigger = 0.5f,
            rightTrigger = 1.0f
        )

        val bytes = SpadPacketSerializer.toBinary(state)
        assertEquals(PacketConstants.PACKET_SIZE_BYTES, bytes.size)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.short
        assertEquals(PacketConstants.MAGIC_HEADER, magic)

        val ver = buffer.get()
        assertEquals(PacketConstants.PROTOCOL_VERSION, ver)

        val flags = buffer.get()
        assertEquals(0.toByte(), flags)

        val seq = buffer.int
        assertEquals(100, seq)
    }

    @Test
    fun profile_json_roundtrip_validation() {
        val original = DefaultProfiles.BALANCED
        val serialized = ProfileRepositoryImpl.serializeProfile(original)

        val parsed = ProfileRepositoryImpl.parseProfileJson(serialized)
        assertNotNull(parsed)
        assertEquals(original.name, parsed?.name)
        assertEquals(original.controls.size, parsed?.controls?.size)
        assertEquals(original.rgbConfig.mode, parsed?.rgbConfig?.mode)
    }

    @Test
    fun xbox_profile_validation() {
        val xbox = DefaultProfiles.XBOX
        assertEquals("Xbox Controller", xbox.name)
        assertTrue(xbox.controls.any { it.id == "ls_btn" })
        assertTrue(xbox.controls.any { it.id == "rs_btn" })
        assertTrue(xbox.controls.any { it.id == "xbox_guide_btn" })

        val serialized = ProfileRepositoryImpl.serializeProfile(xbox)
        val parsed = ProfileRepositoryImpl.parseProfileJson(serialized)
        assertNotNull(parsed)
        assertEquals("Xbox Controller", parsed?.name)
        assertEquals(xbox.controls.size, parsed?.controls?.size)
    }

    @Test
    fun tizen_key_mapper_maps_keys() {
        val mapper = TizenKeyMapper()
        assertEquals(TvKeyCommand.KEY_ENTER, mapper.mapButtonToTvKey(GamepadButton.A))
        assertEquals(TvKeyCommand.KEY_RETURN, mapper.mapButtonToTvKey(GamepadButton.B))
        assertEquals(TvKeyCommand.KEY_UP, mapper.mapButtonToTvKey(GamepadButton.DPAD_UP))
        assertEquals(TvKeyCommand.KEY_DOWN, mapper.mapButtonToTvKey(GamepadButton.DPAD_DOWN))
        assertEquals(TvKeyCommand.KEY_HOME, mapper.mapButtonToTvKey(GamepadButton.START))
        assertEquals(TvKeyCommand.KEY_HOME, mapper.mapButtonToTvKey(GamepadButton.GUIDE))
    }

    @Test
    fun layout_editor_moves_control_by_delta() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.editor.LayoutEditorViewModel(app)
        val initialX = vm.draftProfile.value.controls.first().xPercent
        val controlId = vm.draftProfile.value.controls.first().id

        vm.moveControlByDelta(controlId, 0.05f, 0.02f)
        val updated = vm.draftProfile.value.controls.first { it.id == controlId }
        assertEquals(initialX + 0.05f, updated.xPercent, 0.001f)

        vm.nudgeControl(controlId, -0.02f, 0f)
        val nudged = vm.draftProfile.value.controls.first { it.id == controlId }
        assertEquals(initialX + 0.03f, nudged.xPercent, 0.001f)
    }

    @Test
    fun settings_button_is_in_central_space_and_movable_in_customization_editor() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.editor.LayoutEditorViewModel(app)
        val settingsControl = vm.draftProfile.value.controls.firstOrNull { it.type == com.example.core.model.ControlType.SETTINGS_BUTTON }
        assertNotNull(settingsControl)

        val id = settingsControl!!.id
        val initialX = settingsControl.xPercent
        val initialY = settingsControl.yPercent

        // Verify default position is in central space
        assertEquals(0.50f, initialX, 0.01f)
        assertEquals(0.48f, initialY, 0.01f)

        // Move settings button in customization editor
        vm.moveControlByDelta(id, 0.10f, 0.15f)
        val moved = vm.draftProfile.value.controls.first { it.id == id }
        assertEquals(initialX + 0.10f, moved.xPercent, 0.001f)
        assertEquals(initialY + 0.15f, moved.yPercent, 0.001f)
    }

    @Test
    fun reset_everything_to_default_restores_all_defaults() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.gamepad.GamepadViewModel(app)

        // Modify active profile
        val modifiedProfile = vm.activeProfile.value.copy(
            name = "Modified Profile",
            backgroundBrightness = 0.2f
        )
        vm.selectProfile(modifiedProfile)

        // Execute reset everything to default
        vm.resetEverythingToDefault()

        // Assert active profile is restored to factory Xbox with central settings button
        val active = vm.activeProfile.value
        assertEquals("builtin_xbox", active.id)
        val settingsBtn = active.controls.firstOrNull { it.type == com.example.core.model.ControlType.SETTINGS_BUTTON }
        assertNotNull(settingsBtn)
        assertEquals(0.50f, settingsBtn!!.xPercent, 0.01f)
        assertEquals(0.48f, settingsBtn.yPercent, 0.01f)
    }

    @Test
    fun xbox_profile_contains_polished_view_menu_guide_controls() {
        val xbox = com.example.core.model.DefaultProfiles.XBOX
        val viewBtn = xbox.controls.firstOrNull { it.id == "select_btn" }
        val menuBtn = xbox.controls.firstOrNull { it.id == "start_btn" }
        val guideBtn = xbox.controls.firstOrNull { it.id == "xbox_guide_btn" }
        val actionCluster = xbox.controls.firstOrNull { it.id == "action_cluster" }

        assertNotNull(viewBtn)
        assertNotNull(menuBtn)
        assertNotNull(guideBtn)
        assertNotNull(actionCluster)

        assertEquals(com.example.core.model.ControlShape.CIRCLE, viewBtn!!.shape)
        assertEquals(com.example.core.model.ControlShape.CIRCLE, menuBtn!!.shape)
        assertEquals(com.example.core.model.ControlShape.CIRCLE, guideBtn!!.shape)
    }

    @Test
    fun battery_saver_mode_pauses_rgb_ticker_when_rgb_disabled_or_static() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.gamepad.GamepadViewModel(app)

        vm.switchToXboxController()
        assertEquals(com.example.core.model.RgbEffectMode.STATIC, vm.activeProfile.value.rgbConfig.mode)
        assertEquals(0f, vm.rgbProgress.value, 0.001f)

        val disabled = vm.activeProfile.value.copy(
            rgbConfig = vm.activeProfile.value.rgbConfig.copy(isEnabled = false)
        )
        vm.selectProfile(disabled)
        assertEquals(0f, vm.rgbProgress.value, 0.001f)
    }

    @Test
    fun bluetooth_gamepad_manager_safety_and_lifecycle() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val btManager = com.example.data.bluetooth.BluetoothGamepadManager(app)

        // Initial state is Disabled
        assertEquals(com.example.data.bluetooth.BluetoothControllerState.Disabled, btManager.state.value)

        // Starting controller must NEVER crash with uncaught SecurityException
        btManager.startBluetoothController()

        // State is safely captured (PermissionRequired or Unsupported on test JVM)
        val currentState = btManager.state.value
        assertTrue(
            currentState is com.example.data.bluetooth.BluetoothControllerState.PermissionRequired ||
            currentState is com.example.data.bluetooth.BluetoothControllerState.Unsupported ||
            currentState is com.example.data.bluetooth.BluetoothControllerState.BluetoothDisabledOnDevice ||
            currentState is com.example.data.bluetooth.BluetoothControllerState.ReadyToPair
        )

        // Bonded devices list and autoConnect must execute safely without throwing exceptions
        val bonded = btManager.getBondedDevices()
        assertNotNull(bonded)

        val autoConnectResult = btManager.autoConnect("00:11:22:33:44:55")
        // In JVM test environment with no physical radio, autoConnect returns false without crashing
        assertFalse(autoConnectResult)

        // Stopping restores Disabled safely
        btManager.stop()
        assertEquals(com.example.data.bluetooth.BluetoothControllerState.Disabled, btManager.state.value)
    }
}
