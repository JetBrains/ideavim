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
package com.maddyhome.idea.vim.ex.vimscript

import com.maddyhome.idea.vim.ex.CommandParser
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ui.VimRcFileState
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author vlan
 */
object VimScriptParser {
  // [VERSION UPDATE] 203+ Annotation should be replaced with @NlsSafe
  @NonNls
  private const val VIMRC_FILE_NAME = "ideavimrc"

  // [VERSION UPDATE] 203+ Annotation should be replaced with @NlsSafe
  @NonNls
  private val HOME_VIMRC_PATHS = arrayOf(".$VIMRC_FILE_NAME", "_$VIMRC_FILE_NAME")

  // [VERSION UPDATE] 203+ Annotation should be replaced with @NlsSafe
  @NonNls
  private val XDG_VIMRC_PATH = "ideavim" + File.separator + VIMRC_FILE_NAME
  private val DOUBLE_QUOTED_STRING = Pattern.compile("\"([^\"]*)\"")
  private val SINGLE_QUOTED_STRING = Pattern.compile("'([^']*)'")
  private val REFERENCE_EXPR = Pattern.compile("([A-Za-z_][A-Za-z_0-9]*)")
  private val DEC_NUMBER = Pattern.compile("(\\d+)")

  // This is a pattern used in ideavimrc parsing for a long time. It removes all trailing/leading spaced and blank lines
  private val EOL_SPLIT_PATTERN = Pattern.compile(" *(\r\n|\n)+ *")

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

  fun findOrCreateIdeaVimRc(): File? {
    val found = findIdeaVimRc()
    if (found != null) return found

    val homeDirName = System.getProperty("user.home")
    if (homeDirName != null) {
      for (fileName in HOME_VIMRC_PATHS) {
        try {
          val file = File(homeDirName, fileName)
          file.createNewFile()
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
  fun executeFile(file: File): List<String> {
    val data = try {
      readFile(file)
    } catch (ignored: IOException) {
      return emptyList()
    }
    executeText(data)
    return data
  }

  fun executeText(vararg text: String) {
    executeText(listOf(*text))
  }

  fun executeText(text: List<String>) {
    for (line in text) {
      // TODO: Build a proper parse tree for a VimL file instead of ignoring potentially nested lines (VIM-669)
      if (line.startsWith(" ") || line.startsWith("\t")) continue

      val lineToExecute = if (line.startsWith(":")) line.substring(1) else line
      try {
        val command = CommandParser.parse(lineToExecute)
        val commandHandler = CommandParser.getCommandHandler(command)
        if (commandHandler is VimScriptCommandHandler) {
          commandHandler.execute(command)
        }
      } catch (ignored: ExException) {
      }
    }
  }

  @Throws(ExException::class)
  fun evaluate(expression: String, globals: Map<String?, Any?>): Any {
    // This evaluator is very basic, no proper parsing whatsoever. It is here as the very first step necessary to
    // support mapleader, VIM-650. See also VIM-669.
    var m: Matcher = DOUBLE_QUOTED_STRING.matcher(expression)
    if (m.matches()) return m.group(1)

    m = SINGLE_QUOTED_STRING.matcher(expression)
    if (m.matches()) return m.group(1)

    m = REFERENCE_EXPR.matcher(expression)
    if (m.matches()) {
      val name = m.group(1)
      val value = globals[name]
      return value ?: throw ExException("Undefined variable: $name")
    }

    m = DEC_NUMBER.matcher(expression)
    if (m.matches()) return m.group(1).toInt()

    throw ExException("Invalid expression: $expression")
  }

  @Throws(ExException::class)
  fun expressionToString(value: Any): String {
    // TODO: Return meaningful value representations
    return when (value) {
      is String -> value
      is Int -> value.toString()
      else -> throw ExException("Cannot convert '$value' to string")
    }
  }

  @Throws(IOException::class)
  fun readFile(file: File): List<String> {
    val lines = ArrayList<String>()
    file.forEachLine { line -> lineProcessor(line, lines) }
    return lines
  }

  fun readText(data: CharSequence): List<String> {
    val lines = ArrayList<String>()
    EOL_SPLIT_PATTERN.split(data).forEach { line -> lineProcessor(line, lines) }
    return lines
  }

  fun lineProcessor(line: String, lines: ArrayList<String>) {
    val trimmedLine = line.trim()
    if (trimmedLine.isBlank()) return
    lines += trimmedLine
  }
}
