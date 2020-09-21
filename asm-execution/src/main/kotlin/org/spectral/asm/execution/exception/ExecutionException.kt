package org.spectral.asm.execution.exception

open class ExecutionException : RuntimeException {

    constructor(msg: String, e: Throwable) : super(msg, e)

    constructor(msg: String) : super(msg)

    constructor(e: Throwable) : super(e)

}