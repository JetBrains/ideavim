/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_WINDOW
import com.maddyhome.idea.vim.options.StringListOption
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
  private val options = MultikeyMap()

  public fun initialise() {
    // Do nothing!
    // Calling this method allows for deterministic initialisation of the Options singleton, specifically initialising
    // the properties and registering the IJ specific options. Once added, they can be safely accessed by name, e.g. by
    // the implementation of `:set` while executing ~/.ideavimrc
  }

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

  /*
   * Option declarations
   *
   * Options are declared as strongly typed public properties. The GlobalOptions and EffectiveOptions classes provide
   * strongly typed properties that make it easy to get/set option values from code using native types.
   * A small note on history: Options were originally declared as simple strongly typed properties that wrapped a single
   * global value. This was refactored into a name-based API with the introduction of global and local values (as
   * required by scripting). The downside to this approach was that it naturally became loosely typed, and consuming
   * code was required to upcast option values to the expected type before use. Reintroducing strongly typed properties
   * allows us to build a strongly typed accessor API that makes it easy to get/set option values. We'd like to avoid
   * such API churn in the future of options, if possible.
   *
   * To add an option:
   * * Add a new public property below, sorted alphabetically. Note that the options are grouped into simple
   *   declarations, typically one-liners, followed by options with more complex splitting or validation logic, and then
   *   followed by IdeaVim specific (but implementation agnostic) options
   * * The property should be named the same as the Vim option. Add the name to the ideavim.dic dictionary to avoid
   *   spelling inspections in ~/.ideavimrc
   * * If the option is to be accessed from Java, add @JvmField. This is usually only required for adding a change
   *   listener, and should probably be avoided (or migrated to Kotlin)
   * * Create an instance of the option type, wrapped in a call to addOption. Do not forget to call addOption!
   *   If the option requires additional validation or custom splitting, derive from one of the option types and
   *   implement inline
   * * Add a public var delegated property in GlobalOptions (for global options) or EffectiveOptions (for options that
   *   are local-to-buffer, local-to-window or global-local). The delegated property will handle getting and setting the
   *   option value, as a native type, at the correct scope
   * * Add tests :)
   */

  // Simple options, sorted by name
  public val digraph: ToggleOption = addOption(ToggleOption("digraph", GLOBAL, "dg", false))
  public val gdefault: ToggleOption = addOption(ToggleOption("gdefault", GLOBAL, "gd", false))
  public val history: UnsignedNumberOption = addOption(UnsignedNumberOption("history", GLOBAL, "hi", 50))
  @JvmField
  public val hlsearch: ToggleOption = addOption(ToggleOption("hlsearch", GLOBAL, "hls", false))
  @JvmField
  public val ignorecase: ToggleOption = addOption(ToggleOption("ignorecase", GLOBAL, "ic", false))
  public val incsearch: ToggleOption = addOption(ToggleOption("incsearch", GLOBAL, "is", false))
  public val keymodel: StringListOption = addOption(
    StringListOption(
      "keymodel",
      GLOBAL,
      "km",
      "${OptionConstants.keymodel_continueselect},${OptionConstants.keymodel_stopselect}",
      setOf(
        OptionConstants.keymodel_startsel,
        OptionConstants.keymodel_stopsel,
        OptionConstants.keymodel_stopselect,
        OptionConstants.keymodel_stopvisual,
        OptionConstants.keymodel_continueselect,
        OptionConstants.keymodel_continuevisual
      )
    )
  )
  public val maxmapdepth: NumberOption = addOption(NumberOption("maxmapdepth", GLOBAL, "mmd", 20))
  public val more: ToggleOption = addOption(ToggleOption("more", GLOBAL, "more", true))
  public val nrformats: StringListOption = addOption(
    StringListOption("nrformats", LOCAL_TO_BUFFER, "nf", "hex", setOf("octal", "hex", "alpha"))
  )
  public val number: ToggleOption = addOption(ToggleOption("number", LOCAL_TO_WINDOW, "nu", false))
  public val relativenumber: ToggleOption = addOption(ToggleOption("relativenumber", LOCAL_TO_WINDOW, "rnu", false))
  public val scroll: NumberOption = addOption(NumberOption("scroll", LOCAL_TO_WINDOW, "scr", 0))
  public val scrolloff: NumberOption = addOption(NumberOption("scrolloff", GLOBAL_OR_LOCAL_TO_WINDOW, "so", 0))
  public val selection: StringOption = addOption(
    StringOption(
      "selection",
      GLOBAL,
      "sel",
      "inclusive",
      setOf("old", "inclusive", "exclusive")
    )
  )
  public val selectmode: StringListOption = addOption(
    StringListOption(
      "selectmode", GLOBAL, "slm", "",
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
      "shell",
      GLOBAL,
      "sh",
      if (injector.systemInfoService.isWindows) "cmd.exe" else System.getenv("SHELL") ?: "sh"
    )
  )
  public val shellxescape: StringOption = addOption(
    StringOption(
      "shellxescape",
      GLOBAL,
      "sxe",
      if (injector.systemInfoService.isWindows) "\"&|<>()@^" else ""
    )
  )
  public val showcmd: ToggleOption = addOption(ToggleOption("showcmd", GLOBAL, "sc", true))
  public val showmode: ToggleOption = addOption(ToggleOption("showmode", GLOBAL, "smd", true))
  public val sidescroll: NumberOption = addOption(NumberOption("sidescroll", GLOBAL, "ss", 0))
  public val sidescrolloff: NumberOption = addOption(
    NumberOption("sidescrolloff", GLOBAL_OR_LOCAL_TO_WINDOW, "siso", 0)
  )
  @JvmField
  public val smartcase: ToggleOption = addOption(ToggleOption("smartcase", GLOBAL, "scs", false))
  public val startofline: ToggleOption = addOption(ToggleOption("startofline", GLOBAL, "sol", true))
  public val timeout: ToggleOption = addOption(ToggleOption("timeout", GLOBAL, "to", true))
  public val timeoutlen: UnsignedNumberOption = addOption(UnsignedNumberOption("timeoutlen", GLOBAL, "tm", 1000))
  public val undolevels: NumberOption = addOption(
    // -1 means no undo. Vim uses -123456 as "unset". See `:help undolevels`
    // TODO: This option doesn't appear to be used anywhere...
    NumberOption("undolevels", GLOBAL_OR_LOCAL_TO_BUFFER, "ul", 1000, -123456)
  )
  public val viminfo: StringListOption = addOption(StringListOption("viminfo", GLOBAL, "vi", "'100,<50,s10,h"))
  public val virtualedit: StringListOption = addOption(
    StringListOption(
      "virtualedit",
      GLOBAL_OR_LOCAL_TO_WINDOW,
      "ve",
      "",
      setOf("onemore", "block", "insert", "all")
    )
  )
  public val visualbell: ToggleOption = addOption(ToggleOption("visualbell", GLOBAL, "vb", false))
  public val whichwrap: StringListOption = addOption(
    StringListOption(
      "whichwrap",
      GLOBAL,
      "ww",
      "b,s",
      setOf("b", "s", "h", "l", "<", ">", "~", "[", "]")
    )
  )
  public val wrapscan: ToggleOption = addOption(ToggleOption("wrapscan", GLOBAL, "ws", true))


  // More complex options, with additional validation, etc.
  public val guicursor: StringListOption = addOption(object : StringListOption(
    "guicursor", GLOBAL, "gcr",
    "n-v-c:block-Cursor/lCursor," +
      "ve:ver35-Cursor," +
      "o:hor50-Cursor," +
      "i-ci:ver25-Cursor/lCursor," +
      "r-cr:hor20-Cursor/lCursor," +
      "sm:block-Cursor-blinkwait175-blinkoff150-blinkon175") {
    override fun checkIfValueValid(value: VimDataType, token: String) {
      super.checkIfValueValid(value, token)
      val valueAsString = (value as VimString).value
      valueAsString.split(",").forEach { GuiCursorOptionHelper.convertToken(it) }
    }
  })

  public val iskeyword: StringListOption =
    addOption(object : StringListOption("iskeyword", LOCAL_TO_BUFFER, "isk", "@,48-57,_") {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        if (KeywordOptionHelper.isValueInvalid((value as VimString).value)) {
          throw exExceptionMessage("E474", token)
        }
      }

      override fun split(value: String): List<String> {
        val result = KeywordOptionHelper.parseValues(value)
        StrictMode.assert(result != null, "Cannot split iskeyword value: $ value")

        return result ?: split(defaultValue.value)
      }
    })


  public val matchpairs: StringListOption =
    addOption(object : StringListOption("matchpairs", LOCAL_TO_BUFFER, "mps", "(:),{:},[:]") {
      override fun checkIfValueValid(value: VimDataType, token: String) {
        super.checkIfValueValid(value, token)
        for (v in split((value as VimString).value)) {
          if (!v.matches(Regex(".:."))) {
            throw exExceptionMessage("E474", token)
          }
        }
      }
    })

  public val scrolljump: NumberOption = addOption(object : NumberOption("scrolljump", GLOBAL, "sj", 1) {
    override fun checkIfValueValid(value: VimDataType, token: String) {
      super.checkIfValueValid(value, token)
      if ((value as VimInt).value < -100) {
        throw ExException("E49: Invalid scroll size: $token")
      }
    }
  })

  public val shellcmdflag: StringOption = addOption(object : StringOption("shellcmdflag", GLOBAL, "shcf", "") {
    override val defaultValue: VimString
      get() {
        // Default value depends on the `'shell'` option. Since it's a global option, we can pass null as the editor
        val shell = injector.optionGroup.getOptionValue(shell, OptionAccessScope.GLOBAL(null)).asString()
        return VimString(
          when {
            injector.systemInfoService.isWindows && shell.contains("powershell") -> "-Command"
            injector.systemInfoService.isWindows && !shell.contains("sh") -> "/c"
            else -> "-c"
          }
        )
      }
  })

  public val shellxquote: StringOption = addOption(object : StringOption("shellxquote", GLOBAL, "sxq", "") {
    override val defaultValue: VimString
      get() {
        // Default value depends on the `'shell'` option. Since it's a global option, we can pass null as the editor
        val shell = injector.optionGroup.getOptionValue(shell, OptionAccessScope.GLOBAL(null)).asString()
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
  public val clipboard: StringListOption = addOption(
    StringListOption("clipboard", GLOBAL, "cb", "autoselect,exclude:cons\\|linux")
  )

  // IdeaVim specific options. Put any editor or IDE specific options in IjOptionProperties
  public val ideaglobalmode: ToggleOption = addOption(ToggleOption("ideaglobalmode", GLOBAL, "ideaglobalmode", false))
  public val ideastrictmode: ToggleOption = addOption(ToggleOption("ideastrictmode", GLOBAL, "ideastrictmode", false))
  public val ideatracetime: ToggleOption = addOption(ToggleOption("ideatracetime", GLOBAL, "ideatracetime", false))
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
