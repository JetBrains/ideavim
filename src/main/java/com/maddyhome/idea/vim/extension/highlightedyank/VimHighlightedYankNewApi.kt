/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.highlightedyank

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.util.Alarm
import com.intellij.util.Alarm.ThreadToUse
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Color
import com.intellij.vim.api.HighlightId
import com.intellij.vim.api.Mode
import com.intellij.vim.api.Range
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.extension.thin.api.VimPluginBase

/**
 * @author KostkaBrukowa (@kostkabrukowa)
 *
 * Port of vim-highlightedyank
 * See https://github.com/machakann/vim-highlightedyank
 *
 * if you want to optimize highlight duration, use g:highlightedyank_highlight_duration. Assign a time in milliseconds.
 *
 * let g:highlightedyank_highlight_duration = "1000"
 *
 * A negative number makes the highlight persistent.
 * let g:highlightedyank_highlight_duration = "-1"
 *
 * if you want to change background color of highlight you can provide the rgba of the color you want e.g.
 * let g:highlightedyank_highlight_color = "rgba(160, 160, 160, 155)"
 *
 * if you want to change text color of highlight you can provide the rgba of the color you want e.g.
 * let g:highlightedyank_highlight_foreground_color = "rgba(0, 0, 0, 255)"
 *
 * When a new text is yanked or user starts editing, the old highlighting would be deleted.
 */
class VimHighlightedYankNewApi : VimPluginBase() {
  private val myHighlightIds: MutableList<HighlightId> = mutableListOf()
  private val alarm = Alarm(ThreadToUse.SWING_THREAD)

  override val name: String = "highlightedyank"

  override suspend fun VimScope.init() {
    listeners {
      onModeChange { oldMode ->
        if (mode == Mode.INSERT) {
          clearYankHighlighters()
        }
      }

      onYank { caretRangeMap ->
        highlightYankRange(caretRangeMap)
      }
    }
  }

  override fun VimScope.unload() {
    clearYankHighlighters()
  }

  private fun VimScope.highlightYankRange(caretRangeMap: Map<CaretId, Range.Simple>) {
    clearYankHighlighters()

    val highlightColor: Color = extractColor(HIGHLIGHT_COLOR_VARIABLE) ?: getDefaultHighlightTextColor()
    val foregroundColor: Color? = extractColor(HIGHLIGHT_FOREGROUND_COLOR_VARIABLE)

    editor {
      change {
        caretRangeMap.forEach { (_, range) ->
          val highlighter = addHighlight(
            range.start,
            range.end,
            backgroundColor = highlightColor,
            foregroundColor = foregroundColor
          )
          myHighlightIds.add(highlighter)
        }
      }
    }

    try {
      val highlightDuration: Int = getVariable<Int>(HIGHLIGHT_DURATION_VARIABLE) ?: DEFAULT_DURATION
      if (highlightDuration >= 0) {
        alarm.addRequest(
          request = { clearYankHighlighters() },
          delayMillis = highlightDuration,
          modalityState = ModalityState.any(),
        )
      }
    } catch (e: Exception) {

    }
  }

  private fun VimScope.clearYankHighlighters() {
    alarm.cancelAllRequests()
    forEachEditor {
      change {
        myHighlightIds.forEach { highlighterId -> removeHighlight(highlighterId) }
      }
    }
    myHighlightIds.clear()
  }

  private fun getDefaultHighlightTextColor(): Color {
    return defaultHighlightTextColor
      ?: return EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES.defaultAttributes.backgroundColor.run {
        Color(red, green, blue, alpha)
      }.also { defaultHighlightTextColor = it }
  }

  private fun VimScope.extractColor(colorVarName: String): Color? {
    return try {
      val colorString = getVariable<String>(colorVarName) ?: return null
      parseRgbaColor(colorString)
    } catch (e: Exception) {
      // todo: Throw VimException
      null
    }
  }

  private fun parseRgbaColor(rgbaString: String): Color? {
    val rgba = rgbaString.removePrefix("rgba(")
      .filter { it != '(' && it != ')' && !it.isWhitespace() }
      .split(',')
      .map { it.toInt() }

    if (rgba.size != 4 || rgba.any { it < 0 || it > 255 }) {
      throw IllegalArgumentException("Invalid RGBA values. Each component must be between 0 and 255")
    }

    val (r, g, b, a) = rgba
    return Color(r, g, b, a)
  }

  companion object {
    private const val DEFAULT_DURATION: Int = 300
    private const val HIGHLIGHT_DURATION_VARIABLE = "g:highlightedyank_highlight_duration"
    private const val HIGHLIGHT_COLOR_VARIABLE = "g:highlightedyank_highlight_color"
    private const val HIGHLIGHT_FOREGROUND_COLOR_VARIABLE = "g:highlightedyank_highlight_foreground_color"
    private var defaultHighlightTextColor: Color? = null
  }
}