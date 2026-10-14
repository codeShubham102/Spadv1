# S!pad — Customizable Virtual Gamepad for Samsung TV
## Architecture Specification & Stage 0 Deliverables

S!pad is an original Android virtual gamepad application built with Kotlin and Jetpack Compose. It transforms an Android phone or tablet into a customizable, responsive game controller that communicates with Samsung Smart TVs over local Wi-Fi and connects to PC gaming setups via an optional low-latency receiver.

***

## User Review & Critical Decisions

> [!IMPORTANT]
> **Samsung TV Compatibility Reality Check**:
> - **Local TV Remote Protocol (Port 8001 / 8002 WebSocket)**: Samsung Smart TVs (Tizen OS 2016+) support discrete remote-control keys (`KEY_UP`, `KEY_DOWN`, `KEY_ENTER`, `KEY_RETURN`, etc.) over WebSocket with first-time on-screen TV PIN/Permission authorization. This allows TV menu navigation, media control, and lightweight casual TV app play.
> - **Gaming Hub (Xbox Cloud / GeForce NOW / Luna)**: Samsung Gaming Hub requires direct kernel-level Bluetooth HID or USB HID controller input (e.g., Xbox Wireless Controller or DualSense). Samsung does **NOT** expose a public or documented local network socket that injects analog stick/trigger HID events directly into Gaming Hub cloud streams.
> - **Architectural Solution**: S!pad isolates the TV Connection module behind a pluggable `InputTransport` interface. Phase 1 provides a responsive Smart TV Navigation and Casual Gaming Controller. Phase 2 isolates experimental Gaming Hub input behind a disabled-by-default feature flag with full technical disclosure. For high-fidelity PC gaming, the decoupled PC Receiver module provides low-latency UDP controller streaming directly into Windows via ViGEmBus / Virtual Gamepad drivers.

- **App Package & Identity**: Configured as `com.shubhamkumar.spad` (Application ID) with display name **S!pad**, authored by **Shubham Kumar**, Made in India 🇮🇳.
- **Default Visual Mode**: Dark Mode by default with neon cyber aesthetics, customizable RGB effects, and low-draw Canvas rendering.

***

## 1. System Architecture

S!pad is engineered around Clean Architecture principles, ensuring total decoupling between the **Touch Input Processing Engine**, **Visual Presentation / RGB Layer**, **Profile Persistence**, and **Network Transports**.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│  ┌──────────────────────┐  ┌────────────────────┐  ┌────────────────┐  │
│  │   GamepadScreen      │  │  LayoutEditorView  │  │ SettingsView   │  │
│  │ (Multi-Touch Surface)│  │ (Drag/Resize/Rot)  │  │ (Profiles/RGB) │  │
│  └──────────┬───────────┘  └─────────┬──────────┘  └────────┬───────┘  │
│             └───────────────────┐    │    ┌─────────────────┘          │
│                                 ▼    ▼    ▼                            │
│                         ┌───────────────────────┐                      │
│                         │   GamepadViewModel    │                      │
│                         │ (State, RGB, Profile) │                      │
│                         └───────────┬───────────┘                      │
└─────────────────────────────────────┼──────────────────────────────────┘
                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             DOMAIN LAYER                               │
│  ┌────────────────────┐  ┌───────────────────┐  ┌───────────────────┐  │
│  │  GamepadCoreEngine │  │  ProfileUseCase   │  │   RgbController   │  │
│  │ (Deadzone, Packet) │  │ (Validate/Export) │  │  (Wave/Pulse/Fx)  │  │
│  └─────────┬──────────┘  └─────────┬─────────┘  └─────────┬─────────┘  │
└────────────┼───────────────────────┼──────────────────────┼────────────┘
             ▼                       ▼                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                │
