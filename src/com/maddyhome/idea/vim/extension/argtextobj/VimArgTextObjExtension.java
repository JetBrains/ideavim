package com.maddyhome.idea.vim.extension.argtextobj;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.handler.TextObjectActionHandler;
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor;
import com.maddyhome.idea.vim.listener.VimListenerSuppressor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Stack;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping;
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping;
import static com.maddyhome.idea.vim.group.visual.VisualGroupKt.vimSetSelection;
import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author igrekster
 */

public class VimArgTextObjExtension implements VimExtension {

  @Override
  public @NotNull String getName() {
    return "argtextobj";
  }

  @Override
  public void init() {

    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>InnerArgument"), getOwner(), new VimArgTextObjExtension.ArgumentHandler(true), false);
    putExtensionHandlerMapping(MappingMode.XO, parseKeys("<Plug>OuterArgument"), getOwner(), new VimArgTextObjExtension.ArgumentHandler(false), false);

    putKeyMapping(MappingMode.XO, parseKeys("ia"), getOwner(), parseKeys("<Plug>InnerArgument"), true);
    putKeyMapping(MappingMode.XO, parseKeys("aa"), getOwner(), parseKeys("<Plug>OuterArgument"), true);

  }

  /**
   * The pairs of brackets that delimit different types of argument lists.
   */
  static private class BracketPairs {
    // NOTE: brackets must match by the position, and ordered by rank (highest to lowest).
    @NotNull private final String openBrackets;
    @NotNull private final String closeBrackets;

    static class ParseError extends Exception {
      public ParseError(@NotNull String message) {
        super(message);
      }
    }

    private enum ParseState {
      OPEN,
      COLON,
      CLOSE,
      COMMA,
    }

    /**
     * Constructs @ref BracketPair from a string of bracket pairs with the same syntax
     * as VIM's @c matchpairs option: "(:),{:},[:]"
     *
     * @param bracketPairs comma-separated list of colon-separated bracket pairs.
     * @throws ParseError if a syntax error is detected.
     */
    @NotNull
    static BracketPairs fromBracketPairList(@NotNull final String bracketPairs) throws ParseError {
      StringBuilder openBrackets = new StringBuilder();
      StringBuilder closeBrackets = new StringBuilder();
      ParseState state = ParseState.OPEN;
      for (char ch : bracketPairs.toCharArray()) {
        switch (state) {
          case OPEN:
            openBrackets.append(ch);
            state = ParseState.COLON;
            break;
          case COLON:
            if (ch == ':') {
              state = ParseState.CLOSE;
            } else {
              throw new ParseError("expecting ':', but got '" + ch + "' instead");
            }
            break;
          case CLOSE:
            final char lastOpenBracket = openBrackets.charAt(openBrackets.length() - 1);
            if (lastOpenBracket == ch) {
              throw new ParseError("open and close brackets must be different");
            }
            closeBrackets.append(ch);
            state = ParseState.COMMA;
            break;
          case COMMA:
            if (ch == ',') {
              state = ParseState.OPEN;
            } else {
              throw new ParseError("expecting ',', but got '" + ch + "' instead");
            }
            break;
        }
      }
      if (state != ParseState.COMMA) {
        throw new ParseError("list of pairs is incomplete");
      }
      return new BracketPairs(openBrackets.toString(), closeBrackets.toString());
    }

    BracketPairs(@NotNull final String openBrackets, @NotNull final String closeBrackets) {
      assert openBrackets.length() == closeBrackets.length();
      this.openBrackets = openBrackets;
      this.closeBrackets = closeBrackets;
    }

    int getBracketPrio(char ch) {
      return Math.max(openBrackets.indexOf(ch), closeBrackets.indexOf(ch));
    }

    char matchingBracket(char ch) {
      int idx = closeBrackets.indexOf(ch);
      if (idx != -1) {
        return openBrackets.charAt(idx);
      } else {
        assert isOpenBracket(ch);
        idx = openBrackets.indexOf(ch);
        return closeBrackets.charAt(idx);
      }
    }

    boolean isCloseBracket(final int ch) {
      return closeBrackets.indexOf(ch) != -1;
    }

    boolean isOpenBracket(final int ch) {
      return openBrackets.indexOf(ch) != -1;
    }
  }

  public static final BracketPairs DEFAULT_BRACKET_PAIRS = new BracketPairs("(", ")");

