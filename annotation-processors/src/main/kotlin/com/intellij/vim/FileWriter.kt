/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import java.io.File

class FileWriter {
  fun generateResourceFile(fileName: String, content: String, environment: SymbolProcessorEnvironment) {
    val resourcesDir = environment.options["generated_directory"]
    val file = File("$resourcesDir/$fileName")
    file.writeText(content)
  }
}