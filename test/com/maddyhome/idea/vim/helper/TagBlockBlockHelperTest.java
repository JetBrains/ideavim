package com.maddyhome.idea.vim.helper;

import org.junit.Test;

import static com.maddyhome.idea.vim.helper.TagBlockHelper.*;
import static org.junit.Assert.*;

public class TagBlockBlockHelperTest {

  @Test
  public void testSimpleTag() {
    String chars = "<a></a>";

    TagBlock t = find(chars, 0);

    assertEquals(0, t.getOuterStart());
    assertEquals(3, t.getInnerStart());
    assertEquals(3, t.getInnerEnd());
    assertEquals(6, t.getOuterEnd());
  }

  @Test
  public void testTagPosition() {

    String chars = "abcde<tag>fg</tag>hi";

    TagBlock t = find(chars, 0);

    assertEquals(5, t.getOuterStart());
    assertEquals(17, t.getOuterEnd());
  }

  @Test
  public void testTagWithDifferentCase() {
    String chars = "<a></A>";

    TagBlock t = find(chars, 0);

    assertNotNull("finding tag should not be case sensitive", t);
    assertEquals(0, t.getOuterStart());
    assertEquals(3, t.getInnerStart());
    assertEquals(3, t.getInnerEnd());
    assertEquals(6, t.getOuterEnd());
  }

  @Test
  public void testTagWithParameters() {

    String chars = "<a lang=\"end\">Inner</a>";

    TagBlock t = find(chars, 0);

    assertEquals(0, t.getOuterStart());
    assertEquals(14, t.getInnerStart());
    assertEquals(19, t.getInnerEnd());
    assertEquals(22, t.getOuterEnd());
  }

  @Test
  public void testNestedTagsWithSameNames() {

    String chars = "<a><a></a></a>";

    TagBlock t = find(chars, 0);

    assertEquals(0, t.getOuterStart());
    assertEquals(3, t.getInnerStart());
    assertEquals(10, t.getInnerEnd());
    assertEquals(13, t.getOuterEnd());
  }


  @Test
  public void testNestedTagsWithDifferentNames() {

    String chars = "<a><b></b></a>";

    TagBlock t = find(chars, 0);

    assertEquals(0, t.getOuterStart());
    assertEquals(3, t.getInnerStart());
    assertEquals(10, t.getInnerEnd());
    assertEquals(13, t.getOuterEnd());
  }

  @Test
  public void testNestedTagsWithLesserThanSymbol() {

    String chars = "<a> 1 < 2 </a>";

    TagBlock t = find(chars, 0);

    assertEquals(0, t.getOuterStart());
    assertEquals(3, t.getInnerStart());
    assertEquals(10, t.getInnerEnd());
    assertEquals(13, t.getOuterEnd());
  }


  @Test
  public void testNestedTagsWithLesserThanSymbolNearEnd() {

    String chars = "<a> 1  2 <</a>";

    TagBlock t = find(chars, 0);

    assertEquals(0, t.getOuterStart());
    assertEquals(3, t.getInnerStart());
    assertEquals(10, t.getInnerEnd());
    assertEquals(13, t.getOuterEnd());
  }

  @Test
  public void testMalformedNestedTags() {

    String chars = "<a><a></a>";

    TagBlock t = find(chars, 0);
    assertNull(t);
  }


  @Test
  public void testMalformedNestedTags2() {

    String chars = "< a></a>";

    TagBlock t = find(chars, 0);
    assertNull(t);
  }


  @Test
  public void testMalformedNestedTags3() {

    String chars = "< a</a>";

    TagBlock t = find(chars, 0);
    assertNull(t);
  }


  @Test
  public void testMalformedNestedTags4() {

    String chars = "<a ";

    TagBlock t = find(chars, 0);
    assertNull(t);
  }
}