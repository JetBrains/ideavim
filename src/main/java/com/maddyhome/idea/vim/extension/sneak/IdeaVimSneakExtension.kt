/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.sneak

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.VimProjectService
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.newapi.ij
import org.jetbrains.annotations.TestOnly
import java.awt.Font
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.util.*
import javax.swing.Timer


private const val DEFAULT_HIGHLIGHT_DURATION_SNEAK = 300

// By [Mikhail Levchenko](https://github.com/Mishkun)
// Original repository with the plugin: https://github.com/Mishkun/ideavim-sneak
internal class IdeaVimSneakExtension : VimExtension {
  @Suppress("CompanionObjectInExtension")
  companion object {
    private var highlightHandler: HighlightHandler? = null

    @TestOnly
    internal fun stopTimer() {
      highlightHandler?.stopExistingTimer()
    }
  }

  override fun getName(): String = "sneak"

  override fun init() {
    val _highlightHandler = HighlightHandler()
    highlightHandler = _highlightHandler
    mapToFunctionAndProvideKeys("s", SneakHandler(_highlightHandler, Direction.FORWARD), MappingMode.NXO)

    // vim-sneak uses `Z` for visual mode because `S` conflict with vim-sneak plugin VIM-3330
    mapToFunctionAndProvideKeys("S", SneakHandler(_highlightHandler, Direction.BACKWARD), MappingMode.NO)
    mapToFunctionAndProvideKeys("Z", SneakHandler(_highlightHandler, Direction.BACKWARD), MappingMode.X)

    // workaround to support ; and , commands
    mapToFunctionAndProvideKeys("f", SneakMemoryHandler("f"), MappingMode.NXO)
    mapToFunctionAndProvideKeys("F", SneakMemoryHandler("F"), MappingMode.NXO)
    mapToFunctionAndProvideKeys("t", SneakMemoryHandler("t"), MappingMode.NXO)
    mapToFunctionAndProvideKeys("T", SneakMemoryHandler("T"), MappingMode.NXO)

    mapToFunctionAndProvideKeys(";", SneakRepeatHandler(_highlightHandler, RepeatDirection.IDENTICAL), MappingMode.NXO)
    mapToFunctionAndProvideKeys(",", SneakRepeatHandler(_highlightHandler, RepeatDirection.REVERSE), MappingMode.NXO)
  }

