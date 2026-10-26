/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.util.IJSwingUtilities
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.api.CommandLineCompletion
import com.maddyhome.idea.vim.api.CommandLineType
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimCommandLineCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimKeyGroupBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.ShortcutHelper
import com.maddyhome.idea.vim.helper.requestFocus
import com.maddyhome.idea.vim.helper.selectEditorFont
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptor
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.ui.ExPanelBorder
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.lang.ref.WeakReference
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.GlyphView
import javax.swing.text.View
import kotlin.math.min

/**
 * This is used to enter ex commands such as searches and "colon" commands
 */
class ExEntryPanel private constructor() : JPanel(), VimCommandLine {
  private var weakEditor: WeakReference<Editor?>? = null

  private val glassPaneManager = object : GlassPaneManager() {
    override fun onResize() = positionPanel()
  }

  override var isReplaceMode: Boolean = false
  override var inputProcessing: ((String) -> Unit)? = null
  override var finishOn: Char? = null
  override var lastEntry: String? = null
  override var activeCompletion: CommandLineCompletion? = null

  override var isAbbreviationInvalidated: Boolean = false
  private var lastSeenCmdlineLength: Int = 0

  /** Called from [ExTextField]'s caret listener whenever the cmdline caret moves. */
  internal fun noteCmdlineCaretMove(currentLength: Int) {
    if (currentLength == lastSeenCmdlineLength) {
      isAbbreviationInvalidated = true
    }
    lastSeenCmdlineLength = currentLength
  }

  var inputInterceptor: VimInputInterceptor? = null
  var context: DataContext? = null

  override fun getIncsearchMatchOffset(): Int {
    return preview.matchOffset
  }

  override fun showCompletionBar(completion: CommandLineCompletion) {
    if (!injector.globalOptions().wildmenu) return
    if (ApplicationManager.getApplication().isUnitTestMode) return
    val editor = this.ijEditor ?: return

    completionPanel.updateColors(editor.colorsScheme.defaultForeground, entry.getBackground())
    completionPanel.updateFont(entry.getFont())
    completionPanel.setItems(completion.displayNames, completion.currentIndex)

    if (!isCompletionBarVisible) {
      glassPaneManager.glassPane?.add(completionPanel)
      isCompletionBarVisible = true
    }
    positionCompletionPanel()
  }

  override fun selectCompletionItem(selectedIndex: Int?) {
    if (!isCompletionBarVisible || selectedIndex == null) return
    completionPanel.setSelectedIndex(selectedIndex)
  }

  override fun hideCompletionBar() {
    if (!isCompletionBarVisible) return
    isCompletionBarVisible = false
    glassPaneManager.glassPane?.let {
      it.remove(completionPanel)
      it.repaint()
    }
  }

  private fun dismissCompletionIfTextChanged() {
    val completion = activeCompletion ?: return
    if (text != completion.expectedText) {
      activeCompletion = null
      hideCompletionBar()
    }
  }

  val ijEditor: Editor?
    get() = if (weakEditor != null) weakEditor!!.get() else null

  override val editor: VimEditor
    get() {
      val editor = this.ijEditor ?: throw RuntimeException("Editor was disposed for active command line")
      return IjVimEditor(editor)
    }

  fun setEditor(editor: Editor?) {
    weakEditor = if (editor == null) {
      null
    } else {
      WeakReference<Editor?>(editor)
    }
  }

  override var type: CommandLineType = CommandLineType.Input
    private set

  /**
   * Turns on the ex entry field for the given editor
   *
   * @param editor   The editor to use for display
   * @param context  The data context
   * @param type     The kind of command line
   * @param label    The prompt shown before the text
   * @param initText The initial text for the entry
   */
  fun activate(editor: Editor, context: DataContext?, type: CommandLineType, label: String, initText: String) {
    logger.info("Activate ex entry panel")
    this.type = type
    this.myLabel.setText(label)
    this.myLabel.setFont(selectEditorFont(editor, label))
    entry.reset()
    entry.setText(initText)
    entry.setFont(selectEditorFont(editor, initText))
    parent = editor.contentComponent

    applyColorScheme(editor)

    this.context = context
    setEditor(editor)

    preview = CommandLinePreview.create(type, editor)

    entry.document.addDocumentListener(fontListener)
    entry.document.addDocumentListener(previewListener)

    if (!ApplicationManager.getApplication().isUnitTestMode) {
      glassPaneManager.activate(editor, this)
      positionPanel()
      glassPaneManager.show()
      SwingUtilities.invokeLater { entry.requestFocusInWindow() }
    }
    isActive = true
    // Must run last: entry.setText()/entry.reset() above fire the caret listener, which would
    // otherwise spuriously flip isAbbreviationInvalidated to true before the user has typed.
    resetAbbreviationTracking()
  }

