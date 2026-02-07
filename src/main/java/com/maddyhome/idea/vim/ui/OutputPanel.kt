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
import com.maddyhome.idea.vim.diagnostic.VimLogger
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
import java.lang.ref.WeakReference
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JRootPane
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.ceil
import kotlin.math.min

/**
 * This panel displays text in a `more` like window and implements [VimOutputPanel].
 */
class OutputPanel(editorRef: WeakReference<Editor>) : JBPanel<OutputPanel?>(), VimOutputPanel {
  private val myEditorRef: WeakReference<Editor> = editorRef
  val editor: Editor? get() = myEditorRef.get()

  val myLabel: JLabel = JLabel("more")
  private val myText = JTextArea()
  private val myScrollPane: JScrollPane =
    JBScrollPane(myText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
  private val myAdapter: ComponentAdapter
  private var myLineHeight = 0

  private var myOldGlass: JComponent? = null
  private var myOldLayout: LayoutManager? = null
  private var myWasOpaque = false

  var myActive: Boolean = false

  val isActive: Boolean
    get() = myActive

  init {
    // Create a text editor for the text and a label for the prompt
    val layout = BorderLayout(0, 0)
    setLayout(layout)
    add(myScrollPane, BorderLayout.CENTER)
    add(myLabel, BorderLayout.SOUTH)

    // Set the text area read only, and support wrap
    myText.isEditable = false
    myText.setLineWrap(true)

    myAdapter = object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        positionPanel()
      }
    }

    // Setup some listeners to handle keystrokes
    val moreKeyListener = MoreKeyListener()
    addKeyListener(moreKeyListener)
    myText.addKeyListener(moreKeyListener)

    // Suppress the fancy frame background used in the Islands theme, which comes from a custom Graphics implementation
    // applied to the IdeRoot, and used to paint all children, including this panel. This client property is checked by
    // JBPanel.getComponentGraphics to give us the original Graphics, opting out of the fancy painting.
    ClientProperty.putRecursive<Boolean?>(this, IdeBackgroundUtil.NO_BACKGROUND, true)

