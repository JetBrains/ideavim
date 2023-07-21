/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.regexp.parser.generated.RegexLexer
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.junit.jupiter.api.Test


class RegexpParserTest {

  @Test
  fun `test range both bounds`() {
    val regexLexer = RegexLexer(CharStreams.fromString("\\{2,6}"))
    val tokens = CommonTokenStream(regexLexer)
    val parser = RegexParser(tokens)
    val tree: ParseTree = parser.range()
    println(tree.toStringTree(parser))
  }
}
