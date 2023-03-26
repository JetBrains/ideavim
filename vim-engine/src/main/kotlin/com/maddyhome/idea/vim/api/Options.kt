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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.options.UnsignedNumberOption
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

@Suppress("unused", "SpellCheckingInspection")
public object Options {
  private val logger = vimLogger<Options>()
  private val options = MultikeyMap()

  public fun getOption(key: String): Option<VimDataType>? = options.get(key)
  public fun getAllOptions(): Set<Option<VimDataType>> = options.values.toSet()

  /**
   * Add an option
   *
   * Note that the generic type is `Option<out VimDataType>` so that it will handle derived types that have a more
   * derived type parameter. E.g. `NumberOption`, which derives from `Option<VimInt>`.
   */
  public fun <T : Option<out VimDataType>> addOption(option: T): T {
    return option.also {
      // This suppresses a variance problem. We need to be generic with an upper bound of `Option<out VimDataType` so
      // that we can both accept and then return a derived type which is generic by a type derived from `VimDataType`.
      // But we don't want the stored option to be covariant everywhere, as it's not a covariant type
      @Suppress("UNCHECKED_CAST")
      options.put(option.name, option.abbrev, option as Option<VimDataType>)
    }
  }

  public fun removeOption(optionName: String): Unit = options.remove(optionName)

  /**
   * Override the default value of an option
   *
   * Use with care! This function is intended for an implementation to provide additional values, such as `'clipboard'`
   * supporting 'ideaput' to use IntelliJ's paste handlers.
   */
  public fun <T : VimDataType> overrideDefaultValue(option: Option<T>, newDefaultValue: T) {
    option.overrideDefaultValue(newDefaultValue)
  }

  // Simple options, sorted by name
  // Note that we expose options as strongly typed properties to make it easier to consume them. The VimOptionGroup API
  // will return strongly typed VimDataType derived instances if given a strongly typed option, but fetching by name
  // loses that type information, and we also have to check that the option is not null, even if we know it exists.
  // types
  public val digraph: ToggleOption = addOption(ToggleOption("digraph", "dg", false))
  @JvmField public val gdefault: ToggleOption = addOption(ToggleOption("gdefault", "gd", false))
  public val history: UnsignedNumberOption = addOption(UnsignedNumberOption("history", "hi", 50))
  @JvmField public val hlsearch: ToggleOption = addOption(ToggleOption("hlsearch", "hls", false))
  @JvmField public val ignorecase: ToggleOption = addOption(ToggleOption("ignorecase", "ic", false))
  @JvmField public val incsearch: ToggleOption = addOption(ToggleOption("incsearch", "is", false))
  public val keymodel: StringOption = addOption(StringOption(
    "keymodel",
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
  ))
  public val maxmapdepth: NumberOption = addOption(NumberOption("maxmapdepth", "mmd", 20))
  @JvmField public val more: ToggleOption = addOption(ToggleOption("more", "more", true))
  @JvmField public val nrformats: StringOption =
    addOption(StringOption("nrformats", "nf", "hex", isList = true, setOf("octal", "hex", "alpha")))
  @JvmField public val number: ToggleOption = addOption(ToggleOption("number", "nu", false))
  @JvmField public val relativenumber: ToggleOption = addOption(ToggleOption("relativenumber", "rnu", false))
  public val scroll: NumberOption = addOption(NumberOption("scroll", "scr", 0))
  public val scrolloff: NumberOption = addOption(NumberOption("scrolloff", "so", 0))
  public val selection: StringOption = addOption(
    StringOption(
      "selection",
      "sel",
      "inclusive",
      isList = false,
      setOf("old", "inclusive", "exclusive")
    )
  )
  public val selectmode: StringOption = addOption(
    StringOption(
      "selectmode", "slm", "", isList = true,
      setOf(
        OptionConstants.selectmode_mouse,
        OptionConstants.selectmode_key,
        OptionConstants.selectmode_cmd,
        OptionConstants.selectmode_ideaselection
      )
    )
  )
  @JvmField public val shell: StringOption = addOption(
    StringOption(
      "shell",
      "sh",
      if (injector.systemInfoService.isWindows) "cmd.exe" else System.getenv("SHELL") ?: "sh"
    )
  )
  @JvmField public val shellxescape: StringOption = addOption(
    StringOption(
      "shellxescape",
      "sxe",
      if (injector.systemInfoService.isWindows) "\"&|<>()@^" else "",
      isList = false
    )
  )
  public val showcmd: ToggleOption = addOption(ToggleOption("showcmd", "sc", true))
  public val showmode: ToggleOption = addOption(ToggleOption("showmode", "smd", true))
  public val sidescroll: NumberOption = addOption(NumberOption("sidescroll", "ss", 0))
  public val sidescrolloff: NumberOption = addOption(NumberOption("sidescrolloff", "siso", 0))
  @JvmField public val smartcase: ToggleOption = addOption(ToggleOption("smartcase", "scs", false))
  public val startofline: ToggleOption = addOption(ToggleOption("startofline", "sol", true))
  public val timeout: ToggleOption = addOption(ToggleOption("timeout", "to", true))
  public val timeoutlen: UnsignedNumberOption = addOption(UnsignedNumberOption("timeoutlen", "tm", 1000))
  public val undolevels: UnsignedNumberOption = addOption(UnsignedNumberOption("undolevels", "ul", 1000))
  @JvmField public val viminfo: StringOption = addOption(StringOption("viminfo", "vi", "'100,<50,s10,h", isList = true))
  public val virtualedit: StringOption = addOption(
    StringOption(
      "virtualedit",
      "ve",
      "",
      isList = false,
      setOf("onemore", "block", "insert", "all")
    )
  )
  public val visualbell: ToggleOption = addOption(ToggleOption("visualbell", "vb", false))
  @JvmField public val whichwrap: StringOption = addOption(
    StringOption(
      "whichwrap",
      "ww",
      "b,s",
      true,
      setOf("b", "s", "h", "l", "<", ">", "~", "[", "]")
    )
  )
  @JvmField public val wrapscan: ToggleOption = addOption(ToggleOption("wrapscan", "ws", true))


