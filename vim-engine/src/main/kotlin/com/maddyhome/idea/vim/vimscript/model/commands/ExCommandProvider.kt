/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import org.yaml.snakeyaml.Yaml
import java.io.InputStream

public interface ExCommandProvider {
  public val exCommandsFileName: String

  public fun getCommands(): Map<String, LazyExCommandInstance> {
    val yaml = Yaml()
    val classLoader = this.javaClass.classLoader
    val commandToClass: Map<String, String> = yaml.load(getFile())
    return commandToClass.entries.associate { it.key to LazyExCommandInstance(it.value, classLoader) }
  }

  private fun getFile(): InputStream {
    return object {}.javaClass.classLoader.getResourceAsStream(exCommandsFileName)
      ?: throw RuntimeException("Failed to fetch ex-commands for ${javaClass.name}")
  }
}