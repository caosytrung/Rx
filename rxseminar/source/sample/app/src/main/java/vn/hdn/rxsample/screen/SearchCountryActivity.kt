package vn.hdn.rxsample.screen

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.hbb20.CCPCountry
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import vn.hdn.rxsample.R
import vn.hdn.rxsample.adapter.BindingArrayAdapter
import vn.hdn.rxsample.adapter.holder.BindingHolder
import vn.hdn.rxsample.databinding.ActSearchCountryBinding
import vn.hdn.rxsample.databinding.ItemCountryBinding
import java.util.concurrent.TimeUnit

class SearchCountryActivity : AppCompatActivity() {

    private lateinit var binding: ActSearchCountryBinding
    private var searchDisposable: CompositeDisposable? = null
    private lateinit var keywordSubject: BehaviorSubject<String>
    private lateinit var countryAdapter: CountryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        countryAdapter = CountryAdapter()
        binding = DataBindingUtil.setContentView(this, R.layout.act_search_country)
        binding.recyclerview.run {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = countryAdapter
        }
        binding.svQuery.apply {
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    keywordSubject.onNext(newText ?: "")
                    return false
                }
            })
        }
        searchDisposable = CompositeDisposable()
        keywordSubject = BehaviorSubject.create()
        keywordSubject.subscribeOn(Schedulers.io())
            .debounce(500, TimeUnit.MILLISECONDS)
            .map { it.trim() }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { countryAdapter.clear() }
            .observeOn(Schedulers.io())
            .switchMap {
                if (it.isEmpty()) {
                    Observable.fromIterable(CCPCountry.getLibraryMasterCountriesEnglish())
                } else {
                    val keyword = it;
                    Observable.fromIterable(CCPCountry.getLibraryMasterCountriesEnglish())
                        .filter { it.name.contains(keyword, ignoreCase = true) }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { countryAdapter.addData(it) }
            .also { searchDisposable?.add(it) }

    }

    override fun onDestroy() {
        super.onDestroy()
        searchDisposable?.run {
            if (isDisposed) dispose()
        }
        searchDisposable = null
    }
}

class CountryAdapter(data: MutableList<CCPCountry>? = null) :
    BindingArrayAdapter<CCPCountry>(data) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = Holder(parent)

    inner class Holder(parent: ViewGroup) :
        BindingHolder<ItemCountryBinding, CCPCountry>(parent, R.layout.item_country) {

        override fun onBind(position: Int, model: CCPCountry?) {
            super.onBind(position, model)
            binder.apply {
                if (model != null) {
                    root.visibility = View.VISIBLE
                    model.let {
                        imgFlag.setImageResource(it.flagID)
                        lblCountryName.text = it.name
                    }
                } else {
                    root.visibility = View.GONE
                }
            }
        }
    }
}