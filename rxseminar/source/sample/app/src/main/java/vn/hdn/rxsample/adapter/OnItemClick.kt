package vn.hdn.rxsample.adapter

import android.view.View

interface OnItemClick<T> {
    fun onItemClick(position: Int, view: View, t: T)
}