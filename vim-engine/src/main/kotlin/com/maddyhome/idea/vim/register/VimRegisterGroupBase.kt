/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.register.RegisterConstants.BLACK_HOLE_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.CLIPBOARD_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.CLIPBOARD_REGISTERS
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_SEARCH_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.PLAYBACK_REGISTERS
import com.maddyhome.idea.vim.register.RegisterConstants.PRIMARY_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.READONLY_REGISTERS
import com.maddyhome.idea.vim.register.RegisterConstants.RECORDABLE_REGISTERS
import com.maddyhome.idea.vim.register.RegisterConstants.SMALL_DELETION_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.UNNAMED_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.VALID_REGISTERS
import com.maddyhome.idea.vim.register.RegisterConstants.WRITABLE_REGISTERS
import com.maddyhome.idea.vim.state.mode.SelectionType
import javax.swing.KeyStroke

abstract class VimRegisterGroupBase : VimRegisterGroup {

  override val isRecording: Boolean
    get() = recordRegister != null

  override var recordRegister: Char? = null
    set(value) {
      field = value
      if (value != null) {
        injector.listenersNotifier.notifyMacroRecordingStarted()
      } else {
        injector.listenersNotifier.notifyMacroRecordingFinished()
      }
    }

  @JvmField
  protected var recordList: MutableList<KeyStroke>? = null

  @JvmField
  protected val myRegisters: java.util.HashMap<Char, Register> = HashMap()

  @JvmField
  protected var defaultRegisterChar: Char = UNNAMED_REGISTER

  override var lastRegisterChar: Char = defaultRegisterChar
  override var isRegisterSpecifiedExplicitly: Boolean = false

  /**
   * Gets the last register name selected by the user
   *
   * @return The register name
   */
  override val currentRegister: Char
    get() = lastRegisterChar

  override val defaultRegister: Char
    get() = defaultRegisterChar

  override fun getLastRegister(editor: VimEditor, context: ExecutionContext): Register? {
    return getRegister(editor, context, lastRegisterChar)
  }

  /**
   * Get the last register selected by the user
   *
   * @return The register, null if no such register
   */
  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#getLastRegister(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext)")
  override val lastRegister: Register?
    get() = getRegister(lastRegisterChar)

  private val onClipboardChanged: () -> Unit = {
    val clipboardOptionValue = injector.globalOptions().clipboard
    defaultRegisterChar = when {
      "unnamedplus" in clipboardOptionValue -> {
        if (isPrimaryRegisterSupported()) {
          CLIPBOARD_REGISTER
        } else {
          PRIMARY_REGISTER // for some reason non-X systems use PRIMARY_REGISTER as a clipboard storage
        }
      }
      "unnamed" in clipboardOptionValue -> PRIMARY_REGISTER
      else -> UNNAMED_REGISTER
    }
    lastRegisterChar = defaultRegisterChar
  }

  fun initClipboardOptionListener() {
    injector.optionGroup.addGlobalOptionChangeListener(Options.clipboard, onClipboardChanged)
    onClipboardChanged()
  }

  override fun isValid(reg: Char): Boolean = VALID_REGISTERS.indexOf(reg) != -1

  /**
   * Store which register the user wishes to work with.
   *
   * @param reg The register name
   * @return true if a valid register name, false if not
   */
  override fun selectRegister(reg: Char): Boolean {
    return if (isValid(reg)) {
      isRegisterSpecifiedExplicitly = true
      lastRegisterChar = reg
      logger.debug { "register selected: $lastRegisterChar" }
      true
    } else {
      false
    }
  }

  /**
   * Reset the selected register back to the default register.
   */
  override fun resetRegister() {
    isRegisterSpecifiedExplicitly = false
    lastRegisterChar = defaultRegister
    logger.debug("Last register reset to default register")
  }

  override fun recordKeyStroke(key: KeyStroke) {
    val myRecordList = recordList
    if (isRecording && myRecordList != null) {
      myRecordList.add(key)
    }
  }

  override fun isRegisterWritable(): Boolean = isRegisterWritable(lastRegisterChar)
  override fun isRegisterWritable(reg: Char): Boolean = READONLY_REGISTERS.indexOf(reg) < 0

  override fun resetRegisters() {
    isRegisterSpecifiedExplicitly = false
    defaultRegisterChar = UNNAMED_REGISTER
    lastRegisterChar = defaultRegister
    myRegisters.clear()
  }

