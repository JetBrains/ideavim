/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.google.common.io.CharStreams
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object MacKeyRepeat {
  var isEnabled: Boolean?
    get() {
      return try {
        val process = Runtime.getRuntime().exec(READ_COMMAND)
        val data = read(process.inputStream).trim().toIntOrNull() ?: return null
        data == 0
      } catch (_: IOException) {
        null
      }
    }
    set(value) {
      val command: Array<String>
      if (value == null) {
        command = DELETE_COMMAND
      } else {
        val arg = if (value) "0" else "1"
        command = WRITE_COMMAND + arg
      }
      try {
        val runtime = Runtime.getRuntime()
        val defaults = runtime.exec(command)
        defaults.waitFor()
        val restartSystemUI: Process = runtime.exec(EXEC_COMMAND)
        restartSystemUI.waitFor()
      } catch (_: IOException) {
      } catch (_: InterruptedException) {
      }
    }

  private val EXEC_COMMAND = arrayOf("launchctl", "stop", "com.apple.SystemUIServer.agent")
  private val READ_COMMAND = arrayOf("defaults", "read", "-globalDomain", "ApplePressAndHoldEnabled")
  private val WRITE_COMMAND = arrayOf("defaults", "write", "-globalDomain", "ApplePressAndHoldEnabled")
  private val DELETE_COMMAND = arrayOf("defaults", "delete", "-globalDomain", "ApplePressAndHoldEnabled")

  @Throws(IOException::class)
  private fun read(stream: InputStream): String {
    return CharStreams.toString(InputStreamReader(stream))
  }
}
