package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.option.OptionChangeListener
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.parseNumber
import com.maddyhome.idea.vim.vimscript.model.options.NumberOption
import com.maddyhome.idea.vim.vimscript.model.options.Option
import com.maddyhome.idea.vim.vimscript.model.options.StringOption
import com.maddyhome.idea.vim.vimscript.model.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.vimscript.model.options.helpers.KeywordOptionHelper
import kotlin.math.ceil
import kotlin.math.min

internal object OptionServiceImpl : OptionService {

  // todo use me please :(
  private val logger = logger<OptionServiceImpl>()
  private val options = MultikeyMap(
    listOf(
      NumberOption("scroll", "scr", 0),
      NumberOption("scrolloff", "so", 0),
      NumberOption("sidescroll", "ss", 0),
      NumberOption("sidescrolloff", "siso", 0),
      ToggleOption("digraph", "dg", false),
      ToggleOption("gdefault", "gd", false),
      ToggleOption("hlsearch", "hls", false),
      ToggleOption("ideajoin", "ideajoin", false),
      ToggleOption("ideamarks", "ideamarks", true),
      ToggleOption("ideastrictmode", "ideastrictmode", false),
      ToggleOption("ignorecase", "ic", false),
      ToggleOption("incsearch", "is", false),
      ToggleOption("more", "more", true),
      ToggleOption("number", "nu", false),
      ToggleOption("relativenumber", "rnu", false),
      ToggleOption("showcmd", "sc", true),
      ToggleOption("showmode", "smd", false),
      ToggleOption("smartcase", "scs", false),
      ToggleOption("startofline", "sol", true),
      ToggleOption("timeout", "to", true),
      ToggleOption("visualbell", "vb", false),
      ToggleOption("wrapscan", "ws", true),
      StringOption("ide", "ide", ApplicationNamesInfo.getInstance().fullProductNameWithEdition),
      StringOption("idearefactormode", "idearefactormode", "select", isList = false, setOf("keep", "select", "visual")),
      StringOption("ideastatusicon", "ideastatusicon", "enabled", isList = false, setOf("enabled", "gray", "disabled")),
      StringOption("ideawrite", "ideawrite", "all", isList = false, setOf("all", "file")),
      StringOption("selection", "sel", "inclusive", isList = false, setOf("old", "inclusive", "exclusive")),
      StringOption("shell", "sh", if (SystemInfo.isWindows) "cmd.exe" else System.getenv("SHELL") ?: "sh"),
      StringOption("shellxescape", "sxe", if (SystemInfo.isWindows) "\"&|<>()@^" else "", isList = false),
      StringOption("virtualedit", "ve", "", isList = false, setOf("onemore", "block", "insert", "all")),
      StringOption("viminfo", "vi", "'100,<50,s10,h", isList = true),
      StringOption("nrformats", "nf", "hex", isList = true, setOf("octal", "hex", "alpha")),
      StringOption("clipboard", "cb", "ideaput,autoselect,exclude:cons\\|linux", isList = true),
      StringOption("selectmode", "slm", "", isList = true, setOf("mouse", "key", "cmd", "ideaselection")),
      StringOption("ideavimsupport", "ideavimsupport", "dialog", isList = true, setOf("dialog", "singleline", "dialoglegacy")),
      StringOption("keymodel", "km", "continueselect,stopselect", isList = true, setOf("startsel", "stopsel", "stopselect", "stopvisual", "continueselect", "continuevisual")),
      StringOption("lookupkeys", "lookupkeys", "<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>", isList = true),
      object : StringOption("matchpairs", "mps", "(:),{:},[:]", isList = true) {
        override fun checkIfValueValid(value: VimDataType, token: String) {
          super.checkIfValueValid(value, token)
          for (v in (value as VimString).value.split(",")) {
            if (!v.matches(Regex(".:."))) {
              throw ExException("E474: Invalid argument: $token")
            }
          }
        }
      },
      object : NumberOption("scrolljump", "sj", 1) {
        override fun checkIfValueValid(value: VimDataType, token: String) {
          super.checkIfValueValid(value, token)
          if ((value as VimInt).value < -100) {
            throw ExException("E49: Invalid scroll size: $token")
          }
        }
      },
      object : NumberOption("history", "hi", 50) {
        override fun checkIfValueValid(value: VimDataType, token: String) {
          super.checkIfValueValid(value, token)
          if ((value as VimInt).value < 0) {
            throw ExException("E487: Argument must be positive: $token")
          }
        }
      },
      object : NumberOption("timeoutlen", "tm", 1000) {
        override fun checkIfValueValid(value: VimDataType, token: String) {
          super.checkIfValueValid(value, token)
          if ((value as VimInt).value < 0) {
            throw ExException("E487: Argument must be positive: $token")
          }
        }
      },
      object : NumberOption("undolevels", "ul", 1000) {
        override fun checkIfValueValid(value: VimDataType, token: String) {
          super.checkIfValueValid(value, token)
          if ((value as VimInt).value < 0) {
            throw ExException("E487: Argument must be positive: $token")
          }
        }
      },
      object : NumberOption("visualdelay", "visualdelay", 100) {
        override fun checkIfValueValid(value: VimDataType, token: String) {
          super.checkIfValueValid(value, token)
          if ((value as VimInt).value < 0) {
            throw ExException("E487: Argument must be positive: $token")
          }
        }
      },
      object : StringOption("shellcmdflag", "shcf", "") {
        // default value changes if so does the "shell" option
        override fun getDefaultValue(): VimString {
          val shell = (getGlobalOptionValue("shell") as VimString).value
          return VimString(
            if (SystemInfo.isWindows && !shell.contains("sh"))
              "/c"
            else
              "-c"
          )
        }
      },
      object : StringOption("shellxquote", "sxq", "") {
        // default value changes if so does the "shell" option
        override fun getDefaultValue(): VimString {
          val shell = (getGlobalOptionValue("shell") as VimString).value
          return VimString(
            when {
              SystemInfo.isWindows && shell == "cmd.exe" -> "("
              SystemInfo.isWindows && shell.contains("sh") -> "\""
              else -> ""
            }
          )
        }
      },
      object : StringOption("iskeyword", "isk", "@,48-57,_", isList = true) {
        override fun checkIfValueValid(value: VimDataType, token: String) {
          super.checkIfValueValid(value, token)
          if (KeywordOptionHelper.isValueInvalid((value as VimString).value)) {
            throw ExException("E474: Invalid argument: $token")
          }
        }
      },
      object : StringOption(
        "guicursor", "gcr",
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
    ).map { Triple(it.name, it.abbrev, it) }
  )
  private val globalValues = mutableMapOf<String, VimDataType>()
  private val localValuesKey = Key<MutableMap<String, VimDataType>>("localOptions")

  override fun setOptionValue(scope: OptionService.Scope, optionName: String, value: VimDataType, editor: Editor?, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    option.checkIfValueValid(value, token)
    val oldValue = getOptionValue(scope, optionName, editor)
    when (scope) {
      OptionService.Scope.LOCAL -> {
        if (editor == null) {
          throw ExException("IdeaVimException: Editor is required for local-scoped options")
        }
        setLocalOptionValue(option.name, value, editor, token)
      }
      OptionService.Scope.GLOBAL -> setGlobalOptionValue(option.name, value, token)
    }
    option.onChanged(oldValue, value)
  }

  fun setOptionValue(scope: OptionService.Scope, optionName: String, value: String, editor: Editor, token: String) {
    val vimValue: VimDataType = castToVimDataType(value, optionName, token)
    setOptionValue(scope, optionName, vimValue, editor, token)
  }

  private fun setGlobalOptionValue(optionName: String, value: VimDataType, token: String = optionName) {
    globalValues[optionName] = value
  }

  private fun setLocalOptionValue(optionName: String, value: VimDataType, editor: Editor, token: String = optionName) {
    if (editor.getUserData(localValuesKey) == null) {
      editor.putUserData(localValuesKey, mutableMapOf(optionName to value))
    } else {
      editor.getUserData(localValuesKey)!![optionName] = value
    }
  }

  private fun getGlobalOptionValue(optionName: String): VimDataType? {
    val option = options.get(optionName) ?: return null
    return globalValues[option.name] ?: options.get(option.name)?.getDefaultValue()
  }

  private fun getLocalOptionValue(optionName: String, editor: Editor): VimDataType? {
    val option = options.get(optionName) ?: return null
    return editor.getUserData(localValuesKey)?.get(option.name) ?: getGlobalOptionValue(optionName)
  }

  /**
   * Sets the option on (true)
   */
  override fun setOption(scope: OptionService.Scope, optionName: String, editor: Editor?, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    if (option !is ToggleOption) {
      throw ExException("E474: Invalid argument: $token")
    }
    setOptionValue(scope, optionName, VimInt.ONE, editor, token)
  }

  /**
   * Unsets the option (false)
   */
  override fun unsetOption(scope: OptionService.Scope, optionName: String, editor: Editor?, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    if (option !is ToggleOption) {
      throw ExException("E474: Invalid argument: $token")
    }
    setOptionValue(scope, optionName, VimInt.ZERO, editor, token)
  }

  override fun toggleOption(scope: OptionService.Scope, optionName: String, editor: Editor?, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    if (option !is ToggleOption) {
      throw ExException("E474: Invalid argument: $token")
    }
    val optionValue = getOptionValue(scope, optionName, editor)
    if (optionValue.asBoolean()) {
      setOptionValue(scope, optionName, VimInt.ZERO, editor, token)
    } else {
      setOptionValue(scope, optionName, VimInt.ONE, editor, token)
    }
  }

  override fun isDefault(scope: OptionService.Scope, optionName: String, editor: Editor?, token: String): Boolean {
    val defaultValue = options.get(optionName)?.getDefaultValue() ?: throw ExException("E518: Unknown option: $token")
    return getOptionValue(scope, optionName, editor) == defaultValue
  }

  override fun resetDefault(scope: OptionService.Scope, optionName: String, editor: Editor?, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    setOptionValue(scope, optionName, option.getDefaultValue(), editor, token)
  }

  override fun isSet(scope: OptionService.Scope, optionName: String, editor: Editor?, token: String): Boolean {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    return option is ToggleOption && getOptionValue(scope, optionName, editor).asBoolean()
  }

  override fun getOptionValue(scope: OptionService.Scope, optionName: String, editor: Editor?, token: String): VimDataType {
    return when (scope) {
      OptionService.Scope.LOCAL -> {
        if (editor == null) {
          throw ExException("IdeaVimException: Editor is required for local-scoped options")
        }
        getLocalOptionValue(optionName, editor)
      }
      OptionService.Scope.GLOBAL -> getGlobalOptionValue(optionName)
    } ?: throw ExException("E518: Unknown option: $token")
  }

  fun appendValue(scope: OptionService.Scope, optionName: String, value: String, editor: Editor?, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    val currentValue = getOptionValue(scope, optionName, editor)
    val newValue = option.getValueIfAppend(currentValue, value, token)
    setOptionValue(scope, optionName, newValue, editor, token)
  }

  fun prependValue(scope: OptionService.Scope, optionName: String, value: String, editor: Editor?, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    val currentValue = getOptionValue(scope, optionName, editor)
    val newValue = option.getValueIfPrepend(currentValue, value, token)
    setOptionValue(scope, optionName, newValue, editor, token)
  }

  fun removeValue(scope: OptionService.Scope, optionName: String, value: String, editor: Editor?, token: String) {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    val currentValue = getOptionValue(scope, optionName, editor)
    val newValue = option.getValueIfRemove(currentValue, value, token)
    setOptionValue(scope, optionName, newValue, editor, token)
  }

  override fun resetAllOptions() {
    globalValues.clear()
  }

  override fun isToggleOption(optionName: String): Boolean {
    return options.get(optionName) is ToggleOption
  }

  override fun showAllOptions(editor: Editor, scope: OptionService.Scope, showIntro: Boolean) {
    showOptions(editor, options.primaryKeys.map { Pair(it, it) }, scope, showIntro)
  }

  override fun showChangedOptions(editor: Editor, scope: OptionService.Scope, showIntro: Boolean) {
    val changedOptions = options.primaryKeys.filter { options.get(it)!!.getDefaultValue() != getOptionValue(scope, it, editor) }
    showOptions(editor, changedOptions.map { Pair(it, it) }, scope, showIntro)
  }

  override fun showOptions(editor: Editor, nameAndToken: Collection<Pair<String, String>>, scope: OptionService.Scope, showIntro: Boolean) {
    val optionsToShow = mutableListOf<Option<out VimDataType>>()
    var unknownOption: Pair<String, String>? = null
    for (pair in nameAndToken) {
      if (options.contains(pair.first)) {
        optionsToShow.add(options.get(pair.second)!!)
      } else {
        unknownOption = pair
        break
      }
    }

    val cols = mutableListOf<String>()
    val extra = mutableListOf<String>()
    for (option in optionsToShow) {
      val optionAsString = option.toString(scope, editor)
      if (optionAsString.length > 19) extra.add(optionAsString) else cols.add(optionAsString)
    }

    cols.sort()
    extra.sort()

    var width = EditorHelper.getApproximateScreenWidth(editor)
    if (width < 20) {
      width = 80
    }
    val colCount = width / 20
    val height = ceil(cols.size.toDouble() / colCount.toDouble()).toInt()
    var empty = cols.size % colCount
    empty = if (empty == 0) colCount else empty

    logger.debug { "showOptions, width=$width, colCount=$colCount, height=$height" }

    val res = StringBuilder()
    if (showIntro) {
      res.append("--- Options ---\n")
    }
    for (h in 0 until height) {
      for (c in 0 until colCount) {
        if (h == height - 1 && c >= empty) {
          break
        }

        var pos = c * height + h
        if (c > empty) {
          pos -= c - empty
        }

        val opt = cols[pos]
        res.append(opt.padEnd(20))
      }
      res.append("\n")
    }

    for (opt in extra) {
      val seg = (opt.length - 1) / width
      for (j in 0..seg) {
        res.append(opt, j * width, min(j * width + width, opt.length))
        res.append("\n")
      }
    }
    ExOutputModel.getInstance(editor).output(res.toString())

    if (unknownOption != null) {
      throw ExException("E518: Unknown option: ${unknownOption.second}")
    }
  }

  internal fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>) {
    options.get(optionName)!!.addOptionChangeListener(listener)
  }