│  ┌────────────────────────┐              ┌──────────────────────────┐  │
│  │   ProfileRepository    │              │     SettingsDataStore    │  │
│  │ (Room DB + JSON Parser)│              │  (Theme, Haptics, Audio) │  │
│  └────────────────────────┘              └──────────────────────────┘  │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                     InputTransport (Interface)                   │  │
│  │  + connect(target) / disconnect() / sendInput(packet)            │  │
│  └──────────────┬────────────────────────────────────┬──────────────┘  │
│                 │                                    │                 │
│                 ▼                                    ▼                 │
│  ┌─────────────────────────────┐      ┌─────────────────────────────┐  │
│  │  SamsungTvWebSocketTransport│      │     PcReceiverUdpTransport  │  │
│  │  (mDNS/SSDP, Port 8001/8002,│      │  (Binary/JSON UDP Socket,   │  │
│  │   Tizen Remote Key Mapper)  │      │   Sub-10ms Gamepad Stream)  │  │
│  └─────────────────────────────┘      └─────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Module Responsibilities

1. **Gamepad Core**:
   - Multi-touch pointer tracking using Android Compose `PointerInputScope`.
   - Continuous vector calculation for Left and Right analog sticks with radial and axial dead-zone processing.
   - Sensitivity curvature (linear, exponential, aggressive).
   - Atomic state aggregation producing `SpadInputState`.

2. **Gamepad UI & RGB Engine**:
   - Zero-allocation Canvas draw passes for controller geometry.
   - Dynamic RGB lighting shader (Rainbow Cycle, Breathing Glow, Color Pulse, Neon Wave, Reactive Button Pulse).
   - High-contrast accessibility modes with configurable glow radius and control opacity.
   - Haptic feedback using Android's modern `VibratorManager` / `VibrationEffect` with pre-defined tactile profiles.

3. **Layout Editor**:
   - Interactive canvas allowing selection, free drag, pinch-to-resize, and angular rotation.
   - Snap-to-grid and alignment guidelines.
   - Bounding box collision boundaries preventing controls from leaving screen insets.

4. **Profile Manager**:
   - Room Database for storing user-crafted controller configurations.
   - Robust JSON import/export parser with strict schema validation, range checks, and malformed payload rejection.
   - Bundled stock presets: *Balanced*, *Racing*, *Action*, and *Minimal*.

5. **TV Connection Module (Samsung Tizen Transport)**:
   - Device discovery via mDNS (`_samsungmsf._tcp`) and SSDP (`urn:samsung.com:device:RemoteControlReceiver:1`).
   - Secure and plaintext WebSocket client (`ws://<ip>:8001` and `wss://<ip>:8002`).
   - Token lifecycle management (persisting authorization tokens in DataStore).
   - Mapping engine translating continuous stick pushes and button presses into supported Tizen remote commands.
   - Real-time round-trip latency (ping/pong) monitoring and auto-reconnect backoff.

6. **PC Receiver (Standalone Companion Module)**:
   - Python/C# local service listening on UDP port 5150.
   - Unpacks binary/JSON S!pad packets and feeds inputs directly into the virtual Xbox 360 controller bus (via ViGEmClient or Windows virtual joystick).

***

## 2. Module Dependency Diagram

```
[UI Layer]
   │
   ├── GamepadScreen ─────────► GamepadViewModel
   ├── LayoutEditorScreen ────► LayoutEditorViewModel
   ├── TvConnectDialog ───────► TvConnectionViewModel
   └── SettingsScreen ────────► SettingsViewModel
                                    │
[Domain Layer]                      ▼
   ┌─────────────────────────────────────────────────────────┐
   │ UseCases:                                               │
   │  - ProcessTouchInputUseCase (Deadzone & Math)           │
   │  - DispatchTvCommandUseCase (Rate-limiting & Key map)   │
   │  - ManageProfilesUseCase (JSON Validation & CRUD)       │
   │  - CalculateRgbColorsUseCase (Color interpolation)      │
   └────────────────────────┬────────────────────────────────┘
                            ▼
[Data Layer]
   ┌─────────────────────────────────────────────────────────┐
   │ Repositories & Transports:                              │
   │  - ProfileRepository (Room + kotlinx.serialization)     │
   │  - UserPreferencesRepository (DataStore)                │
   │  - SamsungTvClient (OkHttp WebSocket + mDNS Discovery)   │
   │  - PcStreamClient (Java NIO DatagramChannel)            │
   └─────────────────────────────────────────────────────────┘
```

