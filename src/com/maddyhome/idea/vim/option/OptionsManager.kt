/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.option

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.hasVisualSelection
import com.maddyhome.idea.vim.helper.isBlockCaret
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import org.jetbrains.annotations.Contract
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

@Suppress("unused")
object OptionsManager {
  private val logger = Logger.getInstance(OptionsManager::class.java)

  private val options: MutableMap<String, Option> = mutableMapOf()
  private val abbrevs: MutableMap<String, Option> = mutableMapOf()

  val clipboard = addOption(ListOption(ClipboardOptionsData.name, ClipboardOptionsData.abbr, arrayOf(ClipboardOptionsData.ideaput, "autoselect,exclude:cons\\|linux"), null))
  val digraph = addOption(ToggleOption("digraph", "dg", false))
  val gdefault = addOption(ToggleOption("gdefault", "gd", false))
  val history = addOption(NumberOption("history", "hi", 20, 1, Int.MAX_VALUE))
  val hlsearch = addOption(ToggleOption("hlsearch", "hls", false))
  val ideamarks = addOption(IdeaMarkskOptionsData.option)
  val ignorecase = addOption(ToggleOption(IgnoreCaseOptionsData.name, IgnoreCaseOptionsData.abbr, false))
  val incsearch = addOption(ToggleOption("incsearch", "is", false))
  val iskeyword = addOption(KeywordOption("iskeyword", "isk", arrayOf("@", "48-57", "_")))
  val keymodel = addOption(KeyModelOptionData.option)
  val lookupKeys = addOption(ListOption("lookupkeys", "lookupkeys", arrayOf(), null))
  val matchpairs = addOption(ListOption("matchpairs", "mps", arrayOf("(:)", "{:}", "[:]"), ".:."))
  val more = addOption(ToggleOption("more", "more", true))
  val nrformats = addOption(BoundListOption("nrformats", "nf", arrayOf("octal", "hex"), arrayOf("octal", "hex", "alpha")))
  val number = addOption(ToggleOption("number", "nu", false))
  val relativenumber = addOption(ToggleOption("relativenumber", "rnu", false))
  val scroll = addOption(NumberOption("scroll", "scr", 0))
  val scrolljump = addOption(NumberOption("scrolljump", "sj", 1))
  val scrolloff = addOption(NumberOption("scrolloff", "so", 0))
  val selection = addOption(BoundStringOption("selection", "sel", "inclusive", arrayOf("old", "inclusive", "exclusive")))
  val selectmode = addOption(SelectModeOptionData.option)
  val showmode = addOption(ToggleOption("showmode", "smd", false))
  val sidescroll = addOption(NumberOption("sidescroll", "ss", 0))
  val sidescrolloff = addOption(NumberOption("sidescrolloff", "siso", 0))
  val smartcase = addOption(ToggleOption(SmartCaseOptionsData.name, SmartCaseOptionsData.abbr, false))
  val ideajoin = addOption(IdeaJoinOptionsData.option)
  val timeout = addOption(ToggleOption("timeout", "to", true))
  val timeoutlen = addOption(NumberOption("timeoutlen", "tm", 1000, -1, Int.MAX_VALUE))
  val undolevels = addOption(NumberOption("undolevels", "ul", 1000, -1, Int.MAX_VALUE))
  val viminfo = addOption(ListOption("viminfo", "vi", arrayOf("'100", "<50", "s10", "h"), null))
  val visualbell = addOption(ToggleOption("visualbell", "vb", false))
  val wrapscan = addOption(ToggleOption("wrapscan", "ws", true))
  val visualEnterDelay = addOption(NumberOption("visualdelay", "visualdelay", 100, 0, Int.MAX_VALUE))
  val idearefactormode = addOption(BoundStringOption(IdeaRefactorMode.name, IdeaRefactorMode.name, IdeaRefactorMode.select, IdeaRefactorMode.availableValues))
  val ideastatusbar = addOption(ToggleOption("ideastatusbar", "ideastatusbar", true))

  fun isSet(name: String): Boolean {
    val option = getOption(name)
    return option is ToggleOption && option.getValue()
  }

  fun getListOption(name: String): ListOption? = getOption(name) as? ListOption

  fun resetAllOptions() = options.values.forEach { it.resetDefault() }

  /**
   * Gets an option by the supplied name or short name.
   */
  fun getOption(name: String): Option? = options[name] ?: abbrevs[name]

