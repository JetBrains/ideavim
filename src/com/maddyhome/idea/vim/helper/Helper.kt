/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
import com.maddyhome.idea.vim.common.TextRange
import java.util.*

/**
 * This annotation is created for test functions (methods).
 * It means that original vim behavior has small differences from behavior of IdeaVim.
 * [shouldBeFixed] flag indicates whether the given functionality should be fixed
 *   or the given behavior is normal for IdeaVim and should be leaved as is.
 *
 * E.g. after execution some commands original vim has next text:
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
 * Why this annotation exists?
 * After creating some functionality you can understand that IdeaVim has a bit different behavior, but you
 *   cannot fix it right now because of any reasons (bugs in IDE,
 *   the impossibility of this functionality in IDEA (*[shouldBeFixed] == false*), leak of time for fixing).
 *   In that case, you should NOT remove the corresponding test or leave it without any marks that this test
 *   not fully convenient with vim, but leave the test with IdeaVim's behavior and put this annotation
 *   with description of how original vim works.
 *
 * Note that using this annotation should be avoided as much as possible and behavior of IdeaVim should be as close
 *   to vim as possible.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class VimBehaviorDiffers(
  val originalVimAfter: String = "",
  val description: String = "",
  val shouldBeFixed: Boolean = true
)

/**
 * [VimFunctionMark] and [VimTestFunction] are the simple annotations that simplify to bind test
 *   and functions that are used in that test, but aren't targets of this test
 *
 *   E.g. if you test `n` command and you want to use next command sequence `*n` you can put this test in
 *     SearchAgainNextActionTest test class (because main test target is `n` command) and annotate this function
 *     with @VimTestFunction("com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction") to mark that
 *     this test also uses `*` command.
 *
 * [VimFunctionMark] should annotate some method or class and provide and unique label for it
 * [VimTestFunction] provides marks that point to commands that are tested with this function. Full class name or values
 *   of [VimFunctionMark] can be used as marks.
 *
 * These annotations doesn't affect code behavior, but created only for development purposes
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class VimFunctionMark(val value: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class VimTestFunction(vararg val value: String)

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

fun Editor.isTemplateActive(): Boolean {
  val project = this.project ?: return false
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
