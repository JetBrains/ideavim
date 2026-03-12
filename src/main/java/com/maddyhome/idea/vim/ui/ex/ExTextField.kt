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
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimCommandLineCaret
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineStringHelper.isPrintableCharacter
import com.maddyhome.idea.vim.helper.selectEditorFont
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext
import com.maddyhome.idea.vim.options.helpers.GuiCursorAttributes
import com.maddyhome.idea.vim.options.helpers.GuiCursorMode
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
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
import java.util.stream.Stream
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
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
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

  fun insertText(offset: Int, newText: String?) {
    try {
      // Note that ExDocument.insertString handles overwriting, but not replace mode!
      document.insertString(offset, newText, null)
    } catch (e: BadLocationException) {
      logger.error(e)
    }
    saveLastEntry()
    setFontToJField(text)
  }

  fun deleteText(offset: Int, length: Int) {
    try {
      document.remove(offset, min(length, document.length - offset))
    } catch (e: BadLocationException) {
      logger.error(e)
    }
    saveLastEntry()
    setFontToJField(text)
  }

  // VIM-570
  private fun setFontToJField(stringToDisplay: String) {
    font = selectEditorFont(myParentPanel.ijEditor, stringToDisplay)
  }

  override fun setFont(f: Font?) {
    super.setFont(f)
    val document = this.document
    if (document is StyledDocument) {
      val defaultStyle = document.getStyle(StyleContext.DEFAULT_STYLE)
      if (StyleConstants.getFontFamily(defaultStyle) != font.family) {
        StyleConstants.setFontFamily(defaultStyle, font.family)
      }
      if (StyleConstants.getFontSize(defaultStyle) != font.size) {
        StyleConstants.setFontSize(defaultStyle, font.size)
      }
    }
  }

  override fun setForeground(fg: Color?) {
    super.setForeground(fg)
    val document = this.document
    if (document is StyledDocument) {
      val defaultStyle = document.getStyle(StyleContext.DEFAULT_STYLE)
      StyleConstants.setForeground(defaultStyle, fg)
    }
  }

  fun setSpecialKeyForeground(fg: Color) {
    (document as? ExDocument)?.setSpecialKeyForeground(fg)
  }

  private var commandLoopIterator: LoopIterator? = null

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
      commandLoopIterator = null
    }
    else {
      if (stroke.keyCode != KeyEvent.VK_TAB || !executeTabCompletionIfPossible()) {
        commandLoopIterator = null
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
      val keyHandler = KeyHandler.getInstance()
      val keyStroke = KeyStroke.getKeyStrokeForEvent(e)
      keyHandler.handleKey(
        editor, keyStroke, IjEditorExecutionContext(myParentPanel.context!!),
        keyHandler.keyHandlerState
      )
      e.consume()
    } else {
      commandLoopIterator = null
      super.processKeyEvent(e)
    }
  }

  private fun executeTabCompletionIfPossible(): Boolean {
    if (null == commandLoopIterator || commandLoopIterator!!.size() == 1) {
      val parts = getText().split(" ", limit = 2).takeIf { it.size > 1 } ?: return false
      // If more commands require TAB action handling, a more sophisticated check and delegation is needed
      val command = parts[0].takeIf { supportedTabCompletionCommands.contains(it) } ?: return false
      val input = parts[1]

      val project = (parent as ExEntryPanel).context?.getData<Project>(DataKey.create("project"))
      val projectBasePath = project?.basePath?.let { Path.of(it) } ?: return false
      val inputPath = projectBasePath.resolve(input)
      val searchPrefix = inputPath.pathString
      val basePrefix = searchPrefix + if((input.isBlank() || input.endsWith("/")) && !searchPrefix.endsWith("/")) "/" else ""

      // Would it be useful to ignore file path case?
      try {
        val root =
          (if (input.endsWith("/")) inputPath
          else if (inputPath == projectBasePath) projectBasePath
          else inputPath.parent) ?: return false

        val specialPaths =
          if(input.endsWith(".."))
            Stream.of("/")
          else if(input.endsWith("."))
            Stream.of("./","/")
          else Stream.empty()

        val commands = Stream.concat(specialPaths, (root.let { Files.list(it) }
          .filter { it.pathString.startsWith(searchPrefix) }
          .sorted()
          .map { if (it.isDirectory()) "$it/" else it.pathString }))
          .map { input + it.removePrefix(basePrefix) }
          .map { String.format("%s %s", command, it) }
          .toList().takeIf { it.isNotEmpty() } ?: return false

        commandLoopIterator = LoopIterator(commands)
      } catch (e: IOException) {
        logger.error(e)
        commandLoopIterator = null
      }
    }
    return commandLoopIterator?.let { updateText(it.next()); true } ?: false
  }

  private class LoopIterator(val commands: List<String>) : Iterator<String> {
    private var currentIdx = 0
    override fun next(): String {
      currentIdx = if (currentIdx >= commands.size - 1) 0 else currentIdx + 1
      return commands[currentIdx]
    }
    fun size(): Int = commands.size
    override fun hasNext(): Boolean = true
  }

  private fun isAllowedTypedEvent(event: KeyEvent): Boolean {
    return event.id == KeyEvent.KEY_TYPED && !isKeyCharEnterOrEscape(event.keyChar)
  }

  private fun isAllowedPressedEvent(event: KeyEvent): Boolean {
    return event.id == KeyEvent.KEY_PRESSED && isKeyCodeEnterOrEscape(event.keyCode)
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
    caret.isVisible = false
    resetCaret()
    caret.isVisible = true
  }

  private fun resetCaret() {
    if (caretPosition == text.length) {
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
    commandLineCaret.setAttributes(GuiCursorOptionHelper.getAttributes(GuiCursorMode.CMD_LINE))
  }

  private fun setInsertModeCaret() {
    commandLineCaret.setAttributes(GuiCursorOptionHelper.getAttributes(GuiCursorMode.CMD_LINE_INSERT))
  }

  private fun setReplaceModeCaret() {
    commandLineCaret.setAttributes(GuiCursorOptionHelper.getAttributes(GuiCursorMode.CMD_LINE_REPLACE))
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
        blinkRate = lastBlinkRate
        lastBlinkRate = 0
      }
      super.focusGained(e)
      repaint()
      hasFocus = true
    }

    override fun focusLost(e: FocusEvent?) {
      // We don't call super.focusLost, which would hide the caret
      hasFocus = false
      lastBlinkRate = blinkRate
      blinkRate = 0
      // Make sure the box caret is visible. If we're flashing, this might be false
      isVisible = true
      repaint()
    }

    override fun paint(g: Graphics) {
      if (!isVisible) return

      // Take a copy of the graphics, so we can mess around with it without having to reset after
      val g2d = g.create() as Graphics2D
      try {
        g2d.color = component.background
        g2d.setXORMode(component.caretColor)

        // Convert the caret(/model) offset to a view location. Note that this can return a zero width. AFAICT, the API
        // is intended to return the *location* of the document offset in the view, and not its bounds. The width
        // appears to be optional.
        val r = modelToView(dot) ?: return

        // If the new caret location is not contained by the current bounds of the caret, redraw.
        // Note that we can't use `this.contains(r)` as it will return false if `r.width` or `r.height` is zero. And
        // since `modelToView` can return a zero width, we would continually force a repaint, repeatedly calling `paint`
        // and causing a high CPU busy loop.
        if (width > 0 && height > 0 && !safeContains(r)) {
          // If the current clip region does not contain the current (old) caret bounds, force a repaint of the whole
          // component to remove it.
          val clip = g2d.clipBounds
          if (clip != null && !clip.contains(this)) {
            repaint()
          }

          // Update the caret bounds to the new location and redraw the component
          damage(r.bounds)
        }

        val fm = component.getFontMetrics(component.font)
        val caretWidth = getCaretWidth(fm, r.x, thickness, hasFocus)

        // Paint the caret. Make sure not to use the saved bounds! There is no guarantee that damage() has been called
        // first, especially when the caret has not yet been moved or changed.
        if (!hasFocus) {
          val outlineThickness = PaintUtil.alignToInt(1.0, g2d).toFloat()
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
          val rect = Double(r.x, r.y + r.height - caretHeight, caretWidth, caretHeight)
          g2d.fill(rect)
        }

        // Make sure the caret's width and height are not zero. This is important for redrawing the caret when flashing.
        // If the bounds are zero, nothing is drawn. It's unclear who should set the initial width and height. It's
        // updated when calling damage, but nothing sets the initial bounds
        if (width == 0 || height == 0) {
          width = caretWidth.toInt()
          height = r.bounds.height
        }
      } finally {
        g2d.dispose()
      }
    }

    /**
     * Check if the given rectangle is contained within the current caret bounds
     *
     * This differs to [contains] in that it does not return false if the rectangle's width or height is zero.
     */
    private fun safeContains(r: Rectangle2D) =
      x.toDouble() in r.x..r.x + r.width && y.toDouble() in r.y..r.y + r.height

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

        val fm = component.getFontMetrics(component.font)
        x = r.x
        y = r.y
        width = ceil(getCaretWidth(fm, r.x.toDouble(), 100, false)).toInt() + 1
        height = r.height
        repaint()
      }
    }

    private fun modelToView(dot: Int): Rectangle2D? {
      if (dot > component.document.length) {
        return null
      }

      return try {
        component.ui.modelToView2D(component, dot, dotBias)
      } catch (_: BadLocationException) {
        null
      }
    }

    /**
     * Get the width of the caret as a percentage of the current character width
     *
     * When [coerceCharacterWidth] is false, the caret width is based on the distance to the next character. This is
     * the width of the rendered character and mostly used when we don't have focus so that the outline caret is drawn
     * around the whole rendered character.
     *
     * When [coerceCharacterWidth] is true, the caret width will be the same as the character width. If the caret is a
     * non-printable character, the default character width is used, which prevents the caret stretching to the full
     * width of the rendered character representation (e.g., `^M`)
     */
    @Suppress("RemoveRedundantQualifierName")
    private fun getCaretWidth(fm: FontMetrics, dotX: kotlin.Double, widthPercentage: Int, coerceCharacterWidth: Boolean): kotlin.Double {
      val width: kotlin.Double
      val r = modelToView(dot + 1)
      if (r != null && !coerceCharacterWidth) {
        width = r.x - dotX
      } else {
        var c = ' '
        try {
          if (dot < component.document.length) {
            val documentChar = component.getText(dot, 1)[0]
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
    private fun getCaretHeight(fullHeight: kotlin.Double): kotlin.Double {
      if (mode == GuiCursorType.HOR) {
        return max(1.0, fullHeight * thickness / 100.0)
      }
      return fullHeight
    }

    override var offset: Int
      get() = dot
      set(i) {
        dot = i
      }
  }

  @get:TestOnly
  val caretShape: @NonNls String
    get() = String.format("%s %d", commandLineCaret.mode, commandLineCaret.thickness)

  // We need to store this in a field because we can't trust getCaret(), as it will return an instance of
  // ComposedTextCaret when working with dead keys or input methods
  private val commandLineCaret: CommandLineCaret = CommandLineCaret()

  init {
    commandLineCaret.blinkRate = caret.blinkRate
    caret = commandLineCaret
    setNormalModeCaret()

    val defaultStyle = (document as StyledDocument).getStyle(StyleContext.DEFAULT_STYLE)
    StyleConstants.setForeground(defaultStyle, foreground)

    addCaretListener { _: CaretEvent? -> resetCaret() }
  }

  companion object {
    private val logger = Logger.getInstance(ExTextField::class.java.name)
    private val supportedTabCompletionCommands = setOf("e", "edit", "w", "write")
  }
}