  /**
   * This parses a set of :set commands. The following types of commands are supported:
   *
   *  * :set - show all changed options
   *  * :set all - show all options
   *  * :set all& - reset all options to default values
   *  * :set {option} - set option of boolean, display others
   *  * :set {option}? - display option
   *  * :set no{option} - reset boolean option
   *  * :set inv{option} - toggle boolean option
   *  * :set {option}! - toggle boolean option
   *  * :set {option}& - set option to default
   *  * :set {option}={value} - set option to new value
   *  * :set {option}:{value} - set option to new value
   *  * :set {option}+={value} - append or add to option value
   *  * :set {option}-={value} - remove or subtract from option value
   *  * :set {option}^={value} - prepend or multiply option value
   *
   *
   * @param editor    The editor the command was entered for, null if no editor - reading .ideavimrc
   * @param args      The :set command arguments
   * @param failOnBad True if processing should stop when a bad argument is found, false if a bad argument is simply
   * skipped and processing continues.
   * @return True if no errors were found, false if there were any errors
   */
  fun parseOptionLine(editor: Editor?, args: String, failOnBad: Boolean): Boolean {
    // No arguments so we show changed values
    when {
      args.isEmpty() -> {
        // Show changed options
        showOptions(editor, options.values.filter { !it.isDefault }, true)
        return true
      }
      args == "all" -> {
        showOptions(editor, options.values, true)
        return true
      }
      args == "all&" -> {
        resetAllOptions()
        return true
      }
    }

    // We now have 1 or more option operators separator by spaces
    var error: String? = null
    var token = ""
    val tokenizer = StringTokenizer(args)
    val toShow = mutableListOf<Option>()
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken()
      // See if a space has been backslashed, if no get the rest of the text
      while (token.endsWith("\\")) {
        token = token.substring(0, token.length - 1) + ' '
        if (tokenizer.hasMoreTokens()) {
          token += tokenizer.nextToken()
        }
      }

      // Print the value of an option
      if (token.endsWith("?")) {
        val option = token.dropLast(1)
        val opt = getOption(option)
        if (opt != null) {
          toShow.add(opt)
        } else {
          error = Msg.unkopt
        }
      } else if (token.startsWith("no")) {
        // Reset a boolean option
        val option = token.substring(2)
        val opt = getOption(option)
        if (opt != null) {
          if (opt is ToggleOption) {
            opt.reset()
          } else {
            error = Msg.e_invarg
          }
        } else {
          error = Msg.unkopt
        }
      } else if (token.startsWith("inv")) {
        // Toggle a boolean option
        val option = token.substring(3)
        val opt = getOption(option)
        if (opt != null) {
          if (opt is ToggleOption) {
            opt.toggle()
          } else {
            error = Msg.e_invarg
          }
        } else {
          error = Msg.unkopt
        }
      } else if (token.endsWith("!")) {
        // Toggle a boolean option
        val option = token.dropLast(1)
        val opt = getOption(option)
        if (opt != null) {
          if (opt is ToggleOption) {
            opt.toggle()
          } else {
            error = Msg.e_invarg
          }
        } else {
          error = Msg.unkopt
        }
      } else if (token.endsWith("&")) {
        // Reset option to default
        val option = token.dropLast(1)
        val opt = getOption(option)
        if (opt != null) {
          opt.resetDefault()
        } else {
          error = Msg.unkopt
        }
      } else {
        // This must be one of =, :, +=, -=, or ^=
        // Look for the = or : first
        var eq = token.indexOf('=')
        if (eq == -1) {
          eq = token.indexOf(':')
        }
        // No operator so only the option name was given
        if (eq == -1) {
          val opt = getOption(token)
          if (opt != null) {
            // Valid option so set booleans or display others
            (opt as? ToggleOption)?.set() ?: toShow.add(opt)
          } else {
            error = Msg.unkopt
          }
        } else {
          // Make sure there is an option name
          if (eq > 0) {
            // See if an operator before the equal sign
            val op = token[eq - 1]
            var end = eq
            if (op in "+-^") {
              end--
            }
            // Get option name and value after operator
            val option = token.take(end)
            val value = token.substring(eq + 1)
            val opt = getOption(option)
            if (opt != null) {
              // If not a boolean
              if (opt is TextOption) {
                val res = when (op) {
                  '+' -> opt.append(value)
                  '-' -> opt.remove(value)
                  '^' -> opt.prepend(value)
                  else -> opt.set(value)
                }
                if (!res) {
                  error = Msg.e_invarg
                }
              } else {
                error = Msg.e_invarg
              }// boolean option - no good
            } else {
              error = Msg.unkopt
            }
          } else {
            error = Msg.unkopt
          }
        }
      }

      if (failOnBad && error != null) {
        break
      }
    }

    // Now show all options that were individually requested
    if (toShow.size > 0) {
      showOptions(editor, toShow, false)
    }

    if (editor != null && error != null) {
      VimPlugin.showMessage(MessageHelper.message(error, token))
      VimPlugin.indicateError()
    }

    return error == null
  }

  /**
   * Shows the set of options
   *
   * @param editor    The editor to show them in - if null, this is aborted
   * @param opts      The list of options to display
   * @param showIntro True if intro is displayed, false if not
   */
  private fun showOptions(editor: Editor?, opts: Collection<Option>, showIntro: Boolean) {
    if (editor == null) return

    val cols = mutableListOf<Option>()
    val extra = mutableListOf<Option>()
    for (option in opts) {
      if (option.toString().length > 19) extra.add(option) else cols.add(option)
    }

    cols.sortBy { it.name }
    extra.sortBy { it.name }

    var width = EditorHelper.getScreenWidth(editor)
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
        res.append(opt.toString().padEnd(20))
      }
      res.append("\n")
    }

    for (opt in extra) {
      val value = opt.toString()
      val seg = (value.length - 1) / width
      for (j in 0..seg) {
        res.append(value, j * width, min(j * width + width, value.length))
        res.append("\n")
      }
    }

    ExOutputModel.getInstance(editor).output(res.toString())
  }

  @Contract("_ -> param1")
  fun <T : Option> addOption(option: T): T {
    options += option.name to option
    abbrevs += option.abbrev to option
    return option
  }
}

