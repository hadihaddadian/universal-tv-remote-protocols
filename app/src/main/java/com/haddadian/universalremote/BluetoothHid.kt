package com.haddadian.universalremote

/**
 * Bluetooth HID capability gate.
 *
 * A phone may support Bluetooth remote profiles differently by Android build.
 * The universal engine therefore exposes HID only when the platform/stack
 * reports the required capability. It must never claim mouse support otherwise.
 */
enum class HidCapability { NONE, KEYBOARD, MOUSE, KEYBOARD_AND_MOUSE }

data class HidProfile(
    val capability: HidCapability,
    val paired: Boolean,
    val deviceAddress: String?
)

class BluetoothHidController {
    fun detectCapability(): HidCapability {
        // Android's public Bluetooth APIs do not guarantee that every phone
        // can register itself as a HID device. A production implementation
        // should use the platform APIs available on the target OEM build or
        // a companion accessibility/input stack where permitted.
        return HidCapability.NONE
    }

    fun connect(profile: HidProfile): Result<Unit> =
        Result.failure(IllegalStateException(
            "Bluetooth HID device role is not universally exposed by Android."
        ))
}
