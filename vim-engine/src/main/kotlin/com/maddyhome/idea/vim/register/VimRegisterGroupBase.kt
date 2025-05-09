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
import com.maddyhome.idea.vim.common.VimCopiedText
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

    val copiedText =
      if (start != -1) { // FIXME: so, we had invalid ranges all the time?.. I've never handled such cases
        injector.clipboardManager.collectCopiedText(editor, context, range, text)
      } else {
        injector.clipboardManager.dumbCopiedText(text)
      }
    logger.debug { "Copy to '$lastRegisterChar' with copied text: $copiedText" }
    // If this is an uppercase register, we need to append the text to the corresponding lowercase register
    if (Character.isUpperCase(register)) {
      val lreg = Character.toLowerCase(register)
      val r = myRegisters[lreg]
      // Append the text if the lowercase register existed
      if (r != null) {
        myRegisters[lreg] = r.addText(copiedText.text)
      } else {
        myRegisters[lreg] = Register(lreg, copiedText, type)
        logger.debug { "register '$register' contains: \"$copiedText\"" }
      } // Set the text if the lowercase register didn't exist yet
    } else {
      myRegisters[register] = Register(register, copiedText, type)
      logger.debug { "register '$register' contains: \"$copiedText\"" }
    } // Put the text in the specified register

    if (register == CLIPBOARD_REGISTER) {
      injector.clipboardManager.setClipboardContent(editor, context, copiedText)
      if (!isRegisterSpecifiedExplicitly && !isDelete && isPrimaryRegisterSupported() && OptionConstants.clipboard_unnamedplus in injector.globalOptions().clipboard) {
        injector.clipboardManager.setPrimaryContent(editor, context, copiedText)
      }
    }
    if (register == PRIMARY_REGISTER) {
      if (isPrimaryRegisterSupported()) {
        injector.clipboardManager.setPrimaryContent(editor, context, copiedText)
        if (!isRegisterSpecifiedExplicitly && !isDelete && OptionConstants.clipboard_unnamed in injector.globalOptions().clipboard) {
          injector.clipboardManager.setClipboardContent(editor, context, copiedText)
        }
      } else {
        injector.clipboardManager.setClipboardContent(editor, context, copiedText)
      }
    }

    // Also add it to the unnamed register if the default wasn't specified
    if (register != UNNAMED_REGISTER && ".:/".indexOf(register) == -1) {
      myRegisters[UNNAMED_REGISTER] = Register(UNNAMED_REGISTER, copiedText, type)
      logger.debug { "register '$UNNAMED_REGISTER' contains: \"$copiedText\"" }
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
            myRegisters[incName] = Register(incName, t.copiedText, t.type)
          }
          d--
        }
        myRegisters['1'] = Register('1', copiedText, type)
      }

      // Deletes smaller than one line and without specified register go the the "-" register
      if (smallInlineDeletion && register == defaultRegister) {
        myRegisters[SMALL_DELETION_REGISTER] =
          Register(SMALL_DELETION_REGISTER, copiedText, type)
      }
    } else if (register == defaultRegister) {
      myRegisters['0'] = Register('0', copiedText, type)
      logger.debug { "register '0' contains: \"$copiedText\"" }
    } // Yanks also go to register 0 if the default register was used
    return true
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
    myRegisters[register] = Register(
      register,
      injector.clipboardManager.dumbCopiedText(text),
      SelectionType.CHARACTER_WISE
    ) // TODO why transferable data is not collected?
    logger.debug { "register '$register' contains: \"$text\"" }
    return true
  }

  @Deprecated("Please use com.maddyhome.idea.vim.register.VimRegisterGroup#getRegister(com.maddyhome.idea.vim.api.VimEditor, com.maddyhome.idea.vim.api.ExecutionContext, char)")
  override fun getRegister(r: Char): Register? {
    val dummyEditor = injector.fallbackWindow
    val dummyContext = injector.executionContextManager.getEditorExecutionContext(dummyEditor)
    return getRegister(dummyEditor, dummyContext, r)
  }

  override fun storeText(
    editor: VimEditor,
    context: ExecutionContext,
    register: Char,
    text: String,
    selectionType: SelectionType,
  ): Boolean {
    if (!WRITABLE_REGISTERS.contains(register)) {
      return false
    }
    logger.debug { "register '$register' contains: \"$text\"" }
    val oldRegister = getRegister(editor, context, register.lowercaseChar())
    val newRegister = if (register.isUpperCase() && oldRegister != null) {
      oldRegister.addText(text)
    } else {
      Register(
        register,
        injector.clipboardManager.dumbCopiedText(text),
        selectionType
      ) // FIXME why don't we collect transferable data?
    }
    saveRegister(editor, context, register, newRegister)
    if (register == '/') {
      injector.searchGroup.lastSearchPattern = text // todo we should not have this field if we have the "/" register
    }
    return true
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

  private fun setSystemPrimaryRegisterText(editor: VimEditor, context: ExecutionContext, copiedText: VimCopiedText) {
    logger.trace("Setting text: $copiedText to primary selection...")
    if (isPrimaryRegisterSupported()) {
      try {
        injector.clipboardManager.setPrimaryContent(editor, context, copiedText)
      } catch (e: Exception) {
        logger.warn("False positive X11 primary selection support")
        logger.trace("Setting text to primary selection failed. Setting it to clipboard selection instead")
        setSystemClipboardRegisterText(editor, context, copiedText)
      }
    } else {
      logger.trace("X11 primary selection is not supporting. Setting clipboard selection instead")
      setSystemClipboardRegisterText(editor, context, copiedText)
    }
  }

  private fun setSystemClipboardRegisterText(editor: VimEditor, context: ExecutionContext, copiedText: VimCopiedText) {
    injector.clipboardManager.setClipboardContent(editor, context, copiedText)
  }

  private fun refreshPrimaryRegister(editor: VimEditor, context: ExecutionContext): Register? {
    logger.trace("Syncing cached primary selection value..")
    if (!isPrimaryRegisterSupported()) {
      logger.trace("X11 primary selection is not supported. Syncing clipboard selection..")
      return refreshClipboardRegister(editor, context)
    }
    try {
      val clipboardData = injector.clipboardManager.getPrimaryContent(editor, context) ?: return null
      val currentRegister = myRegisters[PRIMARY_REGISTER]
      if (currentRegister != null && clipboardData.text == currentRegister.text) {
        return currentRegister
      }
      return Register(PRIMARY_REGISTER, clipboardData, guessSelectionType(clipboardData.text))
    } catch (e: Exception) {
      logger.warn("False positive X11 primary selection support")
      logger.trace("Syncing primary selection failed. Syncing clipboard selection instead")
      return refreshClipboardRegister(editor, context)
    }
  }

  private fun refreshClipboardRegister(editor: VimEditor, context: ExecutionContext): Register? {
    // for some reason non-X systems use PRIMARY_REGISTER as a clipboard storage
    val systemAwareClipboardRegister = if (isPrimaryRegisterSupported()) CLIPBOARD_REGISTER else PRIMARY_REGISTER

    val clipboardData = injector.clipboardManager.getClipboardContent(editor, context) ?: return null
    val currentRegister = myRegisters[systemAwareClipboardRegister]
    if (currentRegister != null && clipboardData.text == currentRegister.text) {
      return currentRegister
    }
    return Register(systemAwareClipboardRegister, clipboardData, guessSelectionType(clipboardData.text))
  }

  override fun getRegister(editor: VimEditor, context: ExecutionContext, r: Char): Register? {
    var myR = r
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(myR)) {
      myR = Character.toLowerCase(myR)
    }
    return if (CLIPBOARD_REGISTERS.indexOf(myR) >= 0) refreshClipboardRegister(
      editor,
      context,
      myR
    ) else myRegisters[myR]
  }

  override fun getRegisters(editor: VimEditor, context: ExecutionContext): List<Register> {
    val filteredRegisters = myRegisters.values.filterNot { CLIPBOARD_REGISTERS.contains(it.name) }.toMutableList()
    val clipboardRegisters = CLIPBOARD_REGISTERS
      .filterNot { it == CLIPBOARD_REGISTER && !isPrimaryRegisterSupported() } // for some reason non-X systems use PRIMARY_REGISTER as a clipboard storage
      .mapNotNull { refreshClipboardRegister(editor, context, it) }

    return (filteredRegisters + clipboardRegisters).sortedWith(Register.KeySorter)
  }

  override fun saveRegister(editor: VimEditor, context: ExecutionContext, r: Char, register: Register) {
    var myR = if (Character.isUpperCase(r)) Character.toLowerCase(r) else r

    if (CLIPBOARD_REGISTERS.indexOf(myR) >= 0) {
      when (myR) {
        CLIPBOARD_REGISTER -> {
          if (!isPrimaryRegisterSupported()) {
            // it looks wrong, but for some reason non-X systems use the * register to store the clipboard content
            myR = PRIMARY_REGISTER
          }
          setSystemClipboardRegisterText(editor, context, register.copiedText)
        }

        PRIMARY_REGISTER -> {
          setSystemPrimaryRegisterText(editor, context, register.copiedText)
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
