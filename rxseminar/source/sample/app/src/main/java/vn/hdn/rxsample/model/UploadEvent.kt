package vn.hdn.rxsample.model

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

sealed class UploadEvent
object Initializing : UploadEvent() {
    override fun toString(): String {
        return "Initializing"
    }
}

data class Running(val id: Int, val state: State) : UploadEvent() {
    override fun toString(): String {
        return "Running $id -> $state"
    }
}

data class Failed(val id: Int, val error: Throwable) : UploadEvent() {
    override fun toString(): String {
        return "Failed $id caused ${error.message}"
    }
}

data class Successful(val id: Int) : UploadEvent() {
    override fun toString(): String {
        return "Successful $id"
    }
}

object Submiting : UploadEvent() {
    override fun toString(): String {
        return "Submiting"
    }
}

data class GrandCompleted(val success: Boolean, val error: Throwable?) : UploadEvent() {
    override fun toString(): String {
        if (success)
            return "GrandCompleted success"
        else
            return "GrandCompleted failed caused" + (error?.message ?: "unknown")
    }
}

data class UploadFile(val id: Int, val url: String, var state: State)

enum class State {
    IDLE,
    WAITING,
    PRESIGNING,
    UPLOADING,
    COMPLETED,
    FAILED
}

object Simulation {
    const val INITIALIZING_DURATION = 250L//ms
    const val PRESIGN_DUR_MIN = 1000L//ms
    const val UPLOADING_DUR_MIN = 1000L//ms
    const val DEF_DUR_STEP = 50//ms
    const val PRESIGN_DUR_STEP_LIMIT = 10//times
    const val UPLOADING_DUR_STEP_LIMIT = 45//times
    const val SUBMITING_DURATION = 1500L//ms

    private const val ROOT_URL = "https://homepages.cae.wisc.edu/~ece533/images/"
    private val IMAGES = listOf(
        "airplane.png", "arctichare.png",
        "baboon.png", "barbara.png",
        "boat.png", "cat.png",
        "fruits.png", "frymire.png",
        "girl.png", "goldhill.png",
        "lena.png", "monarch.png",
        "mountain.png", "peppers.png",
        "pool.png", "sails.png",
        "serrano.png", "tulips.png",
        "watch.png", "zelda.png"
    )

    fun initializing(files: List<UploadFile>) =
        Observable.fromIterable(files).delay(INITIALIZING_DURATION, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())

    fun presign(file: UploadFile) = Observable.just(file)
        .delay(
            (0..PRESIGN_DUR_STEP_LIMIT).random() * DEF_DUR_STEP + PRESIGN_DUR_MIN,
            TimeUnit.MILLISECONDS
        ).subscribeOn(Schedulers.io())

    fun uploading(file: UploadFile) = Observable.just(file)
        .delay(
            (0..UPLOADING_DUR_STEP_LIMIT).random() * DEF_DUR_STEP + UPLOADING_DUR_MIN,
            TimeUnit.MILLISECONDS
        ).subscribeOn(Schedulers.io())

    fun submit() = Completable.complete().delay(SUBMITING_DURATION, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.io())

    fun randomImage() = ROOT_URL + IMAGES.random()
}