  private class SneakHandler(
    private val highlightHandler: HighlightHandler,
    private val direction: Direction,
  ) : ExtensionHandler {
    private val useLabel = injector.variableService
      .getGlobalVariableValue("sneak#label")
      ?.toVimNumber()?.booleanValue
      ?: false

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val charone = injector.keyGroup.getChar(editor) ?: return
      val chartwo = injector.keyGroup.getChar(editor) ?: return
      val range = Util.jumpTo(editor, charone, chartwo, direction)
      range?.let { highlightHandler.highlightSneakRange(editor.ij, range) }
      if (useLabel) {
        LabelUtil.jumpTo(editor, charone, chartwo, direction)
          ?.let { highlightHandler.highlightSneakRange(editor.ij, it) }
      }
      Util.lastSymbols = "${charone}${chartwo}"
      Util.lastSDirection = direction
    }
  }

  /**
   * This class acts as proxy for normal find commands because we need to update [Util.lastSDirection]
   */
  private class SneakMemoryHandler(private val char: String) : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      Util.lastSDirection = null
      VimExtensionFacade.executeNormalWithoutMapping(injector.parser.parseKeys(char), editor)
    }
  }

  private class SneakRepeatHandler(
    private val highlightHandler: HighlightHandler,
    private val direction: RepeatDirection,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val lastSDirection = Util.lastSDirection
      if (lastSDirection != null) {
        val (charone, chartwo) = Util.lastSymbols.toList()
        val jumpRange = Util.jumpTo(editor, charone, chartwo, direction.map(lastSDirection))
        jumpRange?.let { highlightHandler.highlightSneakRange(editor.ij, jumpRange) }
      } else {
        VimExtensionFacade.executeNormalWithoutMapping(injector.parser.parseKeys(direction.symb), editor.ij)
      }
    }
  }

  private object Util {
    var lastSDirection: Direction? = null
    var lastSymbols: String = ""
    fun jumpTo(editor: VimEditor, charone: Char, chartwo: Char, sneakDirection: Direction): TextRange? {
      val caret = editor.primaryCaret()
      val position = caret.offset
      val chars = editor.text()
      val foundPosition = sneakDirection.findBiChar(editor, chars, position, charone, chartwo)
      return jumpToPosition(editor, foundPosition)
    }

    fun jumpToPosition(editor: VimEditor, position: Int?): TextRange? {
      if (position != null) {
        editor.primaryCaret().moveToOffset(position)
      }
      editor.ij.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
      return position?.let { TextRange(position, position + 2) }
    }
  }

  private object LabelUtil {
    private val labels = ";sftunq/SFGHLTUNRMQZ?0".toList()
    private val labelInlays: MutableList<Inlay<*>> = mutableListOf()
    private val hintToPositionMap: MutableMap<Char, Int> = mutableMapOf()

    fun jumpTo(editor: VimEditor, charone: Char, chartwo: Char, sneakDirection: Direction): TextRange? {
      try {
        val visibleMatchingPositions = findVisibleMatchingPositions(editor, charone, chartwo, sneakDirection)
        if (visibleMatchingPositions.isEmpty()) {
          return null
        }

        addLabelsToMatches(editor, visibleMatchingPositions)

        // wait for user's input
        val selectedChar = run {
          val selectedChar = injector.keyGroup.getChar(editor)
          // if the 2nd char is \n, we need to wait for the user's input again
          if (selectedChar == null && chartwo == '\n') {
            return@run injector.keyGroup.getChar(editor)
          }
          selectedChar
        }
        val selectedPosition = hintToPositionMap[selectedChar]
        clear()

        if (selectedPosition == null) {
          VimExtensionFacade.executeNormalWithoutMapping(
            injector.parser.parseKeys(selectedChar.toString()),
            editor.ij,
          )
          return null
        }

        return Util.jumpToPosition(editor, selectedPosition)
      } finally {
        clear()
      }
    }

    private fun findVisibleMatchingPositions(
      editor: VimEditor,
      charone: Char,
      chartwo: Char,
      sneakDirection: Direction,
    ): List<Int> {
      val caret = editor.primaryCaret()
      val position = caret.offset
      return sneakDirection.findAllVisibleBiChars(editor, editor.text(), position, charone, chartwo)
    }

    private fun addLabelsToMatches(editor: VimEditor, positions: List<Int>) {
      positions.zip(labels).forEach { (position, label) ->
        val inlay = editor.ij.inlayModel.addInlineElement(position, false, LabelRenderer(label.toString()))
        hintToPositionMap[label] = position
        if (inlay != null) {
          labelInlays.add(inlay)
        }
      }
    }

    private fun clear() {
      labelInlays.forEach { it.dispose() }
      labelInlays.clear()
      hintToPositionMap.clear()
    }
  }

  private enum class Direction(val offset: Int) {
    FORWARD(1) {
      override fun findBiChar(
        editor: VimEditor,
        charSequence: CharSequence,
        position: Int,
        charone: Char,
        chartwo: Char,
      ): Int? {
        for (i in (position + offset) until charSequence.length - 1) {
          if (matches(editor, charSequence, i, charone, chartwo)) {
            return i
          }
        }
        return null
      }

      override fun findAllVisibleBiChars(
        editor: VimEditor,
        charSequence: CharSequence,
        position: Int,
        charone: Char,
        chartwo: Char,
      ): List<Int> {
        val visibleRange = editor.ij.getVisibleRangeOffset()
        val result = mutableListOf<Int>()
        for (i in (position + offset) until visibleRange.endOffset - 1) {
          if (matches(editor, charSequence, i, charone, chartwo)) {
            result.add(i)
          }
        }
        return result
      }
    },
    BACKWARD(-1) {
      override fun findBiChar(
        editor: VimEditor,
        charSequence: CharSequence,
        position: Int,
        charone: Char,
        chartwo: Char,
      ): Int? {
        for (i in (position + offset) downTo 0) {
          if (matches(editor, charSequence, i, charone, chartwo)) {
            return i
          }
        }
        return null
      }

      override fun findAllVisibleBiChars(
        editor: VimEditor,
        charSequence: CharSequence,
        position: Int,
        charone: Char,
        chartwo: Char,
      ): List<Int> {
        val visibleRange = editor.ij.getVisibleRangeOffset()
        val result = mutableListOf<Int>()
        for (i in (position + offset) downTo visibleRange.startOffset) {
          if (matches(editor, charSequence, i, charone, chartwo)) {
            result.add(i)
          }
        }
        return result
      }

    };

    abstract fun findBiChar(
      editor: VimEditor,
      charSequence: CharSequence,
      position: Int,
      charone: Char,
      chartwo: Char,
    ): Int?

    abstract fun findAllVisibleBiChars(
      editor: VimEditor,
      charSequence: CharSequence,
      position: Int,
      charone: Char,
      chartwo: Char,
    ): List<Int>

    fun matches(
      editor: VimEditor,
      charSequence: CharSequence,
      charPosition: Int,
      charOne: Char,
      charTwo: Char,
    ): Boolean {
      var match = charSequence[charPosition].equals(charOne, ignoreCase = injector.options(editor).ignorecase) &&
        charSequence[charPosition + 1].equals(charTwo, ignoreCase = injector.options(editor).ignorecase)

      if (injector.options(editor).ignorecase && injector.options(editor).smartcase) {
        if (charOne.isUpperCase() || charTwo.isUpperCase()) {
          match = charSequence[charPosition].equals(charOne, ignoreCase = false) &&
            charSequence[charPosition + 1].equals(charTwo, ignoreCase = false)
        }
      }
      return match
    }
  }

  private enum class RepeatDirection(val symb: String) {
    IDENTICAL(";") {
      override fun map(direction: Direction): Direction = direction
    },
    REVERSE(",") {
      override fun map(direction: Direction): Direction = when (direction) {
        Direction.FORWARD -> Direction.BACKWARD
        Direction.BACKWARD -> Direction.FORWARD
      }
    };

    abstract fun map(direction: Direction): Direction
  }

  private class HighlightHandler {
    private var editor: Editor? = null
    private val sneakHighlighters: MutableSet<RangeHighlighter> = mutableSetOf()
    private var timer: Timer? = null

    fun highlightSneakRange(editor: Editor, range: TextRange) {
      clearAllSneakHighlighters()

      this.editor = editor
      val project = editor.project
      if (project != null) {
        Disposer.register(VimProjectService.getInstance(project)) {
          this.editor = null
          sneakHighlighters.clear()
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

    fun clearAllSneakHighlighters() {
      sneakHighlighters.forEach { highlighter ->
        editor?.markupModel?.removeHighlighter(highlighter) ?: StrictMode.fail("Highlighters without an editor")
      }

      sneakHighlighters.clear()
    }

    private fun highlightSingleRange(editor: Editor, range: ClosedRange<Int>) {
      val highlighter = editor.markupModel.addRangeHighlighter(
        range.start,
        range.endInclusive,
        HighlighterLayer.SELECTION,
        getHighlightTextAttributes(),
        HighlighterTargetArea.EXACT_RANGE
      )

      sneakHighlighters.add(highlighter)

      setClearHighlightRangeTimer(highlighter)
    }

    private fun setClearHighlightRangeTimer(highlighter: RangeHighlighter) {
      stopExistingTimer()
      timer = Timer(DEFAULT_HIGHLIGHT_DURATION_SNEAK) {
        if (editor?.isDisposed != true) {
          editor?.markupModel?.removeHighlighter(highlighter)
        }
      }
      timer?.isRepeats = false
      timer?.start()
    }

    fun stopExistingTimer() {
      timer?.stop()
      timer?.actionListeners?.forEach { it.actionPerformed(null) }
    }

    private fun getHighlightTextAttributes() = TextAttributes(
      null,
      EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES.defaultAttributes.backgroundColor,
      editor?.colorsScheme?.getColor(EditorColors.CARET_COLOR),
      EffectType.SEARCH_MATCH,
      Font.PLAIN
    )
  }

  private class LabelRenderer(
    private val label: String,
  ) : EditorCustomElementRenderer {
    private fun boldFont(inlay: Inlay<*>): Font = inlay.editor.colorsScheme.getFont(EditorFontType.PLAIN).deriveFont(Font.BOLD)

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
      val font = boldFont(inlay)
      return inlay.editor.contentComponent.getFontMetrics(font).stringWidth(label) + 4
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
      val font = boldFont(inlay)
      g.font = font
      val fontMetrics = inlay.editor.contentComponent.getFontMetrics(font)
      val width = fontMetrics.stringWidth(label) + 4

      g.color = JBColor.YELLOW
      g.fillRect(r.x, r.y, width, r.height)
      g.color = JBColor.BLACK
      g.drawString(label, r.x + 2, r.y + fontMetrics.ascent)
    }
  }
}

/**
 * Map some <Plug>(keys) command to given handler
 *  and create mapping to <Plug>(prefix)[keys]
 */
private fun VimExtension.mapToFunctionAndProvideKeys(
  keys: String, handler: ExtensionHandler, mappingModes: EnumSet<MappingMode>
) {
  VimExtensionFacade.putExtensionHandlerMapping(
    mappingModes,
    injector.parser.parseKeys(command(keys)),
    owner,
    handler,
    false
  )
  VimExtensionFacade.putExtensionHandlerMapping(
    mappingModes,
    injector.parser.parseKeys(commandFromOriginalPlugin(keys)),
    owner,
    handler,
    false
  )

  // This is a combination to meet the following requirements:
  //  - Now we should support mappings from sneak `Sneak_s` and mappings from the previous version of the plugin `(sneak-s)`
  //  - The shortcut should not be registered if any of these mappings is overridden in .ideavimrc
  //  - The shortcut should not be registered if some other shortcut for this key exists
  val fromKeys = injector.parser.parseKeys(keys)
  val filteredModes = mappingModes.filterNotTo(HashSet()) {
    VimPlugin.getKey().hasmapto(it, injector.parser.parseKeys(command(keys)))
  }
  val filteredModes2 = mappingModes.filterNotTo(HashSet()) {
    VimPlugin.getKey().hasmapto(it, injector.parser.parseKeys(commandFromOriginalPlugin(keys)))
  }
  val filteredFromModes = mappingModes.filterNotTo(HashSet()) {
    injector.keyGroup.getKeyMapping(it).getLayer(fromKeys) != null
  }

  val doubleFiltered = mappingModes
    .filter { it in filteredModes2 && it in filteredModes && it in filteredFromModes }
    .toSet()
  putKeyMapping(doubleFiltered, fromKeys, owner, injector.parser.parseKeys(command(keys)), true)
  putKeyMapping(
    doubleFiltered,
    fromKeys,
    owner,
    injector.parser.parseKeys(commandFromOriginalPlugin(keys)),
    true
  )
}

// Derived from vim-flash (https://github.com/yelog/vim-flash)
// Original code licensed under Apache License 2.0
fun Editor.getVisibleRangeOffset(): TextRange {
  val scrollingModel = scrollingModel
  val visibleArea = scrollingModel.visibleArea
  val startLog = xyToLogicalPosition(Point(0, visibleArea.y))
  val lastLog = xyToLogicalPosition(Point(0, visibleArea.y + visibleArea.height))
  val startOff = logicalPositionToOffset(startLog)
  val endOff = logicalPositionToOffset(LogicalPosition(lastLog.line + 1, lastLog.column))
  return TextRange(startOff, endOff)
}

private fun command(keys: String) = "<Plug>(sneak-$keys)"
private fun commandFromOriginalPlugin(keys: String) = "<Plug>Sneak_$keys"
