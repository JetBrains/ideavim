/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.util.ui.EmptyClipboardOwner
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.HeadlessException
import java.awt.Toolkit
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Pushes text to the windowing system's PRIMARY selection.
 */
internal interface PrimarySelectionWriter {
  /** `true` if the write was performed or reliably scheduled; `false` if PRIMARY isn't reachable here. */
  fun write(text: String, transferableData: List<Any>): Boolean
}

internal fun primarySelectionWriter(): PrimarySelectionWriter {
  XclipPrimarySelectionWriter.tryCreate()?.let { return it }
  return AwtPrimarySelectionWriter()
}

internal class AwtPrimarySelectionWriter : PrimarySelectionWriter {
  override fun write(text: String, transferableData: List<Any>): Boolean {
    val clipboard = Toolkit.getDefaultToolkit()?.systemSelection ?: return false
    return try {
      val content = buildIjTextTransferable(text, text, transferableData)
      clipboard.setContents(content, EmptyClipboardOwner.INSTANCE)
      true
    } catch (_: HeadlessException) {
      false
    }
  }
}

/**
 * Mirrors PRIMARY through `xclip -selection primary`.
 */
internal class XclipPrimarySelectionWriter private constructor() : PrimarySelectionWriter {
  private val pendingText = AtomicReference<String?>()
  private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "IdeaVim-PrimarySelection").apply { isDaemon = true }
  }

  override fun write(text: String, transferableData: List<Any>): Boolean {
    pendingText.set(text)
    ApplicationManager.getApplication().invokeLater(
      { executor.schedule(::drain, DEBOUNCE_MS, TimeUnit.MILLISECONDS) },
      ModalityState.any(),
    )
    return true
  }

  private fun drain() {
    val text = pendingText.getAndSet(null) ?: return
    try {
      val process = ProcessBuilder(XCLIP_ARGV).redirectErrorStream(true).start()
      process.outputStream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    } catch (e: Exception) {
      logger.debug { "xclip failed: ${e.message}" }
    }
  }

  companion object {
    private const val DEBOUNCE_MS = 20L
    private val logger = vimLogger<XclipPrimarySelectionWriter>()
    private val XCLIP_ARGV = listOf("xclip", "-selection", "primary")

    fun tryCreate(): XclipPrimarySelectionWriter? {
      if (!isExecutableInPath()) {
        if (System.getenv("WAYLAND_DISPLAY") != null) {
          logger.warn("xclip not on PATH; falling back to AWT for PRIMARY (native-Wayland readers may see stale content)")
        }
        return null
      }
      logger.debug { "PRIMARY mirror will use xclip" }
      return XclipPrimarySelectionWriter()
    }

    private fun isExecutableInPath(): Boolean {
      val path = System.getenv("PATH") ?: return false
      return path.split(File.pathSeparator).any { dir ->
        val candidate = File(dir, "xclip")
        candidate.isFile && candidate.canExecute()
      }
    }
  }
}
