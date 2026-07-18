package com.hect0x7.proxy.core.internal

import io.netty.util.concurrent.Future
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal suspend fun Future<*>.awaitCompletion() {
  suspendCoroutine { continuation ->
    addListener { completed ->
      if (completed.isSuccess) continuation.resume(Unit)
      else continuation.resumeWithException(completed.cause())
    }
  }
}
