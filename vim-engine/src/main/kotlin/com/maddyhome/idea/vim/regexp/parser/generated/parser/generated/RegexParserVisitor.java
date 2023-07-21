// Generated from RegexParser.g4 by ANTLR 4.10.1
package com.maddyhome.idea.vim.regexp.parser.generated;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link RegexParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface RegexParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link RegexParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern(RegexParser.PatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RegexParser#branch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBranch(RegexParser.BranchContext ctx);
	/**
	 * Visit a parse tree produced by {@link RegexParser#concat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConcat(RegexParser.ConcatContext ctx);
	/**
	 * Visit a parse tree produced by {@link RegexParser#piece}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPiece(RegexParser.PieceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OrdinaryAtom}
	 * labeled alternative in {@link RegexParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrdinaryAtom(RegexParser.OrdinaryAtomContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Grouping}
	 * labeled alternative in {@link RegexParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping(RegexParser.GroupingContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ZeroOrMore}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitZeroOrMore(RegexParser.ZeroOrMoreContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OneOrMore}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOneOrMore(RegexParser.OneOrMoreContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ZeroOrOne}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitZeroOrOne(RegexParser.ZeroOrOneContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RangeQuantifier}
	 * labeled alternative in {@link RegexParser#multi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeQuantifier(RegexParser.RangeQuantifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralChar}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralChar(RegexParser.LiteralCharContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AnyChar}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnyChar(RegexParser.AnyCharContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CharClass}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharClass(RegexParser.CharClassContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Collec}
	 * labeled alternative in {@link RegexParser#ordinary_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollec(RegexParser.CollecContext ctx);
	/**
	 * Visit a parse tree produced by {@link RegexParser#range}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange(RegexParser.RangeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Identifier}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(RegexParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IdentifierNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierNotDigit(RegexParser.IdentifierNotDigitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Keyword}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword(RegexParser.KeywordContext ctx);
	/**
	 * Visit a parse tree produced by the {@code KeywordNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeywordNotDigit(RegexParser.KeywordNotDigitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Filename}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilename(RegexParser.FilenameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FilenameNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilenameNotDigit(RegexParser.FilenameNotDigitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Printable}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrintable(RegexParser.PrintableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PrintableNotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrintableNotDigit(RegexParser.PrintableNotDigitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Whitespace}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhitespace(RegexParser.WhitespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotWhitespace}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotWhitespace(RegexParser.NotWhitespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Digit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDigit(RegexParser.DigitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotDigit}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotDigit(RegexParser.NotDigitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Hex}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHex(RegexParser.HexContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotHex}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotHex(RegexParser.NotHexContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Octal}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOctal(RegexParser.OctalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotOctal}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotOctal(RegexParser.NotOctalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Wordchar}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWordchar(RegexParser.WordcharContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Notwordchar}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotwordchar(RegexParser.NotwordcharContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Headofword}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeadofword(RegexParser.HeadofwordContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotHeadOfWord}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotHeadOfWord(RegexParser.NotHeadOfWordContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Alpha}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlpha(RegexParser.AlphaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotAlpha}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotAlpha(RegexParser.NotAlphaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Lcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLcase(RegexParser.LcaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotLcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotLcase(RegexParser.NotLcaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Ucase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUcase(RegexParser.UcaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotUcase}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotUcase(RegexParser.NotUcaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Esc}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsc(RegexParser.EscContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Tab}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTab(RegexParser.TabContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CR}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCR(RegexParser.CRContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BS}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBS(RegexParser.BSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NL}
	 * labeled alternative in {@link RegexParser#char_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNL(RegexParser.NLContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CollectionPos}
	 * labeled alternative in {@link RegexParser#collection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollectionPos(RegexParser.CollectionPosContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CollectionNeg}
	 * labeled alternative in {@link RegexParser#collection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollectionNeg(RegexParser.CollectionNegContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SingleColChar}
	 * labeled alternative in {@link RegexParser#collection_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleColChar(RegexParser.SingleColCharContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RangeColChar}
	 * labeled alternative in {@link RegexParser#collection_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeColChar(RegexParser.RangeColCharContext ctx);
}