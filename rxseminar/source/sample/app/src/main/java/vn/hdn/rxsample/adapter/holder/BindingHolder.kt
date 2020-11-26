package vn.hdn.rxsample.adapter.holder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import vn.hdn.rxsample.adapter.OnItemClick

abstract class BindingHolder<BINDER : ViewDataBinding, MODEL> : RecyclerView.ViewHolder {
    protected var binder: BINDER
        private set
    protected var model: MODEL? = null
        private set

    constructor(binder: BINDER) : super(binder.root) {
        this.binder = binder
    }

    constructor(parent: ViewGroup, @LayoutRes layoutResId: Int)
            : super(inflateLayout(parent, layoutResId)) {
        binder = DataBindingUtil.bind(itemView)!!
    }

    fun registerRootViewAsHolderClickEvent(onRecyclerViewItemClick: OnItemClick<MODEL>?) {
        itemView.setOnClickListener(
            onRecyclerViewItemClick?.let { DelegateOnClick(it) }
        )
    }

    protected fun registerChildViewAsHolderClickEvent(
        view: View,
        onRecyclerViewItemClick: OnItemClick<MODEL>?
    ) {
        itemView.setOnClickListener(null)
        view.setOnClickListener(
            onRecyclerViewItemClick?.let { DelegateOnClick(it) }
        )
    }

    protected fun registerChildViewClickEvent(
        view: View,
        onRecyclerViewItemClick: OnItemClick<MODEL>?
    ) {
        view.setOnClickListener(
            onRecyclerViewItemClick?.let { DelegateOnClick(it) }
        )
    }

    @CallSuper
    open fun onBind(position: Int, model: MODEL?) {
        this.model = model
    }

    fun onUnbind() {}

    protected inner class DelegateOnClick(private val delegate: OnItemClick<MODEL>) :
        View.OnClickListener {
        override fun onClick(v: View) {
            model?.let {
                delegate.onItemClick(layoutPosition, v, it)
            }
        }

    }

    companion object {
        fun <BINDER : ViewDataBinding> inflateBinding(
            parent: ViewGroup,
            @LayoutRes layoutRes: Int
        ): BINDER {
            return DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                layoutRes,
                parent,
                false
            )
        }

        fun inflateLayout(parent: ViewGroup, @LayoutRes layoutRes: Int): View {
            return LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        }
    }
}