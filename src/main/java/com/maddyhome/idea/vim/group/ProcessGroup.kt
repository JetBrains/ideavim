/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.text.CharSequenceReader
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimProcessGroupBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.ij
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer


class ProcessGroup : VimProcessGroupBase() {
  @Throws(ExecutionException::class, ProcessCanceledException::class)
  override fun executeCommand(
    editor: VimEditor,
    command: String,
    input: CharSequence?,
    currentDirectoryPath: String?,
  ): String? {
    // This is a much simplified version of how Vim does this. We're using stdin/stdout directly, while Vim will
    // redirect to temp files ('shellredir' and 'shelltemp') or use pipes. We don't support 'shellquote', because we're
    // not handling redirection, but we do use 'shellxquote' and 'shellxescape', because these have defaults that work
    // better with Windows. We also don't bother using ShellExecute for Windows commands beginning with `start`.
    // Finally, we're also not bothering with the crazy space and backslash handling of the 'shell' options content.

    return ProgressManager.getInstance().runProcessWithProgressSynchronously<String, ExecutionException>(
      {
        val shell = injector.globalOptions().shell
        val shellcmdflag = injector.globalOptions().shellcmdflag
        val shellxescape = injector.globalOptions().shellxescape
        val shellxquote = injector.globalOptions().shellxquote

        // For Win32. See :help 'shellxescape'
        val escapedCommand = if (shellxquote == "(") doEscape(command, shellxescape, "^")
        else command
        // Required for Win32+cmd.exe, defaults to "(". See :help 'shellxquote'
        val quotedCommand = if (shellxquote == "(") "($escapedCommand)"
        else (if (shellxquote == "\"(") "\"($escapedCommand)\""
        else shellxquote + escapedCommand + shellxquote)

        val commands = ArrayList<String>()
        commands.add(shell)
        if (shellcmdflag.isNotEmpty()) {
          // Note that Vim also does a simple whitespace split for multiple parameters
          commands.addAll(ParametersListUtil.parse(shellcmdflag))
        }
        commands.add(quotedCommand)

        if (logger.isDebugEnabled) {
          logger.debug(String.format("shell=%s shellcmdflag=%s command=%s", shell, shellcmdflag, quotedCommand))
        }

        val commandLine = GeneralCommandLine(commands)
        if (currentDirectoryPath != null) {
          commandLine.setWorkDirectory(currentDirectoryPath)
        }
        val handler = CapturingProcessHandler(commandLine)
        if (input != null) {
          handler.addProcessListener(object : ProcessAdapter() {
            override fun startNotified(event: ProcessEvent) {
              try {
                val charSequenceReader = CharSequenceReader(input)
                val outputStreamWriter = BufferedWriter(OutputStreamWriter(handler.processInput))
                copy(charSequenceReader, outputStreamWriter)
                outputStreamWriter.close()
              } catch (e: IOException) {
                logger.error(e)
              }
            }
          })
        }

        val progressIndicator = ProgressIndicatorProvider.getInstance().progressIndicator
        val output = handler.runProcessWithProgressIndicator(progressIndicator)

        if (output.isCancelled) {
          // TODO: Vim will use whatever text has already been written to stdout
          // For whatever reason, we're not getting any here, so just throw an exception
          throw ProcessCanceledException()
        }

        val exitCode = handler.exitCode
        if (exitCode != null && exitCode != 0) {
          VimPlugin.showMessage("shell returned $exitCode")
          VimPlugin.indicateError()
        }
        (output.stderr + output.stdout).replace("\u001B\\[[;\\d]*m".toRegex(), "")
      }, "IdeaVim - !$command", true, editor.ij.project
    )
  }

  @Suppress("SameParameterValue")
  private fun doEscape(original: String, charsToEscape: String, escapeChar: String): String {
    var result = original
    for (c in charsToEscape.toCharArray()) {
      result = result.replace("" + c, escapeChar + c)
    }
    return result
  }

  // TODO: Java 10 has a transferTo method we could use instead
  @Throws(IOException::class)
  private fun copy(from: Reader, to: Writer) {
    val buf = CharArray(2048)
    var cnt: Int
    while ((from.read(buf).also { cnt = it }) != -1) {
      to.write(buf, 0, cnt)
    }
  }

  companion object {
    private val logger = logger<ProcessGroup>()
  }
}
