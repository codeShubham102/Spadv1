package com.example.core.model

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject

object PacketConstants {
    const val PROTOCOL_VERSION: Byte = 1
    const val MAGIC_HEADER: Short = 0x5350.toShort() // "SP"
    const val PACKET_SIZE_BYTES = 24
}

object SpadPacketSerializer {

    /**
     * Serializes SpadInputState to documented JSON format.
     */
    fun toJson(state: SpadInputState): String {
        val root = JSONObject()
        root.put("version", PacketConstants.PROTOCOL_VERSION.toInt())
        root.put("sequence", state.sequenceNumber)
        root.put("timestamp", state.timestampMs)

        val buttons = JSONObject()
        buttons.put("a", state.isButtonPressed(GamepadButton.A))
        buttons.put("b", state.isButtonPressed(GamepadButton.B))
        buttons.put("x", state.isButtonPressed(GamepadButton.X))
        buttons.put("y", state.isButtonPressed(GamepadButton.Y))
        buttons.put("dpadUp", state.isButtonPressed(GamepadButton.DPAD_UP))
        buttons.put("dpadDown", state.isButtonPressed(GamepadButton.DPAD_DOWN))
        buttons.put("dpadLeft", state.isButtonPressed(GamepadButton.DPAD_LEFT))
        buttons.put("dpadRight", state.isButtonPressed(GamepadButton.DPAD_RIGHT))
        buttons.put("l1", state.isButtonPressed(GamepadButton.L1))
        buttons.put("r1", state.isButtonPressed(GamepadButton.R1))
        buttons.put("l2", state.leftTrigger.toDouble())
        buttons.put("r2", state.rightTrigger.toDouble())
        buttons.put("start", state.isButtonPressed(GamepadButton.START))
        buttons.put("select", state.isButtonPressed(GamepadButton.SELECT))
        buttons.put("l3", state.isButtonPressed(GamepadButton.L3))
        buttons.put("r3", state.isButtonPressed(GamepadButton.R3))
        buttons.put("guide", state.isButtonPressed(GamepadButton.GUIDE))
        root.put("buttons", buttons)

        val analog = JSONObject()
        val leftStick = JSONObject()
        leftStick.put("x", state.leftStick.x.toDouble())
        leftStick.put("y", state.leftStick.y.toDouble())
        analog.put("leftStick", leftStick)

        val rightStick = JSONObject()
        rightStick.put("x", state.rightStick.x.toDouble())
        rightStick.put("y", state.rightStick.y.toDouble())
        analog.put("rightStick", rightStick)

        root.put("analog", analog)
        return root.toString()
    }

    /**
     * Serializes SpadInputState to high-performance 24-byte binary packet for UDP PC streaming.
     */
    fun toBinary(state: SpadInputState): ByteArray {
        val buffer = ByteBuffer.allocate(PacketConstants.PACKET_SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)

        // 0..1 Magic Header: 0x5350
        buffer.putShort(PacketConstants.MAGIC_HEADER)
        // 2 Protocol Version: 1
        buffer.put(PacketConstants.PROTOCOL_VERSION)
        // 3 Flags (0 = standard input)
        buffer.put(0.toByte())
        // 4..7 Sequence Number
        buffer.putInt((state.sequenceNumber and 0xFFFFFFFFL).toInt())
        // 8..11 Timestamp (relative lower 32 bits)
        buffer.putInt((state.timestampMs and 0xFFFFFFFFL).toInt())

        // 12..13 Button Bitmask (16-bit)
        var bitmask = 0
        if (state.isButtonPressed(GamepadButton.A)) bitmask = bitmask or (1 shl 0)
        if (state.isButtonPressed(GamepadButton.B)) bitmask = bitmask or (1 shl 1)
        if (state.isButtonPressed(GamepadButton.X)) bitmask = bitmask or (1 shl 2)
        if (state.isButtonPressed(GamepadButton.Y)) bitmask = bitmask or (1 shl 3)
        if (state.isButtonPressed(GamepadButton.DPAD_UP)) bitmask = bitmask or (1 shl 4)
        if (state.isButtonPressed(GamepadButton.DPAD_DOWN)) bitmask = bitmask or (1 shl 5)
        if (state.isButtonPressed(GamepadButton.DPAD_LEFT)) bitmask = bitmask or (1 shl 6)
        if (state.isButtonPressed(GamepadButton.DPAD_RIGHT)) bitmask = bitmask or (1 shl 7)
        if (state.isButtonPressed(GamepadButton.L1)) bitmask = bitmask or (1 shl 8)
        if (state.isButtonPressed(GamepadButton.R1)) bitmask = bitmask or (1 shl 9)
        if (state.isButtonPressed(GamepadButton.START)) bitmask = bitmask or (1 shl 10)
        if (state.isButtonPressed(GamepadButton.SELECT)) bitmask = bitmask or (1 shl 11)
        if (state.isButtonPressed(GamepadButton.L3)) bitmask = bitmask or (1 shl 12)
        if (state.isButtonPressed(GamepadButton.R3)) bitmask = bitmask or (1 shl 13)
        if (state.isButtonPressed(GamepadButton.GUIDE)) bitmask = bitmask or (1 shl 14)
        buffer.putShort(bitmask.toShort())

        // 14..15 Left Stick X: int16 [-32768, 32767]
        buffer.putShort((state.leftStick.x.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        // 16..17 Left Stick Y: int16 [-32768, 32767]
        buffer.putShort((state.leftStick.y.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        // 18..19 Right Stick X: int16 [-32768, 32767]
        buffer.putShort((state.rightStick.x.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        // 20..21 Right Stick Y: int16 [-32768, 32767]
        buffer.putShort((state.rightStick.y.coerceIn(-1f, 1f) * 32767f).toInt().toShort())

        // 22 Trigger L2: uint8 [0, 255]
        buffer.put((state.leftTrigger.coerceIn(0f, 1f) * 255f).toInt().toByte())
        // 23 Trigger R2: uint8 [0, 255]
        buffer.put((state.rightTrigger.coerceIn(0f, 1f) * 255f).toInt().toByte())

        return buffer.array()
    }
}