***

## 3. Input Packet Specification

### 3.1 Versioned JSON Specification (`v1.0.0`)
Ideal for human-readable debugging, profile import/export, and diagnostic tooling.

```json
{
  "version": 1,
  "sequence": 10428,
  "timestamp": 1776073800125,
  "buttons": {
    "a": true,
    "b": false,
    "x": false,
    "y": false,
    "dpadUp": false,
    "dpadDown": false,
    "dpadLeft": false,
    "dpadRight": false,
    "l1": false,
    "r1": true,
    "l2": 0.0,
    "r2": 0.85,
    "start": false,
    "select": false,
    "l3": false,
    "r3": false
  },
  "analog": {
    "leftStick": {
      "x": -0.42,
      "y": 0.88
    },
    "rightStick": {
      "x": 0.0,
      "y": 0.0
    }
  }
}
```

### 3.2 Compact Binary UDP Specification (for Low-Latency PC Streaming)
Total packet size: **24 bytes** fixed payload.

| Byte Offset | Field | Type | Description / Range |
|---|---|---|---|
| `0..1` | Magic Header | `uint16` | `0x5350` ("SP") |
| `2` | Protocol Version | `uint8` | `0x01` |
| `3` | Packet Flags | `uint8` | Bit 0: Haptic ACK req, Bit 1: Heartbeat |
| `4..7` | Sequence ID | `uint32` | Monotonically increasing packet count |
| `8..11` | Timestamp | `uint32` | Milliseconds since connection established |
| `12..13` | Digital Buttons | `uint16` | Bitmask (A, B, X, Y, Dpad, L1, R1, Start, Select, L3, R3) |
| `14..15` | Left Stick X | `int16` | Normalized `[-32768, 32767]` (Center: 0) |
| `16..17` | Left Stick Y | `int16` | Normalized `[-32768, 32767]` (Center: 0) |
| `18..19` | Right Stick X | `int16` | Normalized `[-32768, 32767]` (Center: 0) |
| `20..21` | Right Stick Y | `int16` | Normalized `[-32768, 32767]` (Center: 0) |
| `22` | Trigger L2 | `uint8` | `[0, 255]` |
| `23` | Trigger R2 | `uint8` | `[0, 255]` |

### 3.3 Network Characteristics & Error Handling
- **Update Frequency**: 60 Hz (16.6ms intervals) during continuous stick motion; immediate dispatch on button state transition.
- **Heartbeat & Watchdog**: Heartbeat packet transmitted every 1000ms if input is idle. If no packets received by receiver within 500ms, all inputs are reset to neutral.
- **Simultaneous Input Handling**: Fully multi-pointer tracked in Compose; all 10 fingers tracked concurrently without blocking.

***

## 4. Data Models

