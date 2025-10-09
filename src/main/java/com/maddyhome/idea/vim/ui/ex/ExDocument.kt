/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.util.SystemInfo
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineStringHelper
import java.awt.Color
import java.awt.font.TextAttribute
import java.awt.im.InputMethodHighlight
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.Document
import javax.swing.text.DocumentFilter
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import kotlin.math.max
import kotlin.math.min

/**
 * This document provides insert/overwrite mode
 * Note that PlainDocument will remove CRs from text for single line text fields
 */
class ExDocument : DefaultStyledDocument() {
  /**
   * Checks if this document is in insert or overwrite mode
   *
   * @return true if overwrite, false if insert mode
   */
  var isOverwrite: Boolean = false
    private set

  init {
    documentFilter = ExDocumentFilter()
    val specialStyle = addStyle(SPECIAL_KEY_STYLE_NAME, null)
    val nonPrintableStyle = addStyle(NON_PRINTABLE_ELEMENT_NAME, specialStyle)
    nonPrintableStyle.addAttribute(ElementNameAttribute, NON_PRINTABLE_ELEMENT_NAME)
  }

  /**
   * Set the foreground colour for special key characters
   *
   * Maps to Vim's `SpecialKey` highlight
   */
  fun setSpecialKeyForeground(fg: Color) {
    val style = getStyle(SPECIAL_KEY_STYLE_NAME)
    if (style != null) {
      StyleConstants.setForeground(style, fg)
    }
  }

  /**
   * Toggles the insert/overwrite state
   */
  fun toggleInsertReplace() {
    val commandLine: VimCommandLine? = injector.commandLine.getActiveCommandLine()
    if (commandLine != null) {
      (commandLine as ExEntryPanel).isReplaceMode = !commandLine.isReplaceMode
    }
    this.isOverwrite = !this.isOverwrite
  }

  @Throws(BadLocationException::class)
  override fun insertString(offs: Int, str: String, a: AttributeSet?) {
    addInputMethodAttributes(a)
    super.insertString(offs, str, a)
  }

  // Mac apps will show a highlight for text being composed as part of an input method or dead keys (e.g. <A-N> N will
  // combine a ~ and n to produce ñ on a UK/US keyboard, and `, ' or ~ will combine to add accents on US International
  // keyboards. Java only adds a highlight when the Input Method tells it to, so normal text fields don't get the
  // highlight for dead keys. However, it does make text composition a little easier and more obvious, especially when
  // working with incremental search, and the IntelliJ editor also shows it on Mac, so we'll add it to the ex entry
  // field. Note that Windows doesn't show dead key highlights at all, not even for the IntelliJ editor. I don't know
  // what Linux does
  private fun addInputMethodAttributes(attributeSet: AttributeSet?) {
    if (!SystemInfo.isMac) {
      return
    }

    // See if we have a composed text attribute, and if so, try to cast it to AttributedString
    val composedTextAttribute = attributeSet?.getAttribute(StyleConstants.ComposedTextAttribute)
    if (composedTextAttribute is AttributedString) {
      // Iterate over all the characters in the attributed string (might just be `~` or `^`, or might be a longer prefix
      // from an IME) and add an input method highlight to it if it's not already there
      val iterator = composedTextAttribute.iterator
      while (iterator.current() != AttributedCharacterIterator.DONE) {
        val currentCharAttributes = iterator.attributes
        if (!currentCharAttributes.containsKey(TextAttribute.INPUT_METHOD_HIGHLIGHT) &&
          !currentCharAttributes.containsKey(TextAttribute.INPUT_METHOD_UNDERLINE)
        ) {
          composedTextAttribute.addAttribute(
            TextAttribute.INPUT_METHOD_HIGHLIGHT,
            InputMethodHighlight.UNSELECTED_CONVERTED_TEXT_HIGHLIGHT, iterator.index,
            iterator.index + 1
          )
        }

        iterator.next()
      }
    }
  }

