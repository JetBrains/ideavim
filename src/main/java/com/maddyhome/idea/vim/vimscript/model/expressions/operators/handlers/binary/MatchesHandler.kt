package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

object MatchesHandler : BinaryOperatorWithIgnoreCaseOption(MatchesIgnoreCaseHandler, MatchesCaseSensitiveHandler)
