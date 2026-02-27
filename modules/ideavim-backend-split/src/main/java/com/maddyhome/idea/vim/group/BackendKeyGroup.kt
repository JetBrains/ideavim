/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.NativeAction
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimKeyGroupBase
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import javax.swing.KeyStroke

/**
 * No-op [VimKeyGroupBase] for the backend in split mode.
 * The backend has no editors or UI — key handling lives on the frontend.
 * Extends [VimKeyGroupBase] so that key mappings and command registration
 * still work (needed by [RegisterActions] during plugin initialization).
 */
class BackendKeyGroup : VimKeyGroupBase() {
  override val shortcutConflicts: MutableMap<KeyStroke, ShortcutOwnerInfo> = mutableMapOf()

  override fun getActions(editor: VimEditor, keyStroke: KeyStroke): List<NativeAction> = emptyList()
  override fun getKeymapConflicts(keyStroke: KeyStroke): List<NativeAction> = emptyList()
  override fun showKeyMappings(modes: Set<MappingMode>, prefix: List<KeyStroke>, editor: VimEditor): Boolean = false
  override fun updateShortcutKeysRegistration() {}
  override fun getChar(editor: VimEditor): Char? = null
}
