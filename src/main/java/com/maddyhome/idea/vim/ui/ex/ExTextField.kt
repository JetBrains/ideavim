/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.paint.PaintUtil
import com.intellij.util.ui.JBUI
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimCommandLineCaret
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineStringHelper.isPrintableCharacter
import com.maddyhome.idea.vim.helper.selectEditorFont
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.options.helpers.GuiCursorAttributes
import com.maddyhome.idea.vim.options.helpers.GuiCursorMode
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper.getAttributes
import com.maddyhome.idea.vim.options.helpers.GuiCursorType
import kotlinx.io.IOException
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.Rectangle
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.event.CaretEvent
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultCaret
import javax.swing.text.Document
import javax.swing.text.JTextComponent
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.StyledDocument
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A custom text field for the Ex command line.
 *
 *
 * Note that because this is an instance of [JTextComponent], anything that looks for an IntelliJ [com.intellij.openapi.editor.Editor]
 * in current data context will get an instance of [com.intellij.openapi.editor.textarea.TextComponentEditor], which means that normal IntelliJ
 * shortcuts and actions will work with the Ex command line, even if they're not supposed to. E.g., CMD+V will paste on
 * Mac, but CTRL+V on Windows won't, because the Ex command line handles that shortcut. I don't see a way to fix this.
 *
 */
class ExTextField internal constructor(private val myParentPanel: ExEntryPanel) : JTextField() {
  fun reset() {
    clearCurrentAction()
    setInsertMode()
  }

  fun deactivate() {
    clearCurrentAction()
  }

  override fun getMargin(): Insets {
    return JBUI.emptyInsets()
  }

  override fun getInsets(): Insets {
    return JBUI.emptyInsets()
  }

  // Called when the LAF is changed, but only if the control is visible
  override fun updateUI() {
    // Override the default look and feel specific UI so we can have a completely borderless and margin-less text field.
    // (See TextFieldWithPopupHandlerUI#getDefaultMargins and derived classes). This allows us to draw the text field
    // directly next to the label
    setUI(ExTextFieldUI(ExEditorKit))
    invalidate()

    setBorder(null)
  }

  /**
   * Stores the current text as the last edited entry in the command line's history
   *
   *
   * This method is called whenever the text is changed by the user, either by typing, cut/paste or delete. It remembers
   * the current text as the last entry in the command line's history, so that we can scroll into the past and then
   * return to the uncommitted text we were previously editing.
   *
   */
  fun saveLastEntry() {
    myParentPanel.lastEntry = super.getText()
  }

  /**
   * Update the text in the text field without saving it as the current/last entry in the command line's history
   *
   *
   * This is used to update the text to another entry in the command line's history, without losing the current/last
   * entry.
   *
   */
  fun updateText(string: String) {
    super.setText(string)
    setFontToJField(string)
  }

  override fun setText(string: String) {
    super.setText(string)

    saveLastEntry()
    setFontToJField(string)
  }

  fun insertText(offset: Int, text: String?) {
    try {
      // Note that ExDocument.insertString handles overwriting, but not replace mode!
      document.insertString(offset, text, null)
    } catch (e: BadLocationException) {
      logger.error(e)
    }
    saveLastEntry()
    setFontToJField(getText())
  }

  fun deleteText(offset: Int, length: Int) {
    try {
      document.remove(offset, min(length, document.length - offset))
    } catch (e: BadLocationException) {
      logger.error(e)
    }
    saveLastEntry()
    setFontToJField(getText())
  }

  // VIM-570
  private fun setFontToJField(stringToDisplay: String) {
    setFont(selectEditorFont(myParentPanel.ijEditor, stringToDisplay))
  }

