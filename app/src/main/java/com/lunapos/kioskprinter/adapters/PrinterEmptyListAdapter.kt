package com.lunapos.kioskprinter.adapters

import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class PrinterEmptyListAdapter constructor(rv: RecyclerView?, ev: View?, button: Button?): RecyclerView.AdapterDataObserver() {
    private var emptyView: View? = null
    private var recyclerView: RecyclerView? = null
    private var button: Button? = null

    init {
        this.recyclerView = rv
        this.emptyView = ev
        this.button = button

        checkIfEmpty()
    }

    private fun checkIfEmpty() {
        if (emptyView != null && recyclerView!!.adapter != null) {
            val emptyViewVisible = recyclerView!!.adapter!!.itemCount == 0

            emptyView!!.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
            recyclerView!!.visibility = if (emptyViewVisible) View.GONE else View.VISIBLE

            button!!.visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
        }
    }

    override fun onChanged() {
        super.onChanged()
        checkIfEmpty()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        checkIfEmpty()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        super.onItemRangeChanged(positionStart, itemCount)
        checkIfEmpty()
    }
}