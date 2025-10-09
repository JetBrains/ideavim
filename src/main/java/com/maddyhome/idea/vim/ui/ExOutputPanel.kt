/*
 * Copyright 2003-2023 The IdeaVim authors
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
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.ceil
import kotlin.math.min

/**
 * This panel displays text in a `more` like window.
 */
class ExOutputPanel private constructor(private val myEditor: Editor) : JBPanel<ExOutputPanel?>() {
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

  var text: String?
    get() = myText.getText()
    set(data) {
      var data: String = data!!
      if (!data.isEmpty() && data[data.length - 1] == '\n') {
        data = data.dropLast(1)
      }

      myText.text = data
      myText.setFont(selectEditorFont(myEditor, data))
      myText.setCaretPosition(0)
      if (!data.isEmpty()) {
        activate()
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
   * Turns on the more window for the given editor
   */
  fun activate() {
    val root = SwingUtilities.getRootPane(myEditor.contentComponent)
    myOldGlass = root.getGlassPane() as JComponent?
    if (myOldGlass != null) {
      myOldLayout = myOldGlass!!.layout
      myWasOpaque = myOldGlass!!.isOpaque
      myOldGlass!!.setLayout(null)
      myOldGlass!!.setOpaque(false)
      myOldGlass!!.add(this)
      myOldGlass!!.addComponentListener(myAdapter)
    }

    setFontForElements()
    positionPanel()

    if (myOldGlass != null) {
      myOldGlass!!.isVisible = true
    }

    myActive = true
    requestFocus(myText)
  }

  private fun setFontForElements() {
    myText.setFont(selectEditorFont(myEditor, myText.getText()))
    myLabel.setFont(selectEditorFont(myEditor, myLabel.text))
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
    val `val` = myScrollPane.getVerticalScrollBar().value
    myScrollPane.getVerticalScrollBar().setValue(`val` + more)
    myScrollPane.getHorizontalScrollBar().setValue(0)
    if (`val` + more >=
      myScrollPane.getVerticalScrollBar().maximum - myScrollPane.getVerticalScrollBar().visibleAmount
    ) {
      myLabel.setText(injector.messages.message("message.ex.output.end.prompt"))
    } else {
      myLabel.setText(injector.messages.message("message.ex.output.more.prompt"))
    }
    myLabel.setFont(selectEditorFont(myEditor, myLabel.text))
  }

  val isAtEnd: Boolean
    get() {
      val `val` = myScrollPane.getVerticalScrollBar().value
      return `val` >=
              myScrollPane.getVerticalScrollBar().maximum - myScrollPane.getVerticalScrollBar().visibleAmount
    }

  private fun positionPanel() {
    val contentComponent = myEditor.contentComponent
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

  @JvmOverloads
  fun close(key: KeyStroke? = null) {
    ApplicationManager.getApplication().invokeLater {
      deactivate(true)
      val project = myEditor.project
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
          injector.executionContextManager.getEditorExecutionContext(IjVimEditor(myEditor))
        VimPlugin.getMacro().playbackKeys(IjVimEditor(myEditor), context, 1)
      }
    }
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
    private val LOG: VimLogger = injector.getLogger<ExOutputPanel>(ExOutputPanel::class.java)

    fun getNullablePanel(editor: Editor): ExOutputPanel? {
      return editor.vimMorePanel
    }

    fun isPanelActive(editor: Editor): Boolean {
      return getNullablePanel(editor) != null
    }

    fun getInstance(editor: Editor): ExOutputPanel {
      var panel: ExOutputPanel? = getNullablePanel(editor)
      if (panel == null) {
        panel = ExOutputPanel(editor)
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