  override fun setFont(f: Font?) {
    super.setFont(f)
    val document = getDocument()
    if (document is StyledDocument) {
      val defaultStyle = document.getStyle(StyleContext.DEFAULT_STYLE)
      if (StyleConstants.getFontFamily(defaultStyle) != getFont().family) {
        StyleConstants.setFontFamily(defaultStyle, getFont().family)
      }
      if (StyleConstants.getFontSize(defaultStyle) != getFont().getSize()) {
        StyleConstants.setFontSize(defaultStyle, getFont().getSize())
      }
    }
  }

  override fun setForeground(fg: Color?) {
    super.setForeground(fg)
    val document = getDocument()
    if (document is StyledDocument) {
      val defaultStyle = document.getStyle(StyleContext.DEFAULT_STYLE)
      StyleConstants.setForeground(defaultStyle, fg)
    }
  }

  fun setSpecialKeyForeground(fg: Color) {
    val document = getDocument()
    if (document is ExDocument) {
      document.setSpecialKeyForeground(fg)
    }
  }

  /**
   * Finish handling the keystroke
   *
   *
   * When the key event is first received, the keystroke is sent to the key handler to handle commands or for mapping.
   * The potentially mapped keystroke is then returned to the text field to complete handling. Typically, the only
   * keystrokes passed are typed characters, along with pressed shortcuts that aren't recognised as commands (they won't
   * be recognised by the text field either; we don't register any commands).
   *
   * @param stroke  The potentially mapped keystroke
   */
  fun handleKey(stroke: KeyStroke) {
    if (logger.isDebugEnabled) logger.debug("stroke=$stroke")

    // Typically, we would let the super class handle the keystroke. It would use any registered keybindings to convert
    // it to an action handler, or use the default handler (we don't actually have any keybindings). The default action
    // handler adds all non-control characters to the text field. We want to add all characters, so if we have an
    // actual character, just add it. Anything else, we'll pass to the super class like before (even though it's unclear
    // what it will do with the keystroke)
    if (stroke.keyChar != KeyEvent.CHAR_UNDEFINED) {
      replaceSelection(stroke.keyChar.toString())
    } else {
      if (stroke.keyCode != KeyEvent.VK_TAB || !executeTabCompletionIfPossible()) {
        val event = KeyEvent(
          this, stroke.keyEventType, (Date()).time, stroke.modifiers,
          stroke.keyCode, stroke.keyChar
        )

        // Call super to avoid recursion!
        super.processKeyEvent(event)
      }
    }

    saveLastEntry()
  }

  override fun processKeyEvent(e: KeyEvent) {
    if (logger.isDebugEnabled) logger.debug("key=$e")

    // The user has pressed or typed a key. The text field is first notified of a KEY_PRESSED event. If it's a simple
    // text character, it will be translated to a KEY_TYPED event with a keyChar, and we'll be notified again. We'll
    // forward this typed key to the key handler, which will potentially map it to another keystroke and return it to
    // the text field via ExTextField.handleKey.
    // If it's not a simple text character but a shortcut (i.e., a keypress with one or more modifiers), then it will be
    // handled in the same way as a shortcut activated in the main editor. That is, the IntelliJ action system sees the
    // KEY_PRESSED event first and dispatches it to IdeaVim's action handler, which pushes it through the key handler
    // and invokes the action. The event is handled before this component sees it! This method is not called for
    // registered action shortcuts. It is called for other shortcuts, and we pass those on to the super class.
    // (We should consider passing these to the key handler too, so that "insert literal" (<C-V>) will convert them to
    // typed keys and display them in the text field. As it happens, Vim has shortcuts for all control characters, and
    // it's unclear if/how we should represent other modifier-based shortcuts)
    // Note that Enter and Escape are registered as editor actions rather than action shortcuts. Therefore, we have to
    // handle them separately, as key presses, but not as typed characters.
    if (isAllowedPressedEvent(e) || isAllowedTypedEvent(e)) {
      val editor = myParentPanel.editor
      val keyHandler = getInstance()
      val keyStroke = KeyStroke.getKeyStrokeForEvent(e)
      keyHandler.handleKey(
        editor, keyStroke, IjEditorExecutionContext(myParentPanel.context!!),
        keyHandler.keyHandlerState
      )
      e.consume()
    } else {
      super.processKeyEvent(e)
    }
  }

