/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.Path
import java.nio.file.Paths
import java.nio.file.Path as JavaPath

val Path.javaPath: JavaPath
  get() = Paths.get(protocol, *path)

val JavaPath.vimPath: Path?
  get() {
    val pathComponents: List<String> = iterator().asSequence().map { it.toString() }.toList()
    val protocolRegex = Regex("^[a-zA-Z]:|^/")

    val protocol: String = root?.toString()?.removeSuffix(":")
      ?: pathComponents.firstOrNull()?.takeIf { protocolRegex.matches(it) }?.removeSuffix(":")
      ?: return null

    return object : Path {
      override val protocol: String
        get() = protocol

      override val path: Array<String>
        get() = pathComponents.toTypedArray()
    }
  }