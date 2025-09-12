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
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import com.intellij.util.Alarm.ThreadToUse
import com.jetbrains.rd.util.first
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimYankListener
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.annotations.NonNls
import java.awt.Color
import java.awt.Font

internal const val DEFAULT_HIGHLIGHT_DURATION: Int = 300

@NonNls
private val HIGHLIGHT_DURATION_VARIABLE_NAME = "highlightedyank_highlight_duration"

@NonNls
private val HIGHLIGHT_COLOR_VARIABLE_NAME = "highlightedyank_highlight_color"

@NonNls
private val HIGHLIGHT_FOREGROUND_COLOR_VARIABLE_NAME = "highlightedyank_highlight_foreground_color"

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
 * if you want to change text color of highlight you can provide the rgba of the color you want e.g.
 * let g:highlightedyank_highlight_foreground_color = "rgba(0, 0, 0, 255)"
 *
 * When a new text is yanked or user starts editing, the old highlighting would be deleted.
 */
internal class VimHighlightedYank : VimExtension, VimYankListener, ModeChangeListener {
  private val highlightHandler = HighlightHandler()
  private var initialised = false

  override fun getName() = "highlightedyank"

  override fun init() {
    // Note that these listeners will still be registered when IdeaVim is disabled. However, they'll never get called
    injector.listenersNotifier.modeChangeListeners.add(this)
    injector.listenersNotifier.yankListeners.add(this)

    // Register our own disposable to remove highlights when IdeaVim is disabled. Somehow, we need to re-register when
    // IdeaVim is re-enabled. We don't get a call back for that, but because the listeners are active until the
    // _extension_ is disabled, make sure we're properly initialised each time we're called.
    registerIdeaVimDisabledCallback()
    initialised = true
  }

  private fun registerIdeaVimDisabledCallback() {
    // TODO: IdeaVim should help with lifecycle management here - VIM-3419
    // IdeaVim should possibly unregister extensions, but it would also need to re-register them. We have to do this
    // state management manually for now
    Disposer.register(VimPlugin.getInstance().onOffDisposable) {
      highlightHandler.clearYankHighlighters()
      initialised = false
    }
  }

  override fun dispose() {
    // Called when the extension is disabled with `:set no{extension}` or if the plugin owning the extension unloads
    injector.listenersNotifier.modeChangeListeners.remove(this)
    injector.listenersNotifier.yankListeners.remove(this)

    highlightHandler.clearYankHighlighters()
    initialised = false
  }

