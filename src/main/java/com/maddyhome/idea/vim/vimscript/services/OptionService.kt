package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.options.Option
import com.maddyhome.idea.vim.vimscript.model.options.OptionChangeListener

interface OptionService {

  /**
   * todo doc for each method
   */
  fun getGlobalOptionValue(optionName: String, token: String = optionName): VimDataType {
    return getOptionValue(Scope.GLOBAL, optionName, null, token)
  }

  fun getLocalOptionValue(optionName: String, editor: Editor?, token: String = optionName): VimDataType {
    return getOptionValue(Scope.LOCAL, optionName, editor, token)
  }

  fun getOptionValue(scope: Scope, optionName: String, editor: Editor?, token: String = optionName): VimDataType

  fun setGlobalOptionValue(optionName: String, value: VimDataType, token: String = optionName) {
    setOptionValue(Scope.GLOBAL, optionName, value, null, token)
  }

  fun setLocalOptionValue(optionName: String, value: VimDataType, editor: Editor?, token: String = optionName) {
    setOptionValue(Scope.LOCAL, optionName, value, editor, token)
  }

  fun setOptionValue(scope: Scope, optionName: String, value: VimDataType, editor: Editor?, token: String = optionName)

  fun appendValue(scope: Scope, optionName: String, value: String, editor: Editor?, token: String = optionName)

  fun prependValue(scope: Scope, optionName: String, value: String, editor: Editor?, token: String = optionName)

  fun removeValue(scope: Scope, optionName: String, value: String, editor: Editor?, token: String = optionName)

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

  fun addOption(option: Option<out VimDataType>)

  fun removeOption(optionName: String)

  // todo better generics
  fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean = false)

  fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>)

  enum class Scope {
    LOCAL,
    GLOBAL
  }
}
