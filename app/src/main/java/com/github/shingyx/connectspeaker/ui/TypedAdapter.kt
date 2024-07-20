package com.github.shingyx.connectspeaker.ui

import android.widget.AdapterView
import android.widget.BaseAdapter

abstract class TypedAdapter<T> : BaseAdapter() {
    abstract override fun getItem(position: Int): T

    fun onItemClick(onItemClick: (item: T) -> Unit): AdapterView.OnItemClickListener =
        AdapterView.OnItemClickListener { _, _, position, _ ->
            onItemClick.invoke(getItem(position))
        }
}
