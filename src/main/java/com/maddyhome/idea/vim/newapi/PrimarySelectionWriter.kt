/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.CaretStateTransferableData
import com.intellij.openapi.editor.RawText
import com.intellij.util.ui.EmptyClipboardOwner
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.HeadlessException
import java.awt.Toolkit
import java.io.File

/**
 * Pushes text to the windowing system's PRIMARY selection.
 *
 * AWT is fine on X11 / macOS / Windows. On Wayland, Mutter's Wayland→X11 bridge drops JBR's
 * writes under fast visual selection — external X11 readers see stale content — so we shell out
 * to xclip/wl-copy instead.
 */
internal interface PrimarySelectionWriter {
  /** `true` if the write was performed or reliably scheduled; `false` if PRIMARY isn't reachable here. */
  fun write(text: String, transferableData: List<Any>): Boolean
}

internal fun primarySelectionWriter(): PrimarySelectionWriter =
  if (System.getenv("WAYLAND_DISPLAY") != null) WaylandPrimarySelectionWriter()
  else AwtPrimarySelectionWriter()

/**
 * Builds the `TextBlockTransferable` for clipboard and PRIMARY writes. We pin a single-range
 * `CaretStateTransferableData` so IntelliJ doesn't reshape the pasted text to match whatever
 * multi-caret arrangement the destination editor has.
 *
 * Throws [HeadlessException] on a headless JVM; callers handle.
 */
@Suppress("UNCHECKED_CAST")
internal fun buildIjTextTransferable(
  text: String,
  rawText: String,
  transferableData: List<Any>,
): TextBlockTransferable {
  val mutableData = (transferableData as List<TextBlockTransferableData>).toMutableList()
  val normalized = TextBlockTransferable.convertLineSeparators(text, "\n", mutableData)
  if (mutableData.none { it is CaretStateTransferableData }) {
    mutableData += CaretStateTransferableData(intArrayOf(0), intArrayOf(normalized.length))
  }
  return TextBlockTransferable(normalized, mutableData, RawText(rawText))
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
 * Mirrors PRIMARY through xclip (preferred) or wl-copy (fallback).
 *
 * xclip writes X11 PRIMARY through XWayland and Mutter mirrors X11→Wayland reliably — both
 * sides see the content. wl-copy writes Wayland-native and relies on the broken Wayland→X11
 * bridge, which is the symptom we're working around.
 *
 * The tool is resolved once at construction so the hot path doesn't fork-exec-and-fail. Writes
 * are deferred so they land *after* IntelliJ's synchronous post-yank `updateSystemSelection`
 * overwrite; `ModalityState.any()` keeps them from being queued behind modal dialogs.
 */
internal class WaylandPrimarySelectionWriter : PrimarySelectionWriter {
  private val command: List<String>? = candidates.firstOrNull { isExecutableInPath(it.first()) }

  init {
    if (command == null) {
      logger.warn("Neither xclip nor wl-copy found on PATH; IdeaVim cannot mirror PRIMARY on Wayland")
    } else {
      logger.debug { "PRIMARY mirror will use: ${command.joinToString(" ")}" }
    }
  }

  override fun write(text: String, transferableData: List<Any>): Boolean {
    val argv = command ?: return false
    ApplicationManager.getApplication().invokeLater(
      { runCommand(argv, text) },
      ModalityState.any(),
    )
    return true
  }

  private fun runCommand(argv: List<String>, text: String) {
    try {
      val process = ProcessBuilder(argv).redirectErrorStream(true).start()
      process.outputStream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    } catch (e: Exception) {
      logger.debug { "${argv.joinToString(" ")} failed: ${e.message}" }
    }
  }

  companion object {
    private val logger = vimLogger<WaylandPrimarySelectionWriter>()

    private val candidates = listOf(
      listOf("xclip", "-selection", "primary"),
      listOf("wl-copy", "--primary"),
    )

    private fun isExecutableInPath(name: String): Boolean {
      val path = System.getenv("PATH") ?: return false
      return path.split(File.pathSeparator).any { dir ->
        val candidate = File(dir, name)
        candidate.isFile && candidate.canExecute()
      }
    }
  }
}
