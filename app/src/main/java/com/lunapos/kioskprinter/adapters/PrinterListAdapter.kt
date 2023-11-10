package com.lunapos.kioskprinter.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.lunapos.kioskprinter.Constants.FORM_EDIT_KEY
import com.lunapos.kioskprinter.PrinterInputForm
import com.lunapos.kioskprinter.R
import com.lunapos.kioskprinter.dtos.PrinterData
import com.lunapos.kioskprinter.viewModel.PrinterEntityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrinterListAdapter(private val context: Context, private val resultLauncher: ActivityResultLauncher<Intent>, var dataSet: MutableList<PrinterData>) : RecyclerView.Adapter<PrinterListAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    private var printing = false
    private var printerEntityViewModel: PrinterEntityViewModel? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView
        val tilSelectedPrinter: TextInputLayout
        val selectedPrinter: AutoCompleteTextView
        val testPrintButton: Button
        val deleteButton: Button

        init {
            // Define click listener for the ViewHolder's View
            title = view.findViewById(R.id.title)
            tilSelectedPrinter = view.findViewById(R.id.til_selected_printer)
            selectedPrinter = view.findViewById(R.id.et_printer_name)
            testPrintButton = view.findViewById(R.id.btn_test_print)
            deleteButton = view.findViewById(R.id.btn_delete)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.printer_list_view, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.title.text = dataSet[position].name

        viewHolder.selectedPrinter.setText(dataSet[position].printerName)

        viewHolder.tilSelectedPrinter.setEndIconOnClickListener {
            val intent = Intent(context, PrinterInputForm::class.java)
            intent.putExtra(FORM_EDIT_KEY, dataSet[position])
            resultLauncher.launch(intent)
        }

        viewHolder.selectedPrinter.setOnClickListener {
            val intent = Intent(context, PrinterInputForm::class.java)
            intent.putExtra(FORM_EDIT_KEY, dataSet[position])
            resultLauncher.launch(intent)
        }

        viewHolder.testPrintButton.setOnClickListener {
            doPrint(dataSet[position], viewHolder.testPrintButton)
        }

        viewHolder.deleteButton.setOnClickListener {
            showAlertDialog(dataSet[position])
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    private fun doPrint(printer : PrinterData?, button : Button) {
        printing = true
        button.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                launch {
                    printer!!.text = "Tes"
                    printer.print(context)
                }

                withContext(Dispatchers.Main) {
                    // Enable the button
                    button.isEnabled = true

                    // Enable the print
                    printing = false
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    button.isEnabled = true
                }
            }
            finally {
                printing = false
            }
        }
    }

    private fun showAlertDialog(itemToRemove: PrinterData) {
        // Create and show the AlertDialog here, similar to the previous example
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.alert_dialog, null)
        val positiveButton = view.findViewById<Button>(R.id.positiveButton)
        val negativeButton = view.findViewById<Button>(R.id.negativeButton)

        val alertDialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        positiveButton.setOnClickListener {
            alertDialog.dismiss()

            val index = dataSet.indexOfFirst { it.id == itemToRemove.id }

            if (index != -1) {
                dataSet.remove(itemToRemove)
                printerEntityViewModel!!.deletePrinter(itemToRemove.id!!)
            }
        }

        negativeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    fun setViewModel(viewModel: PrinterEntityViewModel) {
        printerEntityViewModel = viewModel
    }

    fun setData(newDataSet: MutableList<PrinterData>) {
        dataSet = newDataSet
    }
}