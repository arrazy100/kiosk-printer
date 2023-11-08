package com.lunapos.kioskprinter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.text.method.TextKeyListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.google.android.material.textfield.TextInputLayout
import com.lunapos.kioskprinter.databinding.ActivityPrinterInputFormBinding
import com.lunapos.kioskprinter.enums.AutoCutEnum
import com.lunapos.kioskprinter.enums.PaperSizeEnum
import com.lunapos.kioskprinter.enums.PrinterModuleEnum
import com.lunapos.kioskprinter.enums.PrinterTypeEnum
import com.lunapos.kioskprinter.singletons.AUTO_CUT_KEY
import com.lunapos.kioskprinter.singletons.AUTO_CUT_TITLE
import com.lunapos.kioskprinter.singletons.BluetoothPrinterPermissions
import com.lunapos.kioskprinter.singletons.FORM_EDIT_KEY
import com.lunapos.kioskprinter.singletons.PAPER_SIZE_KEY
import com.lunapos.kioskprinter.singletons.PAPER_SIZE_TITLE
import com.lunapos.kioskprinter.singletons.PRINTER_ADDED_KEY
import com.lunapos.kioskprinter.singletons.PRINTER_KEY
import com.lunapos.kioskprinter.singletons.PRINTER_MODULE_KEY
import com.lunapos.kioskprinter.singletons.PRINTER_MODULE_TITLE
import com.lunapos.kioskprinter.singletons.PRINTER_TITLE
import com.lunapos.kioskprinter.singletons.PRINTER_TYPE_KEY
import com.lunapos.kioskprinter.singletons.PRINTER_TYPE_TITLE
import com.lunapos.kioskprinter.singletons.PRINTER_UPDATED_KEY
import com.lunapos.kioskprinter.singletons.PrinterData
import com.lunapos.kioskprinter.singletons.SharedPrefsManager
import com.lunapos.kioskprinter.singletons.USBPrinterPermission
import com.lunapos.kioskprinter.ui.FormFieldAutoComplete
import com.lunapos.kioskprinter.ui.FormFieldText
import com.lunapos.kioskprinter.ui.disable
import com.lunapos.kioskprinter.ui.enable
import com.lunapos.kioskprinter.ui.validate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.view.clicks

class PrinterInputForm : AppCompatActivity() {
    private val binding by lazy { ActivityPrinterInputFormBinding.inflate(layoutInflater) }

    private val printerNameField by lazy {
        FormFieldText(
            scope = lifecycleScope,
            textInputLayout = binding.tilPrinterName,
            textInputEditText = binding.etPrinterName,
            validation = { value ->
                when {
                    value.isNullOrBlank() -> "Nama printer harus diisi"
                    else -> null
                }
            }
        )
    }
    private val printerTypeField by lazy {
        FormFieldAutoComplete(
            scope = lifecycleScope,
            textInputLayout = binding.tilPrinterType,
            textInputEditText = binding.etPrinterType,
            validation = { value ->
                when {
                    value.isNullOrBlank() -> "Tipe printer harus dipilih"
                    else -> null
                }
            }
        )
    }
    private val printerModuleField by lazy {
        FormFieldAutoComplete(
            scope = lifecycleScope,
            textInputLayout = binding.tilPrinterModule,
            textInputEditText = binding.etPrinterModule,
            validation = { value ->
                when {
                    value.isNullOrBlank() -> "Modul printer harus dipilih"
                    else -> null
                }
            }
        )
    }
    private val printerField by lazy {
        FormFieldAutoComplete(
            scope = lifecycleScope,
            textInputLayout = binding.tilPrinter,
            textInputEditText = binding.etPrinter,
            validation = { value ->
                when {
                    value.isNullOrBlank() -> "Printer harus dipilih"
                    binding.etPrinterModule.text.toString() == PrinterModuleEnum.Network.toString() && !isHostPortValid(value) -> "Printer tidak valid"
                    else -> null
                }
            }
        )
    }
    private val paperSizeField by lazy {
        FormFieldAutoComplete(
            scope = lifecycleScope,
            textInputLayout = binding.tilPaperSize,
            textInputEditText = binding.etPaperSize,
            validation = { value ->
                when {
                    value.isNullOrBlank() -> "Ukuran kertas harus dipilih"
                    else -> null
                }
            }
        )
    }
    private val autoCutField by lazy {
        FormFieldAutoComplete(
            scope = lifecycleScope,
            textInputLayout = binding.tilAutoCut,
            textInputEditText = binding.etAutoCut,
            validation = { value ->
                when {
                    value.isNullOrBlank() -> "Pengaturan auto cut harus dipilih"
                    else -> null
                }
            }
        )
    }