object KeyModelOptionData {
  const val name = "keymodel"
  const val abbr = "km"

  const val startsel = "startsel"
  const val stopsel = "stopsel"
  const val stopselect = "stopselect"
  const val stopvisual = "stopvisual"
  const val continueselect = "continueselect"
  const val continuevisual = "continuevisual"

  val options = arrayOf(startsel, stopsel, stopselect, stopvisual, continueselect, continuevisual)
  val default = arrayOf(continueselect, stopselect)
  val option = BoundListOption(name, abbr, default, options)
}

object SelectModeOptionData {
  const val name = "selectmode"
  const val abbr = "slm"

  const val mouse = "mouse"
  const val key = "key"
  const val cmd = "cmd"

  @Deprecated("Please, use `idearefactormode` option")
  const val template = "template"
  @Deprecated("Please, use `ideaselection`")
  const val refactoring = "refactoring"

  const val ideaselection = "ideaselection"

  @Suppress("DEPRECATION")
  val options = arrayOf(mouse, key, cmd, template, refactoring, ideaselection)
  val default = emptyArray<String>()
  val option = BoundListOption(name, abbr, default, options)

  fun ideaselectionEnabled(): Boolean {
    @Suppress("DEPRECATION")
    return ideaselection in OptionsManager.selectmode || refactoring in OptionsManager.selectmode
  }
}

object ClipboardOptionsData {
  const val name = "clipboard"
  const val abbr = "cb"

  const val ideaput = "ideaput"
  var ideaputDisabled = false
    private set

  /**
   * This autocloseable class allows temporary disable ideaput option
   * [ClipboardOptionsData.ideaputDisabled] property indicates if ideaput was disabled
   */
  class IdeaputDisabler : AutoCloseable {
    private val containedBefore: Boolean
    override fun close() {
      if (containedBefore) OptionsManager.clipboard.append(ideaput)
      ideaputDisabled = false
    }

    init {
      val options = OptionsManager.clipboard
      containedBefore = options.contains(ideaput)
      options.remove(ideaput)
      ideaputDisabled = true
    }
  }
}

object IdeaJoinOptionsData {
  const val name = "ideajoin"
  const val defaultValue = false

  val option = ToggleOption(name, name, defaultValue)
}

object IdeaMarkskOptionsData {
  const val name = "ideamarks"
  const val defaultValue = true

  val option = ToggleOption(name, name, defaultValue)
}

object SmartCaseOptionsData {
  const val name = "smartcase"
  const val abbr = "scs"
}

object IgnoreCaseOptionsData {
  const val name = "ignorecase"
  const val abbr = "ic"
}

object IdeaRefactorMode {
  const val name = "idearefactormode"

  const val keep = "keep"
  const val select = "select"
  const val visual = "visual"

  val availableValues = arrayOf(keep, select, visual)

  fun keepMode(): Boolean = OptionsManager.idearefactormode.value == keep
  fun selectMode(): Boolean = OptionsManager.idearefactormode.value == select
  fun visualMode(): Boolean = OptionsManager.idearefactormode.value == visual

  fun correctSelection(editor: Editor) {
    val action: () -> Unit = {
      if (!editor.mode.hasVisualSelection && editor.selectionModel.hasSelection()) {
        SelectionVimListenerSuppressor.lock().use {
          editor.selectionModel.removeSelection()
        }
      }

      if (editor.mode.isBlockCaret) {
        TemplateManagerImpl.getTemplateState(editor)?.currentVariableRange?.let { segmentRange ->
          if (!segmentRange.isEmpty && segmentRange.endOffset == editor.caretModel.offset && editor.caretModel.offset != 0) {
            editor.caretModel.moveToOffset(editor.caretModel.offset - 1)
          }
        }
      }
    }

    val lookup = LookupManager.getActiveLookup(editor) as? LookupImpl
    if (lookup != null) {
      val selStart = editor.selectionModel.selectionStart
      val selEnd = editor.selectionModel.selectionEnd
      lookup.performGuardedChange(action)
      lookup.addLookupListener(object : LookupListener {
        override fun beforeItemSelected(event: LookupEvent): Boolean {
          // FIXME: 01.11.2019 Nasty workaround because of problems in IJ platform
          //   Lookup replaces selected text and not the template itself. So, if there is no selection
          //   in the template, lookup value will not replace the template, but just insert value on the caret position
          lookup.performGuardedChange { editor.selectionModel.setSelection(selStart, selEnd) }
          lookup.removeLookupListener(this)
          return true
        }
      })
    } else {
      action()
    }
  }
}
