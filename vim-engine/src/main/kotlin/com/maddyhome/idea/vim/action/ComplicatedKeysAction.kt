/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action

import javax.swing.KeyStroke

public interface ComplicatedKeysAction {
  public val keyStrokesSet: Set<List<KeyStroke>>
}
