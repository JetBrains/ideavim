/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.signature

import com.intellij.icons.AllIcons
import com.intellij.ide.ui.UISettings
import com.intellij.lang.LangBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.JBColor
import com.intellij.ui.icons.IconReplacer
import com.intellij.ui.icons.IconWrapperWithToolTip
import com.intellij.util.ui.RegionPaintIcon
import com.intellij.util.ui.RegionPainter
import com.intellij.vim.api.VimInitApi
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.VimMarkListener
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.newapi.IjVimEditor
import java.awt.Component
import java.awt.Graphics2D
import javax.swing.Icon

internal const val PLUGIN_NAME: String = "signature"

/**
 * Gutter signs for the local marks of a file, plus the mark commands around them - a port of kshenoy/vim-signature
 * (VIM-1347).
 *
 * Global marks `A`-`Z` are left to the IDE: with the default `ideamarks` they are IDE bookmarks, which the platform
 * already draws in the gutter itself. Local marks `a`-`z` have no visual at all, and that is what this adds.
 */
internal class SignatureExtension : VimExtension, VimMarkListener {

  override fun getName(): String = PLUGIN_NAME

  // VimExtension.getOwner() and Listener.owner are both called "owner", so both are spelled out here
  private val mappingOwner: MappingOwner get() = MappingOwner.Plugin.get(getName())
  override val owner: ListenerOwner = ListenerOwner.Plugin.get(PLUGIN_NAME)

  override fun init(initApi: VimInitApi) {
    injector.listenersNotifier.markListeners.add(this)
    marksChanged(null)

    val owner = mappingOwner
    MAPPINGS.forEach { (keys, plugName, handler) ->
      val plugKeys = injector.parser.parseKeys(plugName)
      VimExtensionFacade.putExtensionHandlerMapping(MappingMode.NXO, plugKeys, owner, handler, false)
      VimExtensionFacade.putKeyMappingIfMissing(
        MappingMode.NXO,
        injector.parser.parseKeys(keys),
        owner,
        plugKeys,
        true,
      )
    }
  }

  override fun dispose() {
    injector.listenersNotifier.markListeners.remove(this)
    injector.keyGroup.removeKeyMapping(mappingOwner)
    injector.editorGroup.getEditors().forEach { removeAllSigns((it as IjVimEditor).editor) }
  }

  /**
   * Redraws the signs of every open editor.
   *
   * Only `a`-`z` are drawn, so a change to any other mark - and IdeaVim sets `.`, `^`, `'`, `[` and `]` on almost every
   * edit and jump - can be dropped without doing the work. [markChar] is null when the caller does not know which marks
   * changed, and then there is no choice but to redraw.
   */
  override fun marksChanged(markChar: Char?) {
    if (markChar != null && markChar !in VimMarkService.LOWERCASE_MARKS) return

    // The markup model is not ours to touch from a background thread, and setMark is reachable from one.
    runOnEdt {
      injector.editorGroup.getEditors().forEach { e ->
        val wanted =
          injector.markService.getAllLocalMarks(e.primaryCaret()).filter { it.key in VimMarkService.LOWERCASE_MARKS }
        val ijEditor = (e as IjVimEditor).editor
        val existingMarks = ijEditor.signHighlighters()

        removeMarks(existingMarks, ijEditor, wanted)
        addMarks(wanted, existingMarks, ijEditor)
      }
    }
  }
}

private data class SignatureMapping(val keys: String, val plugName: String, val handler: ExtensionHandler)

/**
 * `m,`, `m-` and `m<Space>` are `m{invalid-mark}` in Vim and so free to take. They still go through a `<Plug>` name
 * and
 * `putKeyMappingIfMissing`, so a user who wants a different key can map their own to the `<Plug>` name instead.
 *
 * The `<Plug>` names are parenthesised because `putKeyMappingIfMissing` asks `hasMapTo`, which matches the right-hand
 * side as a *substring*: a bare `<Plug>SignaturePurgeMarks` would count as already mapped by
 * `<Plug>SignaturePurgeMarksAtLine` and would silently never be bound.
 */
private val MAPPINGS: List<SignatureMapping> = listOf(
  SignatureMapping("m,", "<Plug>(SignaturePlaceNextMark)", NextAvailableMarkCommand()),
  SignatureMapping("m-", "<Plug>(SignaturePurgeMarksAtLine)", RemoveLineMarkCommand()),
  SignatureMapping("m<Space>", "<Plug>(SignaturePurgeMarks)", RemoveBufferMarksCommand()),
)

private fun runOnEdt(action: () -> Unit) {
  val application = ApplicationManager.getApplication()
  if (application.isDispatchThread) action() else application.invokeLater(action)
}

private fun Editor.signHighlighters(): List<RangeHighlighter> =
  markupModel.allHighlighters.filter { it.gutterIconRenderer is MarkupGutterIconRenderer }

private fun removeAllSigns(ijEditor: Editor) {
  ijEditor.signHighlighters().forEach { ijEditor.markupModel.removeHighlighter(it) }
}

