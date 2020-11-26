package vn.hdn.rxsample.screen

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.github.ybq.android.spinkit.style.CubeGrid
import com.github.ybq.android.spinkit.style.Wave
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import vn.hdn.rxsample.R
import vn.hdn.rxsample.adapter.BindingArrayAdapter
import vn.hdn.rxsample.adapter.holder.BindingHolder
import vn.hdn.rxsample.databinding.ActSequenceUploadBinding
import vn.hdn.rxsample.databinding.ItemImageUploadBinding
import vn.hdn.rxsample.model.*
import vn.hdn.rxsample.model.State.*
import java.lang.Integer.min

class SequenceUploadActivity : AppCompatActivity() {

    private lateinit var eventSubject: PublishSubject<UploadEvent>
    private var binding: ActSequenceUploadBinding? = null
    private lateinit var uploadAdapter: UploadAdapter
    private var disposable: CompositeDisposable? = null
    private var infoSnackbar: Snackbar? = null
    private var count = 0;
    private val limitConcurrent = MutableLiveData(1);
    private val usingObservable = MutableLiveData(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventSubject = PublishSubject.create()
        eventSubject.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onUploadEvent)
            .also {
                disposable = CompositeDisposable().apply {
                    add(it)
                }
            }
        binding = DataBindingUtil.setContentView(this, R.layout.act_sequence_upload)
        binding?.run {
            recyclerview.adapter = UploadAdapter().apply {
                addData(Observable.range(0, 20)
                    .map { UploadFile(it, Simulation.randomImage(), IDLE) }
                    .toList()
                    .blockingGet())
                notifyDataSetChanged()
            }.also {
                uploadAdapter = it
            }
            btnSequenceUpload.setOnClickListener {
                (usingObservable.value ?: false).let {
                    if (it)
                        launchGrandFlowWithObservable(
                            uploadAdapter.getData(),
                            limitConcurrent.value ?: 1
                        )
                    else
                        launchGrandFlowWithSubject(
                            uploadAdapter.getData(),
                            limitConcurrent.value ?: 1
                        )
                }.also {
                    disposable = CompositeDisposable().apply {
                        add(it)
                    }
                }
            }
            btnLimitConcurrent.setOnClickListener {
                limitConcurrent.value = (limitConcurrent.value ?: 0) % 5 + 1
            }
            limitConcurrent.observe(this@SequenceUploadActivity, Observer {
                binding?.run {
                    btnLimitConcurrent.apply {
                        when (it) {
                            1 -> setImageResource(R.drawable.ic_run_sequence)
                            2 -> setImageResource(R.drawable.ic_run_concurrent_2)
                            3 -> setImageResource(R.drawable.ic_run_concurrent_3)
                            4 -> setImageResource(R.drawable.ic_run_concurrent_4)
                            5 -> setImageResource(R.drawable.ic_run_concurrent_5)
                        }
                    }
                }
            })
            btnUsingObservable.setOnClickListener {
                usingObservable.value = usingObservable.value?.not() ?: true
            }
            usingObservable.observe(this@SequenceUploadActivity, Observer {
                val message: String
                if (it) {
                    btnUsingObservable.setImageResource(R.drawable.ic_action_observable)
                    message = "Upload Event sử dụng custom Observable"
                } else {
                    btnUsingObservable.setImageResource(R.drawable.ic_action_subject)
                    message = "Upload Event sử dụng PublishSubject"
                }
                Toast.makeText(this@SequenceUploadActivity, message, Toast.LENGTH_LONG).show()
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.run {
            if (!isDisposed) dispose()
        }
        disposable = null
        val newList = MutableList(5, { i -> Any() })
        val oldList = MutableList(10, { i -> Any() })
        newList.let {
            if (it.isEmpty()) null else it.subList(0, min(it.size, 5))
        }?.let {
            oldList.addAll(it)
        }
    }

    private fun onUploadEvent(uploadEvent: UploadEvent) {
        Timber.d("onUploadEvent on ${Thread.currentThread().name} event=$uploadEvent")
        when (uploadEvent) {
            is Initializing -> {
                binding?.run {
                    count = 0;
                    btnSequenceUpload.hide()
                    btnUsingObservable.hide()
                    infoSnackbar?.run { dismiss() }
                    infoSnackbar = Snackbar.make(
                        this.root,
                        "Initializing...",
                        Snackbar.LENGTH_INDEFINITE
                    ).apply {
                        show()
                    }
                }
            }
            is Running -> {
                uploadAdapter.run {
                    getData()[uploadEvent.id].state = uploadEvent.state
                    notifyItemChanged(uploadEvent.id)
                }
            }
            is Successful -> {
                uploadAdapter.run {
                    infoSnackbar?.apply {
                        count++
                        setText("Hoàn thành $count/${uploadAdapter.getItemCount()}")
                    }
                    getData()[uploadEvent.id].state = COMPLETED
                    notifyItemChanged(uploadEvent.id)
                }
            }
            is Failed -> {
                uploadAdapter.run {
                    getData()[uploadEvent.id].state = FAILED
                    notifyItemChanged(uploadEvent.id)
                    Timber.e(uploadEvent.error)
                }
            }
            is Submiting -> {
                infoSnackbar?.setText("Submiting...")
            }
            is GrandCompleted -> {
                infoSnackbar?.run { dismiss() }
                infoSnackbar = null
                val message =
                    if (uploadEvent.success) "Tải dữ liệu lên thành công" else "Đã có lỗi xảy ra"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                uploadEvent.error?.let {
                    Timber.e(it)
                }
            }
        }
    }

    private fun launchGrandFlowWithObservable(
        files: List<UploadFile>,
        @IntRange(from = 1, to = 5) limitConcurrent: Int
    ): Disposable {
        return UploadObservable(files, limitConcurrent)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this@SequenceUploadActivity::onUploadEvent)
    }

    private fun launchGrandFlowWithSubject(
        files: List<UploadFile>,
        @IntRange(from = 1, to = 5) limitConcurrent: Int
    ): Disposable {
        val init = Observable.just(files)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { eventSubject.onNext(Initializing) }
            .flatMap { Simulation.initializing(it) }
            .doOnNext { eventSubject.onNext(Running(it.id, WAITING)) }
        val iterableSource: Observable<UploadFile>
        if (limitConcurrent <= 1) {
            iterableSource = init.concatMap { launchUploadFlow(it) }
        } else {
            iterableSource = init.flatMap({ launchUploadFlow(it) }, limitConcurrent)
        }
        return iterableSource
            .toList()
            .doOnSuccess { eventSubject.onNext(Submiting) }
            .flatMapCompletable { Simulation.submit() }
            .subscribe({ eventSubject.onNext(GrandCompleted(true, null)) },
                { eventSubject.onNext(GrandCompleted(false, it)) })
    }

    private fun launchUploadFlow(file: UploadFile): Observable<UploadFile> {
        return Observable.just(file)
            .doOnNext { eventSubject.onNext(Running(it.id, PRESIGNING)) }
            .flatMap { Simulation.presign(it) }
            .doOnNext { eventSubject.onNext(Running(it.id, UPLOADING)) }
            .flatMap { Simulation.uploading(it) }
            .doOnNext { eventSubject.onNext(Successful(it.id)) }
            .doOnError { eventSubject.onNext(Failed(file.id, it)) }
    }
}

class UploadAdapter(data: MutableList<UploadFile>? = null) : BindingArrayAdapter<UploadFile>(data) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = Holder(parent)