```kotlin
// Core Gamepad State
data class SpadInputState(
    val sequenceNumber: Long = 0L,
    val timestampMs: Long = 0L,
    val buttonsDown: Set<GamepadButton> = emptySet(),
    val leftStick: StickPosition = StickPosition.ZERO,
    val rightStick: StickPosition = StickPosition.ZERO,
    val leftTrigger: Float = 0.0f,  // 0.0f..1.0f
    val rightTrigger: Float = 0.0f // 0.0f..1.0f
)

data class StickPosition(
    val x: Float, // -1.0f..1.0f
    val y: Float  // -1.0f..1.0f
) {
    companion object { val ZERO = StickPosition(0f, 0f) }
}

enum class GamepadButton {
    A, B, X, Y,
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    L1, R1, L2_DIGITAL, R2_DIGITAL,
    START, SELECT, L3, R3, GUIDE
}

// Layout Configuration
data class GamepadProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetGame: String = "",
    val controls: List<ControlElementConfig>,
    val leftStickConfig: StickBehaviorConfig = StickBehaviorConfig(),
    val rightStickConfig: StickBehaviorConfig = StickBehaviorConfig(),
    val rgbConfig: RgbThemeConfig = RgbThemeConfig(),
    val hapticConfig: HapticFeedbackConfig = HapticFeedbackConfig(),
    val backgroundAsset: String = "bg_futuristic_dark"
)

data class ControlElementConfig(
    val id: String,
    val type: ControlType,
    val xPercent: Float,     // Normalized 0.0f..1.0f screen coordinate
    val yPercent: Float,
    val sizeDp: Float,
    val rotationDeg: Float = 0f,
    val opacity: Float = 0.9f,
    val primaryColorHex: String = "#00F0FF",
    val shape: ControlShape = ControlShape.CIRCLE
)

enum class ControlShape { CIRCLE, ROUNDED_RECT, DIAMOND }
enum class ControlType {
    DPAD, LEFT_STICK, RIGHT_STICK,
    ACTION_CLUSTER, L1_BUTTON, R1_BUTTON,
    L2_TRIGGER, R2_TRIGGER, START_BUTTON, SELECT_BUTTON
}

data class StickBehaviorConfig(
    val deadzonePercent: Float = 0.12f,
    val sensitivity: Float = 1.0f,
    val isFloating: Boolean = false
)

data class RgbThemeConfig(
    val isEnabled: Boolean = true,
    val mode: RgbEffectMode = RgbEffectMode.RAINBOW_CYCLE,
    val primaryColor: Long = 0xFF00E5FF,
    val secondaryColor: Long = 0xFFFF007F,
    val brightness: Float = 0.85f,
    val animationSpeed: Float = 1.0f,
    val glowIntensity: Float = 0.75f
)

enum class RgbEffectMode {
    OFF, STATIC, BREATHING, COLOR_PULSE,
    RAINBOW_CYCLE, DUAL_GRADIENT, NEON_WAVE, REACTIVE_PULSE
}

data class HapticFeedbackConfig(
    val isEnabled: Boolean = true,
    val strengthPercent: Float = 0.8f
)

// Samsung TV Communication Models
data class DiscoveredTv(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8002,
    val isTizen: Boolean = true,
    val token: String? = null
)

sealed interface TvConnectionState {
    data object Disconnected : TvConnectionState
    data object Discovering : TvConnectionState
    data object PairingRequired : TvConnectionState
    data class Connected(val deviceName: String, val latencyMs: Long) : TvConnectionState
    data class Error(val message: String) : TvConnectionState
}
```

***

## 5. TV Compatibility Research Findings

### 5.1 Protocol Analysis & Documented APIs
- **Samsung Tizen Smart TV Remote Control Port**:
  - Legacy (Pre-2016): Port 55000 (Plain TCP text socket)
  - Modern (2016+): Port 8001 (HTTP / unencrypted WebSocket `ws://<ip>:8001/api/v2/channels/samsung.remote.control`)
  - Modern Secure (2018+): Port 8002 (WSS with self-signed certificate, token-authenticated)
- **Handshake Mechanism**:
  A base64-encoded client name is passed as a URL query parameter. On the first connection to Port 8002, the TV flashes an on-screen dialog asking: *"Allow S!pad to connect to this TV?"*. When allowed, the TV returns a JSON payload containing `"data":{"token":"12345678"}`. S!pad persists this token in secure preferences to bypass future prompts.
- **Command Transmission**:
  Commands are transmitted as JSON frames:
  ```json
  {
    "method": "ms.remote.control",
    "params": {
      "Cmd": "Click",
      "DataOfCmd": "KEY_ENTER",
      "Option": "false",
      "TypeOfRemote": "SendRemoteKey"
    }
  }
  ```
- **Supported Discrete Keys**:
  `KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT`, `KEY_ENTER`, `KEY_RETURN`, `KEY_HOME`, `KEY_MENU`, `KEY_INFO`, `KEY_VOLUP`, `KEY_VOLDOWN`, `KEY_MUTE`, `KEY_SOURCE`, `KEY_TOOLS`.

