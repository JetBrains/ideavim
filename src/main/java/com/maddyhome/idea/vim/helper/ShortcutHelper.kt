/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.actionSystem.ShortcutSet
import com.maddyhome.idea.vim.key.RequiredShortcut

object ShortcutHelper {
  @JvmStatic
  fun toShortcutSet(requiredShortcuts: Collection<RequiredShortcut>): ShortcutSet {
    val shortcuts = mutableListOf<Shortcut>()
    for (key in requiredShortcuts) {
      shortcuts.add(KeyboardShortcut(key.keyStroke, null))
    }
    return CustomShortcutSet(*shortcuts.toTypedArray())
  }
}