  @Nullable
  private static String bracketPairsVariable() {
    final VimScriptGlobalEnvironment env = VimScriptGlobalEnvironment.getInstance();
    final Object value = env.getVariables().get("g:argtextobj_pairs");
    if (value instanceof String) {
      return (String) value;
    }
    return null;
  }

  /**
   * A text object for an argument to a function definition or a call.
   */
  static class ArgumentHandler implements VimExtensionHandler {
    final boolean isInner;

    ArgumentHandler(boolean isInner) {
      super();
      this.isInner = isInner;
    }

    static class ArgumentTextObjectHandler extends TextObjectActionHandler {
      private final boolean isInner;

      ArgumentTextObjectHandler(boolean isInner) {
        this.isInner = isInner;
      }

      @Override
      public @Nullable TextRange getRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count, int rawCount, @Nullable Argument argument) {
        BracketPairs bracketPairs = DEFAULT_BRACKET_PAIRS;
        final String bracketPairsVar = bracketPairsVariable();
        if (bracketPairsVar != null) {
          try {
            bracketPairs = BracketPairs.fromBracketPairList(bracketPairsVar);
          } catch (BracketPairs.ParseError parseError) {
            VimPlugin.showMessage("argtextobj: Invalid value of g:argtextobj_pairs -- " + parseError.getMessage());
            VimPlugin.indicateError();
            return null;
          }
        }
        final ArgBoundsFinder finder = new ArgBoundsFinder(editor.getDocument(), bracketPairs);
        int pos = caret.getOffset();

        for (int i = 0; i < count; ++i) {
          if (!finder.findBoundsAt(pos)) {
            VimPlugin.showMessage(finder.errorMessage());
            VimPlugin.indicateError();
            return null;
          }
          if (i + 1 < count) {
            finder.extendTillNext();
          }

          pos = finder.getRightBound();
        }

        if (isInner) {
          finder.adjustForInner();
        } else {
          finder.adjustForOuter();
        }
        return new TextRange(finder.getLeftBound(), finder.getRightBound());
      }

      @NotNull
      @Override
      public TextObjectVisualType getVisualType() {
        return TextObjectVisualType.CHARACTER_WISE;
      }
    }

