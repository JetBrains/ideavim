package org.jetbrains.plugins.ideavim.regexp;

import com.maddyhome.idea.vim.regexp.CharacterClasses;
import org.junit.Assert;
import org.junit.Test;

public class CharacterClassesTest {

  @Test
  public void testIsMaskTrueWhitespace() {
    Assert.assertTrue(CharacterClasses.isMask(' ', CharacterClasses.RI_WHITE, 1));
  }

  @Test
  public void testIsDigitWithLetter() {
    Assert.assertFalse(CharacterClasses.isDigit('a'));
  }

  @Test
  public void testIsHexWithHexLetter() {
    Assert.assertTrue(CharacterClasses.isHex('F'));
  }

  @Test
  public void testIsOctalWithOctalDigit() {
    Assert.assertTrue(CharacterClasses.isOctal('7'));
  }

  @Test
  public void testIsWordWithDigit() {
    Assert.assertTrue(CharacterClasses.isWord('2'));
  }

  @Test
  public void testIsWordWithDot() {
    Assert.assertFalse(CharacterClasses.isWord('.'));
  }

  @Test
  public void testIsWordWithUnderscore() {
    Assert.assertTrue(CharacterClasses.isWord('_'));
  }

  @Test
  public void testIsHeadWithDot() {
    Assert.assertFalse(CharacterClasses.isHead('.'));
  }

  @Test
  public void testIsHeadWithUnderscore() {
    Assert.assertTrue(CharacterClasses.isHead('_'));
  }

  @Test
  public void testIsHeadWithLetter() {
    Assert.assertTrue(CharacterClasses.isHead('Z'));
  }

  @Test
  public void testIsAlphaWithDot() {
    Assert.assertFalse(CharacterClasses.isAlpha('.'));
  }

  @Test
  public void testIsLowerWithUppercase() {
    Assert.assertFalse(CharacterClasses.isLower('Z'));
  }

  @Test
  public void testIsUpperWithLowercase() {
    Assert.assertFalse(CharacterClasses.isUpper('z'));
  }

  @Test
  public void testIsWhiteWithDot() {
    Assert.assertFalse(CharacterClasses.isWhite('.'));
  }

  @Test
  public void testIsGraphWithTab() {
    Assert.assertFalse(CharacterClasses.isGraph('\t'));
  }

  @Test
  public void testIsGraphWithDot() {
    Assert.assertTrue(CharacterClasses.isGraph('.'));
  }

  @Test
  public void testIsPrintWithTab() {
    Assert.assertFalse(CharacterClasses.isPrint('\t'));
  }

  @Test
  public void testIsPrintWithUnderscore() {
    Assert.assertTrue(CharacterClasses.isPrint('_'));
  }

  @Test
  public void testIsPunctWithDigit() {
    Assert.assertFalse(CharacterClasses.isPunct('1'));
  }

  @Test
  public void testIsPunctWithTab() {
    Assert.assertFalse(CharacterClasses.isPunct('\t'));
  }

  @Test
  public void testIsPunctWithBracket() {
    Assert.assertTrue(CharacterClasses.isPunct('('));
  }

  @Test
  public void testIsFileWithDigit() {
    Assert.assertTrue(CharacterClasses.isFile('2'));
  }

  @Test
  public void testIsFileWithTab() {
    Assert.assertFalse(CharacterClasses.isFile('\t'));
  }
}
