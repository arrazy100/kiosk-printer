package com.lunapos.kioskprinter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lunapos.kioskprinter.adapters.DropdownMenuListAdapter
import com.lunapos.kioskprinter.adapters.PrinterEmptyListAdapter
import com.lunapos.kioskprinter.adapters.PrinterListAdapter

class DropdownMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dropdown_menu)

        val title = intent.getStringExtra("title")
        val key = intent.getStringExtra("key")
        val data = intent.getStringArrayListExtra("data")
        val listView: RecyclerView = findViewById(R.id.list_view)
        val listAdapter = data?.let { DropdownMenuListAdapter(this, it, key!!) }
        listView.adapter = listAdapter!!
        listView.layoutManager = LinearLayoutManager(this)

        listAdapter.notifyDataSetChanged()

        val closeButton = findViewById<Button>(R.id.toolbar_title)
        closeButton.text = title
        closeButton.setOnClickListener {
            finish()
        }

    }
}