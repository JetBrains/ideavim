/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("CommandStateHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inVisualMode

internal val Mode.hasVisualSelection
  get() = when (this) {
    is Mode.VISUAL, is Mode.SELECT -> true
    else -> false
  }

val Mode.inNormalMode: Boolean
  get() = this is Mode.NORMAL

@get:JvmName("inInsertMode")
val Editor.inInsertMode: Boolean
  get() = this.vim.mode == Mode.INSERT || this.vim.mode == Mode.REPLACE

@get:JvmName("inVisualMode")
val Editor.inVisualMode: Boolean
  get() = this.vim.inVisualMode

@get:JvmName("inExMode")
internal val Editor.inExMode
  get() = this.vim.mode is Mode.CMD_LINE
