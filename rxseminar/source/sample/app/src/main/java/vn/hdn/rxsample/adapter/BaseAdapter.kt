package vn.hdn.rxsample.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import vn.hdn.rxsample.adapter.itf.OnDataChangedListener

abstract class BaseAdapter<HOLDER : RecyclerView.ViewHolder?> :
    RecyclerView.Adapter<HOLDER?>() {
    private var dataChangedListener: OnDataChangedListener? = null
    private var dataObserver: AdapterDataObserver? = null
    fun setDataChangedListener(dataChangedListener: OnDataChangedListener?) {
        this.dataChangedListener = dataChangedListener
        if (dataChangedListener == null) {
            dataObserver?.let {
                unregisterAdapterDataObserver(it)
            }
            dataObserver = null
            return
        }
        if (dataObserver == null) {
            initDataObserver()
        }
    }

    private fun initDataObserver() {
        dataObserver = object : AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                dataChangedListener?.run {
                    if (itemCount == 0) {
                        onDataSetEmpty()
                    } else {
                        onDataSetFilled()
                    }
                }
            }

            override fun onItemRangeInserted(
                positionStart: Int,
                itemCount: Int
            ) {
                super.onItemRangeInserted(positionStart, itemCount)
                dataChangedListener?.run {
                    if (itemCount > 0) {
                        onDataSetFilled()
                    } else {
                        onDataSetEmpty()
                    }
                }
            }

            override fun onItemRangeRemoved(
                positionStart: Int,
                itemCount: Int
            ) {
                super.onItemRangeRemoved(positionStart, itemCount)
                dataChangedListener?.run {
                    if (getItemCount() == 0)
                        onDataSetEmpty()
                }
            }
        }.also {
            registerAdapterDataObserver(it)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (dataChangedListener != null) {
            if (dataObserver == null) {
                initDataObserver()
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        dataObserver?.let {
            unregisterAdapterDataObserver(it)
        }
        dataObserver = null
    }

    open fun clear() {}
}