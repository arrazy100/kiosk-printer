package com.lunapos.kioskprinter.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.lunapos.kioskprinter.R

class DropdownMenuListAdapter(private val appCompatActivity: AppCompatActivity, private val dataSet: ArrayList<String>, private val key: String) : RecyclerView.Adapter<DropdownMenuListAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView

        init {
            // Define click listener for the ViewHolder's View
            title = view.findViewById(R.id.title)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.dropdown_list_view, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.title.text = dataSet[position]

        viewHolder.title.setOnClickListener {
            val intent = Intent()
            intent.putExtra(key, dataSet[position])
            appCompatActivity.setResult(Activity.RESULT_OK, intent)
            appCompatActivity.finish()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}