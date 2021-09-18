package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.jetbrains.plugins.ideavim.ex.parser.ParserTest
import org.junit.Test
import kotlin.test.assertEquals

class DictionaryTests : ParserTest() {

  @Test
  fun `empty dictionary test`() {
    assertEquals(
      VimDictionary(linkedMapOf()),
      VimscriptParser.parseExpression("{}")!!.evaluate()
    )
  }

  @Test
  fun `dictionary of simple types test`() {
    val dictString = "{$ZERO_OR_MORE_SPACES" +
      "'a'$ZERO_OR_MORE_SPACES:$ZERO_OR_MORE_SPACES'string expression'$ZERO_OR_MORE_SPACES,$ZERO_OR_MORE_SPACES" +
      "'b'$ZERO_OR_MORE_SPACES:$ZERO_OR_MORE_SPACES[1, 2]$ZERO_OR_MORE_SPACES" +
      "}"
    for (s in getTextWithAllSpacesCombinations(dictString)) {
      assertEquals(
        VimDictionary(
          linkedMapOf(
            VimString("a") to VimString("string expression"),
            VimString("b") to VimList(mutableListOf(VimInt(1), VimInt(2))),
          )
        ),
        VimscriptParser.parseExpression(s)!!.evaluate()
      )
    }
  }

  @Test
  fun `dictionary of simple types test 2`() {
    val dictString = "{$ZERO_OR_MORE_SPACES" +
      "'c'$ZERO_OR_MORE_SPACES:$ZERO_OR_MORE_SPACES{'key':'value'},$ZERO_OR_MORE_SPACES" +
      "'d'$ZERO_OR_MORE_SPACES:${ZERO_OR_MORE_SPACES}5$ZERO_OR_MORE_SPACES" +
      "}"
    for (s in getTextWithAllSpacesCombinations(dictString)) {
      assertEquals(
        VimDictionary(
          linkedMapOf(
            VimString("c") to VimDictionary(linkedMapOf(VimString("key") to VimString("value"))),
            VimString("d") to VimInt(5),
          )
        ),
        VimscriptParser.parseExpression(s)!!.evaluate()
      )
    }
  }

  @Test
  fun `dictionary of simple types test 3`() {
    val dictString = "{$ZERO_OR_MORE_SPACES" +
      "'e'$ZERO_OR_MORE_SPACES:${ZERO_OR_MORE_SPACES}4.2$ZERO_OR_MORE_SPACES" +
      "}"
    for (s in getTextWithAllSpacesCombinations(dictString)) {
      assertEquals(
        VimDictionary(
          linkedMapOf(
            VimString("e") to VimFloat(4.2)
          )
        ),
        VimscriptParser.parseExpression(s)!!.evaluate()
      )
    }
  }

  @Test
  fun `empty literal dictionary test`() {
    assertEquals(
      VimDictionary(linkedMapOf()),
      VimscriptParser.parseExpression("#{}")!!.evaluate()
    )
  }

  @Test
  fun `literal dictionary of simple types test`() {
    val dictString =
      "#{${ZERO_OR_MORE_SPACES}test$ZERO_OR_MORE_SPACES:${ZERO_OR_MORE_SPACES}12$ZERO_OR_MORE_SPACES," +
        "${ZERO_OR_MORE_SPACES}2-1$ZERO_OR_MORE_SPACES:$ZERO_OR_MORE_SPACES'string value'$ZERO_OR_MORE_SPACES}"
    for (s in getTextWithAllSpacesCombinations(dictString)) {
      assertEquals(
        VimDictionary(
          linkedMapOf(
            VimString("test") to VimInt(12),
            VimString("2-1") to VimString("string value")
          )
        ),
        VimscriptParser.parseExpression(s)!!.evaluate()
      )
    }
  }

  @Test
  fun `comma at dictionary end test`() {
    assertEquals(
      VimDictionary(linkedMapOf(VimString("one") to VimInt(1))),
      VimscriptParser.parseExpression("{'one': 1,}")!!.evaluate()
    )
  }

  @Test
  fun `comma at literal dictionary end test`() {
    assertEquals(
      VimDictionary(linkedMapOf(VimString("one") to VimInt(1))),
      VimscriptParser.parseExpression("#{one: 1,}")!!.evaluate()
    )
  }
}
