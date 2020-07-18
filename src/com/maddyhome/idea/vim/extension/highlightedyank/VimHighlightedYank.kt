/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.extension.highlightedyank

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.extension.VimExtension
import java.util.*
import kotlin.concurrent.schedule

class VimHighlightedYank: VimExtension {
  override fun getName() = "highlightedyank"

  override fun init() {
    println("init yank")

    Handler.highlightEnabled = true
  }


  override fun dispose() {
    super.dispose()
    println("dispose yank")
    Handler.highlightEnabled = false
  }


  companion object {
    const val DEFAULT_HIGHLIGHT_DURATION: Long = 300
    val DEFAULT_HIGHLIGHT_COLOR: TextAttributesKey = EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
  }

  object Handler {
    var highlightEnabled: Boolean = false
    private val highlightHandler = HighlightHandler()

    fun highlightYankRange(editor: Editor, range: TextRange) {
      highlightHandler.highlightYankRange(editor, range, highlightEnabled)
    }

    fun clearAllYankHighlighters(editor: Editor) {
      highlightHandler.clearAllYankHighlighters(editor)
    }
  }


  private class HighlightHandler {
    private val yankHighlighters: MutableSet<RangeHighlighter> = mutableSetOf()

    fun highlightYankRange(editor: Editor, range: TextRange, highlightEnabled: Boolean) {
      if(!highlightEnabled) return

      //from vim-highlightedyank docs: When a new text is yanked or user starts editing, the old highlighting would be deleted
      clearAllYankHighlighters(editor)

      if (range.isMultiple) {
        for (i in 0 until range.size()) {
          highlightSingleRange(editor, range.startOffsets[i]..range.endOffsets[i])
        }
      } else {
        highlightSingleRange(editor, range.startOffset..range.endOffset)
      }
    }

    fun clearAllYankHighlighters(editor: Editor) {
      yankHighlighters.forEach { editor.markupModel.removeHighlighter(it) }
      yankHighlighters.clear()
    }

    private fun highlightSingleRange(editor: Editor, range: ClosedRange<Int>) {
      val color = DEFAULT_HIGHLIGHT_COLOR
      val highlighter = editor.markupModel.addRangeHighlighter(
        range.start,
        range.endInclusive,
        HighlighterLayer.SELECTION - 1,
        editor.colorsScheme.getAttributes(color),
        HighlighterTargetArea.EXACT_RANGE
      )

      yankHighlighters.add(highlighter)

      setClearHighlightRangeTimer(editor, highlighter)
    }


    private fun setClearHighlightRangeTimer(editor: Editor, highlighter: RangeHighlighter) {
      val timeout = extractHighlightDuration()

      //from vim-highlightedyank docs: A negative number makes the highlight persistent.
      if(timeout > 0) {
        Timer("yankHighlight", false).schedule(timeout) {
          ApplicationManager.getApplication().invokeLater {
            editor.markupModel.removeHighlighter(highlighter)
          }
        }
      }
    }

    private fun extractHighlightDuration(): Long {
      val env = VimScriptGlobalEnvironment.getInstance()
      val value = env.variables["g:highlightedyank_highlight_duration"]

      if(value is String) {
        return try {
            value.toLong()
        }
        catch (e: NumberFormatException){
          VimPlugin.showMessage("highlightedyank: Invalid value of g:highlightedyank_highlight_duration -- " + e.message)
          VimPlugin.indicateError()

          DEFAULT_HIGHLIGHT_DURATION
        }
      }

      return DEFAULT_HIGHLIGHT_DURATION
    }
  }
}
