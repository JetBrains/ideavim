/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.models.Path
import java.nio.file.Paths
import java.nio.file.Path as JavaPath

private const val PATH_DELIMITER = "/"
private const val PROTOCOL_DELIMITER = "://"

val Path.javaPath: JavaPath
  get() {
    val uri = "$protocol$PROTOCOL_DELIMITER${getFilePath()}"
    return Paths.get(uri)
  }

val JavaPath.vimPath: Path?
  get() {
    val pathComponents: List<String> = iterator().asSequence().map { it.toString() }.toList()
    // Protocol is the first component:
    // e.g., temp:///src/aaa.txt => protocol = "temp:", path = ("src", "aaa.txt")
    val protocolRegex = Regex("^([a-zA-Z]+):")
    val protocolString = pathComponents.firstOrNull() ?: return null
    val protocol = protocolRegex.find(protocolString)?.groupValues[1] ?: return null
    // drop protocol from list
    val components = pathComponents.drop(1)

    return object : Path {
      override val protocol: String = protocol
      override val path: Array<String> = components.toTypedArray()
    }
  }


/**
 * Function that will create [Path] from protocol and file path passed as function parameters.
 */
internal fun Path.Companion.createApiPath(protocol: String, filePath: String): Path {
  val pathComponents: Array<String> = filePath
    .split(PATH_DELIMITER)
    .toTypedArray()

  return object : Path {
    override val protocol: String = protocol
    override val path: Array<String> = pathComponents
  }
}

/**
 * Function that returns a string that represents filePath without a protocol.
 */
internal fun Path.getFilePath(): String {
  return path.joinToString(PATH_DELIMITER)
}