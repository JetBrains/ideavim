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

  public fun getOption(key: String): Option<out VimDataType>? = options.get(key)
  public fun getAllOptions(): Set<Option<out VimDataType>> = options.values.toSet()

  public fun <T : Option<out VimDataType>> addOption(option: T): T =
    option.also { options.put(option.name, option.abbrev, option) }

  public fun removeOption(optionName: String): Unit = options.remove(optionName)

  // Simple options, sorted by name
  // Note that we expose options as strongly typed properties to make it easier to consume them. The VimOptionGroup API
  // will return strongly typed VimDataType derived instances if given a strongly typed option, but fetching by name
  // loses that type information, and we also have to check that the option is not null, even if we know it exists.
  // types
  public val digraph: ToggleOption = addOption(ToggleOption(OptionConstants.digraph, "dg", false))
  public val gdefault: ToggleOption = addOption(ToggleOption(OptionConstants.gdefault, "gd", false))
  public val history: UnsignedNumberOption = addOption(UnsignedNumberOption(OptionConstants.history, "hi", 50))
  public val hlsearch: ToggleOption = addOption(ToggleOption(OptionConstants.hlsearch, "hls", false))
  public val ignorecase: ToggleOption = addOption(ToggleOption(OptionConstants.ignorecase, "ic", false))
  public val incsearch: ToggleOption = addOption(ToggleOption(OptionConstants.incsearch, "is", false))
  public val keymodel: StringOption = addOption(StringOption(
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
  ))
  public val maxmapdepth: NumberOption = addOption(NumberOption(OptionConstants.maxmapdepth, "mmd", 20))
  public val more: ToggleOption = addOption(ToggleOption(OptionConstants.more, "more", true))
  public val nrformats: StringOption =
    addOption(StringOption(OptionConstants.nrformats, "nf", "hex", isList = true, setOf("octal", "hex", "alpha")))
  public val number: ToggleOption = addOption(ToggleOption(OptionConstants.number, "nu", false))
  public val relativenumber: ToggleOption = addOption(ToggleOption(OptionConstants.relativenumber, "rnu", false))
  public val scroll: NumberOption = addOption(NumberOption(OptionConstants.scroll, "scr", 0))
  public val scrolloff: NumberOption = addOption(NumberOption(OptionConstants.scrolloff, "so", 0))
  public val selection: StringOption = addOption(
    StringOption(
      OptionConstants.selection,
      "sel",
      "inclusive",
      isList = false,
      setOf("old", "inclusive", "exclusive")
    )
  )
  public val selectmode: StringOption = addOption(
    StringOption(
      OptionConstants.selectmode, "slm", "", isList = true,
      setOf(
        OptionConstants.selectmode_mouse,
        OptionConstants.selectmode_key,
        OptionConstants.selectmode_cmd,
        OptionConstants.selectmode_ideaselection
      )
    )
  )
  public val shell: StringOption = addOption(
    StringOption(
      OptionConstants.shell,
      "sh",
      if (injector.systemInfoService.isWindows) "cmd.exe" else System.getenv("SHELL") ?: "sh"
    )
  )
  public val shellxescape: StringOption = addOption(
    StringOption(
      OptionConstants.shellxescape,
      "sxe",
      if (injector.systemInfoService.isWindows) "\"&|<>()@^" else "",
      isList = false
    )
  )
  public val showcmd: ToggleOption = addOption(ToggleOption(OptionConstants.showcmd, "sc", true))
  public val showmode: ToggleOption = addOption(ToggleOption(OptionConstants.showmode, "smd", true))
  public val sidescroll: NumberOption = addOption(NumberOption(OptionConstants.sidescroll, "ss", 0))
  public val sidescrolloff: NumberOption = addOption(NumberOption(OptionConstants.sidescrolloff, "siso", 0))
  public val smartcase: ToggleOption = addOption(ToggleOption(OptionConstants.smartcase, "scs", false))
  public val startofline: ToggleOption = addOption(ToggleOption(OptionConstants.startofline, "sol", true))
  public val timeout: ToggleOption = addOption(ToggleOption(OptionConstants.timeout, "to", true))
  public val timeoutlen: UnsignedNumberOption = addOption(UnsignedNumberOption(OptionConstants.timeoutlen, "tm", 1000))
  public val undolevels: UnsignedNumberOption = addOption(UnsignedNumberOption(OptionConstants.undolevels, "ul", 1000))
  public val viminfo: StringOption =
    addOption(StringOption(OptionConstants.viminfo, "vi", "'100,<50,s10,h", isList = true))
  public val virtualedit: StringOption = addOption(
    StringOption(
      OptionConstants.virtualedit,
      "ve",
      "",
      isList = false,
      setOf("onemore", "block", "insert", "all")
    )
  )
  public val visualbell: ToggleOption = addOption(ToggleOption(OptionConstants.visualbell, "vb", false))
  public val whichwrap: StringOption = addOption(
    StringOption(
      OptionConstants.whichwrap,
      "ww",
      "b,s",
      true,
      setOf("b", "s", "h", "l", "<", ">", "~", "[", "]")
    )
  )
  public val wrapscan: ToggleOption = addOption(ToggleOption(OptionConstants.wrapscan, "ws", true))


  // More complex options, with additional validation, etc.
  public val guicursor: StringOption = addOption(object : StringOption(
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
  })

  public val iskeyword: StringOption = addOption(object : StringOption(OptionConstants.iskeyword, "isk", "@,48-57,_", isList = true) {
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

  public val matchpairs: StringOption = addOption(object : StringOption(OptionConstants.matchpairs, "mps", "(:),{:},[:]", isList = true) {
    override fun checkIfValueValid(value: VimDataType, token: String) {
      super.checkIfValueValid(value, token)
      for (v in split((value as VimString).value)) {
        if (!v.matches(Regex(".:."))) {
          throw exExceptionMessage("E474", token)
        }
      }
    }
  })

  public val scrolljump: NumberOption = addOption(object : NumberOption(OptionConstants.scrolljump, "sj", 1) {
    override fun checkIfValueValid(value: VimDataType, token: String) {
      super.checkIfValueValid(value, token)
      if ((value as VimInt).value < -100) {
        throw ExException("E49: Invalid scroll size: $token")
      }
    }
  })

  public val shellcmdflag: StringOption = addOption(object : StringOption(OptionConstants.shellcmdflag, "shcf", "") {
    override val defaultValue: VimString
      get() {
        // Default value depends on the "shell" option
        val shell = injector.globalOptions().getStringValue(OptionConstants.shell)
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell.contains("powershell") -> "-Command"
            injector.systemInfoService.isWindows && !shell.contains("sh") -> "/c"
            else -> "-c"
          }
        )
      }
  })

  public val shellxquote: StringOption = addOption(object : StringOption(OptionConstants.shellxquote, "sxq", "") {
    override val defaultValue: VimString
      get() {
        // Default value depends on the "shell" option
        val shell = injector.globalOptions().getStringValue(OptionConstants.shell)
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell == "cmd.exe" -> "("
            injector.systemInfoService.isWindows && shell.contains("sh") -> "\""
            else -> ""
          }
        )
      }
  })

  // TODO: Clipboard is special - ideaput should only be defined in the IntelliJ build
  public val clipboard: StringOption = addOption(
    StringOption(
      OptionConstants.clipboard,
      OptionConstants.clipboardAlias,
      "ideaput,autoselect,exclude:cons\\|linux",
      isList = true
    )
  )

  // IdeaVim specific options. Put any editor or IDE specific options in IjVimOptionService
  public val ideaglobalmode: ToggleOption = addOption(ToggleOption(OptionConstants.ideaglobalmode, OptionConstants.ideaglobalmode, false))
  public val ideastrictmode: ToggleOption = addOption(ToggleOption(OptionConstants.ideastrictmode, OptionConstants.ideastrictmode, false))
  public val ideatracetime: ToggleOption = addOption(ToggleOption(OptionConstants.ideatracetime, OptionConstants.ideatracetime, false))
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

  val values get() = primaryKeyStorage.values
}