    inner class Holder(parent: ViewGroup) :
        BindingHolder<ItemImageUploadBinding, UploadFile>(parent, R.layout.item_image_upload) {

        init {
            binder.apply {
                progressPresign.indeterminateDrawable = Wave()
                progressUpload.indeterminateDrawable = CubeGrid()
            }
        }

        override fun onBind(position: Int, model: UploadFile?) {
            super.onBind(position, model)
            binder.apply {
                if (model == null) {
                    root.visibility = View.GONE
                } else {
                    root.visibility = View.VISIBLE
                    model.let {
                        Glide.with(imgThumb).load(it.url).into(imgThumb)
                        imgThumb.foreground =
                            if (it.state == IDLE) ColorDrawable(Color.TRANSPARENT)
                            else ColorDrawable(
                                ResourcesCompat.getColor(
                                    root.resources,
                                    R.color.foregroundShadow,
                                    null
                                )
                            )
                        progressPresign.visibility = View.GONE
                        progressUpload.visibility = View.GONE
                        imgState.setImageDrawable(null)
                        when (it.state) {
                            IDLE -> {
                            }
                            WAITING -> {
                            }
                            PRESIGNING -> {
                                progressPresign.visibility = View.VISIBLE
                            }
                            UPLOADING -> {
                                progressUpload.visibility = View.VISIBLE
                            }
                            COMPLETED -> {
                                imgState.setImageResource(R.drawable.ic_action_cloud_done)
                            }
                            FAILED -> {
                                imgState.setImageResource(R.drawable.ic_action_cloud_off)
                            }
                        }
                    }
                }
            }
        }
    }
}

