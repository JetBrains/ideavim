/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

interface VimRegisterGroup {

  var lastRegisterChar: Char
  val currentRegister: Char

  val isRecording: Boolean
  val recordRegister: Char?

  /**
   * When we access last register, it can be e.g. " because of two reasons:
   * 1. Because the default register value was used
   * 2. If the " was specified explicitly
   * We have this boolean flag to differ this to cases
   */
  val isRegisterSpecifiedExplicitly: Boolean
  val defaultRegister: Char

  fun getLastRegister(editor: VimEditor, context: ExecutionContext): Register?
  fun isValid(reg: Char): Boolean
  fun selectRegister(reg: Char): Boolean
  fun resetRegister()
  fun resetRegisters()
  fun recordKeyStroke(key: KeyStroke)
  fun isRegisterWritable(): Boolean
  fun isRegisterWritable(reg: Char): Boolean

  /** Store text into the last register. */
  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#storeText(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, com.maddyhome.idea.vim.api.ImmutableVimCaret, com.maddyhome.idea.vim.common.TextRange, com.maddyhome.idea.vim.state.mode.SelectionType, boolean)")
  fun storeText(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    range: TextRange,
    type: SelectionType,
    isDelete: Boolean,
  ): Boolean
  fun storeText(
    editor: VimEditor,
    context: ExecutionContext,
    caret: ImmutableVimCaret,
    range: TextRange,
    type: SelectionType,
    isDelete: Boolean,
  ): Boolean

  /**
   * Stores text to any writable register (used for the let command)
   */
  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#storeText(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char, java.lang.String)")
  fun storeText(register: Char, text: String): Boolean
  fun storeText(editor: VimEditor, context: ExecutionContext, register: Char, text: String): Boolean

  /**
   * Stores text to any writable register (used for multicaret tests)
   */
  @TestOnly
  // todo better tests
  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#storeText(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char, java.lang.String, com.maddyhome.idea.vim.state.mode.SelectionType)")
  fun storeText(register: Char, text: String, selectionType: SelectionType): Boolean
  @TestOnly
  fun storeText(editor: VimEditor, context: ExecutionContext, register: Char, text: String, selectionType: SelectionType): Boolean

  /**
   * Stores text, character wise, in the given special register
   *
   *
   * This method is intended to support writing to registers when the text cannot be yanked from an editor. This is
   * expected to only be used to update the search and command registers. It will not update named registers.
   *
   *
   * While this method allows setting the unnamed register, this should only be done from tests, and only when it's
   * not possible to yank or cut from the fixture editor. This method will skip additional text processing, and won't
   * update other registers such as the small delete register or reorder the numbered registers. It is much more
   * preferable to yank from the fixture editor.
   */
  fun storeTextSpecial(register: Char, text: String): Boolean
  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#getRegister(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char)")
  fun getRegister(r: Char): Register?
  fun getRegister(editor: VimEditor, context: ExecutionContext, r: Char): Register?
  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#getRegisters(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext)")
  fun getRegisters(): List<Register>
  fun getRegisters(editor: VimEditor, context: ExecutionContext): List<Register>
  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#saveRegister(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char, com.maddyhome.idea.vim.register.Register)")
  fun saveRegister(r: Char, register: Register)
  fun saveRegister(editor: VimEditor, context: ExecutionContext, r: Char, register: Register)
  fun startRecording(register: Char): Boolean

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#getPlaybackRegister(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char)")
  fun getPlaybackRegister(r: Char): Register?
  fun getPlaybackRegister(editor: VimEditor, context: ExecutionContext, r: Char): Register?
  fun recordText(text: String)
  fun setKeys(register: Char, keys: List<KeyStroke>)
  fun setKeys(register: Char, keys: List<KeyStroke>, type: SelectionType)
  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#finishRecording(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext)")
  fun finishRecording()
  fun finishRecording(editor: VimEditor, context: ExecutionContext)
  fun getCurrentRegisterForMulticaret(): Char // `set clipbaard+=unnamedplus` should not make system register the default one when working with multiple carets VIM-2804
  fun isSystemClipboard(register: Char): Boolean
  fun isPrimaryRegisterSupported(): Boolean
}
