/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.diagnostic.logger
import com.maddyhome.idea.vim.ui.VimRcFileState
import com.maddyhome.idea.vim.vimscript.Executor
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import java.nio.file.Paths

object VimRcService {
  private val logger = logger<VimRcService>()

  @NonNls
  private const val VIMRC_FILE_NAME = "ideavimrc"

  @NonNls
  private val HOME_VIMRC_PATHS = arrayOf(".$VIMRC_FILE_NAME", "_$VIMRC_FILE_NAME")

  @NonNls
  private val XDG_VIMRC_PATH = "ideavim" + File.separator + VIMRC_FILE_NAME

  @JvmStatic
  fun findIdeaVimRc(): File? {
    val homeDirName = System.getProperty("user.home")
    // Check whether file exists in home dir
    if (homeDirName != null) {
      for (fileName in HOME_VIMRC_PATHS) {
        val file = File(homeDirName, fileName)
        if (file.exists()) {
          return file
        }
      }
    }

    // Check in XDG config directory
    val xdgConfigHomeProperty = System.getenv("XDG_CONFIG_HOME")
    val xdgConfig = if (xdgConfigHomeProperty == null || xdgConfigHomeProperty == "") {
      if (homeDirName != null) Paths.get(homeDirName, ".config", XDG_VIMRC_PATH).toFile() else null
    } else {
      File(xdgConfigHomeProperty, XDG_VIMRC_PATH)
    }
    return if (xdgConfig != null && xdgConfig.exists()) xdgConfig else null
  }

  private val newIdeaVimRcTemplate = """
    "" Source your .vimrc
    "source ~/.vimrc
    
    "" -- Suggested options --
    " Show a few lines of context around the cursor. Note that this makes the
    " text scroll if you mouse-click near the start or end of the window.
    set scrolloff=5

    " Do incremental searching.
    set incsearch

    " Don't use Ex mode, use Q for formatting.
    map Q gq
    
    
    "" -- Map IDE actions to IdeaVim -- https://jb.gg/abva4t
    "" Map \r to the Reformat Code action
    "map \r <Action>(ReformatCode)

    "" Map <leader>d to start debug
    "map <leader>d <Action>(Debug)

    "" Map \b to toggle the breakpoint on the current line
    "map \b <Action>(ToggleLineBreakpoint)
    
    
    " Find more examples here: https://jb.gg/share-ideavimrc
    
  """.trimIndent()

  fun findOrCreateIdeaVimRc(): File? {
    val found = findIdeaVimRc()
    if (found != null) return found

    val homeDirName = System.getProperty("user.home")
    if (homeDirName != null) {
      for (fileName in HOME_VIMRC_PATHS) {
        try {
          val file = File(homeDirName, fileName)
          file.createNewFile()
          file.writeText(newIdeaVimRcTemplate)
          VimRcFileState.filePath = file.absolutePath
          return file
        } catch (ignored: IOException) {
          // Try to create one of two files
        }
      }
    }
    return null
  }

  @JvmStatic
  fun executeIdeaVimRc() {
    try {
      Executor.executingVimScript = true
      val ideaVimRc = findIdeaVimRc()
      if (ideaVimRc != null) {
        logger.info("Execute ideavimrc file: " + ideaVimRc.absolutePath)
        Executor.executeFile(ideaVimRc)
        VimRcFileState.saveFileState(ideaVimRc.absolutePath)
      } else {
        logger.info("ideavimrc file isn't found")
      }
    } finally {
      Executor.executingVimScript = false
    }
  }
}
