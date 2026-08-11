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
   * @see com.maddyhome.idea.vim.api.VimClipboardManager.getOwnedPrimaryContent
   */
  fun ownsSelection(): Boolean

  /** Releases anything held on behalf of the selection. Default: nothing to release. */
  fun dispose() {}
}

internal fun primarySelectionWriter(): PrimarySelectionWriter {
  XclipPrimarySelectionWriter.tryCreate()?.let { return it }
  return AwtPrimarySelectionWriter()
}

internal class AwtPrimarySelectionWriter : PrimarySelectionWriter {
  private val latestClaim = AtomicReference<SelectionClaim?>()
  private val logger = vimLogger<AwtPrimarySelectionWriter>()

  override fun write(text: String, transferableData: List<Any>): Boolean {
    return try {
      val clipboard = Toolkit.getDefaultToolkit()?.systemSelection ?: return noClaim()
      claimSelection(clipboard, buildIjTextTransferable(text, text, transferableData))
      true
    } catch (e: Exception) {
      // Any failure, not just headless: AWT throws IllegalStateException when the selection cannot be
      // opened. Claiming optimistically and leaving the claim behind would report ownership of a write
      // that never happened, and nothing would ever revoke it.
      logger.debug { "Could not claim PRIMARY: ${e.message}" }
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
    // Published only once `setContents` has accepted it, so a throw leaves no claim to get stuck on.
    clipboard.setContents(content, claim)
    latestClaim.set(claim)
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
    try {
      ApplicationManager.getApplication().invokeLater(
        { executor.schedule(::drain, DEBOUNCE_MS, TimeUnit.MILLISECONDS) },
        ModalityState.any(),
      )
    } catch (e: Exception) {
      // Nothing will drain the pending write now, and a pending write counts as ownership — leaving it
      // set would report that we own PRIMARY for the rest of the session.
      pendingText.set(null)
      logger.debug { "Could not schedule the PRIMARY write: ${e.message}" }
      return false
    }
    return true
  }

  override fun ownsSelection(): Boolean = hasUnflushedWrite() || isHoldingSelection()

  private fun hasUnflushedWrite(): Boolean = pendingText.get() != null

  private fun isHoldingSelection(): Boolean = selectionHolder.get()?.isAlive == true

  override fun dispose() {
    executor.shutdownNow()
    pendingText.set(null)
    selectionHolder.getAndSet(null)?.destroy()
  }

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
      if (hasFailedImmediately(process)) {
        // xclip treats an unrecognised argument as a filename and exits without claiming anything, so
        // this is how a bad argv or an unreachable display surfaces — both output streams are
        // discarded, and a small payload fits the pipe buffer, so nothing else would report it.
        logger.warn("xclip exited with ${process.exitValue()} without taking PRIMARY; is `$STAY_IN_FOREGROUND` supported?")
        process = null
      }
    } catch (e: Exception) {
      // The write can fail after the process started, leaving xclip to claim PRIMARY with truncated
      // text. Kill it rather than orphan a holder we no longer track.
      process?.destroy()
      process = null
      logger.debug { "xclip failed: ${e.message}" }
    }
    selectionHolder.set(process)
  }

  /** A healthy foreground xclip stays alive holding the selection; a quick exit means it took nothing. */
  private fun hasFailedImmediately(process: Process): Boolean =
    process.waitFor(STARTUP_GRACE_MS, TimeUnit.MILLISECONDS)

  companion object {
    private const val DEBOUNCE_MS = 20L

    /** Long enough for a rejected argv or an unreachable display to exit, short enough not to stall a yank. */
    private const val STARTUP_GRACE_MS = 50L
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
