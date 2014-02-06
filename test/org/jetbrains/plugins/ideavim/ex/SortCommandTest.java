package org.jetbrains.plugins.ideavim.ex;

import com.google.common.collect.Lists;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author Alex Selesse
 */
public class SortCommandTest extends VimTestCase {
  public void testBasicSort() {
    myFixture.configureByText("a.txt", "Test\n" + "Hello World!\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$j"));
    typeText(keys);
    runExCommand("sort");
    myFixture.checkResult("Hello World!\n" + "Test\n");
  }

  public void testMultipleSortLine() {
    myFixture.configureByText("a.txt", "zee\nyee\na\nb\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$3j"));
    typeText(keys);
    runExCommand("sort");
    myFixture.checkResult("a\nb\nyee\nzee\n");
  }

  public void testInverseSort() {
    myFixture.configureByText("a.txt", "kay\nzee\nyee\na\nb\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$4j"));
    typeText(keys);
    runExCommand("sort !");
    myFixture.checkResult("zee\nyee\nkay\nb\na\n");
  }

  public void testCaseSensitiveSort() {
    myFixture.configureByText("a.txt", "apple\nAppetite\nApp\napparition\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$3j"));
    typeText(keys);
    runExCommand("sort");
    myFixture.checkResult("App\nAppetite\napparition\napple\n");
  }

  public void testCaseInsensitiveSort() {
    myFixture.configureByText("a.txt", "apple\nAppetite\nApp\napparition\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$3j"));
    typeText(keys);
    runExCommand("sort i");
    myFixture.checkResult("App\napparition\nAppetite\napple\n");
  }

  public void testRangeSort() {
    myFixture.configureByText("a.txt", "zee\nc\na\nb\nwhatever\n");
    runExCommand("2,4sort");
    myFixture.checkResult("zee\na\nb\nc\nwhatever\n");
  }

  public void testNumberSort() {
    myFixture.configureByText("a.txt", "120\n70\n30\n2000");
    runExCommand("sort n");
    myFixture.checkResult("30\n70\n120\n2000");
  }

  public void testNaturalOrderSort() {
    myFixture.configureByText("a.txt", "hello1000\nhello102\nhello70000\nhello1001");
    runExCommand("sort n");
    myFixture.checkResult("hello102\nhello1000\nhello1001\nhello70000");
  }

  public void testNaturalOrderReverseSort() {
    myFixture.configureByText("a.txt", "hello1000\nhello102\nhello70000\nhello1001");
    runExCommand("sort n!");
    myFixture.checkResult("hello70000\nhello1001\nhello1000\nhello102");
  }

  public void testGlobalSort() {
    myFixture.configureByText("a.txt", "zee\nc\na\nb\nwhatever");
    runExCommand("sort");
    myFixture.checkResult("a\nb\nc\nwhatever\nzee");
  }
}