  private fun isSmallDeletionSpecialCase(): Boolean {
    val currentCommand = injector.vimState.executingCommand
    if (currentCommand != null) {
      val argument = currentCommand.argument
      if (argument is Argument.Motion) {
        val action = argument.motion
        return action.id == "VimMotionPercentOrMatchAction" ||
          action.id == "VimMotionSentencePreviousStartAction" ||
          action.id == "VimMotionSentenceNextStartAction" ||
          action.id == "VimMotionGotoFileMarkAction" ||
          action.id == "VimProcessExEntryAction" ||
          action.id == "VimSearchAgainNextAction" ||
          action.id == "VimSearchAgainPreviousAction" ||
          action.id == "VimMotionParagraphNextAction" ||
          action.id == "VimMotionParagraphPreviousAction"
      }
    }

    return false
  }

  fun storeTextInternal(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    text: String,
    type: SelectionType,
    register: Char,
    isDelete: Boolean,
  ): Boolean {
    // Null register doesn't get saved, but acts like it was
    if (lastRegisterChar == BLACK_HOLE_REGISTER) return true

    var start = range.startOffset
    var end = range.endOffset

    if (isDelete && start == end) {
      return true
    }

    // Normalize the start and end
    if (start > end) {
      val t = start
      start = end
      end = t
    }

    // If this is an uppercase register, we need to append the text to the corresponding lowercase register
    val transferableData: List<Any> =
      if (start != -1) injector.clipboardManager.getTransferableData(editor, range) else ArrayList()
    val processedText =
      if (start != -1) injector.clipboardManager.preprocessText(editor, range, text, transferableData) else text
    logger.debug {
      val transferableClasses = transferableData.joinToString(",") { it.javaClass.name }
      "Copy to '$lastRegister' with transferable data: $transferableClasses"
    }
    if (Character.isUpperCase(register)) {
      val lreg = Character.toLowerCase(register)
      val r = myRegisters[lreg]
      // Append the text if the lowercase register existed
      if (r != null) {
        myRegisters[lreg] = r.addText(processedText)
      } else {
        myRegisters[lreg] = Register(lreg, type, processedText, ArrayList(transferableData))
        logger.debug { "register '$register' contains: \"$processedText\"" }
      } // Set the text if the lowercase register didn't exist yet
    } else {
      myRegisters[register] = Register(register, type, processedText, ArrayList(transferableData))
      logger.debug { "register '$register' contains: \"$processedText\"" }
    } // Put the text in the specified register

    if (register == CLIPBOARD_REGISTER) {
      injector.clipboardManager.setClipboardText(processedText, text, ArrayList(transferableData))
      if (!isRegisterSpecifiedExplicitly && !isDelete && isPrimaryRegisterSupported() && OptionConstants.clipboard_unnamedplus in injector.globalOptions().clipboard) {
        injector.clipboardManager.setPrimaryText(processedText, text, ArrayList(transferableData))
      }
    }
    if (register == PRIMARY_REGISTER) {
      if (isPrimaryRegisterSupported()) {
        injector.clipboardManager.setPrimaryText(processedText, text, ArrayList(transferableData))
        if (!isRegisterSpecifiedExplicitly && !isDelete && OptionConstants.clipboard_unnamed in injector.globalOptions().clipboard) {
          injector.clipboardManager.setClipboardText(processedText, text, ArrayList(transferableData))
        }
      } else {
        injector.clipboardManager.setClipboardText(processedText, text, ArrayList(transferableData))
      }
    }

    // Also add it to the unnamed register if the default wasn't specified
    if (register != UNNAMED_REGISTER && ".:/".indexOf(register) == -1) {
      myRegisters[UNNAMED_REGISTER] = Register(UNNAMED_REGISTER, type, processedText, ArrayList(transferableData))
      logger.debug { "register '$UNNAMED_REGISTER' contains: \"$processedText\"" }
    }

    if (isDelete) {
      val smallInlineDeletion =
        (
          (type === SelectionType.CHARACTER_WISE || type === SelectionType.BLOCK_WISE) && (
            editor.offsetToBufferPosition(
              start,
            ).line == editor.offsetToBufferPosition(end).line
            )
          )

      // Deletes go into numbered registers only if text is smaller than a line, register is used or it's a special case
      if (!smallInlineDeletion && register == defaultRegister || isSmallDeletionSpecialCase()) {
        // Old 1 goes to 2, etc. Old 8 to 9, old 9 is lost
        var d = '8'
        while (d >= '1') {
          val t = myRegisters[d]
          if (t != null) {
            val incName = (d.code + 1).toChar()
            myRegisters[incName] = Register(incName, t.text, t.type, t.transferableData)
          }
          d--
        }
        myRegisters['1'] = Register('1', type, processedText, ArrayList(transferableData))
      }

      // Deletes smaller than one line and without specified register go the the "-" register
      if (smallInlineDeletion && register == defaultRegister) {
        myRegisters[SMALL_DELETION_REGISTER] =
          Register(SMALL_DELETION_REGISTER, type, processedText, ArrayList(transferableData))
      }
    } else if (register == defaultRegister) {
      myRegisters['0'] = Register('0', type, processedText, ArrayList(transferableData))
      logger.debug { "register '0' contains: \"$processedText\"" }
    } // Yanks also go to register 0 if the default register was used
    return true
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#storeText(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, com.maddyhome.idea.vim.api.ImmutableVimCaret, com.maddyhome.idea.vim.common.TextRange, com.maddyhome.idea.vim.state.mode.SelectionType, boolean)")
  override fun storeText(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    range: TextRange,
    type: SelectionType,
    isDelete: Boolean
  ): Boolean {
    return storeText(editor, injector.executionContextManager.getEditorExecutionContext(editor), caret, range, type, isDelete)
  }

  /**
   * Store text into the last register.
   *
   * @param editor   The editor to get the text from
   * @param range    The range of the text to store
   * @param type     The type of copy
   * @param isDelete is from a delete
   * @return true if able to store the text into the register, false if not
   */
  override fun storeText(
    editor: VimEditor,
    context: ExecutionContext,
    caret: ImmutableVimCaret,
    range: TextRange,
    type: SelectionType,
    isDelete: Boolean,
  ): Boolean {
    if (isRegisterWritable()) {
      val text = preprocessTextBeforeStoring(editor.getText(range), type)
      return storeTextInternal(editor, context, range, text, type, lastRegisterChar, isDelete)
    }

    return false
  }

  protected fun preprocessTextBeforeStoring(text: String, type: SelectionType): String {
    if (type == SelectionType.LINE_WISE && (text.isEmpty() || text[text.length - 1] != '\n')) {
      // Linewise selection always has a new line at the end
      return text + '\n'
    }
    return text
  }

  /**
   * Stores text, character wise, in the given special register
   *
   *
   * This method is intended to support writing to registers when the text cannot be yanked from an editor. This is
   * expected to only be used to update the search and command registers. It will not update named registers.
   *
   *
   * While this method allows setting the unnamed register, this should only be done from tests, and only when it's
   * not possible to yank or cut from the fixture editor. This method will skip additional text processing, and won't
   * update other registers such as the small delete register or reorder the numbered registers. It is much more
   * preferable to yank from the fixture editor.
   *
   * @param register  The register to use for storing the text. Cannot be a normal text register
   * @param text      The text to store, without further processing
   * @return True if the text is stored, false if the passed register is not supported
   */
  override fun storeTextSpecial(register: Char, text: String): Boolean {
    if (READONLY_REGISTERS.indexOf(register) == -1 && register != LAST_SEARCH_REGISTER && register != UNNAMED_REGISTER) {
      return false
    }
    myRegisters[register] = Register(register, SelectionType.CHARACTER_WISE, text, ArrayList())
    logger.debug { "register '$register' contains: \"$text\"" }
    return true
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#getRegister(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char)")
  override fun getRegister(r: Char): Register? {
    val dummyEditor = injector.fallbackWindow
    val dummyContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    return getRegister(dummyEditor, dummyContext, r)
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#storeText(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char, java.lang.String, com.maddyhome.idea.vim.state.mode.SelectionType)")
  override fun storeText(register: Char, text: String, selectionType: SelectionType): Boolean {
    val dummyEditor = injector.fallbackWindow
    val dummyContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    return storeText(dummyEditor, dummyContext, register, text, selectionType)
  }

  override fun storeText(
    editor: VimEditor,
    context: ExecutionContext,
    register: Char,
    text: String,
    selectionType: SelectionType
  ): Boolean {
    if (!WRITABLE_REGISTERS.contains(register)) {
      return false
    }
    logger.debug { "register '$register' contains: \"$text\"" }
    val textToStore = if (register.isUpperCase()) {
      (getRegister(editor, context, register.lowercaseChar())?.text ?: "") + text
    } else {
      text
    }
    val reg = Register(register, selectionType, textToStore, ArrayList())
    saveRegister(editor, context, register, reg)
    if (register == '/') {
      injector.searchGroup.lastSearchPattern = text // todo we should not have this field if we have the "/" register
    }
    return true
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#storeText(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char, java.lang.String)")
  override fun storeText(register: Char, text: String): Boolean {
    val dummyEditor = injector.fallbackWindow
    val dummyContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    return storeText(dummyEditor, dummyContext, register, text)
  }

  override fun storeText(editor: VimEditor, context: ExecutionContext, register: Char, text: String): Boolean {
    return storeText(editor, context, register, text, SelectionType.CHARACTER_WISE)
  }

  private fun guessSelectionType(text: String): SelectionType {
    return if (text.endsWith("\n")) SelectionType.LINE_WISE else SelectionType.CHARACTER_WISE
  }

  /**
   * The contents of [myRegisters] are not synchronized with the primary or clipboard selections.
   * In the following method, we update `myRegisters.get(r)` with the content of the corresponding system selection.
   *
   * @param r - the register character corresponding to either the primary selection (*) or clipboard selection (+)
   * @return the content of the selection, if available, otherwise null
   */
  private fun refreshClipboardRegister(editor: VimEditor, context: ExecutionContext, r: Char): Register? {
    return when (r) {
      PRIMARY_REGISTER -> refreshPrimaryRegister(editor, context)
      CLIPBOARD_REGISTER -> refreshClipboardRegister(editor, context)
      else -> throw RuntimeException("Clipboard register expected, got $r")
    }
  }

  override fun isPrimaryRegisterSupported(): Boolean {
    return System.getenv("DISPLAY") != null && injector.systemInfoService.isXWindow
  }

  private fun setSystemPrimaryRegisterText(editor: VimEditor, context: ExecutionContext, text: String, transferableData: List<Any>) {
    logger.trace("Setting text: $text to primary selection...")
    if (isPrimaryRegisterSupported()) {
      try {
        injector.clipboardManager.setPrimaryText(editor, context, text, transferableData)
      } catch (e: Exception) {
        logger.warn("False positive X11 primary selection support")
        logger.trace("Setting text to primary selection failed. Setting it to clipboard selection instead")
        setSystemClipboardRegisterText(editor, context, text, transferableData)
      }
    } else {
      logger.trace("X11 primary selection is not supporting. Setting clipboard selection instead")
      setSystemClipboardRegisterText(editor, context, text, transferableData)
    }
  }

  private fun setSystemClipboardRegisterText(editor: VimEditor, context: ExecutionContext, text: String, transferableData: List<Any>) {
    injector.clipboardManager.setClipboardText(editor, context, text, transferableData)
  }

  private fun refreshPrimaryRegister(editor: VimEditor, context: ExecutionContext): Register? {
    logger.trace("Syncing cached primary selection value..")
    if (!isPrimaryRegisterSupported()) {
      logger.trace("X11 primary selection is not supported. Syncing clipboard selection..")
      return refreshClipboardRegister(editor, context)
    }
    try {
      val clipboardData = injector.clipboardManager.getPrimaryTextAndTransferableData(editor, context) ?: return null
      val currentRegister = myRegisters[PRIMARY_REGISTER]
      val text = clipboardData.first
      val transferableData = clipboardData.second?.toMutableList()
      if (currentRegister != null && text == currentRegister.text) {
        return currentRegister
      }
      return transferableData?.let { Register(PRIMARY_REGISTER, guessSelectionType(text), text, it) }
    } catch (e: Exception) {
      logger.warn("False positive X11 primary selection support")
      logger.trace("Syncing primary selection failed. Syncing clipboard selection instead")
      return refreshClipboardRegister(editor, context)
    }
  }

  private fun refreshClipboardRegister(editor: VimEditor, context: ExecutionContext): Register? {
    // for some reason non-X systems use PRIMARY_REGISTER as a clipboard storage
    val systemAwareClipboardRegister = if (isPrimaryRegisterSupported()) CLIPBOARD_REGISTER else PRIMARY_REGISTER

    val clipboardData = injector.clipboardManager.getClipboardTextAndTransferableData(editor, context) ?: return null
    val currentRegister = myRegisters[systemAwareClipboardRegister]
    val text = clipboardData.first
    val transferableData = clipboardData.second?.toMutableList()
    if (currentRegister != null && text == currentRegister.text) {
      return currentRegister
    }
    return transferableData?.let { Register(systemAwareClipboardRegister, guessSelectionType(text), text, it) }
  }

  override fun getRegister(editor: VimEditor, context: ExecutionContext, r: Char): Register? {
    var myR = r
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(myR)) {
      myR = Character.toLowerCase(myR)
    }
    return if (CLIPBOARD_REGISTERS.indexOf(myR) >= 0) refreshClipboardRegister(editor, context, myR) else myRegisters[myR]
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#getRegisters(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext)")
  override fun getRegisters(): List<Register> {
    val dummyEditor = injector.fallbackWindow
    val dummyContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    return getRegisters(dummyEditor, dummyContext)
  }

  override fun getRegisters(editor: VimEditor, context: ExecutionContext): List<Register> {
    val filteredRegisters = myRegisters.values.filterNot { CLIPBOARD_REGISTERS.contains(it.name) }.toMutableList()
    val clipboardRegisters = CLIPBOARD_REGISTERS
      .filterNot { it == CLIPBOARD_REGISTER && !isPrimaryRegisterSupported() } // for some reason non-X systems use PRIMARY_REGISTER as a clipboard storage
      .mapNotNull { refreshClipboardRegister(editor, context, it) }

    return (filteredRegisters + clipboardRegisters).sortedWith(Register.KeySorter)
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#saveRegister(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char, com.maddyhome.idea.vim.register.Register)")
  override fun saveRegister(r: Char, register: Register) {
    val dummyEditor = injector.fallbackWindow
    val dummyContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    saveRegister(dummyEditor, dummyContext, r, register)
  }

  override fun saveRegister(editor: VimEditor, context: ExecutionContext, r: Char, register: Register) {
    var myR = if (Character.isUpperCase(r)) Character.toLowerCase(r) else r
    val text = register.text

    if (CLIPBOARD_REGISTERS.indexOf(myR) >= 0) {
      when (myR) {
        CLIPBOARD_REGISTER -> {
          if (!isPrimaryRegisterSupported()) {
            // it looks wrong, but for some reason non-X systems use the * register to store the clipboard content
            myR = PRIMARY_REGISTER
          }
          setSystemClipboardRegisterText(editor, context, text, ArrayList(register.transferableData))
        }
        PRIMARY_REGISTER -> {
          setSystemPrimaryRegisterText(editor, context, text, ArrayList(register.transferableData))
        }
      }
    }
    myRegisters[myR] = register
  }

  override fun startRecording(register: Char): Boolean {
    return if (RECORDABLE_REGISTERS.indexOf(register) != -1) {
      recordRegister = register
      recordList = ArrayList()
      true
    } else {
      false
    }
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#getPlaybackRegister(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char)")
  override fun getPlaybackRegister(r: Char): Register? {
    val dummyEditor = injector.fallbackWindow
    val dummyContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    return getPlaybackRegister(dummyEditor, dummyContext, r)
  }

  override fun getPlaybackRegister(editor: VimEditor, context: ExecutionContext, r: Char): Register? {
    return if (PLAYBACK_REGISTERS.indexOf(r) != 0) getRegister(editor, context, r) else null
  }

  override fun recordText(text: String) {
    val myRecordList = recordList
    if (isRecording && myRecordList != null) {
      myRecordList.addAll(injector.parser.stringToKeys(text))
    }
  }

  override fun setKeys(register: Char, keys: List<KeyStroke>) {
    myRegisters[register] = Register(register, SelectionType.CHARACTER_WISE, keys.toMutableList())
  }

  override fun setKeys(register: Char, keys: List<KeyStroke>, type: SelectionType) {
    myRegisters[register] = Register(register, type, keys.toMutableList())
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#finishRecording(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext)")
  override fun finishRecording() {
    val dummyEditor = injector.fallbackWindow
    val dummyContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    finishRecording(dummyEditor, dummyContext)
  }

  override fun finishRecording(editor: VimEditor, context: ExecutionContext) {
    val register = recordRegister
    if (register != null) {
      var reg: Register? = null
      if (Character.isUpperCase(register)) {
        reg = getRegister(editor, context, register)
      }

      val myRecordList = recordList
      if (myRecordList != null) {
        if (reg == null) {
          reg = Register(Character.toLowerCase(register), SelectionType.CHARACTER_WISE, myRecordList)
          myRegisters[Character.toLowerCase(register)] = reg
        } else {
          myRegisters[reg.name.lowercaseChar()] = reg.addText(injector.parser.toPrintableString(myRecordList))
        }
      }
    }
    recordRegister = null
  }

  companion object {
    val logger: VimLogger = vimLogger<VimRegisterGroupBase>()
  }

  override fun getCurrentRegisterForMulticaret(): Char {
    return if (isRegisterSpecifiedExplicitly || !isSystemClipboard(currentRegister)) {
      currentRegister
    } else {
      '"'
    }
  }

  override fun isSystemClipboard(register: Char): Boolean {
    return register == '+' || register == '*'
  }
}
