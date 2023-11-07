package com.lunapos.kioskprinter.enums

enum class PaperSizeEnum(val value: Int) {
    FiftyEight(58),
    Sixty(60);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value } ?: throw IllegalArgumentException("Invalid type: $value")
    }
}