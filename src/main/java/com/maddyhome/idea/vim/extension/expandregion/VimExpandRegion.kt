/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.expandregion

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.VimInitApi
import com.intellij.vim.api.getVariable
import com.intellij.vim.api.models.Mode
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.scopes.get
import com.intellij.vim.api.scopes.nmapPluginAction
import com.intellij.vim.api.scopes.set
import com.intellij.vim.api.scopes.vmapPluginAction
import com.maddyhome.idea.vim.extension.VimExtension

/**
 * Port of vim-expand-region: https://github.com/terryma/vim-expand-region
 *
 * `+` selects the smallest region around the caret and grows it with each press, `_` shrinks it back. The regions come
 * from text objects, so text objects provided by other extensions can take part too.
 */
internal class VimExpandRegion : VimExtension {

  override fun getName(): String = "expand-region"

  override fun init(initApi: VimInitApi) {
    initApi.mappings {
      nmapPluginAction("+", EXPAND, keepDefaultMapping = true) { step(1, fromVisual = false) }
      vmapPluginAction("+", EXPAND, keepDefaultMapping = true) { step(1, fromVisual = true) }
      vmapPluginAction("_", SHRINK, keepDefaultMapping = true) { step(-1, fromVisual = true) }
    }
  }

  private suspend fun VimApi.step(direction: Int, fromVisual: Boolean) {
    val useSelectMode = useSelectMode()
    // With selectmode=cmd, `v` would start Select mode instead of Visual mode
    val selected = withOption("selectmode", "") {
      val state = (if (fromVisual) continuedState() else null) ?: computeState(caretOffset())
      select(state, state.index + direction)
    }
    if (selected && useSelectMode) normal("<C-G>")
  }

  private suspend fun VimApi.continuedState(): ExpandRegionState? {
    return storage { getWindowData<ExpandRegionState>(STATE_KEY) }
      ?.takeIf { it.currentRegion() == currentSelection() }
  }

  private suspend fun VimApi.select(state: ExpandRegionState, index: Int): Boolean {
    if (index > state.regions.lastIndex) return false
    if (index < 0) {
      if (isVisualOrSelect()) normal("<Esc>")
      return false
    }

    storage { putWindowData(STATE_KEY, state.copy(index = index)) }
    selectTextObject(state.caretOffset, state.regions[index].textObject)
    return true
  }

  private suspend fun VimApi.computeState(caretOffset: Int): ExpandRegionState {
    val measured = mutableListOf<Region>()
    // Text objects that select nothing are expected here, so they must not beep
    withOption("visualbell", true) {
      withOption("wrapscan", false) {
        for ((textObject, recursive) in configuredTextObjects()) {
          measured += measure(caretOffset, textObject, recursive)
        }
      }
    }
    val regions = measured
      .sortedBy { it.size }
      .filter { it.size > 1 && caretOffset >= it.start && caretOffset < it.end }
      .distinctBy { it.start to it.end }
    return ExpandRegionState(caretOffset, regions, -1)
  }

  private suspend fun VimApi.measure(caretOffset: Int, textObject: String, recursive: Boolean): List<Region> {
    val first = measureOne(caretOffset, textObject)
    if (!recursive || first.size <= 1) return listOf(first)

    val regions = mutableListOf(first)
    var count = 2
    while (true) {
      val nested = measureOne(caretOffset, "$count$textObject")
      if (nested.size <= regions.last().size) break
      regions.add(nested)
      count++
    }
    return regions
  }

  private suspend fun VimApi.measureOne(caretOffset: Int, textObject: String): Region {
    selectTextObject(caretOffset, textObject)
    val (start, end) = currentSelection()
    normal("<Esc>")
    editor { change { withPrimaryCaret { updateCaret(caretOffset) } } }
    return Region(textObject, start, end)
  }

  private suspend fun VimApi.selectTextObject(caretOffset: Int, textObject: String) {
    if (isVisualOrSelect()) normal("<Esc>")
    editor { change { withPrimaryCaret { updateCaret(caretOffset) } } }
    normal("v")
    // :normal without ! so that text objects mapped by other extensions work
    execute("normal $textObject")
  }

  private fun VimApi.configuredTextObjects(): Map<String, Boolean> {
    val configured = runCatching { getVariable<Map<String, Int>>("g:expand_region_text_objects") }.getOrNull()
    return configured?.mapValues { it.value != 0 } ?: DEFAULT_TEXT_OBJECTS
  }

  private suspend fun VimApi.useSelectMode(): Boolean {
    val enabled = runCatching { getVariable<Int>("g:expand_region_use_select_mode") }.getOrNull() ?: 0
    return enabled != 0 || "cmd" in option { get<String>("selectmode") }.split(",")
  }

  private suspend inline fun <reified V, T> VimApi.withOption(name: String, value: V, block: () -> T): T {
    val saved = option { get<V>(name) }
    if (saved == value) return block()
    option { set(name, value) }
    try {
      return block()
    } finally {
      option { set(name, saved) }
    }
  }

  private suspend fun VimApi.caretOffset(): Int = editor { read { withPrimaryCaret { offset } } }

  private suspend fun VimApi.currentSelection(): Pair<Int, Int> {
    return when (val selection = editor { read { withPrimaryCaret { selection } } }) {
      is Range.Simple -> selection.start to selection.end
      is Range.Block -> selection.start to selection.end
    }
  }

  private fun VimApi.isVisualOrSelect(): Boolean = mode in VISUAL_AND_SELECT_MODES

  private data class Region(val textObject: String, val start: Int, val end: Int) {
    val size: Int get() = end - start
  }

  private data class ExpandRegionState(val caretOffset: Int, val regions: List<Region>, val index: Int) {
    fun currentRegion(): Pair<Int, Int>? = regions.getOrNull(index)?.let { it.start to it.end }
  }

  companion object {
    private const val EXPAND = "<Plug>(expand_region_expand)"
    private const val SHRINK = "<Plug>(expand_region_shrink)"
    private const val STATE_KEY = "expand_region_state"

    private val VISUAL_AND_SELECT_MODES = setOf(
      Mode.VISUAL_CHARACTER,
      Mode.VISUAL_CHARACTER_FROM_SELECT,
      Mode.VISUAL_LINE,
      Mode.VISUAL_LINE_FROM_SELECT,
      Mode.VISUAL_BLOCK,
      Mode.VISUAL_BLOCK_FROM_SELECT,
      Mode.SELECT_CHARACTER,
      Mode.SELECT_LINE,
      Mode.SELECT_BLOCK,
    )

    private val DEFAULT_TEXT_OBJECTS = linkedMapOf(
      "iw" to false,
      "iW" to false,
      "i\"" to false,
      "i'" to false,
      "i]" to true,
      "ib" to true,
      "iB" to true,
      "il" to false,
      "ip" to false,
      "ie" to false,
    )
  }
}
