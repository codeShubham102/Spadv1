# S!pad — Customizable Virtual Gamepad for Samsung TV

**Creator**: Shubham Kumar  
**Country**: India 🇮🇳  
**Package Identifier**: `com.shubhamkumar.spad`  
**Display Name**: `S!pad`  

***

## 1. Project Overview

**S!pad** turns your Android phone or tablet into an ultra-responsive, fully customizable virtual game controller that connects seamlessly to your Samsung Smart TV over the same local Wi-Fi network. Built from scratch with Kotlin and Jetpack Compose, S!pad eliminates the need for physical Bluetooth pairing, QR code scanning, or third-party dongles.

The app features an original cyberpunk-inspired design with real-time RGB effects, procedural gaming backgrounds, a freeform visual layout editor, and a decoupled networking architecture allowing dual-mode communication:
1. **Samsung Smart TV Remote Control** via Samsung Tizen local WebSocket API (ports 8001/8002).
2. **PC Virtual Gamepad Streaming** via low-latency 24-byte UDP datagrams to the standalone companion Python receiver (`receiver/spad_pc_receiver.py`).

***

## 2. Key Features

- **Gamepad Core & Multi-Touch**:
  - Independent dual analog sticks with continuous radial deadzone calculation and sensitivity scaling.
  - 4-way tactile D-pad with smooth diagonal sliding.
  - Responsive diamond action buttons (A, B, X, Y) with spring press animations.
  - Shoulder bumpers (L1, R1) and analog pressure triggers (L2, R2).
  - Start, Select, and L3/R3 stick-click buttons.
  - Multi-pointer touch tracker allowing simultaneous stick steering, button presses, and trigger pulls without input drops.

- **Original RGB Lighting Engine**:
  - 8 distinct animated illumination presets: *Rainbow Cycle*, *Breathing Glow*, *Color Pulse*, *Dual-Color Gradient*, *Neon Wave*, *Reactive Button Pulse*, *Static RGB*, and *Off*.
  - Configurable brightness, animation cycle speed, and glow intensity.
  - Energy-efficient Compose Canvas rendering without heavy GPU fill-rate overhead.

- **Visual Layout Editor**:
  - Drag controls anywhere on screen with boundary clamping.
  - Real-time size scaling slider (50dp to 250dp).
  - 360-degree rotation slider (-180° to +180°).
  - Per-control color customization.
  - One-tap "Reset to Defaults", preview mode, and instant save to Room database.

- **Profile Management**:
  - Built-in tuned profiles: *Balanced Layout*, *Racing Layout*, *Action Layout*, and *Minimal Layout*.
  - Create, duplicate, rename, and delete custom profiles.
  - Export profile to clean JSON clipboard format.
  - Import profile from JSON with strict schema validation and 100KB payload safety limits.

- **Samsung TV Connection (Phase 1)**:
  - Local SSDP and subnet broadcast discovery for Samsung Smart TVs on Wi-Fi.
  - WebSocket client supporting port 8002 (WSS with token authentication) and fallback to port 8001 (WS).
  - Persistent pairing token management (prompts for TV remote "Allow" permission once).
  - Round-trip ping/pong latency monitor (ms).
  - Automatic stepped directional repeat when holding analog sticks.

- **Companion PC Receiver (Phase 5)**:
  - Standalone Python receiver script (`receiver/spad_pc_receiver.py`).
  - Low-latency 24-byte binary UDP datagram ingestion at 60Hz.
  - Automatic mapping to virtual Xbox 360 controller via ViGEmBus (`vgamepad`).
  - Built-in diagnostic mode if ViGEm driver is absent.
  - 600ms watchdog timeout resetting controller inputs to neutral if link drops.

***

## 3. Architecture

S!pad strictly implements Clean Architecture with MVVM:

