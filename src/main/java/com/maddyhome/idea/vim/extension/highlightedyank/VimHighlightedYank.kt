/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.highlightedyank

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.VimProjectService
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.listener.VimInsertListener
import com.maddyhome.idea.vim.listener.VimYankListener
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.annotations.NonNls
import java.awt.Color
import java.awt.Font
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal const val DEFAULT_HIGHLIGHT_DURATION: Long = 300

@NonNls
private val HIGHLIGHT_DURATION_VARIABLE_NAME = "highlightedyank_highlight_duration"

@NonNls
private val HIGHLIGHT_COLOR_VARIABLE_NAME = "highlightedyank_highlight_color"
private var defaultHighlightTextColor: Color? = null

private fun getDefaultHighlightTextColor(): Color {
  return defaultHighlightTextColor
    ?: return EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES.defaultAttributes.backgroundColor
      .also { defaultHighlightTextColor = it }
}

internal class HighlightColorResetter : LafManagerListener {
  override fun lookAndFeelChanged(source: LafManager) {
    defaultHighlightTextColor = null
  }
}

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
 * When a new text is yanked or user starts editing, the old highlighting would be deleted.
 */
internal class VimHighlightedYank : VimExtension, VimYankListener, VimInsertListener {
  private val highlightHandler = HighlightHandler()

  override fun getName() = "highlightedyank"

  override fun init() {
    VimPlugin.getYank().addListener(this)
    VimPlugin.getChange().addInsertListener(this)
  }

  override fun dispose() {
    VimPlugin.getYank().removeListener(this)
    VimPlugin.getChange().removeInsertListener(this)
  }

  override fun yankPerformed(editor: VimEditor, range: TextRange) {
    highlightHandler.highlightYankRange(editor.ij, range)
  }

  override fun insertModeStarted(editor: Editor) {
    highlightHandler.clearAllYankHighlighters()
  }

  private class HighlightHandler {
    private var editor: Editor? = null
    private val yankHighlighters: MutableSet<RangeHighlighter> = mutableSetOf()

    fun highlightYankRange(editor: Editor, range: TextRange) {
      // from vim-highlightedyank docs: When a new text is yanked or user starts editing, the old highlighting would be deleted
      clearAllYankHighlighters()

      this.editor = editor
      val project = editor.project
      if (project != null) {
        Disposer.register(
          VimProjectService.getInstance(project)
        ) {
          this.editor = null
          yankHighlighters.clear()
        }
      }

      if (range.isMultiple) {
        for (i in 0 until range.size()) {
          highlightSingleRange(editor, range.startOffsets[i]..range.endOffsets[i])
        }
      } else {
        highlightSingleRange(editor, range.startOffset..range.endOffset)
      }
    }

    fun clearAllYankHighlighters() {
      yankHighlighters.forEach { highlighter ->
        editor?.markupModel?.removeHighlighter(highlighter) ?: StrictMode.fail("Highlighters without an editor")
      }

      yankHighlighters.clear()
    }

    private fun highlightSingleRange(editor: Editor, range: ClosedRange<Int>) {
      val highlighter = editor.markupModel.addRangeHighlighter(
        range.start,
        range.endInclusive,
        HighlighterLayer.SELECTION,
        getHighlightTextAttributes(),
        HighlighterTargetArea.EXACT_RANGE
      )

      yankHighlighters.add(highlighter)

      setClearHighlightRangeTimer(highlighter)
    }

    private fun setClearHighlightRangeTimer(highlighter: RangeHighlighter) {
      val timeout = extractUsersHighlightDuration()

      // from vim-highlightedyank docs: A negative number makes the highlight persistent.
      if (timeout >= 0) {
        Executors.newSingleThreadScheduledExecutor().schedule(
          {
            ApplicationManager.getApplication().invokeLater {
              editor?.markupModel?.removeHighlighter(highlighter) ?: StrictMode.fail("Highlighters without an editor")
            }
          },
          timeout, TimeUnit.MILLISECONDS
        )
      }
    }

    private fun getHighlightTextAttributes() = TextAttributes(
      null,
      extractUsersHighlightColor(),
      editor?.colorsScheme?.getColor(EditorColors.CARET_COLOR),
      EffectType.SEARCH_MATCH,
      Font.PLAIN
    )

    private fun extractUsersHighlightDuration(): Long {
      return extractVariable(HIGHLIGHT_DURATION_VARIABLE_NAME, DEFAULT_HIGHLIGHT_DURATION) {
        it.toLong()
      }
    }

    private fun extractUsersHighlightColor(): Color {
      return extractVariable(HIGHLIGHT_COLOR_VARIABLE_NAME, getDefaultHighlightTextColor()) { value ->
        val rgba = value
          .substring(4)
          .filter { it != '(' && it != ')' && !it.isWhitespace() }
          .split(',')
          .map { it.toInt() }

        Color(rgba[0], rgba[1], rgba[2], rgba[3])
      }
    }

    private fun <T> extractVariable(variable: String, default: T, extractFun: (value: String) -> T): T {
      val value = VimPlugin.getVariableService().getGlobalVariableValue(variable)

      if (value is VimString) {
        return try {
          extractFun(value.value)
        } catch (e: Exception) {
          @VimNlsSafe val message = MessageHelper.message(
            "highlightedyank.invalid.value.of.0.1",
            "g:$variable",
            e.message ?: ""
          )
          VimPlugin.showMessage(message)

          default
        }
      }

      return default
    }
  }
}
