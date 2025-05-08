/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.google.common.io.CharStreams
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * @author vlan
 */
class MacKeyRepeat {
  var isEnabled: Boolean?
    get() {
      val command: String = String.format(FMT, read)
      return try {
        val process = Runtime.getRuntime().exec(command)
        val data = read(process.inputStream).trim { it <= ' ' }
        try {
          data.toInt() == 0
        } catch (_: NumberFormatException) {
          null
        }
      } catch (_: IOException) {
        null
      }
    }
    set(value) {
      val command: String
      if (value == null) {
        command = String.format(FMT, delete)
      } else {
        val arg = if (value) "0" else "1"
        command = String.format(FMT, write) + " " + arg
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

  companion object {
    @VimNlsSafe
    const val FMT: String = "defaults %s -globalDomain ApplePressAndHoldEnabled"
    val instance: MacKeyRepeat = MacKeyRepeat()
    private const val EXEC_COMMAND: @NonNls String = "launchctl stop com.apple.SystemUIServer.agent"
    private const val delete: @NonNls String = "delete"
    private const val write: @NonNls String = "write"
    private const val read: @NonNls String = "read"

    @Throws(IOException::class)
    private fun read(stream: InputStream): String {
      return CharStreams.toString(InputStreamReader(stream))
    }
  }
}
