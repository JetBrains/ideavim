package org.jetbrains.plugins.ideavim.option;

import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.option.KeywordOption;
import org.jetbrains.plugins.ideavim.VimTestCase;

public class KeywordOptionTest extends VimTestCase {

  private KeywordOption option;

  public void setUp() throws Exception {
    super.setUp();
    option = new KeywordOption("", "", new String[]{});
  }

  public void testSingleCommaIsAValue() throws ExException {
    option.set(",");
    assertEquals(",", option.values().get(0));
  }

  public void testSingleNegatedCommaIsAValue() throws ExException {
    option.set("^,");
    assertEquals("^,", option.values().get(0));
  }

  public void testCommaInARangeIsAValue() throws ExException {
    option.set("+-,");
    assertEquals("+-,", option.values().get(0));
  }

  public void testSecondCommaIsASeparator() throws ExException {
    option.set(",,a");
    assertEquals(",", option.values().get(0));
    assertEquals("a", option.values().get(1));
  }

  public void testSingleHyphenIsAValue() throws ExException {
    option.set("-");
    assertEquals("-", option.values().get(0));
  }

  public void testHyphenBetweenCharNumsIsARange() throws ExException {
    option.set("a-b");
    assertEquals("a-b", option.values().get(0));
  }

  public void testRangeInWhichLeftValueIsHigherThanRightValueIsInvalid() {
    option.set("b-a");
    assertEmpty(option.values());
  }
}
