package org.jetbrains.plugins.ideavim.action;

import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author Tuomas Tynkkynen
 */
public class ChangeNumberActionTest extends VimTestCase {
  public void testIncrementDecimalZero() {
    doTest(parseKeys("<C-A>"), "0", "1");
  }

  public void testIncrementHexZero() {
    doTest(parseKeys("<C-A>"), "0x0", "0x1");
  }

  public void testDecrementZero() {
    doTest(parseKeys("<C-X>"), "0", "-1");
  }

  public void testIncrementDecimal() {
    doTest(parseKeys("<C-A>"), "199", "200");
  }

  public void testDecrementDecimal() {
    doTest(parseKeys("<C-X>"), "1000", "999");
  }

  public void testIncrementOctal() {
    doTest(parseKeys("<C-A>"), "0477", "0500");
  }

  public void testDecrementOctal() {
    doTest(parseKeys("<C-X>"), "010", "007");
  }

  public void testIncrementHex() {
    doTest(parseKeys("<C-A>"), "0xff", "0x100");
  }

  public void testDecrementHex() {
    doTest(parseKeys("<C-X>"), "0xa100", "0xa0ff");
  }

  public void testIncrementNegativeDecimal() {
    doTest(parseKeys("<C-A>"), "-199", "-198");
  }

  public void testDecrementNegativeDecimal() {
    doTest(parseKeys("<C-X>"), "-1000", "-1001");
  }

  public void testIncrementNegativeOctal() {
    doTest(parseKeys("<C-A>"), "-0477", "-0500");
  }

  public void testDecrementNegativeOctal() {
    doTest(parseKeys("<C-X>"), "-010", "-007");
  }

  public void testIncrementNegativeHex() {
    doTest(parseKeys("<C-A>"), "-0xff", "-0x100");
  }

  public void testDecrementNegativeHex() {
    doTest(parseKeys("<C-X>"), "-0xa100", "-0xa0ff");
  }

  public void testIncrementWithCount() {
    doTest(parseKeys("123<C-A>"), "456", "579");
  }

  public void testDecrementWithCount() {
    doTest(parseKeys("200<C-X>"), "100", "-100");
  }

  public void testIncrementAlphaWithoutNumberFormatAlpha() {
    doTest(parseKeys("<C-A>"), "foo", "foo");
  }

  public void testIncrementAlphaWithNumberFormatAlpha() {
    doTest(parseKeys(":set nf=alpha<Enter>", "<C-A>"), "foo", "goo");
  }

  public void testIncrementZWithNumberFormatAlpha() {
    doTest(parseKeys(":set nf=alpha<Enter>", "<C-A>"), "zzz", "zzz");
  }

  public void testIncrementXInHexNumberWithNumberFormatAlphaButNotHex() {
    doTest(parseKeys(":set nf=alpha<Enter>", "<C-A>"), "0<caret>x1", "0y1");
  }

  public void testIncrementXInHexNumberWithNumberFormatHexAlpha() {
    doTest(parseKeys(":set nf=alpha,hex<Enter>", "<C-A>"), "0<caret>x1", "0x2");
  }

  public void testIncrementHexNumberWithoutNumberFormatHex() {
    doTest(parseKeys(":set nf=octal<Enter>", "<C-A>"), "0x42", "1x42");
  }

  public void testIncrementOctalNumberWithoutNumberFormatOctal() {
    doTest(parseKeys(":set nf=hex<Enter>", "<C-A>"), "077", "078");
  }

  public void testIncrementNegativeOctalNumberWithoutNumberFormatOctal() {
    doTest(parseKeys(":set nf=hex<Enter>", "<C-A>"), "-077", "-076");
  }

  public void testIncrementHexPreservesCaseOfX() {
    doTest(parseKeys("<C-A>"), "0X88", "0X89");
  }

  public void testIncrementHexTakesCaseFromLastLetter() {
    doTest(parseKeys("<C-A>"), "0xaB0", "0xAB1");
  }

  public void testIncrementLocatesNumberOnTheSameLine() {
    doTest(parseKeys("<C-A>"), "foo ->* bar 123\n", "foo ->* bar 12<caret>4\n");
  }
}
