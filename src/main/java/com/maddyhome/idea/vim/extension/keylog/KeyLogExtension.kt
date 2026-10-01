/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.keylog

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.VimKeyTypedListener
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.services.VimRcService
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.io.Writer
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.KeyStroke
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories

@NonNls
private const val KEYLOG_FILE_VARIABLE_NAME = "keylog_file"

@NonNls
private const val DEFAULT_KEYLOG_FILE_NAME = "ideavim.log"

private val logger = logger<KeyLogExtension>()

/**
 * Writes every key the user types to a file, like Vim's `-w {scriptout}` command line argument
 *
 * The keys are written in Vim's key notation, so the log can be analysed to find the most used commands, like with
 * [vimlex](https://github.com/dstokes/vimlex) for Vim. See `doc/IdeaVim Plugins.md` for the user documentation.
 *
 * See https://youtrack.jetbrains.com/issue/VIM-1306
 */
internal class KeyLogExtension : VimExtension, VimKeyTypedListener {
  private val logFile = KeyLogFile()
  private var writer: KeyLogWriter? = null

  override fun getName() = "keylog"

  override fun init() {
    // Parented to the plugin, so the pending keys are written when the IDE exits
    writer = KeyLogWriter().also { Disposer.register(VimPlugin.getInstance(), it) }
    injector.listenersNotifier.keyTypedListeners.add(this)
  }

  override fun dispose() {
    injector.listenersNotifier.keyTypedListeners.remove(this)
    writer?.let { Disposer.dispose(it) }
    writer = null
  }

  override fun keyTyped(editor: VimEditor, key: KeyStroke) {
    val path = logFile.resolve() ?: return
    writer?.append(path, toLogNotation(key))
  }

  // A typed `<` is written as `<lt>`, so it can't be confused with key notation and the log can be parsed back
  private fun toLogNotation(key: KeyStroke) = if (key.keyChar == '<') "<lt>" else injector.parser.toKeyNotation(key)
}

/**
 * The log file named by `g:keylog_file`, or `ideavim.log` next to the ideavimrc file if the variable isn't set
 *
 * A relative path is relative to the directory of the ideavimrc file, or to the home directory if there is no
 * ideavimrc. This is resolved for every typed key, so the result is cached until the variable changes.
 */
private class KeyLogFile {
  private class Resolved(val variable: VimDataType?, val path: Path?)

  // Looking for the ideavimrc touches the file system, so don't do it for every keystroke
  private val baseDirectory: Path by lazy {
    VimRcService.findIdeaVimRc()?.parent ?: Path(System.getProperty("user.home"))
  }

  private var last: Resolved? = null

  /**
   * Returns the path of the log file, or null if `g:keylog_file` isn't a valid path
   */
  fun resolve(): Path? {
    val variable = injector.variableService.getGlobalVariableValue(KEYLOG_FILE_VARIABLE_NAME)
    last?.takeIf { it.variable === variable }?.let { return it.path }
    return toPath(variable).also { last = Resolved(variable, it) }
  }

  private fun toPath(variable: VimDataType?): Path? {
    val fileName = try {
      variable?.toVimString()?.value
    } catch (e: ExException) {
      logger.warn("g:$KEYLOG_FILE_VARIABLE_NAME is not a string, keys are not logged", e)
      return null
    }
    if (fileName.isNullOrBlank()) return baseDirectory.resolve(DEFAULT_KEYLOG_FILE_NAME)
    return try {
      baseDirectory.resolve(injector.pathExpansion.expandPath(fileName))
    } catch (e: InvalidPathException) {
      logger.warn("g:$KEYLOG_FILE_VARIABLE_NAME is not a valid path, keys are not logged: $fileName", e)
      null
    }
  }
}

/**
 * Appends the typed keys to the log file on a background thread
 *
 * All file access runs as tasks on a single thread executor, so the tasks never run concurrently and the state needs no
 * locking. The keys are collected by a [BufferedWriter][java.io.BufferedWriter], which is flushed to the file every few
 * seconds and when the writer is disposed.
 */
private class KeyLogWriter : Disposable {
  private class OpenLogFile(val path: Path, val writer: Writer)

  private val executor = AppExecutorUtil.createBoundedScheduledExecutorService("IdeaVim Key Log", 1)
  private val flushTask: ScheduledFuture<*> =
    executor.scheduleWithFixedDelay(::flush, FLUSH_DELAY_SECONDS, FLUSH_DELAY_SECONDS, TimeUnit.SECONDS)

  private var isDisposed = false
  private var openFile: OpenLogFile? = null
  private var failedPath: Path? = null

  fun append(path: Path, text: String) {
    // The plugin can dispose the writer at IDE exit before the extension stops sending keys
    if (isDisposed) return
    executor.execute {
      val writer = writerFor(path) ?: return@execute
      try {
        writer.write(text)
      } catch (e: IOException) {
        onWriteFailed(e)
      }
    }
  }

  override fun dispose() {
    isDisposed = true
    flushTask.cancel(false)
    // Wait for the pending keys to be written, but don't block the EDT for long if the file system is stuck
    try {
      executor.submit { close() }.get(DISPOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } catch (e: TimeoutException) {
      logger.warn("Timed out writing the key log file", e)
    } catch (e: ExecutionException) {
      logger.warn("Cannot close the key log file", e.cause)
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
    } finally {
      executor.shutdown()
    }
  }

  private fun flush() {
    try {
      openFile?.writer?.flush()
    } catch (e: IOException) {
      onWriteFailed(e)
    }
  }

  /**
   * Returns the writer for the given log file, opening it if `g:keylog_file` has changed
   *
   * A file that could not be opened or written is not retried until the path changes, so we don't try to open it again
   * for every keystroke.
   */
  private fun writerFor(path: Path): Writer? {
    openFile?.takeIf { it.path == path }?.let { return it.writer }
    if (path == failedPath) return null

    close()
    return try {
      path.createParentDirectories()
        .bufferedWriter(Charsets.UTF_8, DEFAULT_BUFFER_SIZE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        .also {
          openFile = OpenLogFile(path, it)
          failedPath = null
        }
    } catch (e: IOException) {
      logger.warn("Cannot open the key log file $path", e)
      failedPath = path
      null
    }
  }

  private fun onWriteFailed(e: IOException) {
    logger.warn("Cannot write to the key log file ${openFile?.path}", e)
    failedPath = openFile?.path
    close()
  }

  private fun close() {
    val file = openFile ?: return
    openFile = null
    try {
      file.writer.close()
    } catch (e: IOException) {
      logger.warn("Cannot close the key log file ${file.path}", e)
    }
  }

  companion object {
    private const val FLUSH_DELAY_SECONDS = 5L
    private const val DISPOSE_TIMEOUT_SECONDS = 2L
  }
}
