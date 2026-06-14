/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key.consumers

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.KeySource
import com.maddyhome.idea.vim.options.helpers.LangMapOptionHelper
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class LangMapConsumer : KeyConsumer {
  override fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    keySource: KeySource,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder
  ): Boolean {
    val commandBuilder = keyProcessResultBuilder.state.commandBuilder
    return key.keyChar != KeyEvent.CHAR_UNDEFINED
      && keySource.allowsKeyMapping
      && allowLangMap(keySource, editor, commandBuilder)
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    keySource: KeySource,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    val char = key.keyChar  // This will always be a typed char, uppercase or lowercase
    val newChar = LangMapOptionHelper.mapChar(char)
    if (newChar != char) {
      keyProcessResultBuilder.addExecutionStep { s, e, c ->
        val newKeyStroke = KeyStroke.getKeyStroke(newChar)
        KeyHandler.getInstance().handleKey(e, newKeyStroke, KeySource.LANG_MAP, c, s)
      }
      return true
    }

    return false
  }

  private fun allowLangMap(keySource: KeySource, editor: VimEditor, commandBuilder: CommandBuilder): Boolean {
    // Remember, 'langmap' converts from a user's own language to Vim's expected English/ASCII characters. So we want to
    // translate incoming keys when Vim is expecting Vim English - Normal/Visual commands, register names after `<C-R>`,
    // marks after `m` and `'`, etc.

    // We never map in Command-line mode, even when accepting a register with `<C-R>`.
    if (editor.mode is Mode.CMD_LINE) {
      return false
    }

    val argumentType = commandBuilder.expectedArgumentType
    if (keySourceAllowsLangMap(keySource)) {
      if (argumentType == Argument.Type.REGISTER || argumentType == Argument.Type.MARK || commandBuilder.isRegisterPending) {
        return true
      }

      if (editor.mode is Mode.OP_PENDING) {
        return true
      }

      if (editor.mode is Mode.NORMAL || editor.mode is Mode.VISUAL) {
        return argumentType == null
      }
    }
    return false
  }

  private fun keySourceAllowsLangMap(keySource: KeySource) = when (keySource) {
    KeySource.TYPED -> true
    KeySource.MACRO_PLAYBACK -> true

    KeySource.LANG_MAP -> false
    KeySource.DIGRAPH -> false
    KeySource.LITERAL -> false
    KeySource.NORMAL_COMMAND -> false
    KeySource.NORMAL_COMMAND_NOT_MAPPED -> false
    KeySource.MAPPING_PREFIX_REPLAY -> false
    KeySource.SYNTHETIC_TYPED_NOT_MAPPED -> false

    KeySource.MAPPED -> injector.globalOptions().langremap
    KeySource.MAPPED_NON_RECURSIVE -> injector.globalOptions().langremap
  }
}
