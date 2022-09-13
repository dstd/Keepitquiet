package dstd.github.silentwake.utils

inline fun suppressExceptions(block: () -> Unit) {
    try { block() } catch (ignore: Throwable) { }
}