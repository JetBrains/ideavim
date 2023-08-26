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
import com.maddyhome.idea.vim.diagnostic.vimLogger
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import java.nio.file.Paths

public object VimRcService {
  private val logger = vimLogger<VimRcService>()

  @NonNls
  public const val VIMRC_FILE_NAME: String = "ideavimrc"

  @NonNls
  private val HOME_VIMRC_PATHS = arrayOf(".$VIMRC_FILE_NAME", "_$VIMRC_FILE_NAME")

  @NonNls
  private val XDG_VIMRC_PATH = "ideavim" + File.separator + VIMRC_FILE_NAME

  @JvmStatic
  public fun findIdeaVimRc(): File? {
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

  public fun findOrCreateIdeaVimRc(): File? {
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
  public fun executeIdeaVimRc(editor: VimEditor) {
    try {
      injector.vimscriptExecutor.executingVimscript = true
      val ideaVimRc = findIdeaVimRc()
      if (ideaVimRc != null) {
        logger.info("Execute ideavimrc file: " + ideaVimRc.absolutePath)
        injector.vimscriptExecutor.executeFile(ideaVimRc, editor)
        injector.vimrcFileState.saveFileState(ideaVimRc.absolutePath)
      } else {
        logger.info("ideavimrc file isn't found")
      }
    } finally {
      injector.vimscriptExecutor.executingVimscript = false
    }
  }
}