  internal fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>) {
    options.get(optionName)!!.removeOptionChangeListener(listener)
  }

  private fun Option<out VimDataType>.toString(scope: OptionService.Scope, editor: Editor): String {
    val value = if (scope == OptionService.Scope.LOCAL) getLocalOptionValue(name, editor)!! else getGlobalOptionValue(name)!!
    return when (this) {
      is ToggleOption -> if (value.asBoolean()) "  $name" else "no$name"
      is NumberOption, is StringOption -> "$name=$value"
    }
  }

  private fun castToVimDataType(value: String, optionName: String, token: String): VimDataType {
    val option = options.get(optionName) ?: throw ExException("E518: Unknown option: $token")
    return when (option) {
      is NumberOption, is ToggleOption -> VimInt(parseNumber(value) ?: throw ExException("E474: Invalid argument: $token"))
      is StringOption -> VimString(value)
    }
  }
}

// todo register options from plugins

class MultikeyMap<T1, T2>(entries: Collection<Triple<T1, T1, T2>>) {
  private val primaryKeyStorage: MutableMap<T1, T2> = mutableMapOf()
  private val secondaryKeyStorage: MutableMap<T1, T2> = mutableMapOf()

  init {
    for (entry in entries) {
      primaryKeyStorage[entry.first] = entry.third
      secondaryKeyStorage[entry.second] = entry.third
    }
  }

  fun put(key1: T1, key2: T1, value: T2) {
    primaryKeyStorage[key1] = value
    secondaryKeyStorage[key2] = value
  }

  fun get(key: T1): T2? {
    return primaryKeyStorage[key] ?: secondaryKeyStorage[key]
  }

  fun contains(key: T1): Boolean {
    return primaryKeyStorage.containsKey(key) || secondaryKeyStorage.containsKey(key)
  }

  val primaryKeys get() = primaryKeyStorage.keys
  val secondaryKeys get() = secondaryKeyStorage.keys
}
