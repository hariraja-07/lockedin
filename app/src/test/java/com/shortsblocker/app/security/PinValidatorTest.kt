package com.shortsblocker.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinValidatorTest {

    @Test
    fun `short pin returns TooShort`() {
        val result = PinValidator.validate("12", "12")
        assertEquals(PinValidationResult.TooShort(4), result)
    }

    @Test
    fun `mismatched pin returns Mismatch`() {
        val result = PinValidator.validate("5829", "5820")
        assertEquals(PinValidationResult.Mismatch, result)
    }

    @Test
    fun `trivial sequential pin returns TrivialPin`() {
        assertEquals(PinValidationResult.TrivialPin, PinValidator.validate("1234", "1234"))
        assertEquals(PinValidationResult.TrivialPin, PinValidator.validate("4321", "4321"))
    }

    @Test
    fun `trivial repeated digits return TrivialPin`() {
        assertEquals(PinValidationResult.TrivialPin, PinValidator.validate("0000", "0000"))
        assertEquals(PinValidationResult.TrivialPin, PinValidator.validate("7777", "7777"))
    }

    @Test
    fun `valid non-trivial pin returns Valid`() {
        val result = PinValidator.validate("8492", "8492")
        assertEquals(PinValidationResult.Valid, result)
    }
}
