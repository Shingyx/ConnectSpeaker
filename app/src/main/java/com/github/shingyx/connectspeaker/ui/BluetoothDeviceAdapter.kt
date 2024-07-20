package com.github.shingyx.connectspeaker.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.github.shingyx.connectspeaker.R
import com.github.shingyx.connectspeaker.data.BluetoothDeviceInfo

class BluetoothDeviceAdapter(
    private val activity: Activity,
) : BaseAdapter(),
    Filterable {
    private val devices = mutableListOf<BluetoothDeviceInfo>()
    private val filter = NoFilter()

    fun updateItems(items: List<BluetoothDeviceInfo>) {
        devices.clear()
        devices.addAll(items)
        notifyDataSetChanged()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?,
    ): View {
        val view =
            convertView
                ?: activity.layoutInflater.inflate(R.layout.dropdown_menu_popup_item, parent, false)
        (view as TextView).text = devices[position].toString()
        return view
    }

    override fun getItem(position: Int): BluetoothDeviceInfo = devices[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = devices.size

    override fun getFilter(): Filter = filter

    fun onItemClick(onItemClick: (item: BluetoothDeviceInfo) -> Unit): AdapterView.OnItemClickListener =
        AdapterView.OnItemClickListener { _, _, position, _ ->
            onItemClick.invoke(getItem(position))
        }

    private inner class NoFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults =
            FilterResults().apply {
                values = devices
                count = devices.size
            }

        override fun publishResults(
            constraint: CharSequence?,
            results: FilterResults?,
        ) {
            notifyDataSetChanged()
        }
    }
}