    updateUI()
  }

  // Called automatically when the LAF is changed and the component is visible, and manually by the LAF listener handler
  override fun updateUI() {
    super.updateUI()

    setBorder(ExPanelBorder())

    // Swing uses a bad pattern of calling updateUI() from the constructor. At this moment, all these variables are null
    @Suppress("SENSELESS_COMPARISON")
    if (myText != null && myLabel != null && myScrollPane != null) {
      setFontForElements()
      myText.setBorder(null)
      myScrollPane.setBorder(null)
      myLabel.setForeground(myText.getForeground())

      // Make sure the panel is positioned correctly in case we're changing font size
      positionPanel()
    }
  }

  override var text: String
    get() = myText.text
    set(value) {
      // ExOutputPanel will strip a trailing newline. We'll do it now so that tests have the same behaviour.
      val newValue = value.removeSuffix("\n")
      myText.text = newValue
      val ed = editor
      if (ed != null) {
        myText.setFont(selectEditorFont(ed, newValue))
      }
      myText.setCaretPosition(0)
      if (newValue.isNotEmpty()) {
        activate()
      }
    }

  override var label: String
    get() = myLabel.text ?: ""
    set(value) {
      myLabel.text = value
      val ed = editor
      if (ed != null) {
        myLabel.setFont(selectEditorFont(ed, value))
      }
    }

  override fun addText(text: String, isNewLine: Boolean) {
    if (this.text.isNotEmpty() && isNewLine) {
      this.text += "\n$text"
    } else {
      this.text += text
    }
  }

  override fun setContent(text: String) {
    this.text = text
  }

  override fun clearText() {
    text = ""
  }

  override fun show() {
    editor ?: return
    val currentPanel = injector.outputPanel.getCurrentOutputPanel()
    if (currentPanel != null && currentPanel != this) currentPanel.close()

    if (!myActive) {
      activate()
    }
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
    if (myText == null) {
      // Swing uses a bad pattern of calling getForeground() from the constructor. At this moment, `myText` is null.
      return super.getForeground()
    }
    return myText.getForeground()
  }

  override fun getBackground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (myText == null) {
      // Swing uses a bad pattern of calling getBackground() from the constructor. At this moment, `myText` is null.
      return super.getBackground()
    }
    return myText.getBackground()
  }

  /**
   * Turns off the ex entry field and optionally puts the focus back to the original component
   */
  fun deactivate(refocusOwningEditor: Boolean) {
    if (!myActive) return
    myActive = false
    myText.text = ""
    val ed = editor
    if (refocusOwningEditor && ed != null) {
      requestFocus(ed.contentComponent)
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
   * Turns on the more window for the given editor
   */
  fun activate() {
    val ed = editor ?: return
    val root = SwingUtilities.getRootPane(ed.contentComponent)
    deactivateOldGlass(root)

    setFontForElements()
    positionPanel()

    if (myOldGlass != null) {
      myOldGlass!!.isVisible = true
    }

    myActive = true
    requestFocus(myText)
  }

  private fun deactivateOldGlass(root: JRootPane?) {
    if (root == null) return
    myOldGlass = root.getGlassPane() as JComponent?
    if (myOldGlass != null) {
      myOldLayout = myOldGlass!!.layout
      myWasOpaque = myOldGlass!!.isOpaque
      myOldGlass!!.setLayout(null)
      myOldGlass!!.setOpaque(false)
      myOldGlass!!.add(this)
      myOldGlass!!.addComponentListener(myAdapter)
    }
  }

  private fun setFontForElements() {
    val ed = editor ?: return
    myText.setFont(selectEditorFont(ed, myText.getText()))
    myLabel.setFont(selectEditorFont(ed, myLabel.text))
  }

  override fun scrollLine() {
    scrollOffset(myLineHeight)
  }

  override fun scrollPage() {
    scrollOffset(myScrollPane.getVerticalScrollBar().visibleAmount)
  }

  override fun scrollHalfPage() {
    val sa = myScrollPane.getVerticalScrollBar().visibleAmount / 2.0
    val offset = ceil(sa / myLineHeight) * myLineHeight
    scrollOffset(offset.toInt())
  }

  fun onBadKey() {
    val ed = editor ?: return
    myLabel.setText(injector.messages.message("message.ex.output.more.prompt.full"))
    myLabel.setFont(selectEditorFont(ed, myLabel.text))
  }

  private fun scrollOffset(more: Int) {
    val ed = editor ?: return
    val `val` = myScrollPane.getVerticalScrollBar().value
    myScrollPane.getVerticalScrollBar().setValue(`val` + more)
    myScrollPane.getHorizontalScrollBar().setValue(0)
    if (isAtEnd) {
      myLabel.setText(injector.messages.message("message.ex.output.end.prompt"))
    } else {
      myLabel.setText(injector.messages.message("message.ex.output.more.prompt"))
    }
    myLabel.setFont(selectEditorFont(ed, myLabel.text))
  }

  val isAtEnd: Boolean
    get() {
      val isSingleLine = myText.getLineCount() == 1
      if (isSingleLine) return true
      val scrollBar = myScrollPane.getVerticalScrollBar()
      val value = scrollBar.value
      if (!scrollBar.isVisible) {
        return true
      }
      return value >= scrollBar.maximum - scrollBar.visibleAmount ||
        scrollBar.maximum <= scrollBar.visibleAmount
    }

  private fun positionPanel() {
    val ed = editor ?: return
    val contentComponent = ed.contentComponent
    val scroll = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, contentComponent)
    val rootPane = SwingUtilities.getRootPane(contentComponent)
    if (scroll == null || rootPane == null) {
      // These might be null if we're invoked during component initialisation and before it's been added to the tree
      return
    }

    size = scroll.size

    myLineHeight = myText.getFontMetrics(myText.getFont()).height
    val count: Int = countLines(myText.getText())
    val visLines = size.height / myLineHeight - 1
    val lines = min(count, visLines)
    setSize(
      size.width,
      lines * myLineHeight + myLabel.getPreferredSize().height + border.getBorderInsets(this).top * 2
    )

    val height = size.height
    val bounds = scroll.bounds
    bounds.translate(0, scroll.getHeight() - height)
    bounds.height = height
    val pos = SwingUtilities.convertPoint(scroll.getParent(), bounds.location, rootPane.getGlassPane())
    bounds.location = pos
    setBounds(bounds)

    myScrollPane.getVerticalScrollBar().setValue(0)
    if (!injector.globalOptions().more) {
      // FIX
      scrollOffset(100000)
    } else {
      scrollOffset(0)
    }
  }

  fun close(key: KeyStroke? = null) {
    val ed = editor ?: return
    ApplicationManager.getApplication().invokeLater {
      deactivate(true)
      val project = ed.project
      if (project != null && key != null && key.keyChar != '\n') {
        val keys: MutableList<KeyStroke> = ArrayList(1)
        keys.add(key)
        if (LOG.isTrace()) {
          LOG.trace(
            "Adding new keys to keyStack as part of playback. State before adding keys: " +
              getInstance().keyStack.dump()
          )
        }
        getInstance().keyStack.addKeys(keys)
        val context: ExecutionContext =
          injector.executionContextManager.getEditorExecutionContext(IjVimEditor(ed))
        VimPlugin.getMacro().playbackKeys(IjVimEditor(ed), context, 1)
      }
    }
  }

  override fun close() {
    close(null)
  }

  private class MoreKeyListener : KeyAdapter() {
    /**
     * Invoked when a key has been pressed.
     */
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

      // This listener is only invoked for local scenarios, and we only need to update local editor UI. This will invoke
      // updateUI on the output pane and it's child components
      for (vimEditor in injector.editorGroup.getEditors()) {
        val editor = (vimEditor as IjVimEditor).editor
        if (!isPanelActive(editor)) continue
        IJSwingUtilities.updateComponentTreeUI(getInstance(editor))
      }
    }
  }

  companion object {
    private val LOG: VimLogger = injector.getLogger<OutputPanel>(OutputPanel::class.java)

    fun getNullablePanel(editor: Editor): OutputPanel? {
      return editor.vimMorePanel
    }

    fun isPanelActive(editor: Editor): Boolean {
      return getNullablePanel(editor)?.myActive ?: false
    }

    fun getInstance(editor: Editor): OutputPanel {
      var panel: OutputPanel? = getNullablePanel(editor)
      if (panel == null) {
        panel = OutputPanel(WeakReference(editor))
        editor.vimMorePanel = panel
      }
      return panel
    }

    private fun countLines(text: String): Int {
      if (text.isEmpty()) {
        return 0
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
  }
}
