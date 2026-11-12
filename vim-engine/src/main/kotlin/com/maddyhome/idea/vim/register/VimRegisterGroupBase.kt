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
import com.maddyhome.idea.vim.api.VirtualBufferKind
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.register.RegisterConstants.ALTERNATE_BUFFER_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.BLACK_HOLE_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.CLIPBOARD_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.CLIPBOARD_REGISTERS
import com.maddyhome.idea.vim.register.RegisterConstants.CURRENT_FILENAME_REGISTER
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
import java.util.EnumMap
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

  /**
   * What IdeaVim last wrote to each selection: copies made by `unnamed` and `unnamedplus` and the visual selection of
   * `autoselect` included, a write to PRIMARY that failed excluded. A selection keeps text only, so this tells the type
   * of text that is still there. The `+` and `*` registers in [myRegisters] are the registers as Vim sees them instead,
   * which neither a copy to the other selection nor `autoselect` changes.
   */
  private val lastWrittenToSelection = EnumMap<SystemSelection, Register>(SystemSelection::class.java)

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
    defaultRegisterChar =
      SystemRegisterPolicy.defaultRegister(injector.globalOptions().clipboard, isPrimaryRegisterSupported())
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
    lastWrittenToSelection.clear()
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
      val storage = storageRegister(register)
      myRegisters[storage] = Register(storage, copiedText, type)
      logger.debug { "register '$storage' contains: \"$copiedText\"" }
    } // Put the text in the specified register

    writeToSystemSelections(editor, context, copiedText, type, register, isRegisterSpecifiedExplicitly, isDelete)

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

    // Notified once for the whole operation, after every register above has been written. Note this
    // is deliberately not raised for the black hole register, which returns early, nor for
    // storeTextSpecial or `let @a = ...`, neither of which routes through here.
    injector.listenersNotifier.notifyRegisterStored(register, copiedText, type, isDelete)
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
      val normalized = if (editor.getVirtualBufferKind() == VirtualBufferKind.ControlCharsEditor) {
        EngineStringHelper.fromPrintableCharacters(text)
      } else text
      return storeTextInternal(editor, context, range, normalized, type, lastRegisterChar, isDelete)
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

  /** @see SystemRegisterPolicy.storageRegister */
  private fun storageRegister(register: Char): Char =
    SystemRegisterPolicy.storageRegister(register, isPrimaryRegisterSupported())

  /**
   * Reads the `+` or `*` register from the selection behind it. [myRegisters] isn't kept in sync with the selections,
   * so it only tells the type of text that is still there.
   *
   * @return the content of the selection, if available, otherwise null
   */
  private fun readSystemRegister(editor: VimEditor, context: ExecutionContext, r: Char): Register? {
    val storage = storageRegister(r)
    return when {
      storage == PRIMARY_REGISTER && isPrimaryRegisterSupported() -> readPrimaryRegister(editor, context)
      storage in CLIPBOARD_REGISTERS -> readClipboardRegister(editor, context, storage)
      else -> throw RuntimeException("Clipboard register expected, got $r")
    }
  }

  override fun isPrimaryRegisterSupported(): Boolean = injector.systemInfoService.hasPrimarySelection

  override fun getLastExplicitlyWrittenRegister(r: Char): Register? {
    require(CLIPBOARD_REGISTERS.contains(r.lowercaseChar())) { "Only clipboard registers are supported: got '$r'" }
    return myRegisters[storageRegister(r.lowercaseChar())]
  }

  /**
   * Writes [copiedText] to the selections [SystemRegisterPolicy] picks for [register]; nothing for a register that
   * isn't backed by one. If PRIMARY can't be written, CLIPBOARD gets the text instead.
   */
  private fun writeToSystemSelections(
    editor: VimEditor,
    context: ExecutionContext,
    copiedText: VimCopiedText,
    selectionType: SelectionType,
    register: Char,
    isExplicit: Boolean,
    isDelete: Boolean,
  ) {
    val selections = SystemRegisterPolicy.selectionsToWrite(
      register,
      isExplicit,
      isDelete,
      injector.globalOptions().clipboard,
      isPrimaryRegisterSupported(),
    )
    val written = Register(register, copiedText, selectionType)
    for (selection in selections) {
      when (selection) {
        SystemSelection.CLIPBOARD -> writeToClipboard(editor, context, written)
        SystemSelection.PRIMARY -> try {
          writeToPrimary(editor, context, written)
        } catch (e: Exception) {
          logger.warn("False positive X11 primary selection support")
          lastWrittenToSelection.remove(SystemSelection.PRIMARY)
          if (SystemSelection.CLIPBOARD !in selections) writeToClipboard(editor, context, written)
        }
      }
    }
  }

  private fun writeToClipboard(editor: VimEditor, context: ExecutionContext, written: Register) {
    injector.clipboardManager.setClipboardContent(editor, context, written.copiedText)
    lastWrittenToSelection[SystemSelection.CLIPBOARD] = written
  }

  /**
   * Only a write that took place is remembered: while IdeaVim owns PRIMARY, the `*` register is taken from this record
   * without reading PRIMARY, and a failed write must not pass for the text a previous write left there.
   */
  private fun writeToPrimary(editor: VimEditor, context: ExecutionContext, written: Register) {
    val isWritten = injector.clipboardManager.setPrimaryContent(editor, context, written.copiedText, written.type)
    if (isWritten) {
      lastWrittenToSelection[SystemSelection.PRIMARY] = written
    } else {
      lastWrittenToSelection.remove(SystemSelection.PRIMARY)
    }
  }

  /**
   * With `autoselect`, the visual selection goes to PRIMARY, see [PrimaryAutoselect]. It is not a yank: the `*` register
   * in [myRegisters] keeps the last yank, which `vp` with `unnamed` puts.
   */
  override fun onVisualSelectionChange(editor: VimEditor) {
    if (!isPrimaryRegisterSupported()) return
    val (text, selectionType) = PrimaryAutoselect.selectionToPublish(editor) ?: return
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    val selection = Register(PRIMARY_REGISTER, injector.clipboardManager.dumbCopiedText(text), selectionType)
    writeToPrimary(editor, context, selection)
  }

  /**
   * Resolves the `*` register, whose backing store is the windowing system's PRIMARY selection.
   *
   * PRIMARY holds text and nothing else, so the selection type has to be found elsewhere. Each source
   * below is less trustworthy than the one before it, ending in a guess made from the text alone.
   */
  private fun readPrimaryRegister(editor: VimEditor, context: ExecutionContext): Register? {
    return try {
      registerWeStillOwn() ?: registerFor(injector.clipboardManager.getPrimaryContent(editor, context))
    } catch (e: Exception) {
      logger.warn("False positive X11 primary selection support")
      readClipboardRegister(editor, context, CLIPBOARD_REGISTER)
    }
  }

  private fun registerWeStillOwn(): Register? {
    if (!injector.clipboardManager.isPrimaryOwned()) return null
    return lastWrittenToSelection[SystemSelection.PRIMARY]?.let { Register(PRIMARY_REGISTER, it.copiedText, it.type) }
  }

  private fun registerFor(selection: VimCopiedText?): Register? {
    if (selection == null || selection.text.isEmpty()) return registerForUnreadableSelection(selection)
    // The IDE republishes the editor selection to PRIMARY on every selection change, as untyped text, so ownership
    // is routinely lost to our own process with no foreign application involved. The text is still ours then.
    return recognise(PRIMARY_REGISTER, selection, lastWrittenToSelection[SystemSelection.PRIMARY])
  }

  /**
   * Wayland/XWayland clears PRIMARY on focus change, so an unreadable selection is far more likely to
   * be that than a deliberate clear — prefer the last value we held. A readable-but-empty selection is
   * still a register: reporting it as absent would drop the `*` row from `:registers` altogether.
   */
  private fun registerForUnreadableSelection(selection: VimCopiedText?): Register? =
    (lastWrittenToSelection[SystemSelection.PRIMARY] ?: myRegisters[PRIMARY_REGISTER])
      ?.let { Register(PRIMARY_REGISTER, it.copiedText, it.type) }
      ?: selection?.let { Register(PRIMARY_REGISTER, it, SelectionType.CHARACTER_WISE) }

  /**
   * The [name] register read from a selection holding [selection], with the type of [lastWritten], what IdeaVim last
   * wrote to that selection, if the selection still holds its text. [myRegisters] comes next, as it is what remains of
   * our writes after a restart. With neither, the type is guessed from the text: someone else put it there.
   */
  private fun recognise(name: Char, selection: VimCopiedText, lastWritten: Register?): Register =
    sequenceOf(lastWritten, myRegisters[name])
      .filterNotNull()
      .firstNotNullOfOrNull { recoverRegister(name, selection, it) }
      ?: Register(name, selection, guessSelectionType(selection.text))

  /**
   * Rebuilds the register from [ours] when the selection still holds its text.
   *
   * On an exact match our own copy is returned, keeping the transferable data collected at yank time.
   * On a stripped match only the *type* is taken from [ours]: the selection's text is what a paste
   * should insert, and substituting ours would hand back characters the selection never held.
   */
  private fun recoverRegister(name: Char, selection: VimCopiedText, ours: Register): Register? = when {
    selection.text == ours.text -> Register(name, ours.copiedText, ours.type)
    selection.isTailStrippedFormOf(ours.text) -> Register(name, selection, ours.type)
    else -> null
  }

  /**
   * Whether the selection can only be [ourText] with its tail eaten in transit — the trailing newline,
   * or that newline together with the whitespace before it. Something republishes our line reformatted
   * (the IDE reindents a pasted range, and a formatter drops trailing whitespace), and the type has to
   * survive that.
   *
   * Deliberately one-directional. Text carrying trailing whitespace of its own was not produced by
   * stripping, so it belongs to whoever put it there; comparing both sides trimmed would also collapse
   * every whitespace-only string to the same value and let any blank selection match any blank register.
   */
  private fun VimCopiedText.isTailStrippedFormOf(ourText: String): Boolean {
    if (text + "\n" == ourText) return true
    if (text != text.trimEnd()) return false
    return text == ourText.trimEnd()
  }

  /**
   * Reads CLIPBOARD into [storage], the register it is kept in. The clipboard is usually faithful, but it is re-read
   * from the X server once another application owns it, so the same lossy-tail tolerance as for PRIMARY applies.
   */
  private fun readClipboardRegister(editor: VimEditor, context: ExecutionContext, storage: Char): Register? {
    val clipboardData = injector.clipboardManager.getClipboardContent(editor, context) ?: return null
    return recognise(storage, clipboardData, lastWrittenToSelection[SystemSelection.CLIPBOARD])
  }

  override fun getRegister(editor: VimEditor, context: ExecutionContext, r: Char): Register? {
    var myR = r
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(myR)) {
      myR = Character.toLowerCase(myR)
    }

    if (r == CURRENT_FILENAME_REGISTER) {
      return getCurrentFileNameRegister(editor)
    }

    if (r == ALTERNATE_BUFFER_REGISTER) {
      return getAlternateBufferName(editor)
    }

    return if (myR in CLIPBOARD_REGISTERS) readSystemRegister(editor, context, myR) else myRegisters[myR]
  }

  override fun getRegisters(editor: VimEditor, context: ExecutionContext): List<Register> {
    val filteredRegisters = myRegisters.values.filterNot { CLIPBOARD_REGISTERS.contains(it.name) }.toMutableList()
    val clipboardRegisters = CLIPBOARD_REGISTERS
      .map { storageRegister(it) }
      .distinct()
      .mapNotNull { readSystemRegister(editor, context, it) }
    return (filteredRegisters + clipboardRegisters + listOfNotNull(
      getCurrentFileNameRegister(editor),
      getAlternateBufferName(editor)
    ))
      .sortedWith(Register.KeySorter)
  }

  override fun saveRegister(editor: VimEditor, context: ExecutionContext, r: Char, register: Register) {
    val myR = storageRegister(r.lowercaseChar())

    // Saving a register names it, as `:let @+ = …` does, so it is written as explicit
    writeToSystemSelections(editor, context, register.copiedText, register.type, myR, isExplicit = true, isDelete = false)
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

private fun getCurrentFileNameRegister(editor: VimEditor): Register? {
  val fileName = injector.file.bufferName(editor) ?: return null
  return Register(
    CURRENT_FILENAME_REGISTER,
    injector.clipboardManager.dumbCopiedText(fileName),
    SelectionType.CHARACTER_WISE
  )
}

private fun getAlternateBufferName(editor: VimEditor): Register? {
  val fileName = injector.file.alternateBufferName(editor) ?: return null
  return Register(
    ALTERNATE_BUFFER_REGISTER,
    injector.clipboardManager.dumbCopiedText(fileName),
    SelectionType.CHARACTER_WISE
  )
}
