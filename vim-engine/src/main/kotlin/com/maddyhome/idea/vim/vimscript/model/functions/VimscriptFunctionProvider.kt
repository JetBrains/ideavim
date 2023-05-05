/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions

import org.yaml.snakeyaml.Yaml
import java.io.InputStream

public interface VimscriptFunctionProvider {
  public val functionListFileName: String

  public fun getFunctions(): Collection<LazyVimscriptFunction> {
    val yaml = Yaml()
    val classLoader = this.javaClass.classLoader
    val functionDict: Map<String, String> = yaml.load(getFile())
    return functionDict.map { LazyVimscriptFunction(it.key, it.value, classLoader) }
  }

  private fun getFile(): InputStream {
    return object {}.javaClass.classLoader.getResourceAsStream(functionListFileName)
      ?: throw RuntimeException("Failed to fetch functions for ${javaClass.name}")
  }
}