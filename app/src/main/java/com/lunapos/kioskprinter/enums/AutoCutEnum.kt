package com.lunapos.kioskprinter.enums

enum class AutoCutEnum(val value: Int) {
    True(1),
    False(0);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value } ?: throw IllegalArgumentException("Invalid type: $value")
    }
}