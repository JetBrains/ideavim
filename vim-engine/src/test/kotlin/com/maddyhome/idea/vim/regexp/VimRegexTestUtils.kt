/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.regexp.nfa.NFA
import com.maddyhome.idea.vim.regexp.parser.VimRegexParser
import com.maddyhome.idea.vim.regexp.parser.VimRegexParserResult
import com.maddyhome.idea.vim.regexp.parser.visitors.PatternVisitor
import org.mockito.Mockito
import org.mockito.kotlin.whenever

internal object VimRegexTestUtils {
  fun buildEditor(text: CharSequence, carets: List<Int> = emptyList()) : VimEditor {
    val editorMock = Mockito.mock<VimEditor>()
    whenever(editorMock.text()).thenReturn(text)

    val trueCarets = ArrayList<VimCaret>()
    for (caret in carets) {
      val caretMock = Mockito.mock<VimCaret>()
      whenever(caretMock.offset).thenReturn(Offset(caret))
      trueCarets.add(caretMock)
    }
    whenever(editorMock.carets()).thenReturn(trueCarets)
    return editorMock
  }

  fun buildNFA(pattern: String) : NFA? {
    val parserResult = VimRegexParser.parse(pattern)
    return when (parserResult) {
      is VimRegexParserResult.Failure -> null
      is VimRegexParserResult.Success -> PatternVisitor().visit(parserResult.tree)
    }
  }
}