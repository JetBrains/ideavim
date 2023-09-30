/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action

import org.jetbrains.annotations.ApiStatus
import javax.swing.KeyStroke

@Deprecated("Vim's key notation should be enough for all of the keys")
@ApiStatus.ScheduledForRemoval(inVersion = "2.7.0")
public interface ComplicatedKeysAction {
  public val keyStrokesSet: Set<List<KeyStroke>>
}
