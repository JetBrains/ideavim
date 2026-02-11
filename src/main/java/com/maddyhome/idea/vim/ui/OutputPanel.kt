/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.intellij.ui.ClientProperty
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.IJSwingUtilities
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimOutputPanel
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.requestFocus
import com.maddyhome.idea.vim.helper.selectEditorFont
import com.maddyhome.idea.vim.helper.vimMorePanel
import com.maddyhome.idea.vim.newapi.IjVimEditor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.LayoutManager
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.text.DefaultCaret
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import kotlin.math.ceil
import kotlin.math.min


/**
 * Panel that displays text in a `more` like window overlaid on the editor.
 */
class OutputPanel private constructor(
  private val editor: Editor,
) : JBPanel<OutputPanel>(), VimOutputPanel {

  private val textPane = JTextPane()
  private val resizeAdapter: ComponentAdapter
  private var defaultForeground: Color? = null

  private var glassPane: JComponent? = null
  private var originalLayout: LayoutManager? = null
  private var wasOpaque = false

  private var active: Boolean = false
  private val segments = mutableListOf<TextLine>()

  private val labelComponent: JLabel = JLabel("more")
  private val scrollPane: JScrollPane =
    JBScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
  private var cachedLineHeight = 0
  private var isSingleLine = false

  init {
    textPane.isEditable = false
    textPane.caret = object : DefaultCaret() {
      override fun setVisible(v: Boolean) {
        super.setVisible(false)
      }
    }
    textPane.highlighter = null

    resizeAdapter = object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        positionPanel()
      }
    }

    // Suppress the fancy frame background used in the Islands theme
    ClientProperty.putRecursive(this, IdeBackgroundUtil.NO_BACKGROUND, true)

    // Initialize panel
    setLayout(BorderLayout(0, 0))
    add(scrollPane, BorderLayout.CENTER)
    add(labelComponent, BorderLayout.SOUTH)

    val keyListener = OutputPanelKeyListener()
    addKeyListener(keyListener)
    textPane.addKeyListener(keyListener)

    updateUI()
  }

  override fun updateUI() {
    super.updateUI()

    setBorder(ExPanelBorder())

    @Suppress("SENSELESS_COMPARISON")
    if (textPane != null && labelComponent != null && scrollPane != null) {
      setFontForElements()
      textPane.setBorder(null)
      scrollPane.setBorder(null)
      labelComponent.setForeground(textPane.getForeground())
      positionPanel()
    }
  }

  override val isActive: Boolean
    get() = active

  override var text: String
    get() = textPane.getText() ?: ""
    set(value) {
      val newValue = value.removeSuffix("\n")
      segments.clear()
      if (newValue.isEmpty()) return
      segments.add(TextLine(newValue, null))
    }

  override var label: String
    get() = labelComponent.text
    set(value) {
      labelComponent.text = value
    }

  /**
   * Sets styled text with multiple segments, each potentially having a different color.
   */
  fun setStyledText(lines: List<TextLine>) {
    val doc = textPane.styledDocument
    doc.remove(0, doc.length)

    if (defaultForeground == null) {
      defaultForeground = textPane.foreground
    }

    if (lines.size > 1) {
      setMultiLineText(lines, doc)
    } else {
      doc.insertString(doc.length, lines[0].text.removeSuffix("\n"), getLineColor(lines[0]))
    }

    val fullText = doc.getText(0, doc.length)
    textPane.setFont(selectEditorFont(editor, fullText))
    textPane.setCaretPosition(0)
    if (fullText.isNotEmpty()) {
      activate()
    }
  }

  private fun setMultiLineText(
    lines: List<TextLine>,
    doc: StyledDocument,
  ) {
    for ((index, line) in lines.withIndex()) {
      val text = line.text.removeSuffix("\n")
      val attrs = getLineColor(line)
      val separator = if (index < lines.size - 1) "\n" else ""
      doc.insertString(doc.length, text + separator, attrs)
    }
  }

  private fun getLineColor(segment: TextLine): SimpleAttributeSet {
    val attrs = SimpleAttributeSet()
    val color = segment.color ?: defaultForeground
    if (color != null) {
      StyleConstants.setForeground(attrs, color)
    }
    return attrs
  }

  override fun addText(text: String, isNewLine: Boolean, color: Color?) {
    segments.add(TextLine(text, color))
  }

  override fun show() {
    val currentPanel = injector.outputPanel.getCurrentOutputPanel()
    if (currentPanel != null && currentPanel != this) currentPanel.close()

    setStyledText(segments)
    if (!active) {
      activate()
    }
  }

  override fun setContent(text: String) {
    this.text = text
  }

  override fun clearText() {
    segments.clear()
  }

  fun clear() {
    text = ""
  }

  override fun handleKey(key: KeyStroke) {
    if (isAtEnd) {
      close(key)
      return
    }

    when (key.keyChar) {
      ' ' -> scrollPage()
      'd' -> scrollHalfPage()
      'q', '\u001b' -> close()
      '\n' -> scrollLine()
      KeyEvent.CHAR_UNDEFINED -> {
        when (key.keyCode) {
          KeyEvent.VK_ENTER -> scrollLine()
          KeyEvent.VK_ESCAPE -> close()
          else -> onBadKey()
        }
      }

      else -> onBadKey()
    }
  }

  override fun getForeground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (textPane == null) {
      return super.getForeground()
    }
    return textPane.getForeground()
  }

  override fun getBackground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (textPane == null) {
      return super.getBackground()
    }
    return textPane.getBackground()
  }

  /**
   * Turns off the output panel and optionally puts the focus back to the original component.
   */
  fun deactivate(refocusOwningEditor: Boolean) {
    if (!active) return
    active = false
    clearText()
    textPane.text = ""
    if (refocusOwningEditor) {
      requestFocus(editor.contentComponent)
    }
    if (glassPane != null) {
      glassPane!!.removeComponentListener(resizeAdapter)
      glassPane!!.isVisible = false
      glassPane!!.remove(this)
      glassPane!!.setOpaque(wasOpaque)
      glassPane!!.setLayout(originalLayout)
    }
  }

  /**
   * Turns on the output panel for the given editor.
   */
  fun activate() {
    disableOldGlass()

    setFontForElements()
    positionPanel()

    if (glassPane != null) {
      glassPane!!.isVisible = true
    }

    active = true
    requestFocus(textPane)
  }

  private fun disableOldGlass() {
    val root = SwingUtilities.getRootPane(editor.contentComponent) ?: return
    glassPane = root.getGlassPane() as JComponent?
    if (glassPane == null) {
      return
    }
    originalLayout = glassPane!!.layout
    wasOpaque = glassPane!!.isOpaque
    glassPane!!.setLayout(null)
    glassPane!!.setOpaque(false)
    glassPane!!.add(this)
    glassPane!!.addComponentListener(resizeAdapter)
  }

  override fun close() {
    close(null)
  }

  fun close(key: KeyStroke?) {
    val passKeyBack = isSingleLine
    ApplicationManager.getApplication().invokeLater {
      deactivate(true)
      val project = editor.project
      // For single line messages, pass any key back to the editor (including Enter)
      // For multi-line messages, don't pass Enter back (it was used to dismiss)
      if (project != null && key != null && (passKeyBack || key.keyChar != '\n')) {
        val keys: MutableList<KeyStroke> = ArrayList(1)
        keys.add(key)
        getInstance().keyStack.addKeys(keys)
        val context: ExecutionContext =
          injector.executionContextManager.getEditorExecutionContext(IjVimEditor(editor))
        VimPlugin.getMacro().playbackKeys(IjVimEditor(editor), context, 1)
      }
    }
  }

  private fun setFontForElements() {
    textPane.setFont(selectEditorFont(editor, textPane.getText()))
    labelComponent.setFont(selectEditorFont(editor, labelComponent.text))
  }

  private fun positionPanel() {
    val scroll = positionPanelStart() ?: return
    val lineHeight = textPane.getFontMetrics(textPane.getFont()).height
    val count = countLines(textPane.getText())
    val visLines = size.height / lineHeight - 1
    val lines = min(count, visLines)

    // Simple output: single line that fits entirely - no label needed
    isSingleLine = count == 1 && count <= visLines
    labelComponent.isVisible = !isSingleLine

    val extraHeight = if (isSingleLine) 0 else labelComponent.getPreferredSize().height
    setSize(
      size.width,
      lines * lineHeight + extraHeight + border.getBorderInsets(this).top * 2
    )

    finishPositioning(scroll)

    // Force layout so that viewport sizes are valid before checking scroll state
    validate()

    // onPositioned
    cachedLineHeight = lineHeight
    scrollPane.getVerticalScrollBar().setValue(0)
    if (!isSingleLine) {
      if (!injector.globalOptions().more) {
        scrollOffset(100000)
      } else {
        scrollOffset(0)
      }
    }
  }

  private fun positionPanelStart(): JScrollPane? {
    val contentComponent = editor.contentComponent
    val scroll = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, contentComponent) as? JScrollPane
    val rootPane = SwingUtilities.getRootPane(contentComponent)
    if (scroll == null || rootPane == null) {
      return null
    }

    size = scroll.size
    return scroll
  }

  private fun finishPositioning(scroll: JScrollPane) {
    val rootPane = SwingUtilities.getRootPane(editor.contentComponent)
    val bounds = scroll.bounds
    bounds.translate(0, scroll.getHeight() - size.height)
    bounds.height = size.height
    val pos = SwingUtilities.convertPoint(scroll.getParent(), bounds.location, rootPane.getGlassPane())
    bounds.location = pos
    setBounds(bounds)
  }

  private fun countLines(text: String): Int {
    if (text.isEmpty()) {
      return 1
    }

    var count = 0
    var pos = -1
    while ((text.indexOf('\n', pos + 1).also { pos = it }) != -1) {
      count++
    }

    if (text[text.length - 1] != '\n') {
      count++
    }

    return count
  }

  override fun scrollLine() {
    scrollOffset(cachedLineHeight)
  }

  override fun scrollPage() {
    scrollOffset(scrollPane.getVerticalScrollBar().visibleAmount)
  }

  override fun scrollHalfPage() {
    val sa = scrollPane.getVerticalScrollBar().visibleAmount / 2.0
    val offset = ceil(sa / cachedLineHeight) * cachedLineHeight
    scrollOffset(offset.toInt())
  }

  fun onBadKey() {
    labelComponent.setText(injector.messages.message("message.ex.output.more.prompt.full"))
    labelComponent.setFont(selectEditorFont(editor, labelComponent.text))
  }

  private fun scrollOffset(more: Int) {
    scrollPane.validate()
    val scrollBar = scrollPane.getVerticalScrollBar()
    val value = scrollBar.value
    scrollBar.setValue(value + more)
    scrollPane.getHorizontalScrollBar().setValue(0)

    // Check if we're at the end or if content fits entirely (nothing to scroll)
    if (isAtEnd) {
      labelComponent.setText(injector.messages.message("message.ex.output.end.prompt"))
    } else {
      labelComponent.setText(injector.messages.message("message.ex.output.more.prompt"))
    }
    labelComponent.setFont(selectEditorFont(editor, labelComponent.text))
  }

  val isAtEnd: Boolean
    get() {
      if (isSingleLine) return true
      val contentHeight = textPane.preferredSize.height
      val viewportHeight = scrollPane.viewport.height
      if (contentHeight <= viewportHeight) return true
      val scrollBar = scrollPane.getVerticalScrollBar()
      return scrollBar.value >= scrollBar.maximum - scrollBar.visibleAmount
    }

  private inner class OutputPanelKeyListener : KeyAdapter() {
    override fun keyTyped(e: KeyEvent) {
      val currentPanel: VimOutputPanel = injector.outputPanel.getCurrentOutputPanel() ?: return

      val keyChar = e.keyChar
      val modifiers = e.modifiersEx
      val keyStroke = KeyStroke.getKeyStroke(keyChar, modifiers)
      currentPanel.handleKey(keyStroke)
    }

    override fun keyPressed(e: KeyEvent) {
      if (!e.isActionKey && e.keyCode != KeyEvent.VK_ENTER) return
      val currentPanel = injector.outputPanel.getCurrentOutputPanel() as? OutputPanel ?: return

      val keyCode = e.keyCode
      val modifiers = e.modifiersEx
      val keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers)

      if (isSingleLine) {
        currentPanel.close(keyStroke)
        e.consume()
        return
      }

      // Multi-line mode: arrow keys scroll, down/right at end closes
      when (keyCode) {
        KeyEvent.VK_ENTER -> {
          if (currentPanel.isAtEnd) currentPanel.close() else currentPanel.scrollLine()
          e.consume()
        }
        KeyEvent.VK_DOWN -> if (currentPanel.isAtEnd) currentPanel.close(keyStroke) else currentPanel.scrollLine()
        KeyEvent.VK_RIGHT -> if (currentPanel.isAtEnd) currentPanel.close(keyStroke) else currentPanel.scrollLine()
        KeyEvent.VK_UP -> currentPanel.scrollOffset(-cachedLineHeight)
        KeyEvent.VK_LEFT -> currentPanel.scrollOffset(-cachedLineHeight)
        KeyEvent.VK_PAGE_DOWN -> if (currentPanel.isAtEnd) currentPanel.close(keyStroke) else currentPanel.scrollPage()
        KeyEvent.VK_PAGE_UP -> currentPanel.scrollOffset(-scrollPane.verticalScrollBar.visibleAmount)
      }
    }
  }

  class LafListener : LafManagerListener {
    override fun lookAndFeelChanged(source: LafManager) {
      if (VimPlugin.isNotEnabled()) return

      for (vimEditor in injector.editorGroup.getEditors()) {
        val editor = (vimEditor as IjVimEditor).editor
        if (!isPanelActive(editor)) continue
        IJSwingUtilities.updateComponentTreeUI(getInstance(editor))
      }
    }
  }

  companion object {
    fun getNullablePanel(editor: Editor): OutputPanel? {
      return editor.vimMorePanel
    }

    fun isPanelActive(editor: Editor): Boolean {
      return getNullablePanel(editor) != null
    }

    fun getInstance(editor: Editor): OutputPanel {
      var panel: OutputPanel? = getNullablePanel(editor)
      if (panel == null) {
        panel = OutputPanel(editor)
        editor.vimMorePanel = panel
      }
      return panel
    }
  }
}


data class TextLine(val text: String, val color: Color?)
