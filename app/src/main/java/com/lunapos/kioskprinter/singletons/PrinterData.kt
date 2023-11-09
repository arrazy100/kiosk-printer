package com.lunapos.kioskprinter.singletons

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.fasterxml.jackson.annotation.JsonIgnore
import com.lunapos.kioskprinter.enums.AutoCutEnum
import com.lunapos.kioskprinter.enums.PaperSizeEnum
import com.lunapos.kioskprinter.enums.PrinterModuleEnum
import com.lunapos.kioskprinter.enums.PrinterTypeEnum

class PrinterData {
    /**
     * Don't set variable that need to be serialized to private
     **/

    var id: Int? = null
    var name: String = ""
    val printerDpi: Int = 203
    var printerWidthMM: Float = 0f
    var printerNbrCharactersPerLine: Int = 0
    var text: String = ""
    val timeout: Int = 1000

    var printerType: PrinterTypeEnum? = null
    var printerModule: PrinterModuleEnum? = null
    var paperSize: PaperSizeEnum? = null
    var autoCut: AutoCutEnum? = null
    var disconnectAfterPrint: Boolean? = null
    var printCopy: Int? = null

    var macAddress: String? = null
    var networkAddress: String? = null
    var usbProductId: Int? = null
    var usbVendorId: Int? = null

    var printerName: String? = null
    private var bluetoothConnection: BluetoothConnection? = null
    private var networkConnection: TcpConnection? = null
    private var usbConnection: UsbConnection? = null

    fun retrieveDeviceConnection(context: Context, appCompatActivity: AppCompatActivity) {
        if (printerModule?.equals(PrinterModuleEnum.Network) == true) {
            val address = networkAddress?.split(":")
            networkConnection = TcpConnection(address?.get(0), address?.get(1)!!.toInt(), timeout)
        }
        else if (printerModule?.equals(PrinterModuleEnum.Bluetooth) == true) {
            bluetoothConnection = BluetoothPrinterPermissions.retrieveBluetoothDevice(macAddress, context, appCompatActivity)
        }
        else if (printerModule?.equals(PrinterModuleEnum.USB) == true) {
            val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
            val usbDevice = USBPrinterPermission.retrieveUsbDevice(usbProductId!!, usbVendorId!!, context, appCompatActivity)
            usbConnection = UsbConnection(usbManager, usbDevice)
        }
        else {
            throw Exception("Modul printer tidak valid")
        }
    }

    suspend fun print(context: Context) {
        var connection: DeviceConnection? = null

        if (printerModule?.equals(PrinterModuleEnum.Network) == true) {
            if (networkConnection != null) {
                connection = networkConnection
            }
        }
        else if (printerModule?.equals(PrinterModuleEnum.Bluetooth) == true) {
            if (bluetoothConnection != null) {
                connection = bluetoothConnection
            }
        }
        else if (printerModule?.equals(PrinterModuleEnum.USB) == true) {
            if (usbConnection != null) {
                connection = usbConnection
            }
        }
        else {
            throw Exception("Modul printer tidak valid")
        }

        if (connection != null) {
            var printer: EscPosPrinter? = null
            try {
                printer = EscPosPrinter(
                    connection, this.printerDpi,
                    this.printerWidthMM, this.printerNbrCharactersPerLine
                )

                if (printCopy != null) {
                    for (num in 1..printCopy!!)
                        printer.printFormattedTextAndCut(this.text.trimIndent())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            finally {
                if (disconnectAfterPrint == true) printer?.disconnectPrinter()
            }
        }
    }

    @JsonIgnore
    @SuppressLint("MissingPermission")
    fun getPrinter(): String {
        return when (printerModule) {
            PrinterModuleEnum.Network -> networkAddress ?: ""
            PrinterModuleEnum.Bluetooth -> bluetoothConnection?.device?.name ?: ""
            PrinterModuleEnum.USB -> usbConnection?.device?.deviceName ?: ""
            else -> throw Exception("Modul printer tidak valid")
        }
    }

}