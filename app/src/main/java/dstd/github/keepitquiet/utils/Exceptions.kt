package dstd.github.keepitquiet.utils

inline fun suppressExceptions(block: () -> Unit) {
    try { block() } catch (ignore: Throwable) { }
}