### 5.2 The Hard Truth About Gaming Hub & Analog Gamepads
- **Finding**: Samsung Gaming Hub (running Xbox Cloud Gaming, GeForce NOW, Utomik, Amazon Luna) operates strictly inside Tizen's secure container or cloud streaming app runtime. Input to Gaming Hub is captured via standard **Linux kernel Bluetooth HID / USB HID drivers** (`/dev/input/js*` or `/dev/input/event*`).
- **Limitation**: The WebSocket remote-control interface on port 8001/8002 does **not** route raw joystick coordinates (`ABS_X`, `ABS_Y`) or trigger pressures into Tizen's gaming subsystem. Sending arbitrary JSON gamepad events to `ms.remote.control` results in silent dropped packets.
- **Architectural Honesty**:
  1. We will NOT mislead users into believing Wi-Fi alone emulates a physical Bluetooth Xbox wireless gamepad for Samsung Gaming Hub without custom root/hardware.
  2. Phase 1 provides full, responsive, zero-lag Smart TV remote navigation, menu selection, and casual TV app control.
  3. Analog sticks in TV mode provide ergonomic 8-way directional stepped navigation (with configurable deadzone & repeat rate).
  4. Phase 2 keeps any experimental Gaming Hub transport research behind a dedicated toggle, while directing users wanting AAA PC gaming to the companion PC Receiver.

***

## 6. Feature Support Matrix

| Feature | S!pad TV (Phase 1) | Experimental Gaming Hub (Phase 2) | S!pad PC Receiver |
|---|---|---|---|
| **D-Pad Navigation** | Full (Native `KEY_*`) | Best-effort | Full (D-Pad bitmask) |
| **A / B Buttons** | Full (Enter / Back) | Best-effort | Full (Xbox A / B) |
| **X / Y Buttons** | Contextual/Remappable | Unsupported | Full (Xbox X / Y) |
| **Shoulder L1 / R1** | Contextual/Remappable | Unsupported | Full (LB / RB) |
| **Analog Sticks (Continuous)**| Directional Stepping | Unsupported via WebSocket | Full (360° ±32767) |
| **Analog Triggers (0-255)** | Digital trigger click | Unsupported via WebSocket | Full (0-255 Pressure) |
| **Pairing Mechanism** | TV On-Screen Prompt | N/A | Local Wi-Fi Auto-Pair |
| **Transport** | WebSocket (8001/8002) | Theoretical / Reverse-eng | Low-latency UDP (5150) |

***

## 7. Project Folder Structure

```
app/src/main/java/com/shubhamkumar/spad/
├── MainActivity.kt
├── SpadApplication.kt
├── core/
│   ├── engine/
│   │   ├── GamepadInputEngine.kt
│   │   ├── DeadzoneCalculator.kt
│   │   └── HapticFeedbackManager.kt
│   ├── model/
│   │   ├── SpadInputState.kt
│   │   ├── GamepadButton.kt
│   │   ├── GamepadProfile.kt
│   │   ├── RgbConfig.kt
│   │   └── PacketModels.kt
│   └── rgb/
│       ├── RgbColorEngine.kt
│       └── RgbShaderUtils.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── ProfileDao.kt
│   │   └── ProfileEntity.kt
│   ├── repository/
│   │   ├── ProfileRepositoryImpl.kt
│   │   └── UserPreferencesRepository.kt
│   └── network/
│       ├── discovery/
│       │   ├── TvDiscoveryService.kt
│       │   └── SsdpClient.kt
│       ├── transport/
│       │   ├── InputTransport.kt
│       │   ├── SamsungTvWebSocketTransport.kt
│       │   └── PcStreamUdpTransport.kt
│       └── tizen/
│           ├── TizenKeyCodes.kt
│           └── TizenMessageFactory.kt
└── ui/
    ├── navigation/
    │   └── SpadDestinations.kt
    ├── gamepad/
    │   ├── GamepadScreen.kt
    │   ├── GamepadViewModel.kt
    │   └── components/
    │       ├── AnalogStickComponent.kt
    │       ├── DpadComponent.kt
    │       ├── ActionButtonCluster.kt
    │       ├── ShoulderTriggerComponent.kt
    │       └── RgbHaloOverlay.kt
    ├── editor/
    │   ├── LayoutEditorScreen.kt
    │   ├── LayoutEditorViewModel.kt
    │   └── components/
    │       ├── DraggableControlWrapper.kt
    │       └── TransformationControlsBar.kt
    ├── tv/
    │   ├── TvConnectDialog.kt
    │   └── TvConnectViewModel.kt
    ├── settings/
    │   ├── SettingsScreen.kt
    │   └── SettingsViewModel.kt
    ├── theme/
    │   ├── Color.kt
    │   ├── Theme.kt
    │   └── Type.kt
    └── splash/
        └── SplashScreen.kt
```

