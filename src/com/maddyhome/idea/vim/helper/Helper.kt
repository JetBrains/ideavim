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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import java.util.*

/**
 * This annotation is created for test functions (methods).
 * It means that original vim behaviour has small differences from behaviour of IdeaVim.
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
 * After creating some functionality you can understand that IdeaVim has a bit different behaviour, but you
 *   cannot fix it right now because of any reasons (bugs in IDE,
 *   the impossibility of this functionality in IDEA (*[shouldBeFixed] == false*), leak of time for fixing).
 *   In that case, you should NOT remove the corresponding test or leave it without any marks that this test
 *   not fully convenient with vim, but leave the test with IdeaVim's behaviour and put this annotation
 *   with description of how original vim works.
 *
 * Note that using this annotation should be avoided as much as possible and behaviour of IdeaVim should be as close
 *   to vim as possible.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class VimBehaviourDiffers(
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

