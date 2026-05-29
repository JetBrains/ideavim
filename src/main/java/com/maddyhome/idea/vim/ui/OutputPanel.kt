/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui

import com.intellij.ide.KeyboardAwareFocusOwner
import com.intellij.ide.RemoteDesktopService
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.intellij.ui.ClientProperty
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.IJSwingUtilities
import com.intellij.util.animation.Easing
import com.intellij.util.animation.JBAnimator
import com.intellij.util.animation.animation
import com.intellij.util.application
import com.intellij.util.ui.launchOnShow
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MessageType
import com.maddyhome.idea.vim.api.VimOutputPanel
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.requestFocus
import com.maddyhome.idea.vim.helper.selectEditorFont
import com.maddyhome.idea.vim.helper.vimMorePanel
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.register.RegisterConstants
import com.maddyhome.idea.vim.ui.ex.GlassPaneManager
import kotlinx.coroutines.delay
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.text.AbstractDocument
import javax.swing.text.DefaultCaret
import javax.swing.text.LabelView
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import javax.swing.text.StyledEditorKit
import javax.swing.text.View
import javax.swing.text.ViewFactory
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds


/**
 * Panel that displays text in a `more` like window overlaid on the editor.
 */
internal class OutputPanel private constructor(private val editor: Editor) : JBPanel<OutputPanel>(), VimOutputPanel {

  private val glassPaneManager = object : GlassPaneManager() {
    override fun onResize() = positionPanel(isInitialPosition = false)
  }

  private val textPane = object : JTextPane(), KeyboardAwareFocusOwner {
    /**
     * Skip the IDE's key event dispatcher when the output panel is active
     *
     * This allows us to skip the standard IDE event dispatcher, so we handle our own keystrokes instead of the IDE
     * stealing them as shortcuts.
     */
    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean {
      return active
    }
  }

  private val promptComponent: JLabel = JLabel("more")
  private val scrollPane: JScrollPane =
    JBScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
  private var defaultForeground: Color? = null

  private val segments = mutableListOf<TextLine>()
  private var cachedLineHeight = 0

  private var allowClose = true

  private val animator = JBAnimator().setPeriod(4).setName("IdeaVim output panel animator")

  var active: Boolean = false

  @get:VisibleForTesting
  var isSingleLine = false
    private set

  init {
    textPane.isEditable = false
    textPane.caret = object : DefaultCaret() {
      override fun setVisible(v: Boolean) {
        super.setVisible(false)
      }

      override fun adjustVisibility(nloc: Rectangle?) {
        // When the caret is moved either explicitly or by changing text, Swing will asynchronously move and repaint the
        // caret, including calling adjustVisibility. The default implementation scrolls to make sure the caret is
        // visible. We don't care about caret location and don't want it interfering with scroll position, so do nothing
      }
    }
    textPane.editorKit = object : StyledEditorKit() {
      override fun getViewFactory(): ViewFactory {
        val factory = super.viewFactory
        return ViewFactory { elem ->
          if (AbstractDocument.ContentElementName == elem.name) CharacterBreakView(elem)
          else factory.create(elem)
        }
      }
    }
    // Force the text cursor. The LAF uses the default cursor when isEditable is false
    textPane.cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)

    // Suppress the fancy frame background used in the Islands theme
    ClientProperty.putRecursive(this, IdeBackgroundUtil.NO_BACKGROUND, true)

    // Initialize panel
    layout = BorderLayout(0, 0)
    add(scrollPane, BorderLayout.CENTER)
    add(promptComponent, BorderLayout.SOUTH)

    val keyListener = OutputPanelKeyListener()
    addKeyListener(keyListener)
    textPane.addKeyListener(keyListener)

    scrollPane.verticalScrollBar.addAdjustmentListener { e ->
      // Update the prompt when scrolling stops, but only for non-single-line output. We will get a scroll update event
      // if resizing the IDE frame forces the single-line output to wrap and become multi-line.
      if (!isSingleLine && !e.valueIsAdjusting) {
        updatePrompt()
      }
    }

    updateUI()

