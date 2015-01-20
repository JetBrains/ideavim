package com.maddyhome.idea.vim.option;

import com.maddyhome.idea.vim.helper.CharacterHelper;
import org.apache.commons.lang.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

public class KeywordOption extends ListOption {

  Pattern validationPattern;

  /**
   * Creates the option
   *
   * @param name   The name of the option
   * @param abbrev The short name
   * @param dflt   The option's default values
   */
  public KeywordOption(String name, String abbrev, String[] dflt) {
    super(name, abbrev, dflt,
          "(\\^?(([^0-9^]|[0-9]{1,3})-([^0-9]|[0-9]{1,3})|([^0-9^]|[0-9]{1,3})),)*\\^?(([^0-9^]|[0-9]{1,3})-([^0-9]|[0-9]{1,3})|([^0-9]|[0-9]{1,3})),?$");
    validationPattern = Pattern.compile(pattern);
  }

  protected List<String> parseVals(String content) {

    // We have two stages to validation. This is the first stage, which makes sure the input string has the right format.
    if (!validationPattern.matcher(content).matches()) {
      return null;
    }

    List<String> result = new ArrayList<String>();

    int index = 0;
    boolean firstCharNumOfPart = true;
    boolean inRange = false;

    StringBuilder option = new StringBuilder();

    // We need to split the input string into parts. However, we can't just split on a comma
    // since a comma can either be a keyword or a separator depending on its location in the string.
    while (index <= content.length()) {

      char curChar = 0;
      if (index < content.length()) {
        curChar = content.charAt(index);
      }

      index++;

      // If we either have a comma separator or are at the end of the content...
      if (curChar == ',' && (!firstCharNumOfPart && !inRange) || index == content.length() + 1) {

        String part = option.toString();

        // This is the second stage of validation, where we check whether the values inputted are valid.
        KeywordSpec spec = new KeywordSpec(part);
        if (!spec.isValid()){
          return null;
        }

        result.add(part);
        option = new StringBuilder();
        inRange = false;
        firstCharNumOfPart = true;
        continue;
      }

      option.append(curChar);

      if (curChar == '^' && option.length() == 1) {
        firstCharNumOfPart = true;
        continue;
      }

      if (curChar == '-' && !firstCharNumOfPart) {
        inRange = true;
        continue;
      }

      firstCharNumOfPart = false;
      inRange = false;
    }

    return result;
  }

  public static class KeywordSpec implements Enumeration<Integer> {

    private static ArrayList<Integer> letters = new ArrayList<Integer>();
    private boolean negate = false;
    private boolean isRange = false;
    private boolean isAllLetters = false;
    private Integer rangeLow;
    private Integer rangeHigh;
    private int currIndex = 0;

    static {
      // @ represents all letters, including those with umlauts, accents, etc., so we prepopulate
      // a list of list of letters upfront
      for (int i = 0; i < 256; i++) {
        if (Character.isLetter(i)) {
          letters.add(i);
        }
      }
    }

    public KeywordSpec(String input) {

      negate = input.matches("^\\^.+");

      if (negate){
        input = input.substring(1);
      }

      String[] keywords = input.split("(?<=.+)-(?=.+)");

      if (keywords.length > 1 || keywords[0].equals("@")){
        isRange = true;
        if (keywords.length > 1) {
          rangeLow = toAscii(keywords[0]);
          rangeHigh = toAscii(keywords[1]);
        } else {
          isAllLetters = true;
        }
      } else {
        int keyword = toAscii(keywords[0]);
        rangeLow = keyword;
        rangeHigh = keyword;
      }

      if (!isAllLetters) {
        currIndex = rangeLow;
      }
    }

    public boolean isValid() {
      return (!isRange || isAllLetters) || (rangeLow <= rangeHigh);
    }

    public boolean isRange() {
      return isRange;
    }

    public boolean negate() {
      return negate;
    }

    @Override
    public boolean hasMoreElements() {
        if (!isAllLetters){
          return (currIndex <= rangeHigh);
        }
        else {
          return (currIndex < letters.size());
        }
    }

    @Override
    public Integer nextElement() {
      int asciiCode = (isAllLetters) ? letters.get(currIndex) : currIndex;
      currIndex++;
      return asciiCode;
    }

    private int toAscii(String str) {
      if (NumberUtils.isNumber(str)) {
        return Integer.parseInt(str); // If we have a number, it represents the ASCII code of a letter
      }
      else {
        return (int)str.charAt(0); // If it's not a number we should only have strings consisting of one char
      }
    }
  }
}