```
┌────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                   │
│   GamepadScreen  •  LayoutEditorScreen  • SettingsScreen│
│   TvConnectDialog • ProfileManagerDialog • SplashScreen │
└──────────────────────────┬─────────────────────────────┘
                           │ (StateFlow / Jetpack Compose)
┌──────────────────────────▼─────────────────────────────┐
│                    VIEWMODEL LAYER                     │
│      GamepadViewModel  •  LayoutEditorViewModel        │
└──────────────┬──────────────────────────┬──────────────┘
               ▼                          ▼
┌──────────────────────────────┐ ┌───────────────────────┐
│         DOMAIN LAYER         │ │      CORE ENGINES     │
│   - ProcessTouchInput        │ │ - GamepadInputEngine  │
│   - DeadzoneCalculator       │ │ - RgbColorEngine      │
│   - TizenKeyMapper           │ │ - HapticFeedbackMgr   │
└──────────────┬───────────────┘ └───────────┬───────────┘
               │                             │
┌──────────────▼─────────────────────────────▼───────────┐
│                       DATA LAYER                       │
│  - ProfileRepositoryImpl (Room Database + JSON parser) │
│  - UserPreferencesRepository (DataStore Preferences)   │
│  - SamsungTvWebSocketTransport (OkHttp WebSocket)      │
│  - PcReceiverUdpTransport (Java NIO DatagramChannel)   │
└────────────────────────────────────────────────────────┘
```

***

## 4. Input Packet Specification

### 4.1 JSON Protocol (`v1.0.0`)
```json
{
  "version": 1,
  "sequence": 42,
  "timestamp": 1776073800000,
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
    "leftStick": { "x": 0.45, "y": -0.80 },
    "rightStick": { "x": 0.00, "y": 0.00 }
  }
}
```

### 4.2 High-Speed Binary UDP Protocol (24 Bytes)
| Byte Offset | Type | Field | Description |
|---|---|---|---|
| `0..1` | `uint16` | Magic Header | `0x5350` ("SP") |
| `2` | `uint8` | Version | `1` |
| `3` | `uint8` | Flags | `0` (Standard Input) |
| `4..7` | `uint32` | Sequence ID | Incremental counter |
| `8..11` | `uint32` | Timestamp | Epoch / Session millisecond timestamp |
| `12..13`| `uint16` | Button Bitmask| Bit 0: A, 1: B, 2: X, 3: Y, 4..7: Dpad, 8: L1, 9: R1, 10: Start, 11: Select, 12: L3, 13: R3 |
| `14..15`| `int16` | Left Stick X | Normalized `[-32768, 32767]` |
| `16..17`| `int16` | Left Stick Y | Normalized `[-32768, 32767]` |
| `18..19`| `int16` | Right Stick X | Normalized `[-32768, 32767]` |
| `20..21`| `int16` | Right Stick Y | Normalized `[-32768, 32767]` |
| `22` | `uint8` | Trigger L2 | Pressure `[0, 255]` |
| `23` | `uint8` | Trigger R2 | Pressure `[0, 255]` |

***

## 5. TV Compatibility & Limitations

| Capability | Supported via WebSocket (Port 8001/8002) | Details |
|---|---|---|
| TV Menu Navigation | **YES** | Directional Up/Down/Left/Right, Enter, Return |
| Volume / Mute Control | **YES** | Mapped to L1/R1 or custom buttons |
| Home / Smart Hub / Apps | **YES** | Direct `KEY_HOME` invocation |
| Casual TV App Gaming | **YES** | Games responding to remote key events |
| Samsung Gaming Hub (Xbox Cloud) | **EXPERIMENTAL / UNSUPPORTED** | Samsung Gaming Hub requires direct kernel-level Bluetooth HID or USB HID controllers. Documented WebSocket ports do not inject analog stick coordinates into Gaming Hub cloud streams. |

***

## 6. Setup & Build Instructions

### Android Applet
- **Prerequisites**: Android SDK 24+ (Android 8.0 to Android 15/16).
- **Compile and Build**:
  ```bash
  gradle :app:assembleDebug
  ```
- **Run Tests**:
  ```bash
  gradle :app:testDebugUnitTest
  ```

### PC Companion Receiver
1. On your Windows PC, install Python 3.9+ and dependencies:
   ```bash
   pip install vgamepad
   ```
2. Run the receiver script:
   ```bash
   python receiver/spad_pc_receiver.py
   ```
3. In S!pad Settings -> Optional PC Receiver, enter your PC's local Wi-Fi IP address (e.g. `192.168.1.100`) and port `5150`.

***

## 7. Privacy & Security

- **100% Local**: S!pad communicates strictly over your local home Wi-Fi network.
- **Zero Cloud Tracking**: No analytics, telemetry, crash reporters, or mandatory cloud logins.
- **No Sensitive Permissions**: No location, microphone, or storage permissions required. Uses zero-permission Android Photo Picker if custom wallpapers are loaded.

***

## 8. Credits

**S!pad**  
Created by **Shubham Kumar**  
Made in India 🇮🇳  
All rights reserved. Original code, artwork, and design.