    // This is usually called in EDT, except for TabmoveTest
    runInEdt {
      launchOnShow("IdeaVim::HideOutputPanel") {
        // Remember, Vim uses the pager for messages and command output only, not for external commands (e.g. "!ls -l")
        // IdeaVim does not distinguish between messages and external command output.
        // The 'more' option controls showing the "-- MORE --" prompt in the pager.
        // The 'messagesopt' "hit-enter" value shows the "press ENTER" prompt regardless of pager use.
        // The 'messagesopt' "wait" value is used when "hit-enter" is not set. Instead of showing "press ENTER", it
        // waits before redrawing the message area. This applies messages, command and external command output. Wait can
        // be mixed with 'nomore'.
        // IdeaVim extends the "wait" value to automatically clear single-line output. Vim doesn't do this because the
        // message area is less intrusive.
        // Setting "mopt=wait:0" will disable the wait at the end of the output, closing it immediately.
        // Setting "hit-enter,wait:0" will show the hit-enter prompt (Vim states that hit-enter takes precedence), and
        // will disable the wait in single-line messages, leaving it open.
        // TODO: Support "hit-enter" properly
        if (isSingleLine /*|| !injector.globalOptions().messagesopt.contains("hit-enter")*/) {
          val wait = injector.globalOptions().messagesopt["wait"]?.toIntOrNull()?.milliseconds
          if (wait != null && wait > 0.milliseconds) {
            delay(wait)
            if (active) close()
          }
        }
      }
    }
  }

  override fun updateUI() {
    super.updateUI()

    border = ExPanelBorder()

    @Suppress("SENSELESS_COMPARISON")
    if (textPane != null && promptComponent != null && scrollPane != null) {
      setFontForElements()
      textPane.border = null
      scrollPane.border = null
      promptComponent.foreground = textPane.foreground
      positionPanel(isInitialPosition = false)
    }
  }

  override var text: String
    get() = textPane.text ?: ""
    set(value) {
      val newValue = value.removeSuffix("\n")
      segments.clear()
      if (newValue.isEmpty()) return
      segments.add(TextLine(newValue, null))
    }

  /**
   * Sets styled text with multiple segments, each potentially having a different color.
   */
  private fun setStyledText(lines: List<TextLine>) {
    val doc = textPane.styledDocument
    doc.remove(0, doc.length)

    if (defaultForeground == null) {
      defaultForeground = textPane.foreground
    }

    setMultiLineText(lines, doc)

    val fullText = doc.getText(0, doc.length)
    textPane.font = selectEditorFont(editor, fullText)
  }

  private fun setMultiLineText(lines: List<TextLine>, doc: StyledDocument) {
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

  override fun addText(text: String, isNewLine: Boolean, messageType: MessageType) {
    val color = when (messageType) {
      MessageType.ERROR -> JBColor.RED
      MessageType.STANDARD -> null
    }
    segments.add(TextLine(text, color))
  }

  override fun show() {
    val currentPanel = injector.outputPanel.getCurrentOutputPanel()
    if (currentPanel != null && currentPanel != this) currentPanel.close()

    setStyledText(segments)

    // Only activate (single-line or multiline) if we have text or enough empty lines to show in the pager (or testing
    // is telling us to always show the output panel). Otherwise, clear the text
    if (!active) {
      if (textPane.text.isNotBlank()
        || (textPane.text.isBlank() && (!allowHideEmptyText || countLines(textPane.text) > injector.globalOptions().cmdheight))) {
        activate()
      }
      else {
        clearText()
      }
    }

    // Don't immediately clear the message if the action that caused it also (indirectly) causes a redraw
    allowClose = false
    injector.application.invokeLater {
      allowClose = true
    }
  }

  override fun clearText() {
    segments.clear()
  }

  override fun getForeground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (textPane == null) {
      return super.foreground
    }
    return textPane.foreground
  }

  override fun getBackground(): Color? {
    @Suppress("SENSELESS_COMPARISON")
    if (textPane == null) {
      return super.background
    }
    return textPane.background
  }

  private fun deactivate() {
    if (!active) return
    active = false
    clearText()
    textPane.text = ""
    glassPaneManager.deactivate()
  }

  /**
   * Turns on the output panel for the given editor.
   */
  private fun activate() {
    glassPaneManager.activate(editor, this)

    setFontForElements()
    positionPanel(isInitialPosition = true)
    resetScroll()

    glassPaneManager.show()
    active = true

    if (!isSingleLine) {
      requestFocus(textPane)
    }
  }

  override fun close() {
    if (allowClose) {
      close(null)
    }
  }

  private fun close(key: KeyStroke?) {
    animator.stop()

    val passKeyBack = isSingleLine
    val doDeactivate = {
      deactivate()
      val project = editor.project
      // For single-line messages, pass any key back to the editor (including Enter)
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
    if (application.isUnitTestMode) {
      doDeactivate()
    }
    else {
      // TODO: Document why this is invoked later
      // It's always been like this. I think it's because when it's called with a keystroke, it's called from the
      // component's key handler, and it's probably a bad idea to handle other keystrokes while in the middle of
      // handling a keystroke
      application.invokeLater(doDeactivate)
    }
  }

  private fun setFontForElements() {
    textPane.font = selectEditorFont(editor, textPane.text)
    promptComponent.font = selectEditorFont(editor, promptComponent.text)
  }

  private fun positionPanel(isInitialPosition: Boolean) {
    val maxPanelSize = getMaxPanelSize() ?: return
    val lineHeight = textPane.getFontMetrics(textPane.font).height
    val lineCount = countLines(textPane.text)
    val maxVisibleLines = (maxPanelSize.height / lineHeight) - 1  // -1 to save space for prompt
    val visibleLines = min(lineCount, maxVisibleLines)

    if (isInitialPosition) {
      // Simple output: single line that fits entirely - no label needed
      // Don't update the flag if we're resizing. We might change text wrapping moving from/to single-line output, but
      // we want to stay in the original mode
      isSingleLine = lineCount <= injector.globalOptions().cmdheight && lineCount <= maxVisibleLines
      promptComponent.isVisible = !isSingleLine
    }

    val extraHeight = if (isSingleLine) 0 else promptComponent.preferredSize.height
    val borderInsets = border.getBorderInsets(this)
    setSize(maxPanelSize.width, visibleLines * lineHeight + extraHeight + borderInsets.top + borderInsets.bottom)
    location = getPanelLocation(size.height) ?: location

    // Force layout so that viewport sizes are valid before checking the scroll state. In tests, we have to do it
    // manually because there isn't a real UI hierarchy.
    if (application.isUnitTestMode) {
      doLayout()
      scrollPane.doLayout()
      scrollPane.viewport.doLayout()
    } else {
      validate()
      // Now that the panel hierarchy has been laid out, set the width of the text pane and revalidate it so that the
      // wrapped lines are calculated synchronously. This would normally happen during painting, but we want it so we
      // can accurately calculate the scroll bar maximum.
      textPane.ui.getRootView(textPane).setSize(textPane.width.toFloat(), Int.MAX_VALUE.toFloat())
      scrollPane.validate()
    }

    cachedLineHeight = lineHeight
  }

  private fun getMaxPanelSize(): Dimension? {
    val scroll = getEditorScrollPane() ?: return null

    // The UI is not available in unit tests, but we do explicitly set the size of the editor's scroll pane's viewport.
    // This ignores borders and the gutter component, but it's good enough for tests.
    return if (application.isUnitTestMode) {
      scroll.viewport.size
    }
    else {
      scroll.size
    }
  }

  private fun getPanelLocation(panelHeight: Int): Point? {
    if (application.isUnitTestMode) {
      // We don't care about location in unit tests, it's always going to be bottom of the editor, and we don't need to
      // assert that.
      return null
    }

    val scroll = getEditorScrollPane() ?: return null
    val point = scroll.location
    point.translate(0, scroll.height - panelHeight)
    point.location = SwingUtilities.convertPoint(scroll.parent, point.location, glassPaneManager.glassPane)
    return point
  }

  private fun getEditorScrollPane(): JScrollPane? {
    if (editor is EditorEx) {
      return editor.scrollPane
    }
    return SwingUtilities.getAncestorOfClass(JScrollPane::class.java, editor.contentComponent) as? JScrollPane
  }

  private fun countLines(text: String): Int {
    if (text.isEmpty()) {
      return 1
    }

    val lineWidth = (width / EditorHelper.getPlainSpaceWidthFloat(editor)).toInt()
    var lineCount = 0
    var last = 0
    text.forEachIndexed { index, ch ->
      if (ch == '\n') {
        lineCount += ceil((index - last) / lineWidth.toDouble()).toInt().coerceAtLeast(1)
        last = index
      }
    }
    lineCount += ceil((text.length - last) / lineWidth.toDouble()).toInt()

    return lineCount
  }

  private fun scrollToStart() {
    if (isSingleLine) return
    scrollByOffset(-Int.MAX_VALUE)
  }

  private fun scrollToEnd() {
    if (isSingleLine) return
    scrollByOffset(Int.MAX_VALUE)
  }

  private fun scrollLine(direction: Int = 1) {
    scrollByOffset(cachedLineHeight * direction)
  }

  private fun scrollPage(direction: Int = 1) {
    scrollByOffset(scrollPane.verticalScrollBar.visibleAmount * direction)
  }

  private fun scrollHalfPage(direction: Int = 1) {
    val sa = scrollPane.verticalScrollBar.visibleAmount / 2.0
    val offset = ceil(sa / cachedLineHeight) * cachedLineHeight
    scrollByOffset(offset.toInt() * direction)
  }

  private fun onBadKey() {
    promptComponent.text = injector.messages.message("message.ex.output.more.prompt.full")
    promptComponent.font = selectEditorFont(editor, promptComponent.text)
  }

  private fun scrollByOffset(offset: Int) {
    scrollPane.validate()

    // Note that we always scroll from the current location. If we're in the middle of scrolling, this will not be the
    // target of the last scroll. I don't think that's important
    val scrollBar = scrollPane.verticalScrollBar
    val value = scrollBar.value

    val duration = Registry.intValue("idea.editor.smooth.scrolling.navigation.duration")
    if (duration > 0 && shouldAnimateScroll() && abs(offset) > cachedLineHeight) {
      val startValue = scrollBar.value
      val endValue = (min(startValue + offset, scrollBar.maximum - scrollBar.visibleAmount) / cachedLineHeight) * cachedLineHeight
      val animation = animation {
        scrollBar.value = (startValue + (endValue - startValue) * it + 0.5).toInt()
      }
      animation.duration = duration
      animation.easing = Easing.EASE_OUT
      animation.runWhenExpired {
        // Avoid any rounding errors and make sure we're at the end
        scrollBar.value = startValue + offset
        updatePrompt()
      }
      animator.animate(animation)
    }
    else {
      scrollBar.value = (min(value + offset, scrollBar.maximum - scrollBar.visibleAmount) / cachedLineHeight) * cachedLineHeight
      scrollPane.horizontalScrollBar.value = 0
      updatePrompt()
    }
  }

  private fun shouldAnimateScroll(): Boolean {
    return !application.isUnitTestMode && editor.settings.isAnimatedScrolling && !RemoteDesktopService.isRemoteSession()
  }

  private fun resetScroll() {
    scrollPane.verticalScrollBar.value = 0
    if (!isSingleLine && !injector.globalOptions().more) {
      scrollByOffset(Int.MAX_VALUE)
    }
    else {
      updatePrompt()
    }
  }

  private fun updatePrompt() {
    // Check if we're at the end or if content fits entirely (nothing to scroll)
    if (isAtEnd) {
      promptComponent.text = injector.messages.message("message.ex.output.end.prompt")
    } else {
      promptComponent.text = injector.messages.message("message.ex.output.more.prompt")
    }
    promptComponent.font = selectEditorFont(editor, promptComponent.text)
  }

  /**
   * Handle the given keystroke to control the pager
   *
   * This will be called for both `KEY_TYPED` and `KEY_PRESSED` keystrokes, so will get virtual key code keystrokes for
   * actions like `VK_UP`, control characters like `<C-F>` and Vim special keys like `<Enter>`. This matches IdeaVim's
   * handling for commands and mappings, and means we can forward the keystroke to the editor when closing the panel.
   *
   * However, remember that `KEY_PRESSED` events can be followed by `KEY_TYPED`, so do not handle (at all!) typed
   * versions of control characters and Enter, etc.
   */
  internal fun handleKey(key: KeyStroke) {
    // Note that it is normally invalid to compare a virtual key code and a Unicode codepoint; however, these virtual
    // key codes are explicitly defined to match ASCII values
    if (key.keyChar.code == KeyEvent.VK_ENTER
      || key.keyChar.code == KeyEvent.VK_ESCAPE
      || key.keyChar.code == KeyEvent.VK_BACK_SPACE
      || (key.keyChar != KeyEvent.CHAR_UNDEFINED && key.keyChar < '\u0020')
    ) {
      return
    }

    if (isAtEnd) {
      handleHitEnterPrompt(key)
    }
    else {
      handleMorePrompt(key)
    }
  }

  private fun handleHitEnterPrompt(key: KeyStroke) = when (key.keyChar) {
    'g' -> scrollToStart()
    'G' -> close(key)
    ' ' -> close()
    'f' -> close(key)
    'd' -> close(key)
    'j' -> close(key)
    'b' -> scrollPage(-1)
    'u' -> scrollHalfPage(-1)
    'k' -> scrollLine(-1)
    'q' -> close()
    KeyEvent.CHAR_UNDEFINED -> when (key.keyCode) {
      KeyEvent.VK_ESCAPE -> close()
      KeyEvent.VK_ENTER -> close()
      KeyEvent.VK_DOWN -> close(key)
      KeyEvent.VK_PAGE_DOWN -> close(key)
      KeyEvent.VK_F if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) -> close(key)
      KeyEvent.VK_BACK_SPACE -> scrollLine(-1)
      KeyEvent.VK_UP -> scrollLine(-1)
      KeyEvent.VK_H if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) -> scrollLine(-1)
      KeyEvent.VK_PAGE_UP -> scrollPage(-1)
      KeyEvent.VK_B if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) -> scrollPage(-1)
      KeyEvent.VK_Y if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) -> copyModelessSelection()
      else -> close(key)
    }
    else -> close(key)
  }

  private fun handleMorePrompt(key: KeyStroke) = when (key.keyChar) {
    'g' -> scrollToStart()
    'G' -> scrollToEnd()
    ' ' -> scrollPage()
    'f' -> scrollPage()
    'd' -> scrollHalfPage()
    'j' -> scrollLine()
    'b' -> scrollPage(-1)
    'u' -> scrollHalfPage(-1)
    'k' -> scrollLine(-1)
    'q' -> close()
    KeyEvent.CHAR_UNDEFINED -> when (key.keyCode) {
      KeyEvent.VK_ESCAPE -> close()
      KeyEvent.VK_ENTER -> scrollLine()
      KeyEvent.VK_DOWN -> scrollLine()
      KeyEvent.VK_PAGE_DOWN -> scrollPage()
      KeyEvent.VK_F if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) -> scrollPage()
      KeyEvent.VK_BACK_SPACE -> scrollLine(-1)
      KeyEvent.VK_UP -> scrollLine(-1)
      KeyEvent.VK_H if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) -> scrollLine(-1)
      KeyEvent.VK_PAGE_UP -> scrollPage(-1)
      KeyEvent.VK_B if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) -> scrollPage(-1)
      KeyEvent.VK_Y if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) -> copyModelessSelection()
      else -> onBadKey()
    }
    else -> onBadKey()
  }

  /**
   * Copy the modeless selection to the system clipboard
   *
   * When selecting text in the command line or at the hit-enter prompt, this does does not affect the current mode, and
   * is known as modeless selection. Pressing `<C-Y>` will copy this selection to the system clipboard.
   *
   * See `:help modeless-selection` and `:help c_CTRL-Y`.
   */
  private fun copyModelessSelection() {
    val selection = textPane.selectedText ?: return
    injector.registerGroup.storeText(
      editor.vim,
      injector.executionContextManager.getEditorExecutionContext(editor.vim),
      RegisterConstants.CLIPBOARD_REGISTER,
      selection
    )
  }

  @get:VisibleForTesting
  val isAtEnd: Boolean
    get() {
      if (isSingleLine) return true
      val contentHeight = textPane.preferredSize.height
      val viewportHeight = scrollPane.viewport.height
      if (contentHeight <= viewportHeight) return true
      val scrollBar = scrollPane.verticalScrollBar
      return scrollBar.value >= scrollBar.maximum - scrollBar.visibleAmount
    }

  @get:TestOnly
  val promptText: String
    get() = promptComponent.text

  @get:TestOnly
  val pageSize: Int
    get() = scrollPane.verticalScrollBar.visibleAmount / cachedLineHeight

  @get:TestOnly
  val topLine: Int
    get() = scrollPane.verticalScrollBar.value / cachedLineHeight

  @TestOnly
  fun scrollToHitEnterPrompt() {
    scrollToEnd()
  }

  @TestOnly
  fun setModelessSelection(start: Int, end: Int) {
    textPane.select(start, end)
  }

  private inner class OutputPanelKeyListener : KeyAdapter() {
    override fun keyTyped(e: KeyEvent) {
      // Do not try to handle KEY_TYPED versions of Enter, Escape or Delete. We've already handled them as KEY_PRESSED.
      // We want to handle them as KEY_PRESSED keystrokes (i.e., virtual key codes, not key chars) because that's what
      // the mapping system uses, and we need to pass these keystrokes on when the panel closes.
      // Note that it is usually illegal to compare Unicode codepoints and virtual key codes. However, these particular
      // values are specifically chosen to match ASCII values.
      if (e.keyChar.code == KeyEvent.VK_ENTER
        || e.keyChar.code == KeyEvent.VK_ESCAPE
        || e.keyChar.code == KeyEvent.VK_BACK_SPACE) {
        return
      }

      handleKey(KeyStroke.getKeyStrokeForEvent(e))
    }

    override fun keyPressed(e: KeyEvent) {
      // Match IdeaVim's handling of keystrokes for commands and mappings, where we prefer virtual keycode based
      // (i.e., KEY_PRESSED) keystrokes for the equivalent of Vim's special keys (actions like `<Up>`, control
      // characters like `<C-F>` and special keys like `<Enter>`). Remember that after a KEY_PRESSED event, we'll
      // typically get a KEY_TYPED event. Don't handle them twice!
     if (e.isActionKey
        || e.keyCode == KeyEvent.VK_ENTER
        || e.keyCode == KeyEvent.VK_ESCAPE
        || e.keyCode == KeyEvent.VK_BACK_SPACE
        || (e.keyCode >= KeyEvent.VK_A && e.keyCode <= KeyEvent.VK_Z && e.modifiersEx and KeyEvent.CTRL_DOWN_MASK != 0)) {
        handleKey(KeyStroke.getKeyStrokeForEvent(e))
      }
    }
  }

  class LafListener : LafManagerListener {
    override fun lookAndFeelChanged(source: LafManager) {
      if (VimPlugin.isNotEnabled()) return

      for (vimEditor in injector.editorGroup.getEditors()) {
        val editor = (vimEditor as IjVimEditor).editor
        val panel = tryGetInstance(editor) ?: continue
        IJSwingUtilities.updateComponentTreeUI(panel)
      }
    }
  }

  companion object {
    fun tryGetInstance(editor: Editor): OutputPanel? {
      return editor.vimMorePanel as OutputPanel?
    }

    fun createInstance(editor: Editor): OutputPanel {
      // This should be null, but let's check anyway
      var panel: OutputPanel? = tryGetInstance(editor)
      if (panel == null) {
        panel = OutputPanel(editor)
        editor.vimMorePanel = panel
      }
      return panel
    }

    /**
     * Allow showing the output panel for empty text
     *
     * Normally, the output panel is not shown for single line, empty text. This is sensible UX - no intrusive popup panel
     * showing nothing. However, it makes it very difficult for testing to verify that a command has correctly tried to
     * output an empty string, or to verify that a command correctly does not output anything. A missing output instead of
     * an empty output could be due to a bug or exception. This flag allows tests to ensure the behaviour is consistent.
     */
    @TestOnly
    internal var allowHideEmptyText = true
  }
}