  private fun executeTabCompletionIfPossible(): Boolean {
    val parts = getText().split(" ", limit = 2)
    if (parts.size < 2) return false
    val command = parts[0]
    // If more commands require TAB action handling, a more sophisticated check and delegation is needed
    if (!supportedTabCompletionCommands.contains(command)) return false

    val input = parts[1]
    val project = (parent as ExEntryPanel).context?.getData<Project>(DataKey.create("project"))
    val projectBasePath = project?.basePath?.let { Path.of(it) } ?: return false
    val inputPath = projectBasePath.resolve(input)
    val inputPathString = inputPath.toString()

    // Would it be useful to ignore file path case?
    try {
      val filePaths = (
        if (inputPath.exists()) {
          if (inputPath.isDirectory())
            Files.list(inputPath)
          else
            Files.list(inputPath.parent)
        } else if (inputPath.isAbsolute || inputPath.exists()) Files.list(inputPath.parent)
          .filter { it.toString().startsWith(inputPathString) }
        else Files.list(projectBasePath)
          .filter { it.toString().startsWith(inputPathString) }
        ).sorted().toList()

      if (filePaths.isEmpty()) return false

      val suggestion =
        if (filePaths.contains(inputPath))
          filePaths[(filePaths.indexOf(inputPath) + 1) % filePaths.size]
        else
          filePaths.first()

      // If a suggested file is descendant of project base path, use relative path. Else, use absolute path.
      val isDir = suggestion.isDirectory()
      val effectivePath = if (projectBasePath > suggestion) suggestion else projectBasePath.relativize(suggestion)
      updateText(String.format("%s %s%s", command, effectivePath, if (isDir) "/" else ""))
      return true
    } catch (e: IOException) {
      logger.error(e)
      return false
    }
  }


  private fun isAllowedTypedEvent(event: KeyEvent): Boolean {
    return event.getID() == KeyEvent.KEY_TYPED && !isKeyCharEnterOrEscape(event.getKeyChar())
  }

  private fun isAllowedPressedEvent(event: KeyEvent): Boolean {
    return event.getID() == KeyEvent.KEY_PRESSED && isKeyCodeEnterOrEscape(event.getKeyCode())
  }

  private fun isKeyCharEnterOrEscape(keyChar: Char): Boolean {
    return keyChar == '\n' || keyChar == '\u001B'
  }

  private fun isKeyCodeEnterOrEscape(keyCode: Int): Boolean {
    return keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE
  }


  /**
   * Creates the default implementation of the model
   * to be used at construction if one isn't explicitly
   * given.  An instance of `PlainDocument` is returned.
   *
   * @return the default model implementation
   */
  override fun createDefaultModel(): Document {
    return ExDocument()
  }

  fun clearCurrentAction() {
    val commandLine: VimCommandLine? = injector.commandLine.getActiveCommandLine()
    commandLine?.clearPromptCharacter()
  }

  private fun setInsertMode() {
    val doc = document as ExDocument
    if (doc.isOverwrite) {
      doc.toggleInsertReplace()
    }
    resetCaret()
  }

  fun toggleInsertReplace() {
    val doc = document as ExDocument
    doc.toggleInsertReplace()

    // Hide/show the caret so its new shape is immediately visible
    caret.setVisible(false)
    resetCaret()
    caret.setVisible(true)
  }

  private fun resetCaret() {
    if (caretPosition == super.getText().length ||
      currentActionPromptCharacterOffset == super.getText().length - 1
    ) {
      setNormalModeCaret()
    } else {
      val doc = document as ExDocument
      if (doc.isOverwrite) {
        setReplaceModeCaret()
      } else {
        setInsertModeCaret()
      }
    }
  }

