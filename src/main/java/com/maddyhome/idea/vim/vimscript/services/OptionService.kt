package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.option.OptionChangeListener
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

interface OptionService {

  /**
   * todo doc for each method
   */
  fun getOptionValue(scope: Scope, optionName: String, editor: Editor?, token: String = optionName): VimDataType

  fun setOptionValue(scope: Scope, optionName: String, value: VimDataType, editor: Editor?, token: String = optionName)

  fun isSet(scope: Scope, optionName: String, editor: Editor?, token: String = optionName): Boolean

  fun isDefault(scope: Scope, optionName: String, editor: Editor?, token: String = optionName): Boolean

  fun resetDefault(scope: Scope, optionName: String, editor: Editor?, token: String = optionName)

  fun resetAllOptions()

  /**
   * Checks if the option with given optionName is a toggleOption
   */
  fun isToggleOption(optionName: String): Boolean

  /**
   * Sets the option on (true)
   */
  fun setOption(scope: Scope, optionName: String, editor: Editor?, token: String = optionName)

  /**
   * Unsets the option (false)
   */
  fun unsetOption(scope: Scope, optionName: String, editor: Editor?, token: String = optionName)

  fun toggleOption(scope: Scope, optionName: String, editor: Editor?, token: String = optionName)

  fun showAllOptions(editor: Editor, scope: Scope, showIntro: Boolean)

  fun showChangedOptions(editor: Editor, scope: Scope, showIntro: Boolean)

  fun showOptions(editor: Editor, nameAndToken: Collection<Pair<String, String>>, scope: Scope, showIntro: Boolean)

  fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean = false)

  fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>)

  enum class Scope {
    LOCAL,
    GLOBAL
  }
}
