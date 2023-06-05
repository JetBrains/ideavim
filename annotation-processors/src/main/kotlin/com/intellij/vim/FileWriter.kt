/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim

import java.io.File
import java.nio.file.Path

class FileWriter {
  fun writeFile(filePath: Path, content: String) {
    val file = File(filePath.toUri())
    file.writeText(content)
  }
}