  // The default cursor shapes for command line are:
  // 'c' command-line normal is block
  // 'ci' command-line insert is ver25
  // 'cr' command-line replace is hor20
  // see :help 'guicursor'
  private fun setNormalModeCaret() {
    caret.setAttributes(getAttributes(GuiCursorMode.CMD_LINE))
  }

  private fun setInsertModeCaret() {
    caret.setAttributes(getAttributes(GuiCursorMode.CMD_LINE_INSERT))
  }

  private fun setReplaceModeCaret() {
    caret.setAttributes(getAttributes(GuiCursorMode.CMD_LINE_REPLACE))
  }

  private class CommandLineCaret : DefaultCaret(), VimCommandLineCaret {
    var mode: GuiCursorType? = null
    var thickness = 100
    private var lastBlinkRate = 0
    private var hasFocus = false

    fun setAttributes(attributes: GuiCursorAttributes) {
      // Note: do not call anything that causes a layout in this method! E.g. setVisible. This method is used as a
      // callback whenever the caret moves, and causing a layout at this point can cause issues such as an infinite
      // loop in the layout algorithm with multi-width characters such as emoji or non-Latin characters (I don't know
      // why the layout algorithm gets stuck, but we can easily avoid it)
      // See VIM-2562

      mode = attributes.type
      thickness = if (mode == GuiCursorType.BLOCK) 100 else attributes.thickness
    }

    override fun focusGained(e: FocusEvent?) {
      if (lastBlinkRate != 0) {
        setBlinkRate(lastBlinkRate)
        lastBlinkRate = 0
      }
      super.focusGained(e)
      repaint()
      hasFocus = true
    }

    override fun focusLost(e: FocusEvent?) {
      // We don't call super.focusLost, which would hide the caret
      hasFocus = false
      lastBlinkRate = getBlinkRate()
      setBlinkRate(0)
      // Make sure the box caret is visible. If we're flashing, this might be false
      setVisible(true)
      repaint()
    }

    override fun paint(g: Graphics) {
      if (!isVisible) return

      // Take a copy of the graphics, so we can mess around with it without having to reset after
      val g2d = g.create() as Graphics2D
      try {
        val component = getComponent()

        g2d.color = component.getBackground()
        g2d.setXORMode(component.caretColor)

        val r = modelToView(getDot()) ?: return

        // Make sure our clip region is still up to date. It might get out of sync due to the IDE scale changing.
        // (Note that the DefaultCaret class makes this check)
        if (width > 0 && height > 0 && !contains(r)) {
          val clip = g2d.clipBounds
          if (clip != null && !clip.contains(this)) {
            repaint()
          }
          damage(r.getBounds())
        }

        // Make sure not to use the saved bounds! There is no guarantee that damage() has been called first, especially
        // when the caret has not yet been moved or changed
        val fm = component.getFontMetrics(component.getFont())
        if (!hasFocus) {
          val outlineThickness = PaintUtil.alignToInt(1.0, g2d).toFloat()
          val caretWidth = getCaretWidth(fm, r.x, 100, false)
          val area = Area(Double(r.x, r.y, caretWidth, r.height))
          area.subtract(
            Area(
              Double(
                r.x + outlineThickness, r.y + outlineThickness,
                caretWidth - (2 * outlineThickness),
                r.height - (2 * outlineThickness)
              )
            )
          )
          g2d.fill(area)
        } else {
          val caretHeight = getCaretHeight(r.height)
          val caretWidth = getCaretWidth(fm, r.x, thickness, true)
          val rect = Double(r.x, r.y + r.height - caretHeight, caretWidth, caretHeight)
          g2d.fill(rect)
        }
      } finally {
        g2d.dispose()
      }
    }

