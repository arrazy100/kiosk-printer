package com.lunapos.kioskprinter.singletons

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.tcp.TcpConnection

class TcpPrinter : AbstractPrinter() {
    override fun onRequestPermissionsResult(
        context: Context,
        appCompatActivity: AppCompatActivity,
        requestCode: Int,
        grantResults: IntArray
    ) {
    }

    var ipAddress: String = ""
    var portAddress: String = ""
    var timeout: Int = 1000

    override suspend fun print(context: Context) {
        if (ipAddress.isNotBlank() && portAddress.isNotBlank()) {
            try {
                val printer = EscPosPrinter(
                    TcpConnection(ipAddress, portAddress.toInt(), timeout),
                    this.printerDpi, this.printerWidthMM, this.printerNbrCharactersPerLine
                )

                printer.printFormattedTextAndCut(this.text.trimIndent())
            }
            catch (_: Exception) {

            }
        }
    }
}