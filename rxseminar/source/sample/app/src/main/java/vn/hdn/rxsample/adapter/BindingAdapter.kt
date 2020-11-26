package vn.hdn.rxsample.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import vn.hdn.rxsample.adapter.holder.BindingHolder

abstract class BindingAdapter<M> :
    BaseAdapter<BindingHolder<out ViewDataBinding, M>>() {
    protected var itemClickListener: OnItemClick<M>? = null
        private set

    override fun onBindViewHolder(
        holder: BindingHolder<out ViewDataBinding, M>,
        position: Int
    ) {
        holder.onBind(position, getItem(position))
    }

    override fun onViewDetachedFromWindow(holder: BindingHolder<out ViewDataBinding, M>) {
        super.onViewDetachedFromWindow(holder)
        holder.onUnbind()
    }

    fun setItemClickListener(itemClickListener: OnItemClick<M>?) {
        this.itemClickListener = itemClickListener
    }

    protected fun setHolderRootViewAsItemClick(target: BindingHolder<out ViewDataBinding, M>) {
        target.registerRootViewAsHolderClickEvent(itemClickListener)
    }

    override fun clear() {}
    abstract fun getItem(position: Int): M?

    companion object {
        protected fun <T : ViewDataBinding> inflate(
            parent: ViewGroup,
            @LayoutRes layoutId: Int
        ): T {
            val inflater = LayoutInflater.from(parent.context)
            return DataBindingUtil.inflate(inflater, layoutId, parent, false)
        }
    }
}