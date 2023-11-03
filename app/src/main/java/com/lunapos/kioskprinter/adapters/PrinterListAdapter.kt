package com.lunapos.kioskprinter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lunapos.kioskprinter.R
import com.lunapos.kioskprinter.singletons.AbstractPrinter

class PrinterListAdapter(private val dataSet: MutableList<AbstractPrinter>) : RecyclerView.Adapter<PrinterListAdapter.ViewHolder>() {
        /**
         * Provide a reference to the type of views that you are using
         * (custom ViewHolder)
         */
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView
            val testPrintButton: Button
            val deleteButton: Button

            init {
                // Define click listener for the ViewHolder's View
                title = view.findViewById(R.id.title)
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

            viewHolder.deleteButton.setOnClickListener {
                dataSet.removeAt(position)
                notifyDataSetChanged()
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = dataSet.size
}