    private fun isHostPortValid(value: String): Boolean {
        // Define a regular expression pattern for host:port
        val pattern = Regex("""^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}|\[([0-9A-Fa-f]*:[0-9A-Fa-f]*:[0-9A-Fa-f]*:[0-9A-Fa-f]*:[0-9A-Fa-f]*:[0-9A-Fa-f]*:[0-9A-Fa-f]*:[0-9A-Fa-f]*)\]):\d{1,5}$""")

        return pattern.matches(value)
    }

    private val formFields by lazy {
        listOf(
            printerNameField,
            printerTypeField,
            printerModuleField,
            printerField,
            paperSizeField,
            autoCutField
        )
    }

    private var currentCopyValue = 1
    private var bluetoothDeviceList = ArrayList<BluetoothConnection?>()
    private var usbDeviceList = ArrayList<UsbDevice>()
    private var macAddress: String? = null
    private var usbProductId: Int? = null
    private var usbVendorId: Int? = null

    private var isUpdate = false
    private var updateId: Int? = null

    @SuppressLint("MissingPermission")
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val printerTypeValue = result.data!!.getStringExtra(PRINTER_TYPE_KEY)
            val printerModuleValue = result.data!!.getStringExtra(PRINTER_MODULE_KEY)
            val printerValue = result.data!!.getStringExtra(PRINTER_KEY)
            val paperSizeValue = result.data!!.getStringExtra(PAPER_SIZE_KEY)
            val autoCutValue = result.data!!.getStringExtra(AUTO_CUT_KEY)

            if (printerTypeValue != null) binding.etPrinterType.setText(printerTypeValue)
            if (printerModuleValue != null) {
                binding.etPrinterModule.setText(printerModuleValue)
                binding.etPrinter.setText("")
                printerChooserIsEnabled(true)

                if (printerModuleValue.toString() == PrinterModuleEnum.Bluetooth.toString()) {
                    binding.etPrinter.hint = "Pilih printer bluetooth"
                    bluetoothDeviceList = BluetoothPrinterPermissions.getBluetoothDevices(applicationContext, this)
                }
                else if (printerModuleValue.toString() == PrinterModuleEnum.Network.toString()) {
                    binding.etPrinter.hint = "10.20.30.1:5001"
                    printerChooserIsEnabled(false)
                }
                else if (printerModuleValue.toString() == PrinterModuleEnum.USB.toString()) {
                    binding.etPrinter.hint = "Pilih printer USB"
                }
                else {
                    throw Exception("Modul printer tidak valid!")
                }
            }
            if (printerValue != null) {
                binding.etPrinter.setText(printerValue)
                val module = binding.etPrinterModule.text.toString()

                if (module == PrinterModuleEnum.Bluetooth.toString()) {
                    val selectedDevice =
                        bluetoothDeviceList.single { device ->
                            device!!.device.name == printerValue
                        }

                    macAddress = selectedDevice!!.device.address
                }
                else if (module == PrinterModuleEnum.USB.toString()) {
                    val selectedUsb =
                        usbDeviceList.single { device ->
                            device.deviceName == printerValue
                        }

                    usbProductId = selectedUsb.productId
                    usbVendorId = selectedUsb.vendorId

                    USBPrinterPermission.requestUsbDevicePermission(applicationContext, this, selectedUsb)
                }
            }
            if (paperSizeValue != null) binding.etPaperSize.setText(paperSizeValue)
            if (autoCutValue != null) binding.etAutoCut.setText(autoCutValue)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        SharedPrefsManager.init(applicationContext)

