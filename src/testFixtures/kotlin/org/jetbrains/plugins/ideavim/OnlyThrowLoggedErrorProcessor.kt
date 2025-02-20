/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim

import com.intellij.testFramework.LoggedErrorProcessor
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

/**
 * By default, LOG.error does three things in tests:
 * - rethrows the exception
 * - logs error
 * - prints to stderr
 *
 * The problem is that if we catch exception in tests, such an approach will print the exception to stderr and it will
 *   look like the exception is not processed.
 * I don't see a need for printing these caught exceptions, so we can use this processor to only rethrow them.
 */
object OnlyThrowLoggedErrorProcessor : LoggedErrorProcessor() {
  override fun processError(category: String, message: String, details: Array<out String>, t: Throwable?): Set<Action> {
    return setOf(Action.RETHROW)
  }
}

/**
 * Asserts that [T] was thrown via `LOG.error("message", e)` call where `e` has a type of [T].
 */
inline fun <reified T : Throwable> assertThrowsLogError(crossinline action: () -> Unit): T {
  val exception = assertThrows<Throwable> {
    LoggedErrorProcessor.executeWith<Throwable>(OnlyThrowLoggedErrorProcessor) {
      action()
    }
  }
  val cause = exception.cause
  if (cause !is T) fail("Expected ${T::class.java} exception in LOG.error, but got $cause")
  return cause
}