  private fun resetAbbreviationTracking() {
    lastSeenCmdlineLength = entry.text.length
    isAbbreviationInvalidated = false
  }

  fun deactivate(refocusOwningEditor: Boolean) {
    deactivate(refocusOwningEditor, true)
  }

  /**
   * Turns off the ex entry field and optionally puts the focus back to the original component
   */
  override fun deactivate(refocusOwningEditor: Boolean, resetCaret: Boolean) {
    logger.info("Deactivate ex entry panel")
    if (!this.isActive) return

    clearPromptCharacter()
    hideCompletionBar()
    activeCompletion = null
    try {
      entry.document.removeDocumentListener(fontListener)
      entry.document.removeDocumentListener(previewListener)
      preview.close(this.ijEditor?.takeUnless { it.isDisposed }, resetCaret)
      preview = NoPreview

      entry.deactivate()
    } finally {
      // Make sure we hide the UI, especially if something goes wrong
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        if (refocusOwningEditor && parent != null) {
          requestFocus(parent!!)
        }

        glassPaneManager.deactivate()
      }

      parent = null
    }

    isReplaceMode = false
    setEditor(null)
    context = null

    // We have this in the end, because `entry.deactivate()` communicates with active panel during deactivation
    this.isActive = false
    finishOn = null
    this.inputInterceptor = null
    inputProcessing = null
  }

  private fun reset() {
    deactivate(false)
  }

  private val fontListener: DocumentListener = object : DocumentAdapter() {
    override fun textChanged(e: DocumentEvent) {
      val text = entry.getText()
      val newFont = selectEditorFont(ijEditor, text)
      if (newFont !== entry.getFont()) {
        entry.setFont(newFont)
      }
    }
  }


  // Fires on every command-line edit to update the preview, e.g. incsearch highlighting or the inccommand substitution
  private val previewListener: DocumentListener = object : DocumentAdapter() {
    override fun textChanged(e: DocumentEvent) {
      val editor = ijEditor ?: return
      try {
        preview.update(editor, text)
      } catch (ex: Throwable) {
        // Make sure the exception doesn't leak out of the handler, because it can break the text entry field and
        // require the editor to be closed/reopened. The worst that will happen is no incsearch highlights or preview.
        logger.error("Error while updating the incremental command-line UI", ex)
      }
    }
  }

  /**
   * `c_CTRL-G` / `c_CTRL-T` - move the incsearch preview to the next ([next] == true) or previous ([next] == false)
   * match, without closing the command line or running the search.
   *
   * Does nothing if 'incsearch' is off or there's no usable pattern (e.g. an empty command line).
   */
  override fun advanceIncsearchMatch(next: Boolean) {
    val editor = ijEditor ?: return
    preview.advanceMatch(editor, text, next)
  }

  /**
   * Gets the label for the ex entry, e.g. ":", "/", "?" or the prompt of an input. See [type] for the kind of entry
   *
   * @return The ex entry label
   */
  override fun getLabel(): String {
    return myLabel.text
  }

  override fun toggleReplaceMode() {
    entry.toggleInsertReplace()
  }

  override val text: String
    get() = entry.getText()

  override val modelessSelection: String
    get() = entry.selectedText ?: ""

  @TestOnly
  fun setModelessSelection(start: Int, end: Int) {
    entry.select(start, end)
  }

  override fun getRenderedText() = buildString {
    getRenderedText(entry.getUI().getRootView(entry))
    if (last() == '\n') {
      deleteCharAt(lastIndex)
    }
  }

  private fun StringBuilder.getRenderedText(view: View) {
    if (view.element.isLeaf) {
      if (view is GlyphView) {
        val text = view.getText(view.getStartOffset(), view.getEndOffset())
        append(text)

        // GlyphView doesn't render a trailing new line, but uses it to flush the characters in the preceding string
        // Typically, we won't get a newline in the middle of a string, but we do add the prompt to the end of the doc
        if (last() == '\n') {
          deleteCharAt(lastIndex)
        }
      } else {
        append("<Unknown leaf view. Expected GlyphView, got: ")
        append(view.javaClass.getName())
        append(">")
      }
    } else {
      val viewCount = view.viewCount
      for (i in 0..<viewCount) {
        val child = view.getView(i)
        getRenderedText(child)
      }
    }
  }

  override fun setPromptCharacter(promptCharacter: Char) {
    val entryUi = entry.getUI()
    if (entryUi is ExTextFieldUI) {
      entryUi.setPromptCharacter(promptCharacter)
    }
  }

  override fun clearPromptCharacter() {
    val exTextFieldUI = entry.getUI()
    if (exTextFieldUI is ExTextFieldUI) {
      exTextFieldUI.clearPromptCharacter()
    }
  }

  /**
   * Pass the keystroke on to the text field for handling
   *
   *
   * The text field for the command line will forward a pressed or typed keystroke to the key handler, which will either
   * consume it for mapping or a command. If it's not consumed or if it's mapped, the keystroke is returned to the
   * command line to complete handling. This includes typed characters as well as pressed shortcuts.
   *
   *
   * @param key The potentially mapped keystroke
   */
  override fun handleKey(key: KeyStroke) {
    entry.handleKey(key)
    val myInputProcessor = inputProcessing
    if (finishOn != null && key.keyChar == finishOn && myInputProcessor != null) {
      val myText = text
      close(refocusOwningEditor = true, resetCaret = true)
      myInputProcessor.invoke(myText)
    }
  }

  // Called automatically when the LAF is changed and the component is visible, and manually by the LAF listener handler
  override fun updateUI() {
    super.updateUI()

    setBorder(ExPanelBorder())

    // Swing uses a bad pattern of calling updateUI() from the constructor. At this moment, `entry` and myLabel are null.
    @Suppress("SENSELESS_COMPARISON")
    if (entry != null && myLabel != null) {
      setFontForElements()

      // Label background is automatically picked up
      myLabel.setForeground(entry.getForeground())

      // Make sure the panel is positioned correctly if we're changing font size
      positionPanel()
    }
  }

  /**
   * Apply the colours that depend on the editor colour scheme.
   */
  private fun applyColorScheme(editor: Editor) {
    val foregroundColour = editor.colorsScheme.defaultForeground
    entry.setForeground(foregroundColour)
    // TODO: Introduce IdeaVim colour scheme for "SpecialKey"?
    val whitespaceColour = editor.colorsScheme.getColor(EditorColors.WHITESPACES_COLOR)
    entry.setSpecialKeyForeground(whitespaceColour ?: foregroundColour)
    this.myLabel.setForeground(entry.getForeground())
  }

  /**
   * Refresh the typed text colour when the editor colour scheme changes while the command line is open.
   *
   * See [applyColorScheme] for why a LAF refresh alone doesn't update the entry's foreground.
   */
  internal fun refreshColors() {
    val editor = ijEditor ?: return
    @Suppress("SENSELESS_COMPARISON")
    if (entry == null) return
    applyColorScheme(editor)
  }

  override fun getForeground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (entry == null) {
      // Swing uses a bad pattern of calling getForeground() from the constructor. At this moment, `entry` is null.
      return super.getForeground()
    }
    return entry.getForeground()
  }

  override fun getBackground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (entry == null) {
      // Swing uses a bad pattern of calling getBackground() from the constructor. At this moment, `entry` is null.
      return super.getBackground()
    }
    return entry.getBackground()
  }

  private fun setFontForElements() {
    myLabel.setFont(selectEditorFont(this.ijEditor, myLabel.text))
    entry.setFont(selectEditorFont(this.ijEditor, entry.getText()))
  }

  private fun positionPanel() {
    if (parent == null) return

    val scroll = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, parent)
    val height = getPreferredSize().getHeight().toInt()
    if (scroll != null) {
      val bounds = scroll.bounds
      bounds.translate(0, scroll.getHeight() - height)
      bounds.height = height
      val pos = SwingUtilities.convertPoint(scroll.getParent(), bounds.location, glassPaneManager.glassPane)
      bounds.location = pos
      setBounds(bounds)
      repaint()
    }

    if (isCompletionBarVisible) {
      positionCompletionPanel()
    }
  }

  private fun positionCompletionPanel() {
    val myBounds = bounds
    if (myBounds.width == 0) return

    val completionHeight = completionPanel.preferredSize.height
    completionPanel.setBounds(
      myBounds.x,
      myBounds.y - completionHeight,
      myBounds.width,
      completionHeight,
    )
    completionPanel.revalidate()
    completionPanel.repaint()
  }

  /**
   * Checks if the ex entry panel is currently active
   *
   * @return true if active, false if not
   */
  var isActive: Boolean = false
    private set

  // UI stuff
  @VisibleForTesting
  val entry: ExTextField = ExTextField(this)

  private var parent: JComponent? = null
  private val myLabel: JLabel = JLabel(" ")
  private val completionPanel = ExCompletionPanel()
  private var isCompletionBarVisible = false

  // Chosen from the type of the command line on every activation
  private var preview: CommandLinePreview = NoPreview

  init {

    val layout = GridBagLayout()
    val gbc = GridBagConstraints()

    setLayout(layout)
    gbc.gridx = 0
    layout.setConstraints(this.myLabel, gbc)
    add(this.myLabel)
    gbc.gridx = 1
    gbc.weightx = 1.0
    gbc.fill = GridBagConstraints.HORIZONTAL
    layout.setConstraints(entry, gbc)
    add(entry)

    // This does not need to be unregistered, it's registered as a custom UI property on this
    EventFacade.getInstance().registerCustomShortcutSet(
      VimShortcutKeyAction.instance, ShortcutHelper.toShortcutSet(
        (injector.keyGroup as VimKeyGroupBase).requiredShortcutKeys
      ), entry
    )

    updateUI()
  }

  override val caret: VimCommandLineCaret
    get() = entry.caret as VimCommandLineCaret

  override fun setText(string: String, updateLastEntry: Boolean) {
    // It's a feature of Swing that the caret is moved when we set a new text. However, our API is Swing independent,
    // and we do not expect this
    val offset = caret.offset
    entry.updateText(string)
    if (updateLastEntry) entry.saveLastEntry()
    caret.offset = min(offset, text.length)

    dismissCompletionIfTextChanged()
  }

  override fun deleteText(offset: Int, length: Int) {
    entry.deleteText(offset, length)
    dismissCompletionIfTextChanged()
  }

  override fun insertText(offset: Int, string: String) {
    // Remember that replace mode is different to overwrite! The document handles overwrite, but we must handle replace
    if (isReplaceMode) {
      entry.deleteText(offset, string.length)
    }
    entry.insertText(offset, string)
    dismissCompletionIfTextChanged()
  }

  override fun clearCurrentAction() {
    entry.clearCurrentAction()
  }

  override fun focus() {
    IdeFocusManager.findInstance().requestFocus(entry, true)
  }

  class LafListener : LafManagerListener {
    override fun lookAndFeelChanged(source: LafManager) {
      if (VimPlugin.isNotEnabled()) return

      // Calls updateUI on this and child components
      if (instance != null) {
        IJSwingUtilities.updateComponentTreeUI(instance)
      }
    }
  }

  /**
   * Refresh the typed text colour when the editor colour scheme changes.
   *
   * The scheme changes both when it's switched directly and when an IDE theme switch swaps in a different bundled
   * scheme. Unlike [LafListener]'s `updateComponentTreeUI`, this re-applies the foreground of the (explicitly coloured)
   * entry field. See [applyColorScheme].
   */
  class ColorSchemeListener : EditorColorsListener {
    override fun globalSchemeChange(scheme: EditorColorsScheme?) {
      if (VimPlugin.isNotEnabled()) return
      instance?.refreshColors()
    }
  }

  companion object {
    var instance: ExEntryPanel? = null

    fun getOrCreatePanelInstance(): ExEntryPanel {
      return instance ?: let {
        val exEntryPanel = ExEntryPanel()
        instance = exEntryPanel
        exEntryPanel
      }
    }

    fun fullReset() {
      val myInstance = instance
      if (myInstance != null) {
        myInstance.reset()
        instance = null
      }
    }

    private val logger = Logger.getInstance(ExEntryPanel::class.java.getName())
  }
}
