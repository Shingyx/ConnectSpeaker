package com.github.shingyx.connectspeaker.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.github.shingyx.connectspeaker.R
import com.github.shingyx.connectspeaker.data.ConnectAction

class ConnectActionAdapter(
    private val activity: Activity,
) : TypedAdapter<ConnectAction>(),
    Filterable {
    private val filter = NoFilter()
    private val connectActions = ConnectAction.entries.toTypedArray()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?,
    ): View {
        val view =
            convertView
                ?: activity.layoutInflater.inflate(R.layout.dropdown_menu_popup_item, parent, false)
        (view as TextView).setText(connectActions[position].actionStringResId)
        return view
    }

    override fun getItem(position: Int): ConnectAction = connectActions[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = connectActions.size

    override fun getFilter(): Filter = filter

    private inner class NoFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults =
            FilterResults().apply {
                values = connectActions
                count = connectActions.size
            }

        override fun publishResults(
            constraint: CharSequence?,
            results: FilterResults?,
        ) {
            notifyDataSetChanged()
        }
    }
}