        formFields

        val formEdit = intent.getStringExtra(FORM_EDIT_KEY)
        if (formEdit != null) {
            val obj = SharedPrefsManager.readFromJSON(formEdit.toString())

            obj.retrieveDeviceConnection(applicationContext, this)

            if (obj.printerModule == PrinterModuleEnum.Network) {
                printerChooserIsEnabled(false)
            }

            printerNameField.value = obj.name
            printerTypeField.value = obj.printerType.toString()
            printerModuleField.value = obj.printerModule.toString()
            printerField.value = obj.getPrinter()
            paperSizeField.value = obj.paperSize!!.value.toString()
            autoCutField.value = obj.autoCut!!.value.toString()
            binding.checkboxDisconnect.isChecked = obj.disconnectAfterPrint == true
            binding.printCopyValue.text = obj.printCopy.toString()

            macAddress = obj.macAddress
            usbProductId = obj.usbProductId
            usbVendorId = obj.usbVendorId

            isUpdate = true
            updateId = obj.id

            if (printerModuleField.value == PrinterModuleEnum.Bluetooth.toString()) {
                bluetoothDeviceList = BluetoothPrinterPermissions.getBluetoothDevices(applicationContext, this)
            }
        }

        binding.toolbarTitle.setOnClickListener {
            finish()
        }

        binding.etPrinterType.setOnClickListener {
            val dataList = ArrayList(enumValues<PrinterTypeEnum>().map { it.name })
            val intent = Intent(this, DropdownMenu::class.java)
            intent.putExtra("title", PRINTER_TYPE_TITLE)
            intent.putExtra("key", PRINTER_TYPE_KEY)
            intent.putStringArrayListExtra("data", dataList)
            resultLauncher.launch(intent)
        }

        binding.etPrinterModule.setOnClickListener {
            val dataList = ArrayList(enumValues<PrinterModuleEnum>().map { it.name })
            val intent = Intent(this, DropdownMenu::class.java)
            intent.putExtra("title", PRINTER_MODULE_TITLE)
            intent.putExtra("key", PRINTER_MODULE_KEY)
            intent.putStringArrayListExtra("data", dataList)
            resultLauncher.launch(intent)
        }

        binding.etPrinter.setOnClickListener {
            printerClickListener()
        }

        binding.etPaperSize.setOnClickListener {
            val dataList = ArrayList(enumValues<PaperSizeEnum>().map { it.value.toString() })
            val intent = Intent(this, DropdownMenu::class.java)
            intent.putExtra("title", PAPER_SIZE_TITLE)
            intent.putExtra("key", PAPER_SIZE_KEY)
            intent.putStringArrayListExtra("data", dataList)
            resultLauncher.launch(intent)
        }

        binding.etAutoCut.setOnClickListener {
            val dataList = ArrayList(enumValues<AutoCutEnum>().map { it.value.toString() })
            val intent = Intent(this, DropdownMenu::class.java)
            intent.putExtra("title", AUTO_CUT_TITLE)
            intent.putExtra("key", AUTO_CUT_KEY)
            intent.putStringArrayListExtra("data", dataList)
            resultLauncher.launch(intent)
        }

        binding.copyDecrement.setOnClickListener {
            currentCopyValue -= 1

            if (currentCopyValue <= 0) currentCopyValue = 1

            binding.printCopyValue.text = currentCopyValue.toString()
        }

        binding.copyIncrement.setOnClickListener {
            currentCopyValue += 1

            binding.printCopyValue.text = currentCopyValue.toString()
        }

