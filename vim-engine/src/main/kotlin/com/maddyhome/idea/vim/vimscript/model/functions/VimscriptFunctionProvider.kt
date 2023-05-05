/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.io.InputStream
import javax.swing.InputMap

public interface VimscriptFunctionProvider {
  public val functionListFile: InputStream

  public fun getFunctions(): Collection<LazyVimscriptFunction> {
    val mapper = YAMLMapper()
    val classLoader = this.javaClass.classLoader
    val typeReference = object : TypeReference<HashMap<String, String>>() {}
    val functionDict = mapper.readValue(functionListFile, typeReference)
    return functionDict.map { LazyVimscriptFunction(it.key, it.value, classLoader) }
  }
}