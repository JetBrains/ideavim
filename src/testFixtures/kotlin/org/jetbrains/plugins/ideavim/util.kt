/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.util.containers.toArray
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.params.provider.Arguments
import kotlin.test.fail

/**
 * This annotation is created for test functions (methods).
 * It means that the original vim behavior has small differences from behavior of IdeaVim.
 * [shouldBeFixed] flag indicates whether the given functionality should be fixed
 *   or the given behavior is normal for IdeaVim and should be leaved as is.
 *
 * E.g. after execution of some commands original vim has the following text:
 *    Hello1
 *    Hello2
 *    Hello3
 *
 * But IdeaVim gives you:
 *    Hello1
 *
 *    Hello2
 *    Hello3
 *
 * In this case you should still create the test function and mark this function with [VimBehaviorDiffers] annotation.
 *
 * Why does this annotation exist?
 * After creating some functionality you can understand that IdeaVim has a bit different behavior, but you
 *   cannot fix it right now because of any reason (bugs in IDE,
 *   the impossibility of this functionality in IDEA (*[shouldBeFixed] == false*), leak of time for fixing).
 *   In that case, you should NOT remove the corresponding test or leave it without any marks that this test
 *   not fully convenient with vim, but leave the test with IdeaVim's behavior and put this annotation
 *   with description of how original vim works.
 *
 * Note that using this annotation should be avoided as much as possible and behavior of IdeaVim should be as close
 *   to vim as possible.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class VimBehaviorDiffers(
  val originalVimAfter: String = "",
  val description: String = "",
  val shouldBeFixed: Boolean = true,
)

inline fun waitAndAssert(timeInMillis: Int = 1000, crossinline condition: () -> Boolean) {
  ApplicationManager.getApplication().invokeAndWait {
    val end = System.currentTimeMillis() + timeInMillis
    while (end > System.currentTimeMillis()) {
      Thread.sleep(10)
      IdeEventQueue.getInstance().flushQueue()
      if (condition()) return@invokeAndWait
    }
    fail()
  }
}

fun assertHappened(timeInMillis: Int = 1000, precision: Int, condition: () -> Boolean) {
  assertDoesntChange(timeInMillis - precision) { !condition() }

  waitAndAssert(precision * 2) { condition() }
}

fun assertDoesntChange(timeInMillis: Int = 1000, condition: () -> Boolean) {
  ApplicationManager.getApplication().invokeAndWait {
    val end = System.currentTimeMillis() + timeInMillis
    while (end > System.currentTimeMillis()) {
      if (!condition()) {
        fail()
      }

      Thread.sleep(10)
      IdeEventQueue.getInstance().flushQueue()
    }
  }
}

fun <T, S, V> Collection<T>.cartesianProduct(
  other: Iterable<S>,
  transformer: (first: T, second: S) -> V,
): List<V> {
  return this.flatMap { first -> other.map { second -> transformer.invoke(first, second) } }
}

// Cartesian product of multiple lists. Useful for making parameterized tests with all available combinations.
// Can be used instead of @Theory from JUnit 4
fun productForArguments(vararg elements: List<String>): List<Arguments> {
  val res = product(*elements)
  return res.map { Arguments.of(*it.toArray(emptyArray())) }
}

fun <T> product(vararg elements: List<T>): List<List<T>> {
  val res = elements.fold(listOf<List<T>>(emptyList())) { acc, items ->
    acc.cartesianProduct(items) { accItems, item ->
      accItems + item
    }
  }
  return res
}

fun waitAndAssertMode(
  fixture: CodeInsightTestFixture,
  mode: Mode,
  timeInMillis: Int? = null,
) {
  val timeout = timeInMillis ?: (injector.globalIjOptions().visualdelay + 1000)
  waitAndAssert(timeout) { fixture.editor.vim.mode == mode }
}

fun waitUntil(timeout: Int = 10_000, condition: () -> Boolean): Boolean {
  val timeEnd = System.currentTimeMillis() + timeout
  while (System.currentTimeMillis() < timeEnd) {
    if (condition()) {
      return true // Condition met within the time limit
    }
    Thread.sleep(100) // Pause briefly to prevent tight loop
  }
  return false // Timed out
}
