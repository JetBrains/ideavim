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
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import kotlin.math.ceil
import kotlin.math.min


/**
 * Panel that displays text in a `more` like window overlaid on the editor.
 */
class OutputPanel private constructor(
  private val myEditor: Editor,
) : JBPanel<OutputPanel>() {

  private val myText = JTextPane()
  private val myAdapter: ComponentAdapter
  private var myDefaultForeground: Color? = null

  private var myOldGlass: JComponent? = null
  private var myOldLayout: LayoutManager? = null
  private var myWasOpaque = false

  var myActive: Boolean = false

  val myLabel: JLabel = JLabel("more")
  private val myScrollPane: JScrollPane =
    JBScrollPane(myText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
  private var myLineHeight = 0
  private var isSimpleOutput = false

  init {
    myText.isEditable = false

    myAdapter = object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        positionPanel()
      }
    }

    // Suppress the fancy frame background used in the Islands theme
    ClientProperty.putRecursive(this, IdeBackgroundUtil.NO_BACKGROUND, true)

    // Initialize panel
    setLayout(BorderLayout(0, 0))
    add(myScrollPane, BorderLayout.CENTER)
    add(myLabel, BorderLayout.SOUTH)

    val keyListener = OutputPanelKeyListener()
    addKeyListener(keyListener)
    myText.addKeyListener(keyListener)

    updateUI()
  }

  override fun updateUI() {
    super.updateUI()

    setBorder(ExPanelBorder())

    @Suppress("SENSELESS_COMPARISON")
    if (myText != null && myLabel != null && myScrollPane != null) {
      setFontForElements()
      myText.setBorder(null)
      myScrollPane.setBorder(null)
      myLabel.setForeground(myText.getForeground())
      positionPanel()
    }
  }

  var text: String?
    get() = myText.getText()
    set(data) {
      var data: String = data!!
      if (data.isNotEmpty() && data[data.length - 1] == '\n') {
        data = data.dropLast(1)
      }

      myText.text = data
      myText.setFont(selectEditorFont(myEditor, data))
      myText.setCaretPosition(0)
      if (data.isNotEmpty()) {
        activate()
      }
    }

  /**
   * Sets styled text with multiple segments, each potentially having a different color.
   */
  fun setStyledText(lines: List<TextLine>) {
    val doc = myText.styledDocument
    doc.remove(0, doc.length)

    if (myDefaultForeground == null) {
      myDefaultForeground = myText.foreground
    }

    if (lines.size > 1) {
      setMultiLineText(lines, doc)
    } else {
      doc.insertString(doc.length, lines[0].text, getLineColor(lines[0]))
    }

    val fullText = doc.getText(0, doc.length)
    myText.setFont(selectEditorFont(myEditor, fullText))
    myText.setCaretPosition(0)
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
    val color = segment.color ?: myDefaultForeground
    if (color != null) {
      StyleConstants.setForeground(attrs, color)
    }
    return attrs
  }

  override fun getForeground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (myText == null) {
      return super.getForeground()
    }
    return myText.getForeground()
  }

  override fun getBackground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (myText == null) {
      return super.getBackground()
    }
    return myText.getBackground()
  }

  /**
   * Turns off the output panel and optionally puts the focus back to the original component.
   */
  fun deactivate(refocusOwningEditor: Boolean) {
    if (!myActive) return
    myActive = false
    myText.text = ""
    if (refocusOwningEditor) {
      requestFocus(myEditor.contentComponent)
    }
    if (myOldGlass != null) {
      myOldGlass!!.removeComponentListener(myAdapter)
      myOldGlass!!.isVisible = false
      myOldGlass!!.remove(this)
      myOldGlass!!.setOpaque(myWasOpaque)
      myOldGlass!!.setLayout(myOldLayout)
    }
  }

  /**
   * Turns on the output panel for the given editor.
   */
  fun activate() {
    disableOldGlass()

    setFontForElements()
    positionPanel()

    if (myOldGlass != null) {
      myOldGlass!!.isVisible = true
    }

    myActive = true
    requestFocus(myText)
  }

  private fun disableOldGlass() {
    val root = SwingUtilities.getRootPane(myEditor.contentComponent) ?: return
    myOldGlass = root.getGlassPane() as JComponent?
    if (myOldGlass == null) {
      return
    }
    myOldLayout = myOldGlass!!.layout
    myWasOpaque = myOldGlass!!.isOpaque
    myOldGlass!!.setLayout(null)
    myOldGlass!!.setOpaque(false)
    myOldGlass!!.add(this)
    myOldGlass!!.addComponentListener(myAdapter)
  }

  @JvmOverloads
  fun close(key: KeyStroke? = null) {
    ApplicationManager.getApplication().invokeLater {
      deactivate(true)
      val project = myEditor.project
      if (project != null && key != null && key.keyChar != '\n') {
        val keys: MutableList<KeyStroke> = ArrayList(1)
        keys.add(key)
        getInstance().keyStack.addKeys(keys)
        val context: ExecutionContext =
          injector.executionContextManager.getEditorExecutionContext(IjVimEditor(myEditor))
        VimPlugin.getMacro().playbackKeys(IjVimEditor(myEditor), context, 1)
      }
    }
  }

  private fun setFontForElements() {
    myText.setFont(selectEditorFont(myEditor, myText.getText()))
    myLabel.setFont(selectEditorFont(myEditor, myLabel.text))
  }

  private fun positionPanel() {
    val scroll = positionPanelStart() ?: return
    val lineHeight = myText.getFontMetrics(myText.getFont()).height
    val count = countLines(myText.getText())
    val visLines = size.height / lineHeight - 1
    val lines = min(count, visLines)

    // Simple output: single line that fits entirely - no label needed
    isSimpleOutput = count == 1 && count <= visLines
    myLabel.isVisible = !isSimpleOutput

    val extraHeight = if (isSimpleOutput) 0 else myLabel.getPreferredSize().height
    setSize(
      size.width,
      lines * lineHeight + extraHeight + border.getBorderInsets(this).top * 2
    )

    finishPositioning(scroll)

    // onPositioned
    myLineHeight = lineHeight
    myScrollPane.getVerticalScrollBar().setValue(0)
    if (!isSimpleOutput) {
      if (!injector.globalOptions().more) {
        scrollOffset(100000)
      } else {
        scrollOffset(0)
      }
    }
  }

  private fun positionPanelStart(): JScrollPane? {
    val contentComponent = myEditor.contentComponent
    val scroll = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, contentComponent) as? JScrollPane
    val rootPane = SwingUtilities.getRootPane(contentComponent)
    if (scroll == null || rootPane == null) {
      return null
    }

    size = scroll.size
    return scroll
  }

  private fun finishPositioning(scroll: JScrollPane) {
    val rootPane = SwingUtilities.getRootPane(myEditor.contentComponent)
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

  fun scrollLine() {
    scrollOffset(myLineHeight)
  }

  fun scrollPage() {
    scrollOffset(myScrollPane.getVerticalScrollBar().visibleAmount)
  }

  fun scrollHalfPage() {
    val sa = myScrollPane.getVerticalScrollBar().visibleAmount / 2.0
    val offset = ceil(sa / myLineHeight) * myLineHeight
    scrollOffset(offset.toInt())
  }

  fun onBadKey() {
    myLabel.setText(injector.messages.message("message.ex.output.more.prompt.full"))
    myLabel.setFont(selectEditorFont(myEditor, myLabel.text))
  }

  private fun scrollOffset(more: Int) {
    myScrollPane.validate()
    val scrollBar = myScrollPane.getVerticalScrollBar()
    val value = scrollBar.value
    scrollBar.setValue(value + more)
    myScrollPane.getHorizontalScrollBar().setValue(0)

    // Check if we're at the end or if content fits entirely (nothing to scroll)
    if (isAtEnd) {
      myLabel.setText(injector.messages.message("message.ex.output.end.prompt"))
    } else {
      myLabel.setText(injector.messages.message("message.ex.output.more.prompt"))
    }
    myLabel.setFont(selectEditorFont(myEditor, myLabel.text))
  }

  val isAtEnd: Boolean
    get() {
      if (isSimpleOutput) return true
      val scrollBar = myScrollPane.getVerticalScrollBar()
      val value = scrollBar.value
      return !scrollBar.isVisible || value >= scrollBar.maximum - scrollBar.visibleAmount ||
        scrollBar.maximum <= scrollBar.visibleAmount
    }

  private class OutputPanelKeyListener : KeyAdapter() {
    override fun keyTyped(e: KeyEvent) {
      val currentPanel: VimOutputPanel = injector.outputPanel.getCurrentOutputPanel() ?: return

      val keyCode = e.getKeyCode()
      val keyChar = e.getKeyChar()
      val modifiers = e.modifiersEx
      val keyStroke = if (keyChar == KeyEvent.CHAR_UNDEFINED)
        KeyStroke.getKeyStroke(keyCode, modifiers)
      else
        KeyStroke.getKeyStroke(keyChar, modifiers)
      currentPanel.handleKey(keyStroke)
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
