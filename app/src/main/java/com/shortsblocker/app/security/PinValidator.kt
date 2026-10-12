package com.shortsblocker.app.security

sealed class PinValidationResult {
    data object Valid : PinValidationResult()
    data class TooShort(val minLength: Int) : PinValidationResult()
    data object Mismatch : PinValidationResult()
    data object TrivialPin : PinValidationResult()
}

object PinValidator {

    private val TRIVIAL_PINS = setOf(
        "1234", "4321", "0123", "9876", "2580",
        "0000", "1111", "2222", "3333", "4444",
        "5555", "6666", "7777", "8888", "9999",
        "123456", "654321", "000000", "111111",
        "222222", "333333", "444444", "555555",
        "666666", "777777", "888888", "999999",
    )

    fun validate(pin: String, confirmPin: String, minLength: Int = 4): PinValidationResult {
        if (pin.length < minLength) {
            return PinValidationResult.TooShort(minLength)
        }
        if (pin != confirmPin) {
            return PinValidationResult.Mismatch
        }
        if (isTrivial(pin)) {
            return PinValidationResult.TrivialPin
        }
        return PinValidationResult.Valid
    }

    fun isTrivial(pin: String): Boolean {
        if (TRIVIAL_PINS.contains(pin)) return true
        if (pin.isNotEmpty() && pin.all { it == pin[0] }) return true
        return false
    }
}
