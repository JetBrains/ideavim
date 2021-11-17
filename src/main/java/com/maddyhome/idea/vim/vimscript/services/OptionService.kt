package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.options.Option
import com.maddyhome.idea.vim.vimscript.model.options.OptionChangeListener

interface OptionService {

  /**
   * todo doc for each method
   */
  fun getOptionValue(scope: Scope, optionName: String, token: String = optionName): VimDataType

  fun setOptionValue(scope: Scope, optionName: String, value: VimDataType, token: String = optionName)

  fun appendValue(scope: Scope, optionName: String, value: String, token: String = optionName)

  fun prependValue(scope: Scope, optionName: String, value: String, token: String = optionName)

  fun removeValue(scope: Scope, optionName: String, value: String, token: String = optionName)

  fun isSet(scope: Scope, optionName: String, token: String = optionName): Boolean

  fun isDefault(scope: Scope, optionName: String, token: String = optionName): Boolean

  fun resetDefault(scope: Scope, optionName: String, token: String = optionName)

  fun resetAllOptions()

  /**
   * Checks if the option with given optionName is a toggleOption
   */
  fun isToggleOption(optionName: String): Boolean

  /**
   * Sets the option on (true)
   */
  fun setOption(scope: Scope, optionName: String, token: String = optionName)

  /**
   * Unsets the option (false)
   */
  fun unsetOption(scope: Scope, optionName: String, token: String = optionName)

  fun toggleOption(scope: Scope, optionName: String, token: String = optionName)

  fun getOptions(): Set<String>

  fun getAbbrevs(): Set<String>

  fun addOption(option: Option<out VimDataType>)

  fun removeOption(optionName: String)

  // todo better generics
  fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean = false)

  fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>)

  sealed class Scope {
    object GLOBAL : Scope()
    class LOCAL(val editor: Editor) : Scope()
  }
}
