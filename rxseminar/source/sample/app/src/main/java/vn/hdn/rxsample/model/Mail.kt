package vn.hdn.rxsample.model

import com.thedeanda.lorem.LoremIpsum
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

data class Mail(
    val id: Int,
    val name: String,
    val title: String,
    val avatar: String,
    var content: String?
)

object Repository {

    fun fetchMail(count: Int) = Observable.range(0, count - 1)
        .map {
            Mail(
                it, LoremIpsum.getInstance().name,
                LoremIpsum.getInstance().getTitle(5, 10),
                Simulation.randomImage(),
                null
            )
        }

    fun fetchBody(id: Int) = Observable.just(id)
        .delay((0..10).random() * 50L + 250, TimeUnit.MILLISECONDS)
        .map { LoremIpsum.getInstance().getWords(10, 20) }
}