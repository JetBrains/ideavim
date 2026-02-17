/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.architecture

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readLines
import kotlin.test.fail

/**
 * Architectural tests that enforce coding conventions across the vim-engine module.
 *
 * Use [java.nio.file.Path] with kotlin.io.path extensions instead of [java.io.File].
 */
class ForbiddenApiTest {

  companion object {
    private val SOURCE_ROOT: Path = Path.of("src/main")

    private val ALLOWED_FILES: Set<String> = setOf()
  }

  @Test
  fun `java_io_File must not be used in vim-engine sources`() {
    val violations = Files.walk(SOURCE_ROOT).use { paths ->
      paths
        .filter { it.extension == "kt" || it.extension == "java" }
        .filter { it.toString() !in ALLOWED_FILES }
        .toList()
        .flatMap { file ->
          file.readLines().mapIndexedNotNull { index, line ->
            if (line.contains("java.io.File")) {
              "${file}:${index + 1}: $line"
            } else {
              null
            }
          }
        }
    }

    if (violations.isNotEmpty()) {
      fail(
        "java.io.File is forbidden in vim-engine. Use java.nio.file.Path with kotlin.io.path extensions instead.\n" +
          "Violations:\n" + violations.joinToString("\n")
      )
    }
  }
}
