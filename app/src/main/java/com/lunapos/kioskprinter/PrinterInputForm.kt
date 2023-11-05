package com.lunapos.kioskprinter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class PrinterInputForm : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_input_form)

        val closeButton = findViewById<Button>(R.id.toolbar_title)
        closeButton.setOnClickListener {
            finish()
        }
    }
}