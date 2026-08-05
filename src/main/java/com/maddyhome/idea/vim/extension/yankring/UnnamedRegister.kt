/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.models.TextType

internal data class RegisterContents(val text: String, val type: TextType)

/**
 * Runs [block] with the unnamed register holding [contents], then puts back whatever it held
 * before.
 *
 * A paste reads the unnamed register, so re-pasting a ring entry means loading it in there first -
 * and the register the user was working with has to survive that untouched.
 */
internal suspend fun VimApi.withUnnamedRegister(contents: RegisterContents, block: suspend () -> Unit) {
  val saved = readUnnamedRegister()
  writeUnnamedRegister(contents)

  block()

  saved?.let { writeUnnamedRegister(it) }
}

private suspend fun VimApi.readUnnamedRegister(): RegisterContents? =
  editor {
    read {
      withPrimaryCaret {
        val text = getReg(UNNAMED_REGISTER) ?: return@withPrimaryCaret null
        RegisterContents(text, getRegType(UNNAMED_REGISTER) ?: TextType.CHARACTER_WISE)
      }
    }
  }

private suspend fun VimApi.writeUnnamedRegister(contents: RegisterContents) {
  editor { read { withPrimaryCaret { setReg(UNNAMED_REGISTER, contents.text, contents.type) } } }
}

internal const val UNNAMED_REGISTER = '"'
