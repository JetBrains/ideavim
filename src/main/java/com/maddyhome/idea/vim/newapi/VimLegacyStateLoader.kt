/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import org.jdom.Element

/**
 * Bridge interface for legacy XML state loading.
 * Implemented by service classes in the frontend module (RegisterGroup, HistoryGroup),
 * used by VimPlugin in common for backwards-compatible state migration.
 */
interface VimLegacyStateLoader {
  fun readData(element: Element)
}
