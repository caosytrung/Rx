package vn.hdn.rxsample.adapter

/**
 * Created by VTI Android Team on 3/30/2018.
 * Copyright © 2018 VTI Inc. All rights reserved.
 */
abstract class BindingArrayAdapter<M>(data: MutableList<out M>? = null) : BindingAdapter<M>() {
    protected var dataList: MutableList<M>

    init {
        dataList = ArrayList()
        data?.let {
            dataList.addAll(it)
        }
    }

    override fun getItem(position: Int): M {
        return dataList[position]
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun getData() = dataList

    fun addData(data: MutableList<out M>?) {
        val startOffset = itemCount
        if (data != null && data.size > 0) {
            dataList.addAll(data)
            notifyItemRangeInserted(startOffset, data.size)
        } else {
            notifyItemRangeInserted(startOffset, 0)
        }
    }

    fun addData(start: Int, data: MutableList<out M>?) {
        val startOffset = Math.min(start, dataList.size)
        if (data != null && data.size > 0) {
            dataList.addAll(start, data)
            notifyItemRangeInserted(startOffset, data.size)
        } else {
            notifyItemRangeInserted(startOffset, 0)
        }
    }

    fun addData(data: M?) {
        data?.let {
            dataList.add(it)
            notifyItemInserted(itemCount - 1)
        }
    }

    fun addData(position: Int, data: M?) {
        if (data != null) {
            dataList.add(position, data)
            notifyItemInserted(position)
        }
    }

    fun update(index: Int, newItem: M) {
        if (index >= 0 && index < dataList.size) {
            dataList.set(index, newItem)
            notifyItemChanged(index)
        }
    }

    fun remove(index: Int) {
        if (index >= 0 && index < dataList.size) {
            dataList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun remove(item: M) {
        val index = dataList.indexOf(item)
        if (index >= 0) {
            dataList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun remove(start: Int, end: Int) {
        if (start >= 0 && start <= end && end < dataList.size) {
            dataList.subList(start, end + 1).clear()
            notifyItemRangeRemoved(start, end - start + 1)
        }
    }

    override fun clear() {
        dataList.clear()
        notifyDataSetChanged()
    }
}