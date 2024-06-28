/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

interface VimscriptFunctionProvider {
  val functionListFileName: String

  @OptIn(ExperimentalSerializationApi::class)
  fun getFunctions(): Collection<LazyVimscriptFunction> {
    val classLoader = this.javaClass.classLoader
    val functionDict: Map<String, String> = Json.decodeFromStream(getFile())
    return functionDict.map { LazyVimscriptFunction(it.key, it.value, classLoader) }
  }

  private fun getFile(): InputStream {
    return object {}.javaClass.classLoader.getResourceAsStream("ksp-generated/$functionListFileName")
      ?: throw RuntimeException("Failed to fetch functions for ${javaClass.name}")
  }
}
