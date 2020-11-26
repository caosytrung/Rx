package vn.hdn.rxsample.screen

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import vn.hdn.rxsample.R
import vn.hdn.rxsample.adapter.BindingArrayAdapter
import vn.hdn.rxsample.adapter.holder.BindingHolder
import vn.hdn.rxsample.databinding.ActOptimizeMailBodyBinding
import vn.hdn.rxsample.databinding.ItemMailBinding
import vn.hdn.rxsample.model.Mail
import vn.hdn.rxsample.model.Repository
import vn.hdn.rxsample.model.Repository.fetchMail

class OptimizeMailLoadActivity : AppCompatActivity() {

    private var binding: ActOptimizeMailBodyBinding? = null
    private lateinit var bodyLoaderSubject: PublishSubject<List<Mail>>
    private var mailAdapter: MailAdapter? = null
    private var disposable: CompositeDisposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disposable = CompositeDisposable()
        binding = DataBindingUtil.setContentView(this, R.layout.act_optimize_mail_body)
        binding?.run {
            recyclerview.adapter = MailAdapter().also { mailAdapter = it }
            recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        scheduleFetchBody()
                    }
                }
            })
        }
        initMailBodyLoader()
    }

    override fun onResume() {
        super.onResume()
        mailAdapter?.clear()
        fetchMail(100)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ mailAdapter?.addData(it) }, { }, {
                binding?.recyclerview?.scrollBy(1, 1)
                scheduleFetchBody()
            }).also {
                disposable?.add(it)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.run {
            if (!isDisposed) dispose()
        }
        disposable = null
    }

    private fun scheduleFetchBody() {
        binding?.run {
            if (recyclerview.layoutManager is LinearLayoutManager) {
                (recyclerview.layoutManager as LinearLayoutManager).let {
                    val startIndex = it.findFirstVisibleItemPosition()
                    val endIndex = it.findLastVisibleItemPosition()
                    if (endIndex - startIndex > 0) {
                        mailAdapter?.getData()?.subList(startIndex, endIndex + 1)?.let {
                            bodyLoaderSubject.onNext(it)
                        }
                    }
                }
            }
        }
    }

    fun initMailBodyLoader(): Disposable {
        bodyLoaderSubject = PublishSubject.create()
        return bodyLoaderSubject.subscribeOn(Schedulers.io())
            .switchMap { Observable.fromIterable(it).filter { it.content == null } }
            .flatMap({ Repository.fetchBody(it.id) }, { mail, body -> mail.id to body }, 1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                mailAdapter?.run {
                    getData()[it.first].content = it.second
                    notifyItemChanged(it.first)
                }
            }
    }
}

class MailAdapter(data: MutableList<Mail>? = null) : BindingArrayAdapter<Mail>(data) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = Holder(parent)

    inner class Holder(parent: ViewGroup) :
        BindingHolder<ItemMailBinding, Mail>(parent, R.layout.item_mail) {

        override fun onBind(position: Int, model: Mail?) {
            super.onBind(position, model)
            binder.run {
                if (model == null) {
                    root.visibility = View.GONE
                } else {
                    model.let {
                        lblTitle.text = it.title
                        lblName.text = it.name
                        Glide.with(imgAvatar).load(it.avatar).into(imgAvatar)
                        lblContent.text = it.content
                    }
                }
            }
        }
    }
}