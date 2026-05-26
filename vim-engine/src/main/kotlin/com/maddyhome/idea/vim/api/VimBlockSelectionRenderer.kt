/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Renders the block-visual selection band using IDE-level highlighters instead of N native
 * IntelliJ carets. Allows navigation (e.g., held `j` / `k`) to update the visible selection
 * in O(1) per motion rather than O(rows).
 *
 * Active only when the experimental virtual-block-selection path is enabled (see
 * EngineVisualGroup#setVisualSelection). Default implementation is a no-op so non-IDE editors
 * and tests are unaffected.
 */
interface VimBlockSelectionRenderer {
  fun isEnabled(editor: VimEditor): Boolean = false

  fun update(
    editor: VimEditor,
    blockStart: BufferPosition,
    blockEnd: BufferPosition,
    dollarExtension: Boolean = false,
  ) {
  }

  /** Called on mode change or editor disposal. */
  fun clear(editor: VimEditor) {}

  /** Returns the current block's logical corners, or null when no virtual block is active. */
  fun getBounds(editor: VimEditor): BlockBounds? = null

  /**
   * Materialize N real IntelliJ carets matching the current block bounds, then dispose the
   * highlighter band. Call this immediately before invoking a mutation that needs IntelliJ's
   * standard multi-caret semantics (e.g. typing in Select-block mode that replaces the
   * selection across N rows). No-op when no virtual block is active.
   */
  fun materializeCarets(editor: VimEditor) {}

  /**
   * Materialize from the bounds saved at the most recent block-visual exit. Used by ex
   * commands (`Command.execute`) that run in Normal mode after the visual exit — by then the
   * active bounds have been cleared but the snapshot still reflects the block range.
   */
  fun materializeCaretsFromSavedExit(editor: VimEditor) {}

  /**
   * Called by KeyHandler after each Vim command (including undo) finishes. The renderer uses
   * this to count commands since the most recent block-visual exit, so the undo virtual fallback
   * (UndoRedoHelper) can distinguish "the immediate destructive block edit" from later edits.
   */
  fun notifyCommandFinished(editor: VimEditor) {}

  data class BlockBounds(
    val anchorLine: Int,
    val anchorCol: Int,
    val activeLine: Int,
    val activeCol: Int,
    /** True if the active corner was extended via `$` — each row's selection runs to its own line end. */
    val dollarExtension: Boolean = false,
  )
}

/** No-op default — used by test stubs / regex evaluator. */
internal object NoOpVimBlockSelectionRenderer : VimBlockSelectionRenderer
