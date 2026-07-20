/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import org.jetbrains.annotations.NonNls
import javax.swing.KeyStroke

class ParseMapCommandArguments {

  @Suppress("unused")
  @NonNls
  enum class SpecialArgument(val myName: String) {
    BUFFER("<buffer>"),
    NOWAIT("<nowait>"),
    SILENT("<silent>"),
    SPECIAL("<special>"),
    SCRIPT("<script>"),
    EXPR("<expr>"),
    UNIQUE("<unique>"),
    ;

    override fun toString(): String {
      return this.myName
    }

    companion object {
      fun fromString(s: String): SpecialArgument? {
        for (argument in entries) {
          if (s == argument.myName) {
            return argument
          }
        }
        return null
      }
    }
  }

  class CommandArguments(
    val specialArguments: Set<SpecialArgument>,
    val fromKeys: List<KeyStroke>,
    val toExpr: Expression,
    val secondArgument: String,
  )

  companion object {

    /**
     * Parses one row of a `:loadkeymap` table: `from<sep>to<sep>description`, separated by spaces or tabs. Only the
     * first two columns (`from` and `to`) are used; any trailing description column is dropped.
     */
    fun parseKeymapEntry(input: String): CommandArguments? =
      buildArguments(input.split(" ", "\t").dropLastWhile { it.isEmpty() }.take(2))

    private fun buildArguments(parts: List<String>): CommandArguments? {
      if (parts.size < 2) {
        return null
      }
      val specialArguments = HashSet<SpecialArgument>()
      val toKeysBuilder = StringBuilder()
      var fromKeys: List<KeyStroke>? = null

      parts.forEach { part ->
        if (fromKeys != null) {
          toKeysBuilder.append(" ")
          toKeysBuilder.append(part)
        } else {
          val specialArgument = SpecialArgument.fromString(part)
          if (specialArgument != null) {
            specialArguments.add(specialArgument)
          } else {
            fromKeys = injector.parser.parseKeys(processBars(part))
          }
        }
      }
      return fromKeys?.let {
        val toExpr = if (specialArguments.contains(SpecialArgument.EXPR)) {
          injector.vimscriptParser.parseExpression(toKeysBuilder.toString().trim())
            ?: throw exExceptionMessage("E15", toKeysBuilder.toString().trim())
        } else {
          SimpleExpression(toKeysBuilder.toString().trimStart())
        }
        CommandArguments(specialArguments, it, toExpr, toKeysBuilder.toString().trimStart())
      }
    }

    private fun processBars(fromString: String): String {
      return fromString.replace("\\|", "|")
    }
  }

}