class UploadObservable(val uploadFiles: List<UploadFile>, val limitConcurrent: Int) :
    Observable<UploadEvent>() {

    override fun subscribeActual(observer: io.reactivex.Observer<in UploadEvent>) {
        val delegatedObserver = DelegatedObserver(observer)
        val init = just(uploadFiles)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { delegatedObserver.onNext(Initializing) }
            .flatMap { Simulation.initializing(it) }
            .doOnNext { delegatedObserver.onNext(Running(it.id, WAITING)) }
        val iterableSource: Observable<UploadFile>
        if (limitConcurrent <= 1) {
            iterableSource = init.concatMap { launchUploadFlow(it, delegatedObserver) }
        } else {
            iterableSource =
                init.flatMap({ launchUploadFlow(it, delegatedObserver) }, limitConcurrent)
        }
        iterableSource
            .toList()
            .doOnSuccess { delegatedObserver.onNext(Submiting) }
            .flatMapCompletable { Simulation.submit() }
            .andThen(just(GrandCompleted(true, null)))
            .onErrorReturn { GrandCompleted(false, it) }
            .subscribe(delegatedObserver)
    }

    private fun launchUploadFlow(
        file: UploadFile,
        delegatedObserver: DelegatedObserver
    ): Observable<UploadFile> {
        return just(file)
            .doOnNext { delegatedObserver.onNext(Running(it.id, PRESIGNING)) }
            .flatMap { Simulation.presign(it) }
            .doOnNext { delegatedObserver.onNext(Running(it.id, UPLOADING)) }
            .flatMap { Simulation.uploading(it) }
            .doOnNext { delegatedObserver.onNext(Successful(it.id)) }
            .doOnError { delegatedObserver.onNext(Failed(file.id, it)) }
    }

    class DelegatedObserver(
        val actualDownstream: io.reactivex.Observer<in UploadEvent>
    ) : io.reactivex.Observer<UploadEvent>, Disposable {

        private var upstream: Disposable? = null

        override fun onComplete() {
            actualDownstream.onComplete()
        }

        override fun onSubscribe(d: Disposable) {
            upstream = d
            actualDownstream.onSubscribe(this)
        }

        override fun onNext(t: UploadEvent) {
            actualDownstream.onNext(t)
        }

        override fun onError(e: Throwable) {
            actualDownstream.onError(e)
        }

        override fun isDisposed() = upstream?.isDisposed ?: true

        override fun dispose() = upstream?.dispose() ?: Unit

    }
}