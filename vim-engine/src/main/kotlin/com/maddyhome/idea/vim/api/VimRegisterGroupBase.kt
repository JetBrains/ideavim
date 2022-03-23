package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import javax.swing.KeyStroke

abstract class VimRegisterGroupBase : VimRegisterGroup {

  @JvmField
  protected var recordRegister: Char = 0.toChar()
  @JvmField
  protected var recordList: MutableList<KeyStroke>? = null

  override fun isValid(reg: Char): Boolean = VALID_REGISTERS.indexOf(reg) == -1

  /**
   * Store which register the user wishes to work with.
   *
   * @param reg The register name
   * @return true if a valid register name, false if not
   */
  override fun selectRegister(reg: Char): Boolean {
    return if (isValid(reg)) {
      lastRegister = reg
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
    lastRegister = defaultRegister
    logger.debug("Last register reset to default register")
  }

  override fun recordKeyStroke(key: KeyStroke) {
    val myRecordList = recordList
    if (recordRegister != 0.toChar() && myRecordList != null) {
      myRecordList.add(key)
    }
  }

  /**
   * Gets the last register name selected by the user
   *
   * @return The register name
   */
  override val currentRegister: Char
    get() = lastRegister

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
