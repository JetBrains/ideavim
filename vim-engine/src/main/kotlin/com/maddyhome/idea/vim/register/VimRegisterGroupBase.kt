package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import javax.swing.KeyStroke

abstract class VimRegisterGroupBase : VimRegisterGroup {

  @JvmField
  protected var recordRegister: Char = 0.toChar()

  @JvmField
  protected var recordList: MutableList<KeyStroke>? = null

  override fun isValid(reg: Char): Boolean = VALID_REGISTERS.indexOf(reg) != -1

  @JvmField
  val myRegisters = HashMap<Char, Register>()

  /**
   * Store which register the user wishes to work with.
   *
   * @param reg The register name
   * @return true if a valid register name, false if not
   */
  override fun selectRegister(reg: Char): Boolean {
    return if (isValid(reg)) {
      VimRegisterGroupBase.lastRegister = reg
      logger.debug { "register selected: $lastRegister" }

      true
    } else {
      false
    }
  }

  /**
   * Reset the selected register back to the default register.
   */
  override fun resetRegister() {
    VimRegisterGroupBase.lastRegister = defaultRegister
    logger.debug("Last register reset to default register")
  }

  override fun recordKeyStroke(key: KeyStroke) {
    val myRecordList = recordList
    if (recordRegister != 0.toChar() && myRecordList != null) {
      myRecordList.add(key)
    }
  }

  override fun isRegisterWritable(): Boolean {
    return READONLY_REGISTERS.indexOf(VimRegisterGroupBase.lastRegister) < 0
  }

  override fun resetRegisters() {
    VimRegisterGroupBase.defaultRegister = UNNAMED_REGISTER
    VimRegisterGroupBase.lastRegister = defaultRegister
    myRegisters.clear()
  }

  protected fun isSmallDeletionSpecialCase(editor: VimEditor): Boolean {
    val currentCommand = CommandState.getInstance(editor).executingCommand
    if (currentCommand != null) {
      val argument = currentCommand.argument
      if (argument != null) {
        val motionCommand = argument.motion
        val action = motionCommand.action
        return action.id == "VimMotionPercentOrMatchAction" ||
          action.id == "VimMotionSentencePreviousStartAction" ||
          action.id == "VimMotionSentenceNextStartAction" ||
          action.id == "VimMotionGotoFileMarkAction" ||
          action.id == "VimSearchEntryFwdAction" ||
          action.id == "VimSearchEntryRevAction" ||
          action.id == "VimSearchAgainNextAction" ||
          action.id == "VimSearchAgainPreviousAction" ||
          action.id == "VimMotionParagraphNextAction" ||
          action.id == "VimMotionParagraphPreviousAction"
      }
    }

    return false
  }

