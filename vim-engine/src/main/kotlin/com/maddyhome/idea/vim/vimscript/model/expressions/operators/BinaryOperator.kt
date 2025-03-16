/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators

import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.AdditionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.BinaryOperatorHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ConcatenationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.DivisionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.DoesntMatchHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.DoesntMatchIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.EqualsCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.EqualsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.EqualsIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterOrEqualsCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterOrEqualsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.GreaterOrEqualsIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsNotCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsNotHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.IsNotIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessOrEqualsCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessOrEqualsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LessOrEqualsIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LogicalAndHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.LogicalOrHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MatchesCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MatchesHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MatchesIgnoreCaseHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ModulusHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MultiplicationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.SubtractionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.UnequalsCaseSensitiveHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.UnequalsHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.UnequalsIgnoreCaseHandler

enum class BinaryOperator(val value: String, internal val handler: BinaryOperatorHandler) {
  MULTIPLICATION("*", MultiplicationHandler),
  DIVISION("/", DivisionHandler),
  ADDITION("+", AdditionHandler),
  SUBTRACTION("-", SubtractionHandler),
  CONCATENATION(".", ConcatenationHandler),
  CONCATENATION2("..", ConcatenationHandler),
  LESS("<", LessHandler),
  LESS_IGNORE_CASE("<?", LessIgnoreCaseHandler),
  LESS_CASE_SENSITIVE("<#", LessCaseSensitiveHandler),
  GREATER(">", GreaterHandler),
  GREATER_IGNORE_CASE(">?", GreaterIgnoreCaseHandler),
  GREATER_CASE_SENSITIVE(">#", GreaterCaseSensitiveHandler),
  EQUALS("==", EqualsHandler),
  EQUALS_IGNORE_CASE("==?", EqualsIgnoreCaseHandler),
  EQUALS_CASE_SENSITIVE("==#", EqualsCaseSensitiveHandler),
  UNEQUALS("!=", UnequalsHandler),
  UNEQUALS_IGNORE_CASE("!=?", UnequalsIgnoreCaseHandler),
  UNEQUALS_CASE_SENSITIVE("!=#", UnequalsCaseSensitiveHandler),
  GREATER_OR_EQUALS(">=", GreaterOrEqualsHandler),
  GREATER_OR_EQUALS_IGNORE_CASE(">=?", GreaterOrEqualsIgnoreCaseHandler),
  GREATER_OR_EQUALS_CASE_SENSITIVE(">=#", GreaterOrEqualsCaseSensitiveHandler),
  LESS_OR_EQUALS("<=", LessOrEqualsHandler),
  LESS_OR_EQUALS_IGNORE_CASE("<=?", LessOrEqualsIgnoreCaseHandler),
  LESS_OR_EQUALS_CASE_SENSITIVE("<=#", LessOrEqualsCaseSensitiveHandler),
  MODULUS("%", ModulusHandler),
  LOGICAL_AND("&&", LogicalAndHandler),
  LOGICAL_OR("||", LogicalOrHandler),
  IS("is", IsHandler),
  IS_IGNORE_CASE("is?", IsIgnoreCaseHandler),
  IS_CASE_SENSITIVE("is#", IsCaseSensitiveHandler),
  IS_NOT("isnot", IsNotHandler),
  IS_NOT_IGNORE_CASE("isnot?", IsNotIgnoreCaseHandler),
  IS_NOT_CASE_SENSITIVE("isnot#", IsNotCaseSensitiveHandler),
  MATCHES("=~", MatchesHandler),
  MATCHES_IGNORE_CASE("=~?", MatchesIgnoreCaseHandler),
  MATCHES_CASE_SENSITIVE("=~#", MatchesCaseSensitiveHandler),
  DOESNT_MATCH("!~", DoesntMatchHandler),
  DOESNT_MATCH_IGNORE_CASE("!~?", DoesntMatchIgnoreCaseHandler),
  DOESNT_MATCH_CASE_SENSITIVE("!~#", DoesntMatchIgnoreCaseHandler),
  ;

  companion object {
    fun getByValue(value: String): BinaryOperator? {
      return entries.firstOrNull { it.value == value }
    }
  }
}