private data class TextLine(val text: String, val color: Color?)

/**
 * A [LabelView] that breaks at character boundaries rather than word boundaries.
 *
 * [getMinimumSpan] returns the width of a single character so the row layout always attempts a
 * break, even when the text contains no spaces.
 * [getBreakWeight] signals that any character position on the X axis is a valid break point.
 * [breakView] finds the last character that completely fits within the available width using the
 * view's own glyph painter, so measurement is consistent with how the view renders itself.
 * [getPreferredSpan] is intentionally not overridden: the view reports its natural preferred width
 * and relies solely on the break machinery to constrain it to the space given by the parent row.
 */
private class CharacterBreakView(elem: javax.swing.text.Element) : LabelView(elem) {

  override fun getMinimumSpan(axis: Int): Float {
    if (axis != X_AXIS) return super.getMinimumSpan(axis)
    val p0 = startOffset
    val p1 = endOffset
    if (p0 >= p1) return 0f
    return glyphPainter.getSpan(this, p0, p0 + 1, null, 0f)
  }

  override fun getBreakWeight(axis: Int, pos: Float, len: Float): Int =
    if (axis == X_AXIS) GoodBreakWeight else BadBreakWeight

  override fun breakView(axis: Int, p0: Int, pos: Float, len: Float): View {
    if (axis != X_AXIS) return this
    val p1 = endOffset
    if (p0 >= p1) return this

    val painter = glyphPainter

    // If everything fits there is nothing to break
    if (painter.getSpan(this, p0, p1, null, pos) <= len) return this

    // Binary search for the rightmost character offset whose span still fits
    var lo = p0
    var hi = p1
    while (lo < hi - 1) {
      val mid = (lo + hi) / 2
      if (painter.getSpan(this, p0, mid, null, pos) <= len) lo = mid else hi = mid
    }

    return if (lo > p0) createFragment(p0, lo) else this
  }
}
