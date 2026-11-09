/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.change

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.ApiStatus

/**
 * Runs [block] with caret movement not being a separate undo step.
 *
 * By default, when the caret moved after a change, e.g. back by one on leaving Insert mode, the platform spends the
 * first undo only on restoring the caret, so `u` seems to do nothing (VIM-4370). The platform has no API for this
 * other than the registry key.
 */
@ApiStatus.Internal
inline fun <T> withTransparentCaretMovement(block: () -> T): T {
  val registryValue = Registry.get("ide.undo.transparent.caret.movement")
  val oldValue = registryValue.asBoolean()
  registryValue.setValue(true)
  try {
    return block()
  } finally {
    registryValue.setValue(oldValue)
  }
}
