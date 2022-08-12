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
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.parseNumber
import com.maddyhome.idea.vim.vimscript.services.OptionService

abstract class VimOptionServiceBase : OptionService {
  private val localOptionsKey = Key<MutableMap<String, VimDataType>>("localOptions")

  private val logger = vimLogger<VimOptionServiceBase>()
  private val options = MultikeyMap(
    NumberOption(OptionConstants.maxmapdepthName, OptionConstants.maxmapdepthAlias, 20),
    NumberOption(OptionConstants.scrollName, OptionConstants.scrollAlias, 0),
    NumberOption(OptionConstants.scrolloffName, OptionConstants.scrolloffAlias, 0),
    NumberOption(OptionConstants.sidescrollName, OptionConstants.sidescrollAlias, 0),
    NumberOption(OptionConstants.sidescrolloffName, OptionConstants.sidescrolloffAlias, 0),
    ToggleOption(OptionConstants.digraphName, OptionConstants.digraphAlias, false),
    ToggleOption(OptionConstants.gdefaultName, OptionConstants.gdefaultAlias, false),
    ToggleOption(OptionConstants.hlsearchName, OptionConstants.hlsearchAlias, false),
    ToggleOption(OptionConstants.ideacopypreprocessName, OptionConstants.ideacopypreprocessAlias, false),
    ToggleOption(OptionConstants.ideastrictmodeName, OptionConstants.ideastrictmodeAlias, false),
    ToggleOption(OptionConstants.ideatracetimeName, OptionConstants.ideatracetimeAlias, false),
    ToggleOption(OptionConstants.ideaglobalmodeName, OptionConstants.ideaglobalmodeAlias, false),
    ToggleOption(OptionConstants.ignorecaseName, OptionConstants.ignorecaseAlias, false),
    ToggleOption(OptionConstants.incsearchName, OptionConstants.incsearchAlias, false),
    ToggleOption(OptionConstants.moreName, OptionConstants.moreAlias, true),
    ToggleOption(OptionConstants.numberName, OptionConstants.numberAlias, false),
    ToggleOption(OptionConstants.relativenumberName, OptionConstants.relativenumberAlias, false),
    ToggleOption(OptionConstants.showcmdName, OptionConstants.showcmdAlias, true),
    ToggleOption(OptionConstants.showmodeName, OptionConstants.showmodeAlias, false),
    ToggleOption(OptionConstants.smartcaseName, OptionConstants.smartcaseAlias, false),
    ToggleOption(OptionConstants.startoflineName, OptionConstants.startoflineAlias, true),
    ToggleOption(OptionConstants.timeoutName, OptionConstants.timeoutAlias, true),
    ToggleOption(OptionConstants.visualbellName, OptionConstants.visualbellAlias, false),
    ToggleOption(OptionConstants.wrapscanName, OptionConstants.wrapscanAlias, true),
    ToggleOption(OptionConstants.ideadelaymacroName, OptionConstants.ideadelaymacroAlias, false),
    ToggleOption(OptionConstants.trackactionidsName, OptionConstants.trackactionidsAlias, false),
    StringOption(OptionConstants.selectionName, OptionConstants.selectionAlias, "inclusive", isList = false, setOf("old", "inclusive", "exclusive")),
    StringOption(OptionConstants.shellName, OptionConstants.shellAlias, if (injector.systemInfoService.isWindows) "cmd.exe" else System.getenv("SHELL") ?: "sh"),
    StringOption(OptionConstants.shellxescapeName, OptionConstants.shellxescapeAlias, if (injector.systemInfoService.isWindows) "\"&|<>()@^" else "", isList = false),
    StringOption(OptionConstants.virtualeditName, OptionConstants.virtualeditAlias, "", isList = false, setOf("onemore", "block", "insert", "all")),
    StringOption(OptionConstants.viminfoName, OptionConstants.viminfoAlias, "'100,<50,s10,h", isList = true),
    StringOption(OptionConstants.nrformatsName, OptionConstants.nrformatsAlias, "hex", isList = true, setOf("octal", "hex", "alpha")),
    StringOption(OptionConstants.clipboardName, OptionConstants.clipboardAlias, "autoselect,exclude:cons\\|linux", isList = true),
    StringOption(
      OptionConstants.selectmodeName, OptionConstants.selectmodeAlias, "", isList = true,
      setOf(
        OptionConstants.selectmode_mouse, OptionConstants.selectmode_key, OptionConstants.selectmode_cmd, OptionConstants.selectmode_ideaselection
      )
    ),
    StringOption(
      OptionConstants.keymodelName, OptionConstants.keymodelAlias, "${OptionConstants.keymodel_continueselect},${OptionConstants.keymodel_stopselect}", isList = true,
      setOf(
        OptionConstants.keymodel_startsel, OptionConstants.keymodel_stopsel, OptionConstants.keymodel_stopselect, OptionConstants.keymodel_stopvisual, OptionConstants.keymodel_continueselect, OptionConstants.keymodel_continuevisual
      )
    ),
    StringOption(OptionConstants.lookupkeysName, OptionConstants.lookupkeysAlias, "<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>", isList = true),
    object : StringOption(OptionConstants.matchpairsName, OptionConstants.matchpairsAlias, "(:),{:},[:]", isList = true) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        for (v in split((value as VimString).value)) {
          if (!v.matches(Regex(".:."))) {
            throw ExException("E474: Invalid argument: $token")
          }
        }
      }
    },
    object : NumberOption(OptionConstants.scrolljumpName, OptionConstants.scrolljumpAlias, 1) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        if ((value as VimInt).value < -100) {
          throw ExException("E49: Invalid scroll size: $token")
        }
      }
    },
    object : NumberOption(OptionConstants.historyName, OptionConstants.historyAlias, 50) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        if ((value as VimInt).value < 0) {
          throw ExException("E487: Argument must be positive: $token")
        }
      }
    },
    object : NumberOption(OptionConstants.timeoutlenName, OptionConstants.timeoutlenAlias, 1000) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        if ((value as VimInt).value < 0) {
          throw ExException("E487: Argument must be positive: $token")
        }
      }
    },
    object : NumberOption(OptionConstants.undolevelsName, OptionConstants.undolevelsAlias, 1000) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        if ((value as VimInt).value < 0) {
          throw ExException("E487: Argument must be positive: $token")
        }
      }
    },
    object : NumberOption(OptionConstants.visualdelayName, OptionConstants.visualdelayAlias, 100) {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        if ((value as VimInt).value < 0) {
          throw ExException("E487: Argument must be positive: $token")
        }
      }
    },
    object : StringOption(OptionConstants.shellcmdflagName, OptionConstants.shellcmdflagAlias, "") {
      // default value changes if so does the "shell" option
      override fun getDefaultValue(): VimString {
        val shell = (getGlobalOptionValue(OptionConstants.shellName) as VimString).value
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell.contains("powershell") -> "-Command"
            injector.systemInfoService.isWindows && !shell.contains("sh") -> "/c"
            else -> "-c"
          }
        )
      }
    },
    object : StringOption(OptionConstants.shellxquoteName, OptionConstants.shellxquoteAlias, "") {
      // default value changes if so does the "shell" option
      override fun getDefaultValue(): VimString {
        val shell = (getGlobalOptionValue(OptionConstants.shellName) as VimString).value
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell == "cmd.exe" -> "("
            injector.systemInfoService.isWindows && shell.contains("sh") -> "\""
            else -> ""
          }
        )
      }
    },
    object : StringOption(OptionConstants.iskeywordName, OptionConstants.iskeywordAlias, "@,48-57,_", isList = true) {
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
          injector.messages.showStatusBarMessage("Failed to parse iskeyword option value")
        }
        return result ?: split(getDefaultValue().value)
      }
    },
    object : StringOption(
      OptionConstants.guicursorName, OptionConstants.guicursorAlias,
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

    ToggleOption(OptionConstants.experimentalapiName, OptionConstants.experimentalapiAlias, false),
    ToggleOption("closenotebooks", "closenotebooks", true),
  )
  private val globalValues = mutableMapOf<String, VimDataType>()

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