    /**
     * Updates the bounds of the caret and repaints those bounds.
     *
     *
     * This method is not guaranteed to be called before paint(). The bounds are for use by repaint().
     *
     * @param r The current location of the caret, usually provided by MapToView. The x and y appear to be the upper
     * left of the character position. The height appears to be correct, but the width is not the character
     * width. We also get an int Rectangle, which might not match the float Rectangle we use to draw the caret
     */
    @Synchronized
    override fun damage(r: Rectangle?) {
      if (r != null) {
        // Always set the bounds to the full character grid, so that we are sure we will always erase any old caret.
        // Note that we get an int Rectangle, while we draw with a float Rectangle. The x value is fine as it will
        // round down when converting. The width is rounded up, but should also include any fraction part from x, so we
        // add one.

        val fm = getComponent().getFontMetrics(getComponent().getFont())
        x = r.x
        y = r.y
        width = ceil(getCaretWidth(fm, r.x.toDouble(), 100, false)).toInt() + 1
        height = r.height
        repaint()
      }
    }

    fun modelToView(dot: Int): Rectangle2D? {
      if (dot > getComponent().document.length) {
        return null
      }

      return try {
        getComponent().getUI().modelToView2D(getComponent(), dot, getDotBias())
      } catch (_: BadLocationException) {
        null
      }
    }

    /**
     * RemoveRedundantQualifierName – Kotlin's Double conflicts with double from Rectangle2D
     */
    @Suppress("RemoveRedundantQualifierName")
    fun getCaretWidth(fm: FontMetrics, dotX: kotlin.Double, widthPercentage: Int, coerceCharacterWidth: Boolean): kotlin.Double {
      // Caret width is based on the distance to the next character. This isn't necessarily the same as the character
      // width. E.g. when using float coordinates, the width of a grid is 8.4, while the character width is only 8. This
      // would give us a caret that is not wide enough.
      // We can also try to coerce to the width of the character, rather than using the View's width. This is so that
      // non-printable characters have the same sized caret as printable characters. The only time we use full width
      // with non-printable characters is when drawing the lost-focus caret (a box enclosing the character).
      val width: kotlin.Double
      val r = modelToView(getDot() + 1)
      if (r != null && !coerceCharacterWidth) {
        width = r.x - dotX
      } else {
        var c = ' '
        try {
          if (getDot() < getComponent().document.length) {
            val documentChar = getComponent().getText(getDot(), 1)[0]
            if (isPrintableCharacter(documentChar)) {
              c = documentChar
            }
          }
        } catch (_: BadLocationException) {
          // Ignore
        }
        width = fm.charWidth(c).toDouble()
      }

      return if (mode == GuiCursorType.VER) max(1.0, width * widthPercentage / 100) else width
    }

    @Suppress("RemoveRedundantQualifierName")
    fun getCaretHeight(fullHeight: kotlin.Double): kotlin.Double {
      if (mode == GuiCursorType.HOR) {
        return max(1.0, fullHeight * thickness / 100.0)
      }
      return fullHeight
    }

    override var offset: Int
      get() = getDot()
      set(i) {
        setDot(i)
      }
  }

  @get:TestOnly
  val caretShape: @NonNls String
    get() {
      val caret = caret
      return String.format("%s %d", caret.mode, caret.thickness)
    }

  // We need to store this in a field because we can't trust getCaret(), as it will return an instance of
  // ComposedTextCaret when working with dead keys or input methods
  private val caret: CommandLineCaret = CommandLineCaret()
  var currentActionPromptCharacterOffset: Int = -1

  init {
    caret.setBlinkRate(caret.blinkRate)
    setCaret(caret)
    setNormalModeCaret()

    val defaultStyle = (document as StyledDocument).getStyle(StyleContext.DEFAULT_STYLE)
    StyleConstants.setForeground(defaultStyle, getForeground())

    addCaretListener { _: CaretEvent? -> resetCaret() }
  }

  companion object {
    private val logger = Logger.getInstance(ExTextField::class.java.getName())
    private val supportedTabCompletionCommands = setOf("e", "edit", "w", "write")
  }
}
