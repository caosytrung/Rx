package vn.hdn.rxsample.screen

import android.os.Bundle
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import vn.hdn.rxsample.R
import vn.hdn.rxsample.adapter.BindingArrayAdapter
import vn.hdn.rxsample.adapter.holder.BindingHolder
import vn.hdn.rxsample.databinding.ActDodgeMultiClicksBinding
import vn.hdn.rxsample.databinding.ItemTimeClickBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DodgeMultiClicksActivity : AppCompatActivity() {

    private lateinit var binding: ActDodgeMultiClicksBinding
    private val liveClickCount = MutableLiveData<Long>()
    private val liveActionCount = MutableLiveData<Long>()
    private val liveThrotleMillis = MutableLiveData<Long>()

    private var clickDisposable: CompositeDisposable? = null
    private lateinit var clickSubject: PublishSubject<Long>
    private lateinit var loggerAdapter: ClickLogAdapter

    companion object {
        private const val THROTLE_MIN = 500L
        private const val THROTLE_STEP = 100L
        private const val THROTLE_STEP_COUNT = 45
        private const val THROTLE_STEP_DEFAULT = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clickDisposable = CompositeDisposable()
        binding = DataBindingUtil.setContentView(this, R.layout.act_dodge_multi_clicks)
        setUpDebounceTime()
        setUpLogger()
        setUpClickEvent()
    }

    private fun setUpDebounceTime() {
        binding.seekbarDebounceTime.apply {
            max = THROTLE_STEP_COUNT
            progress = THROTLE_STEP_DEFAULT
            liveThrotleMillis.value = THROTLE_STEP_DEFAULT * THROTLE_STEP + THROTLE_MIN
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    liveThrotleMillis.value = progress.toLong() * THROTLE_STEP + THROTLE_MIN
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    binding.btnAdd.isClickable = false
                    disposeClickObservables()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    binding.btnAdd.isClickable = true
                    createClickObservables()
                }

            })
        }
        liveThrotleMillis.observe(this, Observer {
            binding.lblDebounceTime.text = resources.getString(R.string.debounce_time, it)
        })
    }

    private fun setUpLogger() {
        loggerAdapter = ClickLogAdapter()
        binding.recyclerview.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = loggerAdapter
        }
    }

    private fun setUpClickEvent() {
        clickSubject = PublishSubject.create()
        binding.btnAdd.setOnClickListener {
            clickSubject.onNext(System.currentTimeMillis())
        }
        liveClickCount.observe(this, Observer {
            binding.lblClickCount.text = resources.getString(R.string.clickcount, it)
        })
        liveActionCount.observe(this, Observer {
            binding.lblEventCount.text = resources.getString(R.string.eventcount, it)
        })
        createClickObservables()
    }

    private fun createClickObservables() {
        disposeClickObservables()
        clickDisposable = CompositeDisposable()
        val throtleTime = liveThrotleMillis.value ?: THROTLE_MIN
        loggerAdapter.addData("Throtle duration set to $throtleTime ms")
        clickSubject.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { liveClickCount.safeAdjust(1) }
            .also { clickDisposable?.add(it) }
        clickSubject.subscribeOn(Schedulers.io())
            .throttleFirst(throtleTime, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                liveActionCount.safeAdjust(1)
                SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).run {
                    format(Date(it))
                }.let {
                    loggerAdapter.addData("Event fired at $it")
                }
            }
            .also { clickDisposable?.add(it) }
    }

    private fun disposeClickObservables() {
        clickDisposable?.run {
            if (!isDisposed) dispose()
        }
        clickDisposable = null
    }

    override fun onDestroy() {
        super.onDestroy()
        clickDisposable?.run {
            if (!isDisposed) dispose()
        }
        clickDisposable = null
    }

    private fun MutableLiveData<Long>.safeAdjust(amount: Long) {
        value = (value ?: 0L) + amount
    }
}

class ClickLogAdapter(data: MutableList<String>? = null) : BindingArrayAdapter<String>(data) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = Holder(parent)

    inner class Holder(parent: ViewGroup) :
        BindingHolder<ItemTimeClickBinding, String>(parent, R.layout.item_time_click) {

        override fun onBind(position: Int, model: String?) {
            super.onBind(position, model)
            binder.labeled.text = model
        }
    }
}