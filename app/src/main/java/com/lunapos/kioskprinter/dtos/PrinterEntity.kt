package com.lunapos.kioskprinter.dtos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lunapos.kioskprinter.enums.AutoCutEnum
import com.lunapos.kioskprinter.enums.PrinterModuleEnum
import com.lunapos.kioskprinter.enums.PrinterTypeEnum

@Entity(tableName = "printer")
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "printerDpi") val printerDpi: Int,
    @ColumnInfo(name = "printerWidthMM") val printerWidthMM: Float,
    @ColumnInfo(name = "printerNbrCharactersPerLine") val printerNbrCharactersPerLine: Int,
    @ColumnInfo(name = "printerType") val printerType: PrinterTypeEnum?,
    @ColumnInfo(name = "printerModule") val printerModule: PrinterModuleEnum?,
    @ColumnInfo(name = "autoCut") val autoCut: AutoCutEnum?,
    @ColumnInfo(name = "disconnectAfterPrint") val disconnectAfterPrint: Boolean?,
    @ColumnInfo(name = "printCopy") val printCopy: Int?,
    @ColumnInfo(name = "macAddress") val macAddress: String?,
    @ColumnInfo(name = "networkAddress") val networkAddress: String?,
    @ColumnInfo(name = "usbProductId") val usbProductId: Int?,
    @ColumnInfo(name = "usbVendorId") val usbVendorId: Int?,
    @ColumnInfo(name = "printerName") val printerName: String?
) {
    constructor(
        name: String,
        printerDpi: Int,
        printerWidthMM: Float,
        printerNbrCharactersPerLine: Int,
        printerType: PrinterTypeEnum?,
        printerModule: PrinterModuleEnum?,
        autoCut: AutoCutEnum?,
        disconnectAfterPrint: Boolean?,
        printCopy: Int?,
        macAddress: String?,
        networkAddress: String?,
        usbProductId: Int?,
        usbVendorId: Int?,
        printerName: String?
    ) : this(
        0, // 'id' is set to 0 in the secondary constructor
        name,
        printerDpi,
        printerWidthMM,
        printerNbrCharactersPerLine,
        printerType,
        printerModule,
        autoCut,
        disconnectAfterPrint,
        printCopy,
        macAddress,
        networkAddress,
        usbProductId,
        usbVendorId,
        printerName
    )
}