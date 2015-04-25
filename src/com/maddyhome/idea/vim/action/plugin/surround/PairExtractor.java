package com.maddyhome.idea.vim.action.plugin.surround;

/**
 *
 * @author dhleong
 */
public abstract class PairExtractor {

  public interface PairListener {
    void onPair(SurroundPair pair);
  }

  static PairExtractor[] handlers = new PairExtractor[] {
    // Should we make these proper classes?
    new PairExtractor("b()B{}r[]a<>") {
      @Override
      void handle(char chKey, PairListener notify) {

        final int index = matchingChars.indexOf(chKey);
        final String spc = (index % 3) == 1 ? " " : "";
        final int idx = index / 3 * 3;
        final String before = matchingChars.charAt(idx + 1) + spc;
        final String after  = spc + matchingChars.charAt(idx + 2);

        notify.onPair(new SurroundPair(before, after));
      }
    }
  };


  String matchingChars;

  PairExtractor(String matchingChars) {
    this.matchingChars = matchingChars;
  }

  abstract void handle(char chKey, PairListener notify);

  public static void extract(char chKey, PairListener listener) {

    // should we index into a map here?
    for (PairExtractor extractor : handlers) {
      if (-1 != extractor.matchingChars.indexOf(chKey)) {
        extractor.handle(chKey, listener);
        return;
      }
    }

    // default:
    if (Character.isLetter(chKey)) {
      listener.onPair(new SurroundPair(chKey, chKey));
    }

    // else nothing, I guess
  }
}
