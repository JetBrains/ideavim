/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.processors

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.writeText


internal class JsonFileWriter(
  @PublishedApi internal val environment: SymbolProcessorEnvironment,
) {

  @OptIn(ExperimentalSerializationApi::class)
  @PublishedApi
  internal val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
  }

  inline fun <reified T> write(fileName: String, data: T) {
    val generatedDirPath = Path(environment.options["generated_directory"]!!)
    Files.createDirectories(generatedDirPath)

    val filePath = generatedDirPath.resolve(fileName)
    val fileContent = json.encodeToString(data)
    filePath.writeText(fileContent)
  }
}
