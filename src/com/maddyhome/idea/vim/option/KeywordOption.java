package com.maddyhome.idea.vim.option;

import org.apache.commons.lang.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class KeywordOption extends ListOption {
  @NotNull private final Pattern validationPattern;

  // KeywordSpecs are the option values in reverse order
  @NotNull private List<KeywordSpec> keywordSpecs = new ArrayList<>();

  public KeywordOption(@NotNull String name, @NotNull String abbrev, @NotNull String[] defaultValue) {
    super(name, abbrev, defaultValue,
          "(\\^?(([^0-9^]|[0-9]{1,3})-([^0-9]|[0-9]{1,3})|([^0-9^]|[0-9]{1,3})),)*\\^?(([^0-9^]|[0-9]{1,3})-([^0-9]|[0-9]{1,3})|([^0-9]|[0-9]{1,3})),?$");
    validationPattern = Pattern.compile(pattern);
    set(getValue());
  }

  @Override
  public boolean append(@NotNull String val) {
    final List<String> vals = parseVals(val);
    final List<KeywordSpec> specs = valsToValidatedAndReversedSpecs(vals);
    if (vals == null || specs == null || value == null) {
      return false;
    }
    value.addAll(vals);
    keywordSpecs.addAll(0, specs);
    fireOptionChangeEvent();
    return true;
  }

  @Override
  public boolean prepend(@NotNull String val) {
    final List<String> vals = parseVals(val);
    final List<KeywordSpec> specs = valsToValidatedAndReversedSpecs(vals);
    if (vals == null || specs == null || value == null) {
      return false;
    }
    value.addAll(0, vals);
    keywordSpecs.addAll(specs);
    fireOptionChangeEvent();
    return true;
  }


  @Override
  public boolean remove(@NotNull String val) {
    final List<String> vals = parseVals(val);
    final List<KeywordSpec> specs = valsToValidatedAndReversedSpecs(vals);
    if (vals == null || specs == null || value == null) {
      return false;
    }
    value.removeAll(vals);
    keywordSpecs.removeAll(specs);
    fireOptionChangeEvent();
    return true;
  }

  @Override
  public boolean set(@NotNull String val) {
    final List<String> vals = parseVals(val);
    final List<KeywordSpec> specs = valsToValidatedAndReversedSpecs(vals);
    if (vals == null || specs == null || value == null) {
      return false;
    }
    value = vals;
    keywordSpecs = specs;
    fireOptionChangeEvent();
    return true;
  }

  @Override
  public void resetDefault() {
    if (!dflt.equals(value)) {
      value = dflt;
      set(getValue());
    }
  }

  @Nullable
  private List<KeywordSpec> valsToValidatedAndReversedSpecs(@Nullable List<String> vals) {
    final List<KeywordSpec> specs = new ArrayList<>();
    if (vals != null) {
      for (String val : vals) {
        KeywordSpec spec = new KeywordSpec(val);
        if (!spec.isValid()) {
          return null;
        }
        specs.add(spec);
      }
      Collections.reverse(specs);
    }
    return specs;
  }

  @Nullable
  @Override
  protected List<String> parseVals(@NotNull String content) {
    if (!validationPattern.matcher(content).matches()) {
      return null;
    }

    int index = 0;
    boolean firstCharNumOfPart = true;
    boolean inRange = false;

    final List<String> vals = new ArrayList<>();
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

        vals.add(part);
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

    return vals;
  }

  public boolean isKeyword(char c) {
    final int code = (int)c;
    if (code >= '\u0100') {
      return true;
    }
    for (KeywordSpec spec : keywordSpecs) {
      if (spec.contains(code)) {
        return !spec.negate();
      }
    }
    return false;
  }

  private static final class KeywordSpec {
    private String part;
    private boolean negate;
    private boolean isRange = false;
    private boolean isAllLetters = false;
    private Integer rangeLow;
    private Integer rangeHigh;

    public KeywordSpec(@NotNull String part) {

      this.part = part;

      negate = part.matches("^\\^.+");

      if (negate) {
        part = part.substring(1);
      }

      final String[] keywords = part.split("(?<=.)-(?=.+)");

      if (keywords.length > 1 || keywords[0].equals("@")) {
        isRange = true;
        if (keywords.length > 1) {
          rangeLow = toUnicode(keywords[0]);
          rangeHigh = toUnicode(keywords[1]);
        }
        else {
          isAllLetters = true;
        }
      }
      else {
        int keyword = toUnicode(keywords[0]);
        rangeLow = keyword;
        rangeHigh = keyword;
      }
    }

    private int toUnicode(@NotNull String str) {
      if (NumberUtils.isNumber(str)) {
        return Integer.parseInt(str); // If we have a number, it represents the Unicode code point of a letter
      }
      else {
        return (int)str.charAt(0); // If it's not a number we should only have strings consisting of one char
      }
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      KeywordSpec that = (KeywordSpec)o;

      return part.equals(that.part);
    }

    @Override
    public int hashCode() {
      return part.hashCode();
    }

    public boolean isValid() {
      return (!isRange || isAllLetters) || (rangeLow <= rangeHigh);
    }

    public boolean negate() {
      return negate;
    }

    public boolean contains(int code) {
      if (isAllLetters) {
        return Character.isLetter(code);
      }
      if (isRange) {
        return (code >= rangeLow && code <= rangeHigh);
      }
      return code == rangeLow;
    }
  }
}
