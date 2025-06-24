/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.Path
import java.nio.file.Path as JavaPath

val Path.javaPath: JavaPath
  get() = TODO("Not yet implemented")

val JavaPath.vimPath: Path
  get() {
    return object : Path {
      override val protocol: String
        get() = TODO("Not yet implemented")
      override val path: Array<String>
        get() = TODO("Not yet implemented")
    }
  }