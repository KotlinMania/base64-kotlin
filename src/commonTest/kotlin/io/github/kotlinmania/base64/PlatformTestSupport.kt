package io.github.kotlinmania.base64

/**
 * Returns true if the current test runner execution environment is Kotlin/JS (Node.js or browser).
 *
 * Kotlin/JS IR compiles Kotlin classes with prototype registration and newThrowable runtime helpers,
 * which are uniquely visible in its execution call stacks, differentiating it from JVM, Native, and Wasm.
 */
internal val isJsPlatform: Boolean
    get() {
        val trace = Throwable().stackTraceToString()
        return (trace.contains(".protoOf.") || trace.contains("newThrowable")) && !trace.contains("wasm")
    }
