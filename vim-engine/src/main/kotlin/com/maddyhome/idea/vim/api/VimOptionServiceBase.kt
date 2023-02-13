/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.option.NumberOption
import com.maddyhome.idea.vim.option.ToggleOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.UnsignedNumberOption
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.parseNumber
import com.maddyhome.idea.vim.vimscript.services.OptionService
import com.maddyhome.idea.vim.vimscript.services.OptionValueAccessor

abstract class VimOptionServiceBase : OptionService {

  private lateinit var globalOptions: OptionValueAccessor

  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("localOptions")

  private val logger = vimLogger<VimOptionServiceBase>()
  private val globalValues = mutableMapOf<String, VimDataType>()
  private val options = MultikeyMap(
    // Simple options, sorted by name
    StringOption(OptionConstants.clipboard, OptionConstants.clipboardAlias, "autoselect,exclude:cons\\|linux", isList = true),
    ToggleOption(OptionConstants.digraph, "dg", false),
    ToggleOption(OptionConstants.gdefault, "gd", false),
    UnsignedNumberOption(OptionConstants.history, "hi", 50),
    ToggleOption(OptionConstants.hlsearch, "hls", false),
    ToggleOption(OptionConstants.ignorecase, "ic", false),
    ToggleOption(OptionConstants.incsearch, "is", false),
    NumberOption(OptionConstants.maxmapdepth, "mmd", 20),
    ToggleOption(OptionConstants.more, "more", true),
    StringOption(OptionConstants.nrformats, "nf", "hex", isList = true, setOf("octal", "hex", "alpha")),
    ToggleOption(OptionConstants.number, "nu", false),
    ToggleOption(OptionConstants.relativenumber, "rnu", false),
    NumberOption(OptionConstants.scroll, "scr", 0),
    NumberOption(OptionConstants.scrolloff, "so", 0),
    StringOption(OptionConstants.selection, "sel", "inclusive", isList = false, setOf("old", "inclusive", "exclusive")),
    StringOption(OptionConstants.shell, "sh", if (injector.systemInfoService.isWindows) "cmd.exe" else System.getenv("SHELL") ?: "sh"),
    StringOption(OptionConstants.shellxescape, "sxe", if (injector.systemInfoService.isWindows) "\"&|<>()@^" else "", isList = false),
    ToggleOption(OptionConstants.showcmd, "sc", true),
    ToggleOption(OptionConstants.showmode, "smd", true),
    NumberOption(OptionConstants.sidescroll, "ss", 0),
    NumberOption(OptionConstants.sidescrolloff, "siso", 0),
    ToggleOption(OptionConstants.smartcase, "scs", false),
    ToggleOption(OptionConstants.startofline, "sol", true),
    ToggleOption(OptionConstants.timeout, "to", true),
    UnsignedNumberOption(OptionConstants.timeoutlen, "tm", 1000),
    UnsignedNumberOption(OptionConstants.undolevels, "ul", 1000),
    StringOption(OptionConstants.viminfo, "vi", "'100,<50,s10,h", isList = true),
    StringOption(OptionConstants.virtualedit, "ve", "", isList = false, setOf("onemore", "block", "insert", "all")),
    ToggleOption(OptionConstants.visualbell, "vb", false),
    StringOption(OptionConstants.whichwrap, "ww", "b,s", true, setOf("b", "s", "h", "l", "<", ">", "~", "[", "]")),
    ToggleOption(OptionConstants.wrapscan, "ws", true),

    // Options with longer defaults or additional validation, sorted by name
    object : StringOption(
      OptionConstants.guicursor, "gcr",
      "n-v-c:block-Cursor/lCursor," +
        "ve:ver35-Cursor," +
        "o:hor50-Cursor," +
        "i-ci:ver25-Cursor/lCursor," +
        "r-cr:hor20-Cursor/lCursor," +
        "sm:block-Cursor-blinkwait175-blinkoff150-blinkon175",
      isList = true
    ) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        val valueAsString = (value as VimString).value
        valueAsString.split(",").forEach { GuiCursorOptionHelper.convertToken(it) }
      }
    },
    object : StringOption(OptionConstants.iskeyword, "isk", "@,48-57,_", isList = true) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        if (KeywordOptionHelper.isValueInvalid((value as VimString).value)) {
          throw ExException("E474: Invalid argument: $token")
        }
      }

      override fun split(value: String): List<String> {
        val result = KeywordOptionHelper.parseValues(value)
        if (result == null) {
          logger.error("KeywordOptionHelper failed to parse $value")
          injector.messages.indicateError()
          injector.messages.showStatusBarMessage(editor = null, "Failed to parse iskeyword option value")
        }
        return result ?: split(getDefaultValue().value)
      }
    },
    StringOption(
      OptionConstants.keymodel,
      "km",
      "${OptionConstants.keymodel_continueselect},${OptionConstants.keymodel_stopselect}",
      isList = true,
      setOf(
        OptionConstants.keymodel_startsel,
        OptionConstants.keymodel_stopsel,
        OptionConstants.keymodel_stopselect,
        OptionConstants.keymodel_stopvisual,
        OptionConstants.keymodel_continueselect,
        OptionConstants.keymodel_continuevisual
      )
    ),
    object : StringOption(OptionConstants.matchpairs, "mps", "(:),{:},[:]", isList = true) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        for (v in split((value as VimString).value)) {
          if (!v.matches(Regex(".:."))) {
            throw ExException("E474: Invalid argument: $token")
          }
        }
      }
    },
    object : NumberOption(OptionConstants.scrolljump, "sj", 1) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        if ((value as VimInt).value < -100) {
          throw ExException("E49: Invalid scroll size: $token")
        }
      }
    },
    StringOption(
      OptionConstants.selectmode, "slm", "", isList = true,
      setOf(
        OptionConstants.selectmode_mouse,
        OptionConstants.selectmode_key,
        OptionConstants.selectmode_cmd,
        OptionConstants.selectmode_ideaselection
      )
    ),
    object : StringOption(OptionConstants.shellcmdflag, "shcf", "") {
      // default value changes if so does the "shell" option
      override fun getDefaultValue(): VimString {
        val shell = (getGlobalOptionValue(OptionConstants.shell) as VimString).value
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell.contains("powershell") -> "-Command"
            injector.systemInfoService.isWindows && !shell.contains("sh") -> "/c"
            else -> "-c"
          }
        )
      }
    },
    object : StringOption(OptionConstants.shellxquote, "sxq", "") {
      // default value changes if so does the "shell" option
      override fun getDefaultValue(): VimString {
        val shell = (getGlobalOptionValue(OptionConstants.shell) as VimString).value
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell == "cmd.exe" -> "("
            injector.systemInfoService.isWindows && shell.contains("sh") -> "\""
            else -> ""
          }
        )
      }
    },

    // IdeaVim specific options. Put any editor/IDE specific options in IjVimOptionService
    ToggleOption(OptionConstants.experimentalapi, OptionConstants.experimentalapi, false),
    ToggleOption(OptionConstants.ideaglobalmode, OptionConstants.ideaglobalmode, false),
    ToggleOption(OptionConstants.ideastrictmode, OptionConstants.ideastrictmode, false),
    ToggleOption(OptionConstants.ideatracetime, OptionConstants.ideatracetime, false),
  )

  override fun setOptionValue(scope: OptionScope, optionName: String, value: VimDataType, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    option.checkIfValueValid(value, token)
    val oldValue = getOptionValue(scope, optionName)
    when (scope) {
      is OptionScope.LOCAL -> {
        setLocalOptionValue(option.name, value, scope.editor)
      }
      is OptionScope.GLOBAL -> setGlobalOptionValue(option.name, value)
    }
    option.onChanged(scope, oldValue)
  }

  override fun contains(scope: OptionScope, optionName: String, value: String): Boolean {
    val option = options.get(optionName) as? StringOption ?: return false
    return value in option.split(getOptionValue(scope, optionName, optionName).asString())
  }

  override fun getValues(scope: OptionScope, optionName: String): List<String>? {
    val option = options.get(optionName)
    if (option !is StringOption) return null
    return option.split(getOptionValue(scope, optionName).asString())
  }

  override fun setOptionValue(scope: OptionScope, optionName: String, value: String, token: String) {
    val vimValue: VimDataType = castToVimDataType(value, optionName, token)
    setOptionValue(scope, optionName, vimValue, token)
  }

  private fun setGlobalOptionValue(optionName: String, value: VimDataType) {
    globalValues[optionName] = value
  }

  private fun getLocalOptions(editor: VimEditor): MutableMap<String, VimDataType> {
    val storageService = injector.vimStorageService
    val storedData = storageService.getDataFromEditor(editor, localOptionsKey)
    if (storedData != null) {
      return storedData
    }
    val localOptions = mutableMapOf<String, VimDataType>()
    storageService.putDataToEditor(editor, localOptionsKey, localOptions)
    return localOptions
  }

  private fun setLocalOptionValue(optionName: String, value: VimDataType, editor: VimEditor) {
    val localOptions = getLocalOptions(editor)
    localOptions[optionName] = value
  }

  private fun getGlobalOptionValue(optionName: String): VimDataType? {
    val option = options.get(optionName) ?: return null
    return globalValues[option.name] ?: options.get(option.name)?.getDefaultValue()
  }

  private fun getLocalOptionValue(optionName: String, editor: VimEditor): VimDataType? {
    val localOptions = getLocalOptions(editor)
    return localOptions[optionName] ?: getGlobalOptionValue(optionName)
  }

  /**
   * Sets the option on (true)
   */
  override fun setOption(scope: OptionScope, optionName: String, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    if (option !is ToggleOption) {
      throw ExException("E474: Invalid argument: $token")
    }
    setOptionValue(scope, optionName, VimInt.ONE, token)
  }

  override fun setOption(scope: OptionService.Scope, optionName: String, token: String) {
    val newScope = when (scope) {
      is OptionService.Scope.GLOBAL -> OptionScope.GLOBAL
      is OptionService.Scope.LOCAL -> OptionScope.LOCAL(scope.editor)
    }
    this.setOption(newScope, optionName, token)
  }

  /**
   * Unsets the option (false)
   */
  override fun unsetOption(scope: OptionScope, optionName: String, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    if (option !is ToggleOption) {
      throw ExException("E474: Invalid argument: $token")
    }
    setOptionValue(scope, optionName, VimInt.ZERO, token)
  }

  override fun toggleOption(scope: OptionScope, optionName: String, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    if (option !is ToggleOption) {
      throw ExException("E474: Invalid argument: $token")
    }
    val optionValue = getOptionValue(scope, optionName)
    if (optionValue.asBoolean()) {
      setOptionValue(scope, optionName, VimInt.ZERO, token)
    } else {
      setOptionValue(scope, optionName, VimInt.ONE, token)
    }
  }

  override fun isDefault(scope: OptionScope, optionName: String, token: String): Boolean {
    val defaultValue = options.get(optionName)?.getDefaultValue() ?: throw ExException("E518: Unknown option: $token")
    return getOptionValue(scope, optionName) == defaultValue
  }

  override fun resetDefault(scope: OptionScope, optionName: String, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    setOptionValue(scope, optionName, option.getDefaultValue(), token)
  }

  override fun isSet(scope: OptionScope, optionName: String, token: String): Boolean {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    return option is ToggleOption && getOptionValue(scope, optionName).asBoolean()
  }

  override fun getOptionValue(scope: OptionScope, optionName: String, token: String): VimDataType {
    return when (scope) {
      is OptionScope.LOCAL -> {
        getLocalOptionValue(optionName, scope.editor)
      }
      is OptionScope.GLOBAL -> getGlobalOptionValue(optionName)
    } ?: throw ExException("E518: Unknown option: $token")
  }

  override fun appendValue(scope: OptionScope, optionName: String, value: String, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    val currentValue = getOptionValue(scope, optionName)
    val newValue = option.getValueIfAppend(currentValue, value, token)
    setOptionValue(scope, optionName, newValue, token)
  }

  override fun prependValue(scope: OptionScope, optionName: String, value: String, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    val currentValue = getOptionValue(scope, optionName)
    val newValue = option.getValueIfPrepend(currentValue, value, token)
    setOptionValue(scope, optionName, newValue, token)
  }

  override fun removeValue(scope: OptionScope, optionName: String, value: String, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    val currentValue = getOptionValue(scope, optionName)
    val newValue = option.getValueIfRemove(currentValue, value, token)
    setOptionValue(scope, optionName, newValue, token)
  }

  override fun resetAllOptions() {
    globalValues.clear()
    injector.editorGroup.localEditors()
      .forEach { injector.vimStorageService.getDataFromEditor(it, localOptionsKey)?.clear() }
  }

  override fun isToggleOption(optionName: String): Boolean {
    return options.get(optionName) is ToggleOption
  }

  override fun getOptions(): Set<String> {
    return options.primaryKeys
  }

  override fun getAbbrevs(): Set<String> {
    return options.secondaryKeys
  }

  override fun addOption(option: Option<out VimDataType>) {
    options.put(option.name, option.abbrev, option)
  }

  override fun removeOption(optionName: String) {
    options.remove(optionName)
  }

  override fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean) {
    options.get(optionName)!!.addOptionChangeListener(listener)
    if (executeOnAdd) {
      listener.processGlobalValueChange(getGlobalOptionValue(optionName))
    }
  }

  override fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>) {
    options.get(optionName)!!.removeOptionChangeListener(listener)
  }

  override fun getOptionByNameOrAbbr(key: String): Option<out VimDataType>? {
    return options.get(key)
  }

  override fun getValueAccessor(editor: VimEditor?): OptionValueAccessor {
    return if (editor == null) {
      if (!::globalOptions.isInitialized) {
        globalOptions = OptionValueAccessor(this, OptionScope.GLOBAL)
      }
      globalOptions
    } else {
      // Maybe cache in editor's user data?
      OptionValueAccessor(this, OptionScope.LOCAL(editor))
    }
  }

  private fun castToVimDataType(value: String, optionName: String, token: String): VimDataType {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    return when (option) {
      is NumberOption -> VimInt(parseNumber(value) ?: throw ExException("E521: Number required after =: $token"))
      is ToggleOption -> throw ExException("E474: Invalid argument: $token")
      is StringOption -> VimString(value)
      /**
       * COMPATIBILITY-LAYER: New branch
       * Please see: https://jb.gg/zo8n0r
       */
      else -> error("")
    }
  }
}

private class MultikeyMap(vararg entries: Option<out VimDataType>) {
  private val primaryKeyStorage: MutableMap<String, Option<out VimDataType>> = mutableMapOf()
  private val secondaryKeyStorage: MutableMap<String, Option<out VimDataType>> = mutableMapOf()

  init {
    for (entry in entries) {
      primaryKeyStorage[entry.name] = entry
      secondaryKeyStorage[entry.abbrev] = entry
    }
  }

  fun put(key1: String, key2: String, value: Option<out VimDataType>) {
    primaryKeyStorage[key1] = value
    secondaryKeyStorage[key2] = value
  }

  fun get(key: String): Option<out VimDataType>? {
    return primaryKeyStorage[key] ?: secondaryKeyStorage[key]
  }

  fun remove(key: String) {
    val option = primaryKeyStorage[key] ?: secondaryKeyStorage[key]
    primaryKeyStorage.values.remove(option)
    secondaryKeyStorage.values.remove(option)
  }

  fun contains(key: String): Boolean {
    return primaryKeyStorage.containsKey(key) || secondaryKeyStorage.containsKey(key)
  }

  val primaryKeys get() = primaryKeyStorage.keys
  val secondaryKeys get() = secondaryKeyStorage.keys
}
