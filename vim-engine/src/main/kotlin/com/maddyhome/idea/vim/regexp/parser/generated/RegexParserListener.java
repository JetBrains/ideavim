// Generated from RegexParser.g4 by ANTLR 4.10.1
package com.maddyhome.idea.vim.regexp.parser.generated;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RegexParser}.
 */
public interface RegexParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RegexParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPattern(RegexParser.PatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPattern(RegexParser.PatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexParser#branch}.
	 * @param ctx the parse tree
	 */
	void enterBranch(RegexParser.BranchContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexParser#branch}.
	 * @param ctx the parse tree
	 */
	void exitBranch(RegexParser.BranchContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexParser#concat}.
	 * @param ctx the parse tree
	 */
	void enterConcat(RegexParser.ConcatContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexParser#concat}.
	 * @param ctx the parse tree
	 */
	void exitConcat(RegexParser.ConcatContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexParser#piece}.
	 * @param ctx the parse tree
	 */
	void enterPiece(RegexParser.PieceContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexParser#piece}.
	 * @param ctx the parse tree
	 */
	void exitPiece(RegexParser.PieceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OrdinaryAtom}
	 * labeled alternative in {@link RegexParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterOrdinaryAtom(RegexParser.OrdinaryAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OrdinaryAtom}
	 * labeled alternative in {@link RegexParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitOrdinaryAtom(RegexParser.OrdinaryAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Grouping}
	 * labeled alternative in {@link RegexParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterGrouping(RegexParser.GroupingContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Grouping}
	 * labeled alternative in {@link RegexParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitGrouping(RegexParser.GroupingContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ZeroOrMore}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 */
	void enterZeroOrMore(RegexParser.ZeroOrMoreContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ZeroOrMore}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 */
	void exitZeroOrMore(RegexParser.ZeroOrMoreContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OneOrMore}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 */
	void enterOneOrMore(RegexParser.OneOrMoreContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OneOrMore}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 */
	void exitOneOrMore(RegexParser.OneOrMoreContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ZeroOrOne}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 */
	void enterZeroOrOne(RegexParser.ZeroOrOneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ZeroOrOne}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 */
	void exitZeroOrOne(RegexParser.ZeroOrOneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RangeQuantifier}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 */
	void enterRangeQuantifier(RegexParser.RangeQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RangeQuantifier}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 */
	void exitRangeQuantifier(RegexParser.RangeQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LiteralChar}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 */
	void enterLiteralChar(RegexParser.LiteralCharContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LiteralChar}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 */
	void exitLiteralChar(RegexParser.LiteralCharContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AnyChar}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 */
	void enterAnyChar(RegexParser.AnyCharContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AnyChar}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 */
	void exitAnyChar(RegexParser.AnyCharContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CharClass}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 */
	void enterCharClass(RegexParser.CharClassContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CharClass}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 */
	void exitCharClass(RegexParser.CharClassContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Collec}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 */
	void enterCollec(RegexParser.CollecContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Collec}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 */
	void exitCollec(RegexParser.CollecContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexParser#range}.
	 * @param ctx the parse tree
	 */
	void enterRange(RegexParser.RangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexParser#range}.
	 * @param ctx the parse tree
	 */
	void exitRange(RegexParser.RangeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Identifier}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(RegexParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Identifier}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(RegexParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IdentifierNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierNotDigit(RegexParser.IdentifierNotDigitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IdentifierNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierNotDigit(RegexParser.IdentifierNotDigitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Keyword}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterKeyword(RegexParser.KeywordContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Keyword}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitKeyword(RegexParser.KeywordContext ctx);
	/**
	 * Enter a parse tree produced by the {@code KeywordNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterKeywordNotDigit(RegexParser.KeywordNotDigitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code KeywordNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitKeywordNotDigit(RegexParser.KeywordNotDigitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Filename}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterFilename(RegexParser.FilenameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Filename}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitFilename(RegexParser.FilenameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FilenameNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterFilenameNotDigit(RegexParser.FilenameNotDigitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FilenameNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitFilenameNotDigit(RegexParser.FilenameNotDigitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Printable}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterPrintable(RegexParser.PrintableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Printable}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitPrintable(RegexParser.PrintableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PrintableNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterPrintableNotDigit(RegexParser.PrintableNotDigitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PrintableNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitPrintableNotDigit(RegexParser.PrintableNotDigitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Whitespace}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterWhitespace(RegexParser.WhitespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Whitespace}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitWhitespace(RegexParser.WhitespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotWhitespace}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotWhitespace(RegexParser.NotWhitespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotWhitespace}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotWhitespace(RegexParser.NotWhitespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Digit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterDigit(RegexParser.DigitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Digit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitDigit(RegexParser.DigitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotDigit(RegexParser.NotDigitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotDigit(RegexParser.NotDigitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Hex}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterHex(RegexParser.HexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Hex}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitHex(RegexParser.HexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotHex}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotHex(RegexParser.NotHexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotHex}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotHex(RegexParser.NotHexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Octal}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterOctal(RegexParser.OctalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Octal}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitOctal(RegexParser.OctalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotOctal}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotOctal(RegexParser.NotOctalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotOctal}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotOctal(RegexParser.NotOctalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Wordchar}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterWordchar(RegexParser.WordcharContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Wordchar}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitWordchar(RegexParser.WordcharContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Notwordchar}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotwordchar(RegexParser.NotwordcharContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Notwordchar}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotwordchar(RegexParser.NotwordcharContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Headofword}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterHeadofword(RegexParser.HeadofwordContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Headofword}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitHeadofword(RegexParser.HeadofwordContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotHeadOfWord}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotHeadOfWord(RegexParser.NotHeadOfWordContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotHeadOfWord}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotHeadOfWord(RegexParser.NotHeadOfWordContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Alpha}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterAlpha(RegexParser.AlphaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Alpha}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitAlpha(RegexParser.AlphaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotAlpha}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotAlpha(RegexParser.NotAlphaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotAlpha}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotAlpha(RegexParser.NotAlphaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Lcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterLcase(RegexParser.LcaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Lcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitLcase(RegexParser.LcaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotLcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotLcase(RegexParser.NotLcaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotLcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotLcase(RegexParser.NotLcaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Ucase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterUcase(RegexParser.UcaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Ucase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitUcase(RegexParser.UcaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotUcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNotUcase(RegexParser.NotUcaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotUcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNotUcase(RegexParser.NotUcaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Esc}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterEsc(RegexParser.EscContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Esc}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitEsc(RegexParser.EscContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Tab}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterTab(RegexParser.TabContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Tab}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitTab(RegexParser.TabContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CR}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterCR(RegexParser.CRContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CR}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitCR(RegexParser.CRContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BS}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterBS(RegexParser.BSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BS}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitBS(RegexParser.BSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NL}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void enterNL(RegexParser.NLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NL}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 */
	void exitNL(RegexParser.NLContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CollectionPos}
	 * labeled alternative in {@link RegexParser#collection}.
	 * @param ctx the parse tree
	 */
	void enterCollectionPos(RegexParser.CollectionPosContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CollectionPos}
	 * labeled alternative in {@link RegexParser#collection}.
	 * @param ctx the parse tree
	 */
	void exitCollectionPos(RegexParser.CollectionPosContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CollectionNeg}
	 * labeled alternative in {@link RegexParser#collection}.
	 * @param ctx the parse tree
	 */
	void enterCollectionNeg(RegexParser.CollectionNegContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CollectionNeg}
	 * labeled alternative in {@link RegexParser#collection}.
	 * @param ctx the parse tree
	 */
	void exitCollectionNeg(RegexParser.CollectionNegContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RangeColElem}
	 * labeled alternative in {@link RegexParser#collection_elem}.
	 * @param ctx the parse tree
	 */
	void enterRangeColElem(RegexParser.RangeColElemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RangeColElem}
	 * labeled alternative in {@link RegexParser#collection_elem}.
	 * @param ctx the parse tree
	 */
	void exitRangeColElem(RegexParser.RangeColElemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SingleColElem}
	 * labeled alternative in {@link RegexParser#collection_elem}.
	 * @param ctx the parse tree
	 */
	void enterSingleColElem(RegexParser.SingleColElemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SingleColElem}
	 * labeled alternative in {@link RegexParser#collection_elem}.
	 * @param ctx the parse tree
	 */
	void exitSingleColElem(RegexParser.SingleColElemContext ctx);
}