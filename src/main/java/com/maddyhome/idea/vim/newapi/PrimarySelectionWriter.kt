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
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.Transferable
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

  /**
   * `true` while nothing else has claimed PRIMARY since our last [write], meaning the selection still
   * holds exactly the text we put there.
   *
   * @see com.maddyhome.idea.vim.api.VimClipboardManager.ownsPrimaryContent
   */
  fun ownsSelection(): Boolean
}

internal fun primarySelectionWriter(): PrimarySelectionWriter {
  XclipPrimarySelectionWriter.tryCreate()?.let { return it }
  return AwtPrimarySelectionWriter()
}

internal class AwtPrimarySelectionWriter : PrimarySelectionWriter {
  private val latestClaim = AtomicReference<SelectionClaim?>()

  override fun write(text: String, transferableData: List<Any>): Boolean {
    return try {
      val clipboard = Toolkit.getDefaultToolkit()?.systemSelection ?: return noClaim()
      claimSelection(clipboard, buildIjTextTransferable(text, text, transferableData))
      true
    } catch (_: HeadlessException) {
      noClaim()
    }
  }

  override fun ownsSelection(): Boolean = latestClaim.get()?.isStillHeld == true

  private fun noClaim(): Boolean {
    latestClaim.set(null)
    return false
  }

  private fun claimSelection(clipboard: Clipboard, content: Transferable) {
    val claim = SelectionClaim()
    latestClaim.set(claim)
    clipboard.setContents(content, claim)
  }

  /** Per-write, because AWT revokes a superseded claim asynchronously — a shared one would be cleared late. */
  private class SelectionClaim : ClipboardOwner {
    @Volatile
    var isStillHeld: Boolean = true
      private set

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
      isStillHeld = false
    }
  }
}

/**
 * Mirrors PRIMARY through `xclip -selection primary`.
 */
internal class XclipPrimarySelectionWriter private constructor() : PrimarySelectionWriter {
  private val pendingText = AtomicReference<String?>()
  private val selectionHolder = AtomicReference<Process?>()
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

  override fun ownsSelection(): Boolean = hasUnflushedWrite() || isHoldingSelection()

  private fun hasUnflushedWrite(): Boolean = pendingText.get() != null

  private fun isHoldingSelection(): Boolean = selectionHolder.get()?.isAlive == true

  private fun drain() {
    val text = pendingText.getAndSet(null) ?: return
    var process: Process? = null
    try {
      process = ProcessBuilder(XCLIP_ARGV)
        // A foreground xclip chatters about selection requests; an unread pipe would fill and block it.
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
      // Closing stdin is what makes xclip claim the selection; the previous holder then releases it
      // and exits by itself.
      process.outputStream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    } catch (e: Exception) {
      // The write can fail after the process started, leaving xclip to claim PRIMARY with truncated
      // text. Kill it rather than orphan a holder we no longer track.
      process?.destroy()
      process = null
      logger.debug { "xclip failed: ${e.message}" }
    }
    selectionHolder.set(process)
  }

  companion object {
    private const val DEBOUNCE_MS = 20L
    private val logger = vimLogger<XclipPrimarySelectionWriter>()

    // Detached, xclip outlives the Process we hold, leaving us blind to ownership. In the
    // foreground the Process *is* the owner: alive while it holds PRIMARY, gone once it loses it.
    private const val STAY_IN_FOREGROUND = "-quiet"
    private val XCLIP_ARGV = listOf("xclip", "-selection", "primary", STAY_IN_FOREGROUND)

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
