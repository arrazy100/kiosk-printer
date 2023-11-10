package com.lunapos.kioskprinter.enums

enum class PaperSizeEnum(val value: Int) {
    FortyEight(48),
    FiftySeven(57),
    SeventySix(76),
    SeventyEight(78),
    Eighty(80);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value } ?: throw IllegalArgumentException("Invalid type: $value")
    }
}