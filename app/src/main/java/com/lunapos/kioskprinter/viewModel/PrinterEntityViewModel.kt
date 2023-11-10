package com.lunapos.kioskprinter.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.lunapos.kioskprinter.PrinterUtil.PrinterEntityDatabase
import com.lunapos.kioskprinter.dtos.PrinterData
import com.lunapos.kioskprinter.dtos.PrinterEntity
import com.lunapos.kioskprinter.dtos.PrinterEntityDAO
import com.lunapos.kioskprinter.enums.AutoCutEnum
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PrinterEntityViewModel(application: Application) : AndroidViewModel(application) {
    private val printerDao: PrinterEntityDAO
    private val executorService: ExecutorService

    init {
        printerDao = PrinterEntityDatabase.getInstance(application).printerDao()
        executorService = Executors.newSingleThreadExecutor()
    }

    val allPrinters: LiveData<List<PrinterEntity>>
        get() = printerDao.getAll()

    fun getPrinters(): List<PrinterEntity> {
        var printers = listOf<PrinterEntity>()
        executorService.execute {
            printers = printerDao.getAllNonLive()
        }

        return printers
    }

    fun convertToPrinterData(printers: List<PrinterEntity>): MutableList<PrinterData> {
        val printersData = mutableListOf<PrinterData>()

        printers.forEach { printer ->
            val data = PrinterData()
            data.id = printer.id
            data.name = printer.name
            data.printerDpi = printer.printerDpi
            data.printerWidthMM = printer.printerWidthMM
            data.printerNbrCharactersPerLine = printer.printerNbrCharactersPerLine
            data.printerType = printer.printerType
            data.printerModule = printer.printerModule
            data.autoCut = printer.autoCut
            data.disconnectAfterPrint = printer.disconnectAfterPrint
            data.printCopy = printer.printCopy
            data.macAddress = printer.macAddress
            data.networkAddress = printer.networkAddress
            data.usbProductId = printer.usbProductId
            data.usbVendorId = printer.usbVendorId
            data.printerName = printer.printerName

            printersData.add(data)
        }

        return printersData
    }

    fun savePrinter(printer: PrinterEntity?): Int {
        var insertedId = 0

        executorService.execute {
            if (printer != null) {
                insertedId = printerDao.insert(printer).toInt()
            }
        }

        return insertedId
    }

    fun updatePrinter(printer: PrinterEntity?) {
        executorService.execute {
            if (printer != null) {
                printerDao.update(printer)
            }
        }
    }

    fun deletePrinter(id: Int) {
        executorService.execute {
            printerDao.delete(id)
        }
    }

    fun convertToPrinterEntity(printer: PrinterData) : PrinterEntity {
        return PrinterEntity(
            name = printer.name,
            printerDpi = printer.printerDpi,
            printerWidthMM = printer.printerWidthMM,
            printerNbrCharactersPerLine = printer.printerNbrCharactersPerLine,
            printerType = printer.printerType,
            printerModule = printer.printerModule,
            autoCut = printer.autoCut ?: AutoCutEnum.False,
            disconnectAfterPrint = printer.disconnectAfterPrint ?: false,
            printCopy = printer.printCopy ?: 1,
            macAddress = printer.macAddress,
            networkAddress = printer.networkAddress,
            usbProductId = printer.usbProductId,
            usbVendorId = printer.usbVendorId,
            printerName = printer.printerName
        )
    }
}