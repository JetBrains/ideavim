/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.util.IJSwingUtilities
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimCommandLineCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimKeyGroupBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.group.KeyGroup
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.requestFocus
import com.maddyhome.idea.vim.helper.selectEditorFont
import com.maddyhome.idea.vim.helper.updateIncsearchHighlights
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptor
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.ui.ExPanelBorder
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.GlobalCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.annotations.Contract
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.LayoutManager
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
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
import kotlin.math.max
import kotlin.math.min

/**
 * This is used to enter ex commands such as searches and "colon" commands
 */
class ExEntryPanel private constructor() : JPanel(), VimCommandLine {
  override var isReplaceMode: Boolean = false
  override var inputProcessing: ((String) -> Unit)? = null
  override var finishOn: Char? = null

  var inputInterceptor: VimInputInterceptor? = null
  private var weakEditor: WeakReference<Editor?>? = null
  var context: DataContext? = null
  override var histIndex: Int = 0
  override var lastEntry: String? = null

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

  /**
   * Turns on the ex entry field for the given editor
   *
   * @param editor   The editor to use for display
   * @param context  The data context
   * @param label    The label for the ex entry (i.e. :, /, or ?)
   * @param initText The initial text for the entry
   */
  fun activate(editor: Editor, context: DataContext?, label: String, initText: String) {
    logger.info("Activate ex entry panel")
    this.myLabel.setText(label)
    this.myLabel.setFont(selectEditorFont(editor, label))
    entry.reset()
    entry.setText(initText)
    entry.setFont(selectEditorFont(editor, initText))
    parent = editor.contentComponent

    val foregroundColour = editor.colorsScheme.defaultForeground
    entry.setForeground(foregroundColour)
    // TODO: Introduce IdeaVim colour scheme for "SpecialKey"?
    val whitespaceColour = editor.colorsScheme.getColor(EditorColors.WHITESPACES_COLOR)
    entry.setSpecialKeyForeground(whitespaceColour ?: foregroundColour)
    this.myLabel.setForeground(entry.getForeground())

    this.context = context
    setEditor(editor)

    histIndex = VimPlugin.getHistory().getEntries(historyType, 0, 0).size

    entry.document.addDocumentListener(fontListener)
    if (this.isIncSearchEnabled) {
      entry.document.addDocumentListener(incSearchDocumentListener)
      caretOffset = editor.caretModel.offset
      verticalOffset = editor.scrollingModel.verticalScrollOffset
      horizontalOffset = editor.scrollingModel.horizontalScrollOffset
    }

    if (!ApplicationManager.getApplication().isUnitTestMode) {
      val root = SwingUtilities.getRootPane(parent) ?: return
      val glassPane = root.getGlassPane() as JComponent
      oldGlass = glassPane
      oldLayout = glassPane.layout
      wasOpaque = glassPane.isOpaque
      glassPane.setLayout(null)
      glassPane.setOpaque(false)
      glassPane.add(this)
      glassPane.addComponentListener(resizePanelListener)
      positionPanel()
      glassPane.isVisible = true
      entry.requestFocusInWindow()
    }
    this.isActive = true
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
    try {
      entry.document.removeDocumentListener(fontListener)
      // incsearch won't change in the lifetime of this activation
      if (this.isIncSearchEnabled) {
        entry.document.removeDocumentListener(incSearchDocumentListener)

        // TODO: Reduce the amount of unnecessary work here
        // If incsearch and hlsearch are enabled, and if this is a search panel, we'll have all of the results correctly
        // highlighted. But because we don't know why we're being closed, and what handler is being called next, we need
        // to reset state. This will remove all highlights and reset back to the last accepted search results. This is
        // fine for <Esc>. But if we hit <Enter>, the search handler will remove the highlights again, perform the same
        // search that we did for incsearch and add highlights back. The `:nohlsearch` command, even if bound to a
        // shortcut, is still processed by the ex entry panel, so deactivating will force update remove, search and add
        // of the current search results before the `NoHLSearchHandler` will remove all highlights again
        val editor = this.ijEditor
        if (editor != null && !editor.isDisposed && resetCaret) {
          resetCaretOffset(editor)
        }

        VimPlugin.getSearch().resetIncsearchHighlights()
      }

      entry.deactivate()
    } finally {
      // Make sure we hide the UI, especially if something goes wrong
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        if (refocusOwningEditor && parent != null) {
          requestFocus(parent!!)
        }

        oldGlass!!.removeComponentListener(resizePanelListener)
        oldGlass!!.isVisible = false
        oldGlass!!.remove(this)
        oldGlass!!.setOpaque(wasOpaque)
        oldGlass!!.setLayout(oldLayout)
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

  private fun resetCaretOffset(editor: Editor) {
    // Reset the original caret, with original scroll offsets
    val primaryCaret = editor.caretModel.primaryCaret
    if (primaryCaret.offset != caretOffset) {
      IjVimCaret(primaryCaret).moveToOffset(caretOffset)
    }
    val scrollingModel = editor.scrollingModel
    if (scrollingModel.horizontalScrollOffset != horizontalOffset ||
      scrollingModel.verticalScrollOffset != verticalOffset
    ) {
      scrollingModel.scroll(horizontalOffset, verticalOffset)
    }
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

  private val incSearchDocumentListener: DocumentListener = object : DocumentAdapter() {
    override fun textChanged(e: DocumentEvent) {
      try {
        val editor: Editor = ijEditor ?: return

        val labelText: String = myLabel.text // Either '/', '?' or ':'boolean searchCommand = false;

        var searchCommand = false
        var searchRange: LineRange? = null
        var separator = labelText[0]
        var searchText = text
        if (labelText == ":") {
          if (searchText.isEmpty()) return
          val command = getIncsearchCommand(searchText) ?: return
          searchCommand = true
          searchText = ""
          val argument = command.commandArgument
          if (argument.length > 1) {  // E.g. skip '/' in `:%s/`. `%` is range, `s` is command, `/` is argument
            separator = argument[0]
            searchText = argument.substring(1)
          }
          if (!searchText.isEmpty()) {
            searchRange = command.getLineRangeSafe(IjVimEditor(editor))
          }
          if (searchText.isEmpty() || searchRange == null) {
            // Reset back to the original search highlights after deleting a search from a substitution command.Or if
            // there is no search range (because the user entered an invalid range, e.g. mark not set).
            // E.g. Highlight `whatever`, type `:%s/foo` + highlight `foo`, delete back to `:%s/` and reset highlights
            // back to `whatever`
            VimPlugin.getSearch().resetIncsearchHighlights()
            resetCaretOffset(editor)
            return
          }
        }

        // Get a snapshot of the count for the in progress command, and coerce it to 1. This value will include all
        // count components - selecting register(s), operator and motions. E.g. `2"a3"b4"c5d6/` will return 720.
        // If we're showing highlights for an Ex command like `:s`, the command builder will be empty, but we'll still
        // get a valid value.
        val count1 = max(
          1, getInstance().keyHandlerState.editorCommandBuilder
            .calculateCount0Snapshot()
        )

        if (labelText == "/" || labelText == "?" || searchCommand) {
          val forwards = labelText != "?" // :s, :g, :v are treated as forwards
          val patternEnd: Int = injector.searchGroup.findEndOfPattern(searchText, separator, 0)
          val pattern = searchText.take(patternEnd)

          VimPlugin.getEditor().closeEditorSearchSession(editor)
          val matchOffset =
            updateIncsearchHighlights(
              editor, pattern, count1, forwards, caretOffset,
              searchRange
            )
          if (matchOffset != -1) {
            // Moving the caret will update the Visual selection, which is only valid while performing a search. We want
            // to remove the Visual selection when the incsearch is for a command, as this is always unrelated to the
            // current selection.
            // E.g. `V/foo` should update the selection to the location of the search result. But `V` followed by
            // `:<C-U>%s/foo` should remove the selection first.
            // We're actually in Command-line with Visual pending. Exiting Visual replaces this with just Command-line
            if (searchCommand) {
              IjVimEditor(editor).exitVisualMode()
            }
            IjVimCaret(editor.caretModel.primaryCaret).moveToOffset(matchOffset)
          } else {
            resetCaretOffset(editor)
          }
        }
      } catch (ex: Throwable) {
        // Make sure the exception doesn't leak out of the handler, because it can break the text entry field and
        // require the editor to be closed/reopened. The worst that will happen is no incsearch highlights
        logger.error("Error while trying to show incsearch highlights", ex)
      }
    }

    @Contract("null -> null")
    private fun getIncsearchCommand(commandText: String?): Command? {
      if (commandText == null) return null
      try {
        val exCommand = VimscriptParser.parseCommand(commandText)
        // TODO: Add smagic and snomagic here if/when the commands are supported
        if (exCommand is SubstituteCommand || exCommand is GlobalCommand) {
          return exCommand
        }
      } catch (e: Exception) {
        logger.error("Cannot parse command for incsearch", e)
      }

      return null
    }
  }

  /**
   * Gets the label for the ex entry. This should be one of ":", "/", or "?"
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

  override fun getRenderedText(): String {
    val stringBuilder = StringBuilder()
    getRenderedText(entry.getUI().getRootView(entry), stringBuilder)
    if (stringBuilder.get(stringBuilder.length - 1) == '\n') {
      stringBuilder.deleteCharAt(stringBuilder.length - 1)
    }
    return stringBuilder.toString()
  }

  private fun getRenderedText(view: View, stringBuilder: StringBuilder) {
    if (view.element.isLeaf) {
      if (view is GlyphView) {
        val text = view.getText(view.getStartOffset(), view.getEndOffset())
        stringBuilder.append(text)

        // GlyphView doesn't render a trailing new line, but uses it to flush the characters in the preceding string
        // Typically, we won't get a newline in the middle of a string, but we do add the prompt to the end of the doc
        if (stringBuilder.get(stringBuilder.length - 1) == '\n') {
          stringBuilder.deleteCharAt(stringBuilder.length - 1)
        }
      } else {
        stringBuilder.append("<Unknown leaf view. Expected GlyphView, got: ")
        stringBuilder.append(view.javaClass.getName())
        stringBuilder.append(">")
      }
    } else {
      val viewCount = view.viewCount
      for (i in 0..<viewCount) {
        val child = view.getView(i)
        getRenderedText(child, stringBuilder)
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
   * consume it for mapping or a command. If it's not consumed, or if it's mapped, the keystroke is returned to the
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

    // Swing uses a bad pattern of calling updateUI() from the constructor. At this moment, `entry` and myLabel is null.
    @Suppress("SENSELESS_COMPARISON")
    if (entry != null && myLabel != null) {
      setFontForElements()

      // Label background is automatically picked up
      myLabel.setForeground(entry.getForeground())

      // Make sure the panel is positioned correctly if we're changing font size
      positionPanel()
    }
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
      val pos = SwingUtilities.convertPoint(scroll.getParent(), bounds.location, oldGlass)
      bounds.location = pos
      setBounds(bounds)
      repaint()
    }
  }

  private val isIncSearchEnabled: Boolean
    get() = injector.globalOptions().incsearch

  /**
   * Checks if the ex entry panel is currently active
   *
   * @return true if active, false if not
   */
  var isActive: Boolean = false
    private set

  // UI stuff
  private var parent: JComponent? = null
  private val myLabel: JLabel = JLabel(" ")
  val entry: ExTextField = ExTextField(this)
  private var oldGlass: JComponent? = null
  private var oldLayout: LayoutManager? = null
  private var wasOpaque = false

  // incsearch stuff
  private var verticalOffset = 0
  private var horizontalOffset = 0
  private var caretOffset = 0

  private val resizePanelListener: ComponentListener = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent?) {
      positionPanel()
    }
  }

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
      VimShortcutKeyAction.instance, KeyGroup.toShortcutSet(
        (injector.keyGroup as VimKeyGroupBase).requiredShortcutKeys
      ), entry
    )

    updateUI()
  }

  override val caret: VimCommandLineCaret
    get() = entry.caret as VimCommandLineCaret

  override fun setText(string: String, updateLastEntry: Boolean) {
    // It's a feature of Swing that caret is moved when we set new text. However, our API is Swing independent and we do not expect this
    val offset = caret.offset
    entry.updateText(string)
    if (updateLastEntry) entry.saveLastEntry()
    caret.offset = min(offset, text.length)
  }

  override fun deleteText(offset: Int, length: Int) {
    entry.deleteText(offset, length)
  }

  override fun insertText(offset: Int, string: String) {
    // Remember that replace mode is different to overwrite! The document handles overwrite, but we must handle replace
    if (isReplaceMode) {
      entry.deleteText(offset, string.length)
    }
    entry.insertText(offset, string)
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
