/*
 * Copyright 2003-2026 The IdeaVim authors
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
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText

object VimRcService {
  private val logger = vimLogger<VimRcService>()

  @NonNls
  const val VIMRC_FILE_NAME: String = "ideavimrc"

  @NonNls
  private val HOME_VIMRC_PATHS = arrayOf(".$VIMRC_FILE_NAME", "_$VIMRC_FILE_NAME")

  @NonNls
  private val XDG_VIMRC_PATH = "ideavim/$VIMRC_FILE_NAME"

  @NonNls
  private const val DEBUG_VIMRC_FILE_NAME: String = "debug.ideavimrc"

  @NonNls
  private val DEBUG_HOME_VIMRC_PATHS = arrayOf(".$DEBUG_VIMRC_FILE_NAME", "_$DEBUG_VIMRC_FILE_NAME")

  @NonNls
  private val DEBUG_XDG_VIMRC_PATH = "ideavim/$DEBUG_VIMRC_FILE_NAME"

  /**
   * When this system property is set to `"true"`, IdeaVim prefers the debug ideavimrc
   * (`~/.debug.ideavimrc`) over the regular one, falling back to the regular file if the debug
   * file does not exist. The `runIde` Gradle task sets this property so development runs can use a
   * separate config without touching the real `~/.ideavimrc`.
   */
  @NonNls
  const val USE_DEBUG_VIMRC_PROPERTY: String = "ideavim.use.debug.ideavimrc"

  @JvmStatic
  fun findIdeaVimRc(): Path? {
    if (System.getProperty(USE_DEBUG_VIMRC_PROPERTY) == "true") {
      val debugVimRc = findIdeaVimRc(DEBUG_HOME_VIMRC_PATHS, DEBUG_XDG_VIMRC_PATH)
      if (debugVimRc != null) {
        logger.info("Using debug ideavimrc file: " + debugVimRc.absolutePathString())
        return debugVimRc
      }
      logger.info("Debug ideavimrc requested but not found, falling back to the regular ideavimrc")
    }
    return findIdeaVimRc(HOME_VIMRC_PATHS, XDG_VIMRC_PATH)
  }

  private fun findIdeaVimRc(homeVimrcPaths: Array<String>, xdgVimrcPath: String): Path? {
    val customVimrc = System.getenv("IDEA_VIM_CUSTOM_VIMRC")
    if (!customVimrc.isNullOrEmpty()) {
      val file = Path(customVimrc)
      if (file.exists()) {
        logger.debug { "Found ideavimrc file: $file" }
        return file
      }
    }

    // Check whether file exists in home dir
    val homeDirName = System.getProperty("user.home")
    if (homeDirName != null) {
      for (fileName in homeVimrcPaths) {
        val file = Path(homeDirName, fileName)
        if (file.exists()) {
          logger.debug { "Found ideavimrc file: $file" }
          return file
        }
      }
    } else {
      logger.info("User's home directory is not defined. Cannot locate ~/.ideavimrc or ~/_ideavimrc file.")
    }

    // Check in XDG config directory
    val xdgConfig = getXdgConfigHome()?.resolve(xdgVimrcPath)
    return xdgConfig?.let {
      if (it.exists()) {
        logger.debug { "Found ideavimrc file: $it" }
        it
      } else null
    }
  }

  /**
   * The base XDG config directory: `$XDG_CONFIG_HOME` if set (with a leading `~` expanded), otherwise `~/.config`.
   * IdeaVim's own config lives under `<this>/ideavim/` — e.g. the ideavimrc (`ideavim/ideavimrc`) and keymap files
   * (`ideavim/keymap/<name>.vim`). Returns null if the home directory can't be resolved.
   */
  @TestOnly
  @JvmField
  var xdgConfigHomeProvider: () -> String? = { System.getenv("XDG_CONFIG_HOME") }

  @JvmStatic
  fun getXdgConfigHome(): Path? {
    val homeDirName = System.getProperty("user.home")
    val xdgConfigHomeProperty = xdgConfigHomeProvider()
    return if (xdgConfigHomeProperty.isNullOrEmpty()) {
      if (homeDirName != null) Path(homeDirName, ".config") else null
    } else {
      val configHome = if (xdgConfigHomeProperty.startsWith("~/") || xdgConfigHomeProperty.startsWith("~\\")) {
        homeDirName + xdgConfigHomeProperty.substring(1)
      } else {
        xdgConfigHomeProperty
      }
      Path(configHome)
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

  fun findOrCreateIdeaVimRc(): Path? {
    val found = findIdeaVimRc()
    if (found != null) return found

    val homeDirName = System.getProperty("user.home")
    val vimrc = sourceVimrc(homeDirName)
    if (homeDirName != null) {
      for (fileName in HOME_VIMRC_PATHS) {
        try {
          val file = Path(homeDirName, fileName)
          file.createFile()
          file.writeText(getNewIdeaVimRcTemplate(vimrc))
          injector.vimrcFileState.filePath = file.absolutePathString()
          return file
        } catch (ignored: IOException) {
          // Try to create one of two files
        }
      }
    }
    return null
  }

  private fun sourceVimrc(homeDirName: String): String {
    if (Path(homeDirName, ".vimrc").exists()) {
      return """
        |" Source your .vimrc
        ||source ~/.vimrc
        |
      """.trimMargin()
    }
    if (Path(homeDirName, "_vimrc").exists()) {
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
      logger.info("Execute ideavimrc file: " + ideaVimRc.absolutePathString())

      // clear all previously enabled extensions
      IdeaPlug.Companion.EnabledExtensions.clearExtensions()

      injector.vimscriptExecutor.executeFile(ideaVimRc, editor, fileIsIdeaVimRcConfig = true)
    } else {
      logger.info("ideavimrc file isn't found")
    }
  }

  fun isIdeaVimRcFile(file: Path): Boolean {
    val ideaVimRc = findIdeaVimRc() ?: return false
    return ideaVimRc == file
  }
}