  fun storeTextInternal(
    editor: VimEditor, range: TextRange, text: String,
    type: SelectionType, register: Char, isDelete: Boolean,
  ): Boolean {
    // Null register doesn't get saved, but acts like it was
    if (VimRegisterGroupBase.lastRegister == BLACK_HOLE_REGISTER) return true

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
      if (start != -1) getTransferableData(editor, range, text) as List<Any> else ArrayList()
    val processedText = if (start != -1) preprocessText(editor, range, text, transferableData) else text
    logger.debug {
      val transferableClasses = transferableData.joinToString(",") { it.javaClass.name }
      "Copy to '$lastRegister' with transferable data: $transferableClasses"
    }
    if (Character.isUpperCase(register)) {
      val lreg = Character.toLowerCase(register)
      val r = myRegisters[lreg]
      // Append the text if the lowercase register existed
      if (r != null) {
        r.addTextAndResetTransferableData(processedText)
      } else {
        myRegisters[lreg] = Register(lreg, type, processedText, ArrayList(transferableData))
        logger.debug { "register '$register' contains: \"$processedText\"" }
      }// Set the text if the lowercase register didn't exist yet
    } else {
      myRegisters[register] = Register(register, type, processedText, ArrayList(transferableData))
      logger.debug { "register '$register' contains: \"$processedText\"" }
    }// Put the text in the specified register

    if (CLIPBOARD_REGISTERS.indexOf(register) >= 0) {
      injector.clipboardManager.setClipboardText(processedText, text, ArrayList(transferableData))
    }

    // Also add it to the unnamed register if the default wasn't specified
    if (register != UNNAMED_REGISTER && ".:/".indexOf(register) == -1) {
      myRegisters[UNNAMED_REGISTER] = Register(UNNAMED_REGISTER, type, processedText, ArrayList(transferableData))
      logger.debug { "register '$UNNAMED_REGISTER' contains: \"$processedText\"" }
    }

    if (isDelete) {
      val smallInlineDeletion =
        ((type === SelectionType.CHARACTER_WISE || type === SelectionType.BLOCK_WISE) && (editor.offsetToLogicalPosition(
          start
        ).line == editor.offsetToLogicalPosition(end).line))

      // Deletes go into numbered registers only if text is smaller than a line, register is used or it's a special case
      if (!smallInlineDeletion || register != defaultRegister || isSmallDeletionSpecialCase(editor)) {
        // Old 1 goes to 2, etc. Old 8 to 9, old 9 is lost
        var d = '8'
        while (d >= '1') {
          val t = myRegisters[d]
          if (t != null) {
            t.name = (d.code + 1).toChar()
            myRegisters[(d.code + 1).toChar()] = t
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
    }// Yanks also go to register 0 if the default register was used

    if (start != -1) {
      injector.markGroup.setChangeMarks(editor, TextRange(start, end))
    }

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
    range: TextRange,
    type: SelectionType,
    isDelete: Boolean,
  ): Boolean {
    if (isRegisterWritable()) {
      var text = injector.engineEditorHelper.getText(editor, range)

      if (type == SelectionType.LINE_WISE && (text.isEmpty() || text[text.length - 1] != '\n')) {
        // Linewise selection always has a new line at the end
        text += '\n'.toString()
      }

      return storeTextInternal(editor, range, text, type, VimRegisterGroupBase.lastRegister, isDelete)
    }

    return false
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
   * @return          True if the text is stored, false if the passed register is not supported
   */
  override fun storeTextSpecial(register: Char, text: String): Boolean {
    if (READONLY_REGISTERS.indexOf(register) == -1 && register != LAST_SEARCH_REGISTER && register != UNNAMED_REGISTER) {
      return false
    }
    myRegisters[register] = Register(register, SelectionType.CHARACTER_WISE, text, ArrayList())
    logger.debug { "register '$register' contains: \"$text\"" }
    return true
  }

  private fun guessSelectionType(text: String): SelectionType {
    return if (text.endsWith("\n")) SelectionType.LINE_WISE else SelectionType.CHARACTER_WISE
  }

  protected fun refreshClipboardRegister(r: Char): Register? {
    val clipboardData = injector.clipboardManager.getClipboardTextAndTransferableData() ?: return null
    val currentRegister = myRegisters[r]
    val text = clipboardData.first
    val transferableData = clipboardData.second?.toMutableList()
    if (currentRegister != null && text == currentRegister.text) {
      return currentRegister
    }
    return transferableData?.let { Register(r, guessSelectionType(text), text, it) }
  }

  override fun getRegister(r: Char): Register? {
    var myR = r
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(myR)) {
      myR = Character.toLowerCase(myR)
    }
    return if (CLIPBOARD_REGISTERS.indexOf(myR) >= 0) refreshClipboardRegister(myR) else myRegisters[myR]
  }

  override fun getRegisters(): List<Register> {
    val res = ArrayList(myRegisters.values)
    for (i in CLIPBOARD_REGISTERS.indices) {
      val r = CLIPBOARD_REGISTERS[i]
      val register = refreshClipboardRegister(r)
      if (register != null) {
        res.add(register)
      }
    }
    res.sortWith(Register.KeySorter)
    return res
  }

  override fun saveRegister(r: Char, register: Register) {
    var myR = r
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(myR)) {
      myR = Character.toLowerCase(myR)
    }
    if (CLIPBOARD_REGISTERS.indexOf(myR) >= 0) {
      val text = register.text
      val rawText = register.rawText
      if (text != null && rawText != null) {
        injector
          .clipboardManager
          .setClipboardText(text, rawText, ArrayList(register.transferableData))
      }
    }
    myRegisters[myR] = register
  }

  override fun startRecording(editor: VimEditor, register: Char): Boolean {
    return if (RECORDABLE_REGISTERS.indexOf(register) != -1) {
      CommandState.getInstance(editor).isRecording = true
      recordRegister = register
      recordList = ArrayList()
      true
    } else {
      false
    }
  }

  /**
   * Get the last register selected by the user
   *
   * @return The register, null if no such register
   */
  override val lastRegister: Register?
    get() = getRegister(VimRegisterGroupBase.lastRegister)

  override fun getPlaybackRegister(r: Char): Register? {
    return if (PLAYBACK_REGISTERS.indexOf(r) != 0) getRegister(r) else null
  }

  override fun recordText(text: String) {
    val myRecordList = recordList
    if (recordRegister != 0.toChar() && myRecordList != null) {
      myRecordList.addAll(injector.parser.stringToKeys(text))
    }
  }

  override fun setKeys(register: Char, keys: List<KeyStroke>) {
    myRegisters[register] = Register(register, SelectionType.CHARACTER_WISE, keys.toMutableList())
  }

  override fun setKeys(register: Char, keys: List<KeyStroke>, type: SelectionType) {
    myRegisters[register] = Register(register, type, keys.toMutableList())
  }

  override fun finishRecording(editor: VimEditor) {
    if (recordRegister != 0.toChar()) {
      var reg: Register? = null
      if (Character.isUpperCase(recordRegister)) {
        reg = getRegister(recordRegister)
      }

      val myRecordList = recordList
      if (myRecordList != null) {
        if (reg == null) {
          reg = Register(Character.toLowerCase(recordRegister), SelectionType.CHARACTER_WISE, myRecordList)
          myRegisters[Character.toLowerCase(recordRegister)] = reg
        } else {
          reg.addKeys(myRecordList)
        }
      }
      CommandState.getInstance(editor).isRecording = false
    }

    recordRegister = 0.toChar()
  }

  /**
   * Gets the last register name selected by the user
   *
   * @return The register name
   */
  override val currentRegister: Char
    get() = VimRegisterGroupBase.lastRegister

  override val defaultRegister: Char
    get() = VimRegisterGroupBase.defaultRegister

  companion object {
    const val UNNAMED_REGISTER = '"'
    const val LAST_SEARCH_REGISTER = '/'        // IdeaVim does not support writing to this register
    const val LAST_COMMAND_REGISTER = ':'
    const val LAST_INSERTED_TEXT_REGISTER = '.'
    const val SMALL_DELETION_REGISTER = '-'
    const val BLACK_HOLE_REGISTER = '_'
    const val ALTERNATE_BUFFER_REGISTER = '#'  // Not supported
    const val EXPRESSION_BUFFER_REGISTER = '='
    const val CURRENT_FILENAME_REGISTER = '%'  // Not supported
    const val CLIPBOARD_REGISTERS = "*+"
    const val NUMBERED_REGISTERS = "0123456789"
    const val NAMED_REGISTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    const val WRITABLE_REGISTERS = (NUMBERED_REGISTERS + NAMED_REGISTERS + CLIPBOARD_REGISTERS
      + SMALL_DELETION_REGISTER + BLACK_HOLE_REGISTER + UNNAMED_REGISTER + LAST_SEARCH_REGISTER)

    const val READONLY_REGISTERS = (""
      + CURRENT_FILENAME_REGISTER + LAST_COMMAND_REGISTER + LAST_INSERTED_TEXT_REGISTER + ALTERNATE_BUFFER_REGISTER
      + EXPRESSION_BUFFER_REGISTER) // Expression buffer is not actually readonly

    const val RECORDABLE_REGISTERS = NUMBERED_REGISTERS + NAMED_REGISTERS + UNNAMED_REGISTER
    const val PLAYBACK_REGISTERS =
      RECORDABLE_REGISTERS + UNNAMED_REGISTER + CLIPBOARD_REGISTERS + LAST_INSERTED_TEXT_REGISTER
    const val VALID_REGISTERS = WRITABLE_REGISTERS + READONLY_REGISTERS

    @JvmField
    var defaultRegister = UNNAMED_REGISTER

    @JvmField
    var lastRegister = defaultRegister

    val logger = vimLogger<VimRegisterGroupBase>()
  }
}