        binding.btnSubmit.clicks().onEach {
            submit()
        }.launchIn(lifecycleScope)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        BluetoothPrinterPermissions.onRequestPermissionsResult(
            applicationContext,
            this,
            requestCode,
            grantResults
        )
    }

    private fun submit() = lifecycleScope.launch {
        binding.btnSubmit.isEnabled = false

        formFields.disable()
        if (formFields.validate()) {
            val data = PrinterData()

            data.name = printerNameField.value.toString()
            data.printerName = printerModuleField.value.toString() + " - " + printerField.value.toString()
            data.printerType = PrinterTypeEnum.valueOf(printerTypeField.value.toString())

            // set on printer module
            data.printerModule = PrinterModuleEnum.valueOf(printerModuleField.value.toString())
            if (data.printerModule.toString() == PrinterModuleEnum.Bluetooth.toString()) {
                data.macAddress = macAddress
            }
            else if (data.printerModule.toString() == PrinterModuleEnum.USB.toString()) {
                data.usbProductId = usbProductId
                data.usbVendorId = usbVendorId
            }
            else if (data.printerModule.toString() == PrinterModuleEnum.Network.toString()) {
                data.networkAddress = printerField.value
            }

            // set on paper size
            data.paperSize = paperSizeField.value?.let { PaperSizeEnum.fromInt(it.toInt()) }
            if (data.paperSize == PaperSizeEnum.FiftyEight) {
                data.printerNbrCharactersPerLine = 32
                data.printerWidthMM = 58f
            }
            else if (data.paperSize == PaperSizeEnum.Sixty) {
                data.printerNbrCharactersPerLine = 32
                data.printerWidthMM = 60f
            }

            data.autoCut = autoCutField.value?.let { AutoCutEnum.fromInt(it.toInt()) }
            data.disconnectAfterPrint = binding.checkboxDisconnect.isChecked
            data.printCopy = currentCopyValue

            if (isUpdate) {
                SharedPrefsManager.updateListAt(updateId!!, data)
                val converted = SharedPrefsManager.writeAsJSON(data)
                val intent = Intent()
                intent.putExtra(PRINTER_UPDATED_KEY, converted)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            else {
                val serialized = SharedPrefsManager.writeToList(data)
                val intent = Intent()
                intent.putExtra(PRINTER_ADDED_KEY, serialized)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
        formFields.enable()

        binding.btnSubmit.isEnabled = true
    }

    private fun printerChooserIsEnabled(isEnabled : Boolean) {
        if (isEnabled) {
            binding.etPrinter.isClickable = true
            binding.etPrinter.isFocusable = false
            binding.etPrinter.setOnClickListener {
                printerClickListener()
            }
            binding.tilPrinter.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
        }
        else {
            binding.etPrinter.isClickable = false
            binding.etPrinter.isFocusable = true
            binding.etPrinter.isFocusableInTouchMode = true
            binding.etPrinter.setOnClickListener(null)
            binding.etPrinter.keyListener = TextKeyListener.getInstance()
            binding.tilPrinter.endIconMode = TextInputLayout.END_ICON_NONE
        }
    }

    private fun printerClickListener() {
        val dataList = generatePrinterList()
        val intent = Intent(this, DropdownMenu::class.java)
        intent.putExtra("title", PRINTER_TITLE)
        intent.putExtra("key", PRINTER_KEY)
        intent.putStringArrayListExtra("data", dataList)
        resultLauncher.launch(intent)
    }

    @SuppressLint("MissingPermission")
    private fun generatePrinterList() : ArrayList<String> {
        val module = binding.etPrinterModule.text

        var result = ArrayList<String>()

        if (module.toString() == PrinterModuleEnum.Bluetooth.toString()) {
            bluetoothDeviceList = BluetoothPrinterPermissions.getBluetoothDevices(applicationContext, this)
            result = BluetoothPrinterPermissions.listBluetoothDevice(bluetoothDeviceList)
        }
        else if (module.toString() == PrinterModuleEnum.USB.toString()) {
            usbDeviceList = USBPrinterPermission.getUsbDevices(applicationContext)
            result = USBPrinterPermission.listUsbDevice(usbDeviceList)
        }

        return result
    }
}