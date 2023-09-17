package dstd.github.keepitquiet

import android.annotation.SuppressLint
import android.util.Log
import dstd.github.keepitquiet.utils.suppressExceptions
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class Logger {
    var enabled = false

    fun print(priority: Int, tag: String, msg: String) {
        if (!enabled)
            return

        Log.println(priority, tag, msg)
        val sync = priority == Log.ASSERT
        if (sync)
            lock.acquire()
        val timestamp = Date()
        worker.execute {
            printToFile(timestamp, priority, tag, msg)
            if (sync)
                lock.release()

            if (worker.isShutdown) {
                suppressExceptions {
                    _fileOutput?.close()
                    _fileOutput = null
                }
            }
        }
        flushFuture?.cancel(false)
        flushFuture = worker.schedule(flushTask, 3, TimeUnit.SECONDS)
        if (sync) {
            lock.acquire()
            lock.release()
        }
    }

    fun shutdown() {
        worker.shutdown()
    }

    private fun printToFile(timestamp: Date, priority: Int, tag: String, msg: String) {
        val out = fileOutput ?: return
        out.print(timeFormat.format(timestamp))
        out.print(" ")
        out.print(tag)
        out.print(": ")
        out.println(msg)

        if (priority == Log.VERBOSE || priority == Log.ASSERT)
            out.flush()

        rollIfNeeded()
    }

    private val flushTask = Runnable {
        Log.println(Log.DEBUG, "Logs", "#flush")
        fileOutput?.flush()
    }
    private var flushFuture: ScheduledFuture<*>? = null

    private fun rollIfNeeded() {
        val filePath = filePath ?: return
        if (filePath.length() < MAX_FILE_SIZE)
            return

        _fileOutput?.close()
        _fileOutput = null
    }

    private val worker = Executors.newSingleThreadScheduledExecutor()
    private val lock = Semaphore(1)
    private var filePath: File? = null
    private var _fileOutput: PrintStream? = null
    private val fileOutput: PrintStream? get() {
        if (_fileOutput != null || worker.isShutdown)
            return _fileOutput

        synchronized(lock) {
            if (_fileOutput != null)
                return _fileOutput

            return try {
                val context = App.context ?: return null
                val file = File("${context.filesDir.absolutePath}/logs/${generateLogName()}.log")
                file.parentFile?.mkdirs()
                PrintStream(BufferedOutputStream(FileOutputStream(file))).also {
                    filePath = file
                    _fileOutput = it
                }
            } catch (ignore: Throwable) {
                null
            }
        }
    }

    @SuppressLint("ConstantLocale")
    private val timeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun generateLogName() = SimpleDateFormat("yyyy-MM-dd--HH-mm-ss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    companion object {
        const val MAX_FILE_SIZE = 10_000_000
    }
}

var logger: Logger = Logger()

val <T: Any> KClass<T>.nameTag: String get() = this.java.name.substringAfterLast(".").removeSuffix("\$Companion")
inline fun Any.logd(msg: () -> String) {
    logger.print(Log.DEBUG, this::class.nameTag, msg())
}
