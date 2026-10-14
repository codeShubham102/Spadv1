#!/usr/bin/env python3
"""
S!pad PC Receiver — Low-Latency Virtual Gamepad Server
Author: Shubham Kumar
Country: India 🇮🇳

Receives 24-byte UDP packets from S!pad Android client and maps them
to a virtual Xbox 360 controller using vgamepad (ViGEmBus driver)
or falls back to diagnostic printing mode if driver is not installed.
"""

import socket
import struct
import sys
import time

# Protocol Constants
MAGIC_HEADER = 0x5350  # "SP"
EXPECTED_VERSION = 1
PACKET_SIZE = 24
DEFAULT_PORT = 5150
TIMEOUT_SECONDS = 0.6  # Reset controller if no packet for 600ms

# Button Bitmask Flags (matching Android SpadPacketSerializer)
BTN_A = 1 << 0
BTN_B = 1 << 1
BTN_X = 1 << 2
BTN_Y = 1 << 3
BTN_DPAD_UP = 1 << 4
BTN_DPAD_DOWN = 1 << 5
BTN_DPAD_LEFT = 1 << 6
BTN_DPAD_RIGHT = 1 << 7
BTN_L1 = 1 << 8
BTN_R1 = 1 << 9
BTN_START = 1 << 10
BTN_SELECT = 1 << 11
BTN_L3 = 1 << 12
BTN_R3 = 1 << 13
BTN_GUIDE = 1 << 14

# Attempt to import vgamepad (ViGEmBus wrapper)
try:
    import vgamepad as vg
    VIRTUAL_PAD_AVAILABLE = True
except ImportError:
    VIRTUAL_PAD_AVAILABLE = False


def print_banner(port: int):
    print("=" * 60)
    print("           S!pad PC Receiver v1.0")
    print("      Made by Shubham Kumar — Made in India 🇮🇳")
    print("=" * 60)
    print(f"[*] Listening on UDP 0.0.0.0:{port}")
    if VIRTUAL_PAD_AVAILABLE:
        print("[+] ViGEmBus Virtual Xbox 360 Controller: ACTIVE")
    else:
        print("[!] ViGEmBus / vgamepad not found.")
        print("[!] Running in DIAGNOSTIC DISPLAY MODE.")
        print("[!] (Install 'pip install vgamepad' and ViGEmBus driver for game injection)")
    print("=" * 60)
    print("[*] Waiting for S!pad Android controller connection...\n")


def parse_packet(data: bytes):
    if len(data) != PACKET_SIZE:
        return None

    # Unpack Little-Endian:
    # <H (uint16 magic), B (uint8 version), B (uint8 flags),
    #  I (uint32 seq), I (uint32 timestamp),
    #  H (uint16 buttons),
    #  h (int16 stick_lx), h (int16 stick_ly),
    #  h (int16 stick_rx), h (int16 stick_ry),
    #  B (uint8 trigger_l), B (uint8 trigger_r)
    magic, ver, flags, seq, ts, buttons, lx, ly, rx, ry, lt, rt = struct.unpack(
        "<HBBIIHhhhhBB", data
    )

    if magic != MAGIC_HEADER or ver != EXPECTED_VERSION:
        return None

    return {
        "sequence": seq,
        "timestamp": ts,
        "buttons": buttons,
        "stick_lx": lx,
        "stick_ly": ly,
        "stick_rx": rx,
        "stick_ry": ry,
        "trigger_l": lt,
        "trigger_r": rt,
    }


def main():
    port = DEFAULT_PORT
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print(f"Invalid port: {sys.argv[1]}. Using default {DEFAULT_PORT}")

    print_banner(port)

    # Initialize Virtual Gamepad
    gamepad = vg.VX360Gamepad() if VIRTUAL_PAD_AVAILABLE else None

    # Create UDP socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(("0.0.0.0", port))
    sock.settimeout(0.1)

    last_packet_time = 0.0
    connected_client = None
    packet_count = 0

    try:
        while True:
            try:
                data, addr = sock.recvfrom(64)
                parsed = parse_packet(data)
                if not parsed:
                    continue

                now = time.time()
                last_packet_time = now

                if connected_client != addr:
                    connected_client = addr
                    print(f"[+] Client connected from {addr[0]}:{addr[1]}")

                packet_count += 1

                # Update virtual controller
                if gamepad:
                    # Analog sticks: normalize to [-32768, 32767]
                    gamepad.left_joystick(x_value=parsed["stick_lx"], y_value=parsed["stick_ly"])
                    gamepad.right_joystick(x_value=parsed["stick_rx"], y_value=parsed["stick_ry"])

                    # Triggers: 0..255
                    gamepad.left_trigger(value=parsed["trigger_l"])
                    gamepad.right_trigger(value=parsed["trigger_r"])

                    # Digital buttons
                    b = parsed["buttons"]
                    if b & BTN_A: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_A)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_A)

                    if b & BTN_B: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_B)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_B)

                    if b & BTN_X: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_X)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_X)

                    if b & BTN_Y: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_Y)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_Y)

                    if b & BTN_DPAD_UP: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP)

                    if b & BTN_DPAD_DOWN: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN)

                    if b & BTN_DPAD_LEFT: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT)

                    if b & BTN_DPAD_RIGHT: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT)

                    if b & BTN_L1: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER)

                    if b & BTN_R1: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER)

                    if b & BTN_START: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_START)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_START)

                    if b & BTN_SELECT: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK)

                    if b & BTN_L3: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB)

                    if b & BTN_R3: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB)

                    if b & BTN_GUIDE: gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_GUIDE)
                    else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_GUIDE)

                    gamepad.update()
                else:
                    # Diagnostic print every 30 packets (~500ms)
                    if packet_count % 30 == 0:
                        print(
                            f"\r[Diag] Seq:{parsed['sequence']} | "
                            f"LX:{parsed['stick_lx']:+06d} LY:{parsed['stick_ly']:+06d} | "
                            f"RX:{parsed['stick_rx']:+06d} RY:{parsed['stick_ry']:+06d} | "
                            f"LT:{parsed['trigger_l']:03d} RT:{parsed['trigger_r']:03d} | "
                            f"Btns:0x{parsed['buttons']:04X}",
                            end="",
                            flush=True,
                        )

            except socket.timeout:
                # Check for client watchdog timeout
                if connected_client and (time.time() - last_packet_time > TIMEOUT_SECONDS):
                    print(f"\n[-] Client {connected_client} timed out. Resetting controller state to neutral.")
                    connected_client = None
                    if gamepad:
                        gamepad.reset()
                        gamepad.update()

    except KeyboardInterrupt:
        print("\n[*] Shutting down S!pad PC receiver. Goodbye!")
    finally:
        sock.close()
        if gamepad:
            gamepad.reset()
            gamepad.update()


if __name__ == "__main__":
    main()