  override fun yankPerformed(caretToRange: Map<ImmutableVimCaret, TextRange>) {
    ensureInitialised()
    highlightHandler.highlightYankRange(caretToRange)
  }

  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    if (editor.mode !is Mode.INSERT) return
    ensureInitialised()
    highlightHandler.clearYankHighlighters()
  }

  private fun ensureInitialised() {
    if (!initialised) {
      registerIdeaVimDisabledCallback()
      initialised = true
    }
  }

  private class HighlightHandler {
    private val alarm = Alarm(ThreadToUse.SWING_THREAD)
    private var lastEditor: Editor? = null
    private val highlighters = mutableSetOf<RangeHighlighter>()

    fun highlightYankRange(caretToRange: Map<ImmutableVimCaret, TextRange>) {
      // from vim-highlightedyank docs: When a new text is yanked or user starts editing, the old highlighting would be deleted
      clearYankHighlighters()

      val editor = caretToRange.first().key.editor.ij
      lastEditor = editor

      val attributes = getHighlightTextAttributes(editor)
      for (range in caretToRange.values) {
        for (i in 0 until range.size()) {
          val highlighter = editor.markupModel.addRangeHighlighter(
            range.startOffsets[i],
            range.endOffsets[i],
            HighlighterLayer.SELECTION,
            attributes,
            HighlighterTargetArea.EXACT_RANGE,
          )
          highlighters.add(highlighter)
        }
      }

      // from vim-highlightedyank docs: A negative number makes the highlight persistent.
      val timeout = extractUsersHighlightDuration()
      if (timeout >= 0) {
        // Note modality. This is important when highlighting an editor when a modal dialog is open, such as the resolve
        // conflict diff view
        alarm.addRequest(
          { clearYankHighlighters() },
          timeout,
          ModalityState.any()
        )
      }
    }

    fun clearYankHighlighters() {
      alarm.cancelAllRequests()
      // Make sure the last editor we saved is still alive before we use it. We can't just use the list of open editors
      // because this list is empty when IdeaVim is disabled, so we're unable to clean up
      lastEditor?.let { editor ->
        if (!editor.isDisposed) {
          highlighters.forEach { highlighter -> editor.markupModel.removeHighlighter(highlighter) }
        }
      }
      lastEditor = null
      highlighters.clear()
    }

    private fun getHighlightTextAttributes(editor: Editor): TextAttributes {
      return TextAttributes(
        extractUserHighlightForegroundColor(),
        extractUsersHighlightColor(),
        editor.colorsScheme.getColor(EditorColors.CARET_COLOR),
        EffectType.SEARCH_MATCH,
        Font.PLAIN,
      )
    }

    private fun extractUsersHighlightDuration(): Int {
      return extractVariable(HIGHLIGHT_DURATION_VARIABLE_NAME, DEFAULT_HIGHLIGHT_DURATION) {
        // toVimNumber will return 0 for an invalid string. Let's force it to throw
        when (it) {
          is VimString -> it.value.toInt()
          else -> it.toVimNumber().value
        }
      }
    }

    private fun extractUsersHighlightColor(): Color {
      val value =
        VimPlugin.getVariableService().getGlobalVariableValue(HIGHLIGHT_COLOR_VARIABLE_NAME)?.toVimString()?.value
      if (value != null) {
        return try {
          parseRgbaColor(value)
        } catch (e: Exception) {
          @Suppress("DialogTitleCapitalization")
          @VimNlsSafe val message = MessageHelper.message(
            "highlightedyank.error.invalid.value.of.0.1",
            "g:$HIGHLIGHT_COLOR_VARIABLE_NAME",
            e.message ?: "",
          )
          VimPlugin.showMessage(message)
          getDefaultHighlightTextColor()
        }
      }
      return getDefaultHighlightTextColor()
    }

    private fun extractUserHighlightForegroundColor(): Color? {
      val value =
        VimPlugin.getVariableService().getGlobalVariableValue(HIGHLIGHT_FOREGROUND_COLOR_VARIABLE_NAME)
          ?.toVimString()?.value
          ?: return null

      return try {
        parseRgbaColor(value)
      } catch (e: Exception) {
        @Suppress("DialogTitleCapitalization")
        @VimNlsSafe val message = MessageHelper.message(
          "highlightedyank.error.invalid.value.of.0.1",
          "g:$HIGHLIGHT_FOREGROUND_COLOR_VARIABLE_NAME",
          e.message ?: "",
        )
        VimPlugin.showMessage(message)
        null
      }
    }

    private fun parseRgbaColor(colorString: String): Color {
      val rgba = colorString
        .substring(4)
        .filter { it != '(' && it != ')' && !it.isWhitespace() }
        .split(',')
        .map { it.toInt() }

      if (rgba.size != 4 || rgba.any { it < 0 || it > 255 }) {
        throw IllegalArgumentException("Invalid RGBA values. Each component must be between 0 and 255")
      }

      return Color(rgba[0], rgba[1], rgba[2], rgba[3])
    }

    private fun <T> extractVariable(variable: String, default: T, extractFun: (value: VimDataType) -> T): T {
      val value = VimPlugin.getVariableService().getGlobalVariableValue(variable)
      if (value != null) {
        return try {
          extractFun(value)
        } catch (e: Exception) {
          @Suppress("DialogTitleCapitalization")
          @VimNlsSafe val message = MessageHelper.message(
            "highlightedyank.error.invalid.value.of.0.1",
            "g:$variable",
            e.message ?: "",
          )
          VimPlugin.showMessage(message)

          default
        }
      }

      return default
    }
  }
}