    @Override
    public void execute(@NotNull Editor editor, @NotNull DataContext context) {

      @NotNull CommandState commandState = CommandState.getInstance(editor);
      int count = Math.max(1, commandState.getCommandBuilder().getCount());

      final ArgumentTextObjectHandler textObjectHandler = new ArgumentTextObjectHandler(isInner);
      if (!commandState.isOperatorPending()) {
        editor.getCaretModel().runForEachCaret((Caret caret) -> {
          final TextRange range = textObjectHandler.getRange(editor, caret, context, count, 0, null);
          if (range != null) {
            try (VimListenerSuppressor.Locked ignored = SelectionVimListenerSuppressor.INSTANCE.lock()) {
              if (commandState.getMode() == CommandState.Mode.VISUAL) {
                vimSetSelection(caret, range.getStartOffset(), range.getEndOffset() - 1, true);
              } else {
                caret.moveToOffset(range.getStartOffset());
              }
            }
          }

        });
      } else {
        commandState.getCommandBuilder().completeCommandPart(new Argument(new Command(count,
          textObjectHandler, Command.Type.MOTION, EnumSet.noneOf(CommandFlags.class))));
      }
    }
  }

  /**
   * Helper class to find argument boundaries starting at the specified
   * position
   */
  private static class ArgBoundsFinder {
    @NotNull private final CharSequence text;
    @NotNull private final Document document;
    @NotNull private final BracketPairs brackets;
    private int leftBound = Integer.MAX_VALUE;
    private int rightBound = Integer.MIN_VALUE;
    private int leftBracket;
    private int rightBracket;
    private String error = null;
    private static final String QUOTES = "\"'";

    private static final int MAX_SEARCH_LINES = 10;
    private static final int MAX_SEARCH_OFFSET = MAX_SEARCH_LINES * 80;

    ArgBoundsFinder(@NotNull Document document, @NotNull BracketPairs bracketPairs) {
      this.text = document.getImmutableCharSequence();
      this.document = document;
      this.brackets = bracketPairs;
    }

    /**
     * Finds left and right boundaries of an argument at the specified
     * position. If successful @ref getLeftBound() will point to the left
     * argument delimiter and @ref getRightBound() will point to the right
     * argument delimiter. Use @ref adjustForInner or @ref adjustForOuter to
     * fix the boundaries based on the type of text object.
     *
     * @param position starting position.
     */
    boolean findBoundsAt(int position) throws IllegalStateException {
      if (text.length() == 0) {
        error = "empty document";
        return false;
      }
      leftBound = Math.min(position, leftBound);
      rightBound = Math.max(position, rightBound);
      getOutOfQuotedText();
      if (rightBound == leftBound) {
        if (brackets.isCloseBracket(getCharAt(rightBound))) {
          --leftBound;
        } else {
          ++rightBound;
        }
      }
      int nextLeft = leftBound;
      int nextRight = rightBound;
      final int leftLimit = leftLimit(position);
      final int rightLimit = rightLimit(position);
      //
      // Try to extend the bounds until one of the bounds is a comma.
      // This handles cases like: fun(a, (30 + <cursor>x) * 20, c)
      //
      boolean bothBrackets;
      do {
        leftBracket = nextLeft;
        rightBracket = nextRight;
        if (!findOuterBrackets(leftLimit, rightLimit)) {
          error = "not inside argument list";
          return false;
        }
        leftBound = nextLeft;
        findLeftBound();
        nextLeft = leftBound - 1;
        rightBound = nextRight;
        findRightBound();
        nextRight = rightBound + 1;
        //
        // If reached text boundaries
        //
        if (nextLeft < leftLimit || nextRight > rightLimit) {
          error = "not an argument";
          return false;
        }
        bothBrackets = getCharAt(leftBound) != ',' && getCharAt(rightBound) != ',';
        final boolean nonEmptyArg = (rightBound - leftBound) > 1;
        if (bothBrackets && nonEmptyArg && isIdentPreceding()) {
          // Looking at a pair of brackets preceded by an
          // identifier -- single argument function call.
          break;
        }
      }
      while (leftBound > leftLimit && rightBound < rightLimit && bothBrackets);
      return true;
    }

    /**
     * Skip left delimiter character and any following whitespace.
     */
    void adjustForInner() {
      ++leftBound;
      while (leftBound < rightBound && Character.isWhitespace(getCharAt(leftBound))) {
        ++leftBound;
      }
    }

    /**
     * Exclude left bound character for the first argument, include the
     * right bound character and any following whitespace.
     */
    void adjustForOuter() {
      if (getCharAt(leftBound) != ',') {
        ++leftBound;
        extendTillNext();
      }
    }

    /**
     * Extend the right bound to the beginning of the next argument (if any).
     */
    void extendTillNext() {
      if (rightBound + 1 < rightBracket && getCharAt(rightBound) == ',') {
        ++rightBound;
        while (rightBound + 1 < rightBracket && Character.isWhitespace(getCharAt(rightBound))) {
          ++rightBound;
        }
      }
    }

    int getLeftBound() {
      return leftBound;
    }

    int getRightBound() {
      return rightBound;
    }

    private boolean isIdentPreceding() {
      int i = leftBound - 1;
      final int idEnd = i;
      while (i >= 0 && Character.isJavaIdentifierPart(getCharAt(i))) {
        --i;
      }
      return (idEnd - i) > 0 && Character.isJavaIdentifierStart(getCharAt(i + 1));
    }

    /**
     * Detects if current position is inside a quoted string and adjusts
     * left and right bounds to the boundaries of the string.
     *
     * NOTE: Does not support line continuations for quoted string ('\' at the end of line).
     */
    private void getOutOfQuotedText() {
      // TODO this method should use IdeaVim methods to determine if the current position is in the string
      final int lineNo = document.getLineNumber(leftBound);
      final int lineStartOffset = document.getLineStartOffset(lineNo);
      final int lineEndOffset = document.getLineEndOffset(lineNo);
      int i = lineStartOffset;
      while (i <= leftBound) {
        if (isQuote(i)) {
          final int endOfQuotedText = skipQuotedTextForward(i, lineEndOffset);
          if (endOfQuotedText >= leftBound) {
            leftBound = i - 1;
            rightBound = endOfQuotedText + 1;
            break;
          } else {
            i = endOfQuotedText;
          }
        }
        ++i;
      }
    }

    private void findRightBound() {
      while (rightBound < rightBracket) {
        final char ch = getCharAt(rightBound);
        if (ch == ',') {
          break;
        }
        if (brackets.isOpenBracket(ch)) {
          rightBound = skipSexp(rightBound, rightBracket, SexpDirection.forward(brackets));
        } else {
          if (isQuoteChar(ch)) {
            rightBound = skipQuotedTextForward(rightBound, rightBracket);
          }
          ++rightBound;
        }
      }
    }

    private void findLeftBound() {
      while (leftBound > leftBracket) {
        final char ch = getCharAt(leftBound);
        if (ch == ',') {
          break;
        }
        if (brackets.isCloseBracket(ch)) {
          leftBound = skipSexp(leftBound, leftBracket, SexpDirection.backward(brackets));
        } else {
          if (isQuoteChar(ch)) {
            leftBound = skipQuotedTextBackward(leftBound, leftBracket);
          }
          --leftBound;
        }
      }
    }

    private boolean isQuote(final int i) {
      return QUOTES.indexOf(getCharAt(i)) != -1;
    }

    private static boolean isQuoteChar(final int ch) {
      return QUOTES.indexOf(ch) != -1;
    }

    private char getCharAt(int logicalOffset) {
      assert logicalOffset < text.length();
      return text.charAt(logicalOffset);
    }

    private int skipQuotedTextForward(final int start, final int end) {
      assert start < end;
      final char quoteChar = getCharAt(start);
      boolean backSlash = false;
      int i = start + 1;

      while (i <= end) {
        final char ch = getCharAt(i);
        if (ch == quoteChar && !backSlash) {
          // Found a matching quote, and it's not escaped.
          break;
        } else {
          backSlash = ch == '\\' && !backSlash;
        }
        ++i;
      }
      return i;
    }

    private int skipQuotedTextBackward(final int start, final int end) {
      assert start > end;
      final char quoteChar = getCharAt(start);
      int i = start - 1;

      while (i > end) {
        final char ch = getCharAt(i);
        final char prevChar = getCharAt(i - 1);
        // NOTE: doesn't handle cases like \\"str", but they make no
        //       sense anyway.
        if (ch == quoteChar && prevChar != '\\') {
          // Found a matching quote, and it's not escaped.
          break;
        }
        --i;
      }
      return i;
    }

    private int leftLimit(final int pos) {
      final int offsetLimit = Math.max(pos - MAX_SEARCH_OFFSET, 0);
      final int lineNo = document.getLineNumber(pos);
      final int lineOffsetLimit = document.getLineStartOffset(Math.max(0, lineNo - MAX_SEARCH_LINES));
      return Math.max(offsetLimit, lineOffsetLimit);
    }

    private int rightLimit(final int pos) {
      final int offsetLimit = Math.min(pos + MAX_SEARCH_OFFSET, text.length());
      final int lineNo = document.getLineNumber(pos);
      final int lineOffsetLimit = document.getLineEndOffset(Math.min(document.getLineCount() - 1, lineNo + MAX_SEARCH_LINES));
      return Math.min(offsetLimit, lineOffsetLimit);
    }

    String errorMessage() {
      return error;
    }

    /**
     * Interface to parametrise S-expression traversal direction.
     */
    abstract static class SexpDirection {
      abstract int delta();

      abstract boolean isOpenBracket(char ch);

      abstract boolean isCloseBracket(char ch);

      abstract int skipQuotedText(int pos, int end, ArgBoundsFinder self);

      static SexpDirection forward(BracketPairs brackets) {
        return new SexpDirection() {
          @Override
          int delta() {
            return 1;
          }

          @Override
          boolean isOpenBracket(char ch) {
            return brackets.isOpenBracket(ch);
          }

          @Override
          boolean isCloseBracket(char ch) {
            return brackets.isCloseBracket(ch);
          }

          @Override
          int skipQuotedText(int pos, int end, ArgBoundsFinder self) {
            return self.skipQuotedTextForward(pos, end);
          }
        };
      }

      static SexpDirection backward(BracketPairs brackets) {
        return new SexpDirection() {
          @Override
          int delta() {
            return -1;
          }

          @Override
          boolean isOpenBracket(char ch) {
            return brackets.isCloseBracket(ch);
          }

          @Override
          boolean isCloseBracket(char ch) {
            return brackets.isOpenBracket(ch);
          }

          @Override
          int skipQuotedText(int pos, int end, ArgBoundsFinder self) {
            return self.skipQuotedTextBackward(pos, end);
          }
        };
      }
    }

    /**
     * Skip over an S-expression considering priorities when unbalanced.
     *
     * @param start position of the starting bracket.
     * @param end   maximum position
     * @param dir   direction instance
     * @return position after the S-expression, or the next to the start position if
     * unbalanced.
     */
    private int skipSexp(final int start, final int end, SexpDirection dir) {
      char lastChar = getCharAt(start);
      assert dir.isOpenBracket(lastChar);
      Stack<Character> bracketStack = new Stack<>();
      bracketStack.push(lastChar);
      int i = start + dir.delta();
      while (!bracketStack.empty() && i != end) {
        final char ch = getCharAt(i);
        if (dir.isOpenBracket(ch)) {
          bracketStack.push(ch);
        } else {
          if (dir.isCloseBracket(ch)) {
            if (bracketStack.lastElement() == brackets.matchingBracket(ch)) {
              bracketStack.pop();
            } else {
              //noinspection StatementWithEmptyBody
              if (brackets.getBracketPrio(ch) < brackets.getBracketPrio(bracketStack.lastElement())) {
                // (<...) ->  (...)
                bracketStack.pop();
                // Retry the same character again for cases like (...<<...).
                continue;
              } else {                        // Unbalanced brackets -- check ranking.
                // Ignore lower-priority closing brackets.
                // (...> ->  (....
              }
            }
          } else {
            if (isQuoteChar(ch)) {
              i = dir.skipQuotedText(i, end, this);
            }
          }
        }
        i += dir.delta();
      }
      if (bracketStack.empty()) {
        return i;
      } else {
        return start + dir.delta();
      }
    }

    /**
     * Find a pair of brackets surrounding (leftBracket..rightBracket) block.
     *
     * @param start minimum position to look for
     * @param end   maximum position
     * @return true if found
     */
    boolean findOuterBrackets(final int start, final int end) {
      boolean hasNewBracket = findPrevOpenBracket(start) && findNextCloseBracket(end);
      while (hasNewBracket) {
        final int leftPrio = brackets.getBracketPrio(getCharAt(leftBracket));
        final int rightPrio = brackets.getBracketPrio(getCharAt(rightBracket));
        if (leftPrio == rightPrio) {
          // matching brackets
          return true;
        } else {
          if (leftPrio < rightPrio) {
            if (rightBracket + 1 < end) {
              ++rightBracket;
              hasNewBracket = findNextCloseBracket(end);
            } else {
              hasNewBracket = false;
            }
          } else {
            if (leftBracket > 1) {
              --leftBracket;
              hasNewBracket = findPrevOpenBracket(start);
            } else {
              hasNewBracket = false;
            }
          }
        }
      }
      return false;
    }

    /**
     * Finds unmatched open bracket starting at @a leftBracket.
     *
     * @param start minimum position.
     * @return true if found
     */
    private boolean findPrevOpenBracket(final int start) {
      char ch;
      while (!brackets.isOpenBracket(ch = getCharAt(leftBracket))) {
        if (brackets.isCloseBracket(ch)) {
          leftBracket = skipSexp(leftBracket, start, SexpDirection.backward(brackets));
        } else {
          if (isQuoteChar(ch)) {
            leftBracket = skipQuotedTextBackward(leftBracket, start);
          } else {
            if (leftBracket == start) {
              return false;
            }
          }
          --leftBracket;
        }
      }
      return true;
    }

    /**
     * Finds unmatched close bracket starting at @a rightBracket.
     *
     * @param end maximum position.
     * @return true if found
     */
    private boolean findNextCloseBracket(final int end) {
      char ch;
      while (!brackets.isCloseBracket(ch = getCharAt(rightBracket))) {
        if (brackets.isOpenBracket(ch)) {
          rightBracket = skipSexp(rightBracket, end, SexpDirection.forward(brackets));
        } else {
          if (isQuoteChar(ch)) {
            rightBracket = skipQuotedTextForward(rightBracket, end);
          }
          ++rightBracket;
        }
        if (rightBracket >= end) {
          return false;
        }
      }
      return true;
    }
  }

}
