package com.maddyhome.idea.vim.helper;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.lexer.XmlLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlTokenType;

/**
 * Helper class for manipulating tag blocks.
 * We try to comply as much as possible to the specs
 * of a tag block in vim: http://vimdoc.sourceforge.net/htmldoc/motion.html#tag-blocks
 */
public class TagBlockHelper {

  private enum State { LOOK_OPENING_TAG, LOOK_ENDING_TAG, END  }

  /**
   * Represents a tag block.
   * It has a name
   * a inner/outer start position
   * and an inner/outer end position
   *
   * Note that a TagBlock has sense only in context of a text parsed text.
   * therefore there is no equals/hashcode semantics.
   * Also note that a TagBlock is immutable.
   */
  public static class TagBlock {
    private int os = -1; // outer start
    private int is = -1; // inner start
    private int ie = -1; // inner end
    private int oe = -1; // outer end
    private String name = null; // name of the tag

    TagBlock() { }

    TagBlock(int os, int is, int ie, int oe, String name) {
      this.os = os;
      this.is = is;
      this.ie = ie;
      this.oe = oe;
      this.name = name;
    }

    TagBlock merge(TagBlock tagBlock) {

      if (tagBlock == null)
        return this; // we can do thig since TagBlock is immutable

      TagBlock result = new TagBlock();
      result.name = (tagBlock.name == null)? this.name: tagBlock.name;
      result.os = (tagBlock.os == -1)? this.os : tagBlock.os;
      result.is = (tagBlock.is == -1)? this.is : tagBlock.is;
      result.ie = (tagBlock.ie == -1)? this.ie: tagBlock.ie;
      result.oe = (tagBlock.oe == -1)? this.oe: tagBlock.oe;
      return result;
    }

    public int getOuterStart() {
      return os;
    }

    public int getInnerStart() {
      return is;
    }

    public int getInnerEnd() {
      return ie;
    }

    public int getOuterEnd() {
      return oe;
    }

    public int length() {
      return oe - os;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "TagBlock{" +
        "os=" + os +
        ", is=" + is +
        ", ie=" + ie +
        ", oe=" + oe +
        ", name='" + name + '\'' +
        '}';
    }
  }

  /** find the nearest tag block positioned forward in seq */
  static public TagBlock find(CharSequence seq, int pos) {

    XmlLexer lexer = new XmlLexer();
    lexer.start(seq, pos, seq.length() -1);
    lexer.advance();
    return find(lexer);
  }

  static private TagBlock find(Lexer lexer) {

    /* *********************************************
     *
     * state machine which:
     * 1 look for an open tag form : <name *...*>
     * 2 when opening tag found, look for the end tag form: </name
     *   2.* if we find an open tag form during end tag form lookup,
     *       recursively find tag block (step 1).
     *       Any nested tag block is discarded, the parsing continues after
     *       the nested tag block.
     *       If the nested tag block is malformed (or it was not a tag block)
     *       parsing resumes before the nested tag block.
     *
     * *********************************************/
    TagBlock result = null;
    TagBlock buildingTagBlock = new TagBlock();
    State state = State.LOOK_OPENING_TAG;
    while (lexer.getTokenType() != null
      && state != State.END) {

      switch (state) {
        case LOOK_OPENING_TAG:
          buildingTagBlock = buildingTagBlock.merge(lookForOpeningTag(lexer));
          if (buildingTagBlock.name != null) {
            state = State.LOOK_ENDING_TAG;
          }
          break;
        case LOOK_ENDING_TAG:
          buildingTagBlock = buildingTagBlock.merge(lookForClosingTag(lexer, buildingTagBlock.name));
          if (buildingTagBlock.oe != -1) {
            state = State.END;
            result = buildingTagBlock;
          }
          break;
        default:
          //NOOP
          break;
      }

      lexer.advance();
    }

    return result;
  }

  static private TagBlock lookForClosingTag(Lexer lexer, String name) {

    int ie = -1; // inner end. ie: position of '</' form
    int oe = -1; // outer end. ie: the last closing '>' of tag block

    while (lexer.getTokenType() != null && oe == -1) {

      IElementType tokenType = lexer.getTokenType();

      if (XmlTokenType.XML_START_TAG_START.equals(tokenType)) { // maybe a nested tag block

        LexerPosition lexerPosition = lexer.getCurrentPosition();
        TagBlock nestedTagBlock = find(lexer);

        if (nestedTagBlock == null) { // this is not a valid nested tag
          lexer.restore(lexerPosition); // we resume parsing at opening of 'not nested tag'
          lexer.advance();
        } else {
          continue; // we resume parsing at end of nested tag
        }
      } else if (XmlTokenType.XML_END_TAG_START.equals(tokenType)) {

        ie = lexer.getCurrentPosition().getOffset();
        lexer.advance();
        if (XmlTokenType.XML_NAME == lexer.getTokenType()
          && name.equals(lexer.getTokenText().toUpperCase())) {

          oe = lexer.getCurrentPosition().getOffset() + lexer.getTokenText().length();
        }
      }
      lexer.advance();
    }
    return new TagBlock(-1, -1, ie, oe, name);
  }

  private static TagBlock lookForOpeningTag(Lexer lexer) {

    int ob; // outer begin, ie: first '<' of the tag block
    int ib; // inner begin, ie: closing '>' of the opening tag of tag block
    String name;

    //
    if (lexer.getTokenType() != XmlTokenType.XML_START_TAG_START)
      return null;

    ob = lexer.getCurrentPosition().getOffset();

    //
    lexer.advance();
    if (lexer.getTokenType() != XmlTokenType.XML_NAME)
      return null;

    name = lexer.getTokenText().toUpperCase();

    //
    while (lexer.getTokenType() != XmlTokenType.XML_TAG_END
      && lexer.getTokenType() != null) {
      lexer.advance();
    }
    if (lexer.getTokenType() != XmlTokenType.XML_TAG_END)
      return null;
    ib = lexer.getCurrentPosition().getOffset() + 1;

    return new TagBlock(ob, ib, -1, -1, name);
  }
}