  /**
   * A document filter to apply special attributes to non-printable characters
   *
   *
   * Filters document changes to insert non-printable characters as separate changes with special attributes. This
   * allows us identifying document elements containing non-printable characters and create a different view for them,
   * rendering them appropriately.
   *
   */
  private class ExDocumentFilter : DocumentFilter() {
    /**
     * Invoked before inserting a region of text in the specified Document.
     *
     *
     * This is called by [AbstractDocument.insertString], which has few callers in our usage. Most changes to the
     * text happen by replacing either the entire text or the selection, via [AbstractDocument.replace] (and
     * therefore also @{link DocumentFilter#replace}). Some things will still call [AbstractDocument.insertString]
     * such as IntelliJ's own paste action, which operates on the Ex text field component because it gets a
     * [com.intellij.openapi.editor.textarea.TextComponentEditor] instance from the current data context.
     *
     */
    @Throws(BadLocationException::class)
    override fun insertString(fb: FilterBypass, offset: Int, string: String, attr: AttributeSet?) {
      var offset = offset
      var string = string
      val originalOffset = offset

      string = filterNewLines(fb.document, string)

      var start = 0
      var pos = 0
      while (pos < string.length) {
        val isPrintable: Boolean = isPrintableCharacter(string[pos])
        while (pos < string.length && isPrintableCharacter(string[pos]) == isPrintable) {
          pos++
        }

        val a = if (isPrintable) attr else getNonPrintableAttributes(fb.document, attr)
        val s = string.substring(start, pos)
        fb.insertString(offset, s, a)
        offset += s.length

        start = pos
      }

      val exDocument = fb.document
      if (exDocument is ExDocument && exDocument.isOverwrite) {
        val rest = originalOffset + string.length
        if (rest < exDocument.length) {
          val len = min(string.length, exDocument.length - rest)
          fb.remove(rest, len)
        }
      }
    }

    /**
     * Invoked before replacing a region of text in the specified Document.
     *
     *
     * This method is called for most changes to the document. When handing a [java.awt.event.KeyEvent.KEY_TYPED] event, the
     * document is modified with a call to [javax.swing.text.JTextComponent.replaceSelection], which calls
     * [AbstractDocument.replace], passing the last offset and a length of 0. If client code calls
     * [javax.swing.text.JTextComponent.setText], this will call [AbstractDocument.replace] with an offset of 0 and the full
     * text length.
     *
     */
    @Throws(BadLocationException::class)
    override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
      var offset = offset
      var length = length
      var text = text
      if (text.isEmpty()) {
        fb.replace(offset, length, text, attrs)
        return
      }

      text = filterNewLines(fb.document, text)

      val exDocument = fb.document
      if (exDocument is ExDocument && exDocument.isOverwrite) {
        length = if (offset + text.length > exDocument.length) {
          exDocument.length - offset
        } else {
          max(length, text.length)
        }
      }

      var start = 0
      var pos = 0
      while (pos < text.length) {
        val isPrintable: Boolean = isPrintableCharacter(text[pos])
        while (pos < text.length && isPrintableCharacter(text[pos]) == isPrintable) {
          pos++
        }

        // Note that fb.replace will remove the existing text and then add the new text. If we're replacing the whole
        // text (e.g., ExTextField.setText), then this can reset the scroll position. Note that ExTextField has methods
        // to set, insert and delete text to avoid this situation.
        val a = if (isPrintable) attrs else getNonPrintableAttributes(fb.document, attrs)
        val s = text.substring(start, pos)
        if (start == 0) {
          fb.replace(offset, length, s, a)
        } else {
          fb.insertString(offset, s, a)
        }
        offset += s.length

        start = pos
      }
    }

    fun filterNewLines(document: Document, string: String): String {
      // A text field shouldn't have new lines
      val filterNewlines = document.getProperty("filterNewlines")
      if (filterNewlines is Boolean && filterNewlines) {
        return string.replace("\n".toRegex(), " ")
      }
      return string
    }

    companion object {
      private fun isPrintableCharacter(c: Char): Boolean {
        return EngineStringHelper.isPrintableCharacter(c)
      }

      private fun getNonPrintableAttributes(document: Document?, attrs: AttributeSet?): AttributeSet {
        if (document is ExDocument) {
          return document.getStyle(NON_PRINTABLE_ELEMENT_NAME)
        } else {
          val attributeSet = SimpleAttributeSet()
          if (attrs != null) {
            attributeSet.addAttributes(attrs)
          }
          attributeSet.addAttribute(ElementNameAttribute, NON_PRINTABLE_ELEMENT_NAME)
          return attributeSet
        }
      }
    }
  }

  companion object {
    const val SPECIAL_KEY_STYLE_NAME: String = "SpecialKey"
    const val NON_PRINTABLE_ELEMENT_NAME: String = "non-printable"
  }
}