***

## 8. Development Roadmap

- **Stage 0 (Current)**: Architecture specification, TV compatibility research, data modeling, input protocol definition, and user approval.
- **Stage 1 — Gamepad UI & Core Engine**:
  - Implement multi-touch input surface, analog stick math, D-pad, action buttons, triggers.
  - Implement Landscape gaming view with immersive edge-to-edge system bars.
  - Implement haptic feedback engine with `VibrationEffect`.
  - Implement RGB lighting engine with Compose Canvas drawing.
  - Generate original high-resolution gaming background assets via image generation.
- **Stage 2 — Layout Editor**:
  - Interactive visual editor with drag, pinch-to-resize, and rotation.
  - Control boundary snapping, undo/redo state stack, and live preview.
- **Stage 3 — Profiles & Persistence**:
  - Room database integration for custom profiles.
  - JSON import/export with strict schema validation.
  - Built-in presets (*Balanced*, *Racing*, *Action*, *Minimal*).
- **Stage 4 — Samsung TV Connection Module**:
  - mDNS & SSDP TV discovery.
  - WebSocket client (port 8001/8002) with token negotiation and on-screen pairing feedback.
  - Configurable TV remote key mapping and round-trip latency counter.
  - Transparent compatibility UI distinguishing TV remote keys from experimental features.
- **Stage 5 — PC Receiver Companion**:
  - Complete, standalone Python receiver script with UDP socket and virtual Xbox controller binding.
- **Stage 6 — Polish, Branding & Testing**:
  - Splash screen with "Made in India 🇮🇳" and Shubham Kumar credits.
  - Comprehensive Robolectric tests for packet serialization and layout validation.
  - Full project documentation and README.

***

## 9. Testing Strategy

1. **Local JVM Robolectric Tests**:
   - `GamepadCoreEngineTest`: Verify stick normalization, radial dead-zone cutoff, and sensitivity curves.
   - `ProfileValidationTest`: Test malformed JSON rejection, oversized payload protection, and boundary enforcement.
   - `TizenPacketFormattingTest`: Test serialization of `KEY_ENTER`, `KEY_RETURN`, and Base64 client handshake string.
2. **Touch & Geometry Hit-Testing**:
   - Verify simultaneous touch events (e.g., Left Stick vector + A button tap + R1 hold).
   - Test rotation transformations: confirm hit detection rotates with the control angle.
3. **Network Resiliency Simulation**:
   - Simulate WebSocket dropouts, verify automatic exponential backoff reconnection.
   - Test timeout watchdog: confirm controller states zero out upon link loss.

***

## 10. Technical Risks and Mitigation

| Risk | Impact | Mitigation Strategy |
|---|---|---|
| **Samsung TV Model Variations** | Port 8001 disabled on newer TVs; Port 8002 requires TLS | Dual-port auto-fallback: Probe port 8002 (WSS) first, fallback to port 8001 (WS). Trust manager configured for Tizen self-signed cert. |
| **Wi-Fi Packet Jitter & Latency** | Input lag during gameplay | Direct UDP streaming for PC; WebSocket frame prioritization for TV; zero recomposition in the touch-loop. |
| **Accidental System Gestures** | Android back-gesture triggered while flicking stick | Immersive sticky fullscreen mode (`WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). |
| **Battery & Thermal Throttling** | High GPU drain from continuous RGB redraws | Efficient Canvas `drawCircle`/`drawPath` passes without layered runtime blur shaders; RGB toggle to disable animation loop. |
