/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.util.SystemInfo
import com.maddyhome.idea.vim.api.SystemInfoService

internal class IjVimSystemInfoService : SystemInfoService {
  override val isWindows: Boolean
    get() = SystemInfo.isWindows
  override val isXWindow: Boolean
    get() = SystemInfo.isXWindow
}