private fun addMarks(
  wanted: List<Mark>,
  existingMarks: List<RangeHighlighter>,
  ijEditor: Editor,
) {
  wanted.forEach { mark ->
    // Marks are stored per file path with a plain line number and outlive the editor, so a file that has shrunk since
    // the mark was set - or since the last session, as local marks are persisted - can carry a mark past its end.
    // addLineHighlighter throws IndexOutOfBoundsException for such a line.
    if (mark.line >= ijEditor.document.lineCount || mark.line < 0) return@forEach
    if (!isMarkAlreadyPresent(mark, existingMarks, ijEditor)) {
      val highlighter = ijEditor.markupModel.addLineHighlighter(null, mark.line, HighlighterLayer.ADDITIONAL_SYNTAX)
      highlighter.gutterIconRenderer = MarkupGutterIconRenderer(mark.key)
    }
  }
}

private fun removeMarks(
  existingMarks: List<RangeHighlighter>,
  ijEditor: Editor,
  wanted: List<Mark>,
) {
  existingMarks.forEach { highlighter ->
    if (!isHighlighterStillNeeded(highlighter, ijEditor, wanted)) {
      ijEditor.markupModel.removeHighlighter(highlighter)
    }
  }
}

private fun isMarkAlreadyPresent(mark: Mark, existingMarks: List<RangeHighlighter>, ijEditor: Editor): Boolean {
  return existingMarks.any { it.isRepresentingMark(mark, ijEditor) }
}

private fun isHighlighterStillNeeded(highlighter: RangeHighlighter, ijEditor: Editor, wanted: List<Mark>): Boolean {
  return wanted.any { highlighter.isRepresentingMark(it, ijEditor) }
}

private fun RangeHighlighter.isRepresentingMark(mark: Mark, ijEditor: Editor): Boolean {
  val markChar = (gutterIconRenderer as MarkupGutterIconRenderer).mark
  if (mark.key != markChar) return false
  if (!isValid) return false
  return mark.line == ijEditor.document.getLineNumber(startOffset)
}

private class MarkupGutterIconRenderer(val mark: Char) : GutterIconRenderer() {
  // getIcon is called on every gutter repaint, so the icon is built once per mark rather than per paint.
  private val icon: Icon = SignatureBookmarkIcon.of(mark)

  override fun getIcon(): Icon = icon
  override fun getTooltipText(): String = mark.toString()
  override fun equals(obj: Any?): Boolean {
    return obj is MarkupGutterIconRenderer && mark == obj.mark
  }

  override fun hashCode(): Int {
    return mark.hashCode()
  }
}

private class SignatureMnemonicPainter(val icon: Icon, val mnemonic: String) : RegionPainter<Component?> {
  override fun toString() = "SignatureBookmarkMnemonicIcon:$mnemonic"
  override fun hashCode() = mnemonic.hashCode()
  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    val painter = other as? SignatureMnemonicPainter ?: return false
    return painter.mnemonic == mnemonic
  }

  override fun paint(g: Graphics2D, x: Int, y: Int, width: Int, height: Int, c: Component?) {
    icon.paintIcon(null, g, x, y)

    val foreground = EditorColorsUtil.getColor(
      null, EditorColorsUtil.createColorKey(
        "SignatureBookmark.Mnemonic.iconForeground",
        JBColor(0x000000, 0xBBBBBB)
      )
    )
    g.paint = foreground
    UISettings.setupAntialiasing(g)
    val frc = g.fontRenderContext
    val font = EditorFontType.PLAIN.globalFont

    val size1 = .75f * height
    val vector1 = font.deriveFont(size1).createGlyphVector(frc, mnemonic)
    val bounds1 = vector1.visualBounds

    val dx = x - bounds1.x + .5 * (width - bounds1.width)
    val dy = y - bounds1.y + .5 * (height - bounds1.height)
    g.drawGlyphVector(vector1, dx.toFloat(), dy.toFloat())
  }
}

private class SignatureBookmarkIcon : IconWrapperWithToolTip {
  val mnemonic: Char

  private constructor(mnemonic: Char, icon: Icon) : super(icon, LangBundle.messagePointer("tooltip.bookmarked")) {
    this.mnemonic = mnemonic
  }

  override fun replaceBy(replacer: IconReplacer): SignatureBookmarkIcon {
    return SignatureBookmarkIcon(mnemonic, replacer.replaceIcon(retrieveIcon()))
  }

  companion object {
    private val cache: MutableMap<Char, SignatureBookmarkIcon> = HashMap()

    @Synchronized
    fun of(mnemonic: Char): SignatureBookmarkIcon =
      cache.getOrPut(mnemonic) { SignatureBookmarkIcon(mnemonic, createBookmarkIcon(mnemonic)) }

    private fun createBookmarkIcon(mnemonic: Char): Icon {
      if (mnemonic == 0.toChar()) {
        return AllIcons.Gutter.Bookmark
      }
      val icon = AllIcons.Gutter.Mnemonic
      val painter = SignatureMnemonicPainter(icon, mnemonic.toString())
      val paintSize = if (ExperimentalUI.isNewUI()) 14 else 12
      return RegionPaintIcon(paintSize, paintSize, 0, painter).withIconPreScaled(false)
    }
  }
}
