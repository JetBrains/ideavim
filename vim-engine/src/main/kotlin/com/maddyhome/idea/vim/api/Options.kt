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

public object Options {
  private val logger = vimLogger<Options>()

  public fun getOption(key: String): Option<out VimDataType>? = options.get(key)
  public fun getAllOptions(): Set<Option<out VimDataType>> = options.values.toSet()

  public fun addOption(option: Option<out VimDataType>): Unit = options.put(option.name, option.abbrev, option)
  public fun removeOption(optionName: String): Unit = options.remove(optionName)

  private val options = MultikeyMap(
    // Simple options, sorted by name
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
            throw exExceptionMessage("E474", token)
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
    StringOption(
      OptionConstants.shell,
      "sh",
      if (injector.systemInfoService.isWindows) "cmd.exe" else System.getenv("SHELL") ?: "sh"
    ),
    object : StringOption(OptionConstants.shellcmdflag, "shcf", "") {
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
    },
    StringOption(
      OptionConstants.shellxescape,
      "sxe",
      if (injector.systemInfoService.isWindows) "\"&|<>()@^" else "",
      isList = false
    ),
    object : StringOption(OptionConstants.shellxquote, "sxq", "") {
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
    },

    // TODO: Clipboard is special - it is overridden to provide a new default value for the IJ implementation
    StringOption(
      OptionConstants.clipboard,
      OptionConstants.clipboardAlias,
      "autoselect,exclude:cons\\|linux",
      isList = true
    ),

    // IdeaVim specific options. Put any editor/IDE specific options in IjVimOptionService
    ToggleOption(OptionConstants.ideaglobalmode, OptionConstants.ideaglobalmode, false),
    ToggleOption(OptionConstants.ideastrictmode, OptionConstants.ideastrictmode, false),
    ToggleOption(OptionConstants.ideatracetime, OptionConstants.ideatracetime, false),
  )
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