  // More complex options, with additional validation, etc.
  public val guicursor: StringOption = addOption(object : StringOption(
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
  })

  public val iskeyword: StringOption = addOption(object : StringOption("iskeyword", "isk", "@,48-57,_", isList = true) {
    override fun checkIfValueValid(value: VimDataType, token: String) {
      super.checkIfValueValid(value, token)
      if (KeywordOptionHelper.isValueInvalid((value as VimString).value)) {
        throw exExceptionMessage("E474", token)
      }
    }

    override fun split(value: String): List<String> {
      val result = KeywordOptionHelper.parseValues(value)
      if (result == null) {
        logger.error("KeywordOptionHelper failed to parse $value")
        injector.messages.indicateError()
        injector.messages.showStatusBarMessage(editor = null, "Failed to parse iskeyword option value")
      }
      return result ?: split(defaultValue.value)
    }
  })

  @JvmField
  public val matchpairs: StringOption = addOption(object : StringOption("matchpairs", "mps", "(:),{:},[:]", isList = true) {
    override fun checkIfValueValid(value: VimDataType, token: String) {
      super.checkIfValueValid(value, token)
      for (v in split((value as VimString).value)) {
        if (!v.matches(Regex(".:."))) {
          throw exExceptionMessage("E474", token)
        }
      }
    }
  })

  public val scrolljump: NumberOption = addOption(object : NumberOption("scrolljump", "sj", 1) {
    override fun checkIfValueValid(value: VimDataType, token: String) {
      super.checkIfValueValid(value, token)
      if ((value as VimInt).value < -100) {
        throw ExException("E49: Invalid scroll size: $token")
      }
    }
  })

  @JvmField
  public val shellcmdflag: StringOption = addOption(object : StringOption("shellcmdflag", "shcf", "") {
    override val defaultValue: VimString
      get() {
        // Default value depends on the "shell" option
        val shell = injector.optionGroup.getOptionValue(shell, OptionScope.GLOBAL).asString()
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell.contains("powershell") -> "-Command"
            injector.systemInfoService.isWindows && !shell.contains("sh") -> "/c"
            else -> "-c"
          }
        )
      }
  })

  @JvmField
  public val shellxquote: StringOption = addOption(object : StringOption("shellxquote", "sxq", "") {
    override val defaultValue: VimString
      get() {
        // Default value depends on the "shell" option
        val shell = injector.optionGroup.getOptionValue(shell, OptionScope.GLOBAL).asString()
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell == "cmd.exe" -> "("
            injector.systemInfoService.isWindows && shell.contains("sh") -> "\""
            else -> ""
          }
        )
      }
  })

  // Note that IntelliJ overrides clipboard's default value to include the `ideaput` option.
  // TODO: Technically, we should validate values, but that requires handling exclude, which is irrelevant to us
  public val clipboard: StringOption = addOption(
    StringOption(
      "clipboard",
      "cb",
      "autoselect,exclude:cons\\|linux",
      isList = true
    )
  )

  // IdeaVim specific options. Put any editor or IDE specific options in IjVimOptionService
  public val ideaglobalmode: ToggleOption = addOption(ToggleOption("ideaglobalmode", "ideaglobalmode", false))
  public val ideastrictmode: ToggleOption = addOption(ToggleOption("ideastrictmode", "ideastrictmode", false))
  public val ideatracetime: ToggleOption = addOption(ToggleOption("ideatracetime", "ideatracetime", false))
}

private class MultikeyMap(vararg entries: Option<VimDataType>) {
  private val primaryKeyStorage: MutableMap<String, Option<VimDataType>> = mutableMapOf()
  private val secondaryKeyStorage: MutableMap<String, Option<VimDataType>> = mutableMapOf()

  init {
    for (entry in entries) {
      primaryKeyStorage[entry.name] = entry
      secondaryKeyStorage[entry.abbrev] = entry
    }
  }

  fun put(key1: String, key2: String, value: Option<VimDataType>) {
    primaryKeyStorage[key1] = value
    secondaryKeyStorage[key2] = value
  }

  fun get(key: String): Option<VimDataType>? {
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

  val values get() = primaryKeyStorage.values
}
