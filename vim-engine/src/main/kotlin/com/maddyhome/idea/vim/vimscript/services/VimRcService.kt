/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.vimscript.model.commands.IdeaPlug
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import java.nio.file.Paths

object VimRcService {
  private val logger = vimLogger<VimRcService>()

  @NonNls
  const val VIMRC_FILE_NAME: String = "ideavimrc"

  @NonNls
  private val HOME_VIMRC_PATHS = arrayOf(".$VIMRC_FILE_NAME", "_$VIMRC_FILE_NAME")

  @NonNls
  private val XDG_VIMRC_PATH = "ideavim" + File.separator + VIMRC_FILE_NAME

  @JvmStatic
  fun findIdeaVimRc(): File? {
    // Check whether file exists in home dir
    val homeDirName = System.getProperty("user.home")
    if (homeDirName != null) {
      for (fileName in HOME_VIMRC_PATHS) {
        val file = File(homeDirName, fileName)
        if (file.exists()) {
          logger.debug { "Found ideavimrc file: $file" }
          return file
        }
      }
    }
    else {
      logger.info("User's home directory is not defined. Cannot locate ~/.ideavimrc or ~/_ideavimrc file.")
    }

    // Check in XDG config directory
    val xdgConfigHomeProperty = System.getenv("XDG_CONFIG_HOME")
    val xdgConfig = if (xdgConfigHomeProperty == null || xdgConfigHomeProperty == "") {
      logger.debug("XDG_CONFIG_HOME is not defined. Trying to locate ~/.config/ideavim/ideavimrc file.")
      if (homeDirName != null) Paths.get(homeDirName, ".config", XDG_VIMRC_PATH).toFile() else null
    } else {
      logger.debug { "XDG_CONFIG_HOME set to '$xdgConfigHomeProperty'. Trying to locate \$XDG_CONFIG_HOME/ideavim/ideavimrc file" }
      val configHome = if (xdgConfigHomeProperty.startsWith("~/") || xdgConfigHomeProperty.startsWith("~\\")) {
        val expandedConfigHome = homeDirName + xdgConfigHomeProperty.substring(1)
        logger.debug { "Expanded \$XDG_CONFIG_HOME to '$expandedConfigHome'" }
        expandedConfigHome
      }
      else {
        xdgConfigHomeProperty
      }
      File(configHome, XDG_VIMRC_PATH)
    }
    return xdgConfig?.let {
      if (it.exists()) {
        logger.debug { "Found ideavimrc file: $it" }
        it
      } else null
    }
  }

  private fun getNewIdeaVimRcTemplate(vimrc: String) = """
    |" .ideavimrc is a configuration file for IdeaVim plugin. It uses
    |"   the same commands as the original .vimrc configuration.
    |" You can find a list of commands here: https://jb.gg/h38q75
    |" Find more examples here: https://jb.gg/share-ideavimrc

    |$vimrc
    |"" -- Suggested options --
    |" Show a few lines of context around the cursor. Note that this makes the
    |" text scroll if you mouse-click near the start or end of the window.
    |set scrolloff=5

    |" Do incremental searching.
    |set incsearch

    |" Don't use Ex mode, use Q for formatting.
    |map Q gq

    |" --- Enable IdeaVim plugins https://jb.gg/ideavim-plugins

    |" Highlight copied text
    |Plug 'machakann/vim-highlightedyank'
    |" Commentary plugin
    |Plug 'tpope/vim-commentary'


    |"" -- Map IDE actions to IdeaVim -- https://jb.gg/abva4t
    |"" Map \r to the Reformat Code action
    |"map \r <Action>(ReformatCode)

    |"" Map <leader>d to start debug
    |"map <leader>d <Action>(Debug)

    |"" Map \b to toggle the breakpoint on the current line
    |"map \b <Action>(ToggleLineBreakpoint)

  """.trimMargin()

  fun findOrCreateIdeaVimRc(): File? {
    val found = findIdeaVimRc()
    if (found != null) return found

    val homeDirName = System.getProperty("user.home")
    val vimrc = sourceVimrc(homeDirName)
    if (homeDirName != null) {
      for (fileName in HOME_VIMRC_PATHS) {
        try {
          val file = File(homeDirName, fileName)
          file.createNewFile()
          file.writeText(getNewIdeaVimRcTemplate(vimrc))
          injector.vimrcFileState.filePath = file.absolutePath
          return file
        } catch (ignored: IOException) {
          // Try to create one of two files
        }
      }
    }
    return null
  }

  private fun sourceVimrc(homeDirName: String): String {
    if (File(homeDirName, ".vimrc").exists()) {
      return """
        |" Source your .vimrc
        ||source ~/.vimrc
        |
      """.trimMargin()
    }
    if (File(homeDirName, "_vimrc").exists()) {
      return """
        |" Source your _vimrc
        ||source ~/_vimrc
        |
      """.trimMargin()
    }
    return ""
  }

  @JvmStatic
  fun executeIdeaVimRc(editor: VimEditor) {
    val ideaVimRc = findIdeaVimRc()
    if (ideaVimRc != null) {
      logger.info("Execute ideavimrc file: " + ideaVimRc.absolutePath)

      // clear all previously enabled extensions
      IdeaPlug.Companion.EnabledExtensions.clearExtensions()

      injector.vimscriptExecutor.executeFile(ideaVimRc, editor, fileIsIdeaVimRcConfig = true)
    } else {
      logger.info("ideavimrc file isn't found")
    }
  }

  fun isIdeaVimRcFile(file: File): Boolean {
    val ideaVimRc = findIdeaVimRc() ?: return false
    return ideaVimRc == file
  }
}
