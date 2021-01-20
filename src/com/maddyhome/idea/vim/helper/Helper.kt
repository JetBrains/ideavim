/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.common.TextRange
import java.util.*

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
  val shouldBeFixed: Boolean = true
)

fun <T : Comparable<T>> sort(a: T, b: T) = if (a > b) b to a else a to b

inline fun <reified T : Enum<T>> noneOfEnum(): EnumSet<T> = EnumSet.noneOf(T::class.java)
inline fun <reified T : Enum<T>> enumSetOf(vararg value: T): EnumSet<T> = when (value.size) {
  0 -> noneOfEnum()
  1 -> EnumSet.of(value[0])
  else -> EnumSet.of(value[0], *value.slice(1..value.lastIndex).toTypedArray())
}

inline fun Editor.vimForEachCaret(action: (caret: Caret) -> Unit) {
  if (this.inBlockSubMode) {
    action(this.caretModel.primaryCaret)
  } else {
    this.caretModel.allCarets.forEach(action)
  }
}

fun Editor.getTopLevelEditor() = if (this is EditorWindow) this.delegate else this

@Suppress("IncorrectParentDisposable")
fun Editor.isTemplateActive(): Boolean {
  val project = this.project ?: return false
  if (Disposer.isDisposed(project)) return false
  return TemplateManager.getInstance(project).getActiveTemplate(this) != null
}


/**
 * This annotations marks if annotated function required read or write lock
 */
@Target
annotation class RWLockLabel {
  /**
   * [Readonly] annotation means that annotated function should be called from read action
   * This annotation is only a marker and doesn't enable r/w lock automatically
   */
  @Target(AnnotationTarget.FUNCTION)
  annotation class Readonly

  /**
   * [Writable] annotation means that annotated function should be called from write action
   * This annotation is only a marker and doesn't enable r/w lock automatically
   */
  @Target(AnnotationTarget.FUNCTION)
  annotation class Writable

  /**
   * [SelfSynchronized] annotation means that annotated function handles read/write lock by itself
   * This annotation is only a marker and doesn't enable r/w lock automatically
   */
  @Target(AnnotationTarget.FUNCTION)
  annotation class SelfSynchronized

  /**
   * [NoLockRequired] annotation means that annotated function doesn't require any lock
   * This annotation is only a marker and doesn't enable r/w lock automatically
   */
  @Target(AnnotationTarget.FUNCTION)
  annotation class NoLockRequired
}

val TextRange.endOffsetInclusive
  get() = if (this.endOffset > 0 && this.endOffset > this.startOffset) this.endOffset - 1 else this.endOffset
