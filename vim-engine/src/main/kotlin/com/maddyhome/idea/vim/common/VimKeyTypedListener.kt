/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.VimEditor
import javax.swing.KeyStroke

/**
 * Notified about every key the user types, before any mapping is applied
 *
 * Keys that IdeaVim feeds to itself (mapping right-hand sides, macro playback, `.` repeat, replayed mapping prefixes)
 * are not reported. This is the stream Vim writes with `-w {scriptout}`.
 */
interface VimKeyTypedListener : Listener {
  fun keyTyped(editor: VimEditor, key: KeyStroke)
}
