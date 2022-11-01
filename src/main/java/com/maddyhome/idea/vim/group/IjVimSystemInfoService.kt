/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.util.SystemInfo
import com.maddyhome.idea.vim.api.SystemInfoService

class IjVimSystemInfoService : SystemInfoService {
  override val isWindows: Boolean
    get() = SystemInfo.isWindows
}
