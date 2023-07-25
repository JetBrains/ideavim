// Generated from RegexParser.g4 by ANTLR 4.10.1
package com.maddyhome.idea.vim.regexp.parser.generated;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class RegexParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.10.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ALTERNATION_MAGIC=1, AND_MAGIC=2, LEFT_PAREN_MAGIC=3, RIGHT_PAREN_MAGIC=4, 
		LITERAL_CHAR_MAGIC=5, DOT_MAGIC=6, STAR_MAGIC=7, PLUS_MAGIC=8, OPTIONAL_MAGIC=9, 
		RANGE_START_MAGIC=10, COLLECTION_START_MAGIC=11, CLASS_IDENTIFIER_MAGIC=12, 
		CLASS_IDENTIFIER_D_MAGIC=13, CLASS_KEYWORD_MAGIC=14, CLASS_KEYWORD_D_MAGIC=15, 
		CLASS_FILENAME_MAGIC=16, CLASS_FILENAME_D_MAGIC=17, CLASS_PRINTABLE_MAGIC=18, 
		CLASS_PRINTABLE_D_MAGIC=19, CLASS_WS_MAGIC=20, CLASS_NOT_WS_MAGIC=21, 
		CLASS_DIGIT_MAGIC=22, CLASS_NOT_DIGIT_MAGIC=23, CLASS_HEX_MAGIC=24, CLASS_NOT_HEX_MAGIC=25, 
		CLASS_OCTAL_MAGIC=26, CLASS_NOT_OCTAL_MAGIC=27, CLASS_WORD_MAGIC=28, CLASS_NOT_WORD_MAGIC=29, 
		CLASS_HEADWORD_MAGIC=30, CLASS_NOT_HEADWORD_MAGIC=31, CLASS_ALPHA_MAGIC=32, 
		CLASS_NOT_ALPHA_MAGIC=33, CLASS_LCASE_MAGIC=34, CLASS_NOT_LCASE_MAGIC=35, 
		CLASS_UCASE_MAGIC=36, CLASS_NOT_UCASE_MAGIC=37, CLASS_ESC_MAGIC=38, CLASS_TAB_MAGIC=39, 
		CLASS_CR_MAGIC=40, CLASS_BS_MAGIC=41, CLASS_NL_MAGIC=42, SETMAGIC_MAGIC=43, 
		SETNOMAGIC_MAGIC=44, SETVMAGIC_MAGIC=45, SETVNOMAGIC_MAGIC=46, ALTERNATION_NOMAGIC=47, 
		AND_NOMAGIC=48, LEFT_PAREN_NOMAGIC=49, RIGHT_PAREN_NOMAGIC=50, LITERAL_CHAR_NOMAGIC=51, 
		DOT_NOMAGIC=52, STAR_NOMAGIC=53, PLUS_NOMAGIC=54, OPTIONAL_NOMAGIC=55, 
		RANGE_START_NOMAGIC=56, COLLECTION_START_NOMAGIC=57, CLASS_IDENTIFIER_NOMAGIC=58, 
		CLASS_IDENTIFIER_D_NOMAGIC=59, CLASS_KEYWORD_NOMAGIC=60, CLASS_KEYWORD_D_NOMAGIC=61, 
		CLASS_FILENAME_NOMAGIC=62, CLASS_FILENAME_D_NOMAGIC=63, CLASS_PRINTABLE_NOMAGIC=64, 
		CLASS_PRINTABLE_D_NOMAGIC=65, CLASS_WS_NOMAGIC=66, CLASS_NOT_WS_NOMAGIC=67, 
		CLASS_DIGIT_NOMAGIC=68, CLASS_NOT_DIGIT_NOMAGIC=69, CLASS_HEX_NOMAGIC=70, 
		CLASS_NOT_HEX_NOMAGIC=71, CLASS_OCTAL_NOMAGIC=72, CLASS_NOT_OCTAL_NOMAGIC=73, 
		CLASS_WORD_NOMAGIC=74, CLASS_NOT_WORD_NOMAGIC=75, CLASS_HEADWORD_NOMAGIC=76, 
		CLASS_NOT_HEADWORD_NOMAGIC=77, CLASS_ALPHA_NOMAGIC=78, CLASS_NOT_ALPHA_NOMAGIC=79, 
		CLASS_LCASE_NOMAGIC=80, CLASS_NOT_LCASE_NOMAGIC=81, CLASS_UCASE_NOMAGIC=82, 
		CLASS_NOT_UCASE_NOMAGIC=83, CLASS_ESC_NOMAGIC=84, CLASS_TAB_NOMAGIC=85, 
		CLASS_CR_NOMAGIC=86, CLASS_BS_NOMAGIC=87, CLASS_NL_NOMAGIC=88, SETMAGIC_NOMAGIC=89, 
		SETNOMAGIC_NOMAGIC=90, SETVMAGIC_NOMAGIC=91, SETVNOMAGIC_NOMAGIC=92, RANGE_END=93, 
		INT=94, COMMA=95, COLLECTION_END=96, CARET=97, DASH=98, ESCAPED_CHAR=99, 
		COLLECTION_CHAR=100;
	public static final int
		RULE_pattern = 0, RULE_branch = 1, RULE_concat = 2, RULE_piece = 3, RULE_atom = 4, 
		RULE_multi = 5, RULE_ordinary_atom = 6, RULE_range = 7, RULE_char_class = 8, 
		RULE_collection = 9, RULE_collection_elem = 10;
	private static String[] makeRuleNames() {
		return new String[] {
			"pattern", "branch", "concat", "piece", "atom", "multi", "ordinary_atom", 
			"range", "char_class", "collection", "collection_elem"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, "'.'", "'*'", null, null, null, "'['", 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, "'\\.'", "'\\*'", null, null, null, "'\\['", 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, "'}'", 
			null, "','", "']'", "'^'", "'-'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "ALTERNATION_MAGIC", "AND_MAGIC", "LEFT_PAREN_MAGIC", "RIGHT_PAREN_MAGIC", 
			"LITERAL_CHAR_MAGIC", "DOT_MAGIC", "STAR_MAGIC", "PLUS_MAGIC", "OPTIONAL_MAGIC", 
			"RANGE_START_MAGIC", "COLLECTION_START_MAGIC", "CLASS_IDENTIFIER_MAGIC", 
			"CLASS_IDENTIFIER_D_MAGIC", "CLASS_KEYWORD_MAGIC", "CLASS_KEYWORD_D_MAGIC", 
			"CLASS_FILENAME_MAGIC", "CLASS_FILENAME_D_MAGIC", "CLASS_PRINTABLE_MAGIC", 
			"CLASS_PRINTABLE_D_MAGIC", "CLASS_WS_MAGIC", "CLASS_NOT_WS_MAGIC", "CLASS_DIGIT_MAGIC", 
			"CLASS_NOT_DIGIT_MAGIC", "CLASS_HEX_MAGIC", "CLASS_NOT_HEX_MAGIC", "CLASS_OCTAL_MAGIC", 
			"CLASS_NOT_OCTAL_MAGIC", "CLASS_WORD_MAGIC", "CLASS_NOT_WORD_MAGIC", 
			"CLASS_HEADWORD_MAGIC", "CLASS_NOT_HEADWORD_MAGIC", "CLASS_ALPHA_MAGIC", 
			"CLASS_NOT_ALPHA_MAGIC", "CLASS_LCASE_MAGIC", "CLASS_NOT_LCASE_MAGIC", 
			"CLASS_UCASE_MAGIC", "CLASS_NOT_UCASE_MAGIC", "CLASS_ESC_MAGIC", "CLASS_TAB_MAGIC", 
			"CLASS_CR_MAGIC", "CLASS_BS_MAGIC", "CLASS_NL_MAGIC", "SETMAGIC_MAGIC", 
			"SETNOMAGIC_MAGIC", "SETVMAGIC_MAGIC", "SETVNOMAGIC_MAGIC", "ALTERNATION_NOMAGIC", 
			"AND_NOMAGIC", "LEFT_PAREN_NOMAGIC", "RIGHT_PAREN_NOMAGIC", "LITERAL_CHAR_NOMAGIC", 
			"DOT_NOMAGIC", "STAR_NOMAGIC", "PLUS_NOMAGIC", "OPTIONAL_NOMAGIC", "RANGE_START_NOMAGIC", 
			"COLLECTION_START_NOMAGIC", "CLASS_IDENTIFIER_NOMAGIC", "CLASS_IDENTIFIER_D_NOMAGIC", 
			"CLASS_KEYWORD_NOMAGIC", "CLASS_KEYWORD_D_NOMAGIC", "CLASS_FILENAME_NOMAGIC", 
			"CLASS_FILENAME_D_NOMAGIC", "CLASS_PRINTABLE_NOMAGIC", "CLASS_PRINTABLE_D_NOMAGIC", 
			"CLASS_WS_NOMAGIC", "CLASS_NOT_WS_NOMAGIC", "CLASS_DIGIT_NOMAGIC", "CLASS_NOT_DIGIT_NOMAGIC", 
			"CLASS_HEX_NOMAGIC", "CLASS_NOT_HEX_NOMAGIC", "CLASS_OCTAL_NOMAGIC", 
			"CLASS_NOT_OCTAL_NOMAGIC", "CLASS_WORD_NOMAGIC", "CLASS_NOT_WORD_NOMAGIC", 
			"CLASS_HEADWORD_NOMAGIC", "CLASS_NOT_HEADWORD_NOMAGIC", "CLASS_ALPHA_NOMAGIC", 
			"CLASS_NOT_ALPHA_NOMAGIC", "CLASS_LCASE_NOMAGIC", "CLASS_NOT_LCASE_NOMAGIC", 
			"CLASS_UCASE_NOMAGIC", "CLASS_NOT_UCASE_NOMAGIC", "CLASS_ESC_NOMAGIC", 
			"CLASS_TAB_NOMAGIC", "CLASS_CR_NOMAGIC", "CLASS_BS_NOMAGIC", "CLASS_NL_NOMAGIC", 
			"SETMAGIC_NOMAGIC", "SETNOMAGIC_NOMAGIC", "SETVMAGIC_NOMAGIC", "SETVNOMAGIC_NOMAGIC", 
			"RANGE_END", "INT", "COMMA", "COLLECTION_END", "CARET", "DASH", "ESCAPED_CHAR", 
			"COLLECTION_CHAR"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "RegexParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public RegexParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class PatternContext extends ParserRuleContext {
		public List<BranchContext> branch() {
			return getRuleContexts(BranchContext.class);
		}
		public BranchContext branch(int i) {
			return getRuleContext(BranchContext.class,i);
		}
		public List<TerminalNode> ALTERNATION_MAGIC() { return getTokens(RegexParser.ALTERNATION_MAGIC); }
		public TerminalNode ALTERNATION_MAGIC(int i) {
			return getToken(RegexParser.ALTERNATION_MAGIC, i);
		}
		public List<TerminalNode> ALTERNATION_NOMAGIC() { return getTokens(RegexParser.ALTERNATION_NOMAGIC); }
		public TerminalNode ALTERNATION_NOMAGIC(int i) {
			return getToken(RegexParser.ALTERNATION_NOMAGIC, i);
		}
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		PatternContext _localctx = new PatternContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_pattern);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(27);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(22);
					branch();
					setState(23);
					_la = _input.LA(1);
					if ( !(_la==ALTERNATION_MAGIC || _la==ALTERNATION_NOMAGIC) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					} 
				}
				setState(29);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(30);
			branch();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BranchContext extends ParserRuleContext {
		public List<ConcatContext> concat() {
			return getRuleContexts(ConcatContext.class);
		}
		public ConcatContext concat(int i) {
			return getRuleContext(ConcatContext.class,i);
		}
		public List<TerminalNode> AND_MAGIC() { return getTokens(RegexParser.AND_MAGIC); }
		public TerminalNode AND_MAGIC(int i) {
			return getToken(RegexParser.AND_MAGIC, i);
		}
		public List<TerminalNode> AND_NOMAGIC() { return getTokens(RegexParser.AND_NOMAGIC); }
		public TerminalNode AND_NOMAGIC(int i) {
			return getToken(RegexParser.AND_NOMAGIC, i);
		}
		public BranchContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_branch; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterBranch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitBranch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitBranch(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BranchContext branch() throws RecognitionException {
		BranchContext _localctx = new BranchContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_branch);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(37);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(32);
					concat();
					setState(33);
					_la = _input.LA(1);
					if ( !(_la==AND_MAGIC || _la==AND_NOMAGIC) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					} 
				}
				setState(39);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
			}
			setState(40);
			concat();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConcatContext extends ParserRuleContext {
		public List<PieceContext> piece() {
			return getRuleContexts(PieceContext.class);
		}
		public PieceContext piece(int i) {
			return getRuleContext(PieceContext.class,i);
		}
		public ConcatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_concat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterConcat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitConcat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitConcat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConcatContext concat() throws RecognitionException {
		ConcatContext _localctx = new ConcatContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_concat);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(43); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(42);
				piece();
				}
				}
				setState(45); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LEFT_PAREN_MAGIC) | (1L << LITERAL_CHAR_MAGIC) | (1L << DOT_MAGIC) | (1L << COLLECTION_START_MAGIC) | (1L << CLASS_IDENTIFIER_MAGIC) | (1L << CLASS_IDENTIFIER_D_MAGIC) | (1L << CLASS_KEYWORD_MAGIC) | (1L << CLASS_KEYWORD_D_MAGIC) | (1L << CLASS_FILENAME_MAGIC) | (1L << CLASS_FILENAME_D_MAGIC) | (1L << CLASS_PRINTABLE_MAGIC) | (1L << CLASS_PRINTABLE_D_MAGIC) | (1L << CLASS_WS_MAGIC) | (1L << CLASS_NOT_WS_MAGIC) | (1L << CLASS_DIGIT_MAGIC) | (1L << CLASS_NOT_DIGIT_MAGIC) | (1L << CLASS_HEX_MAGIC) | (1L << CLASS_NOT_HEX_MAGIC) | (1L << CLASS_OCTAL_MAGIC) | (1L << CLASS_NOT_OCTAL_MAGIC) | (1L << CLASS_WORD_MAGIC) | (1L << CLASS_NOT_WORD_MAGIC) | (1L << CLASS_HEADWORD_MAGIC) | (1L << CLASS_NOT_HEADWORD_MAGIC) | (1L << CLASS_ALPHA_MAGIC) | (1L << CLASS_NOT_ALPHA_MAGIC) | (1L << CLASS_LCASE_MAGIC) | (1L << CLASS_NOT_LCASE_MAGIC) | (1L << CLASS_UCASE_MAGIC) | (1L << CLASS_NOT_UCASE_MAGIC) | (1L << CLASS_ESC_MAGIC) | (1L << CLASS_TAB_MAGIC) | (1L << CLASS_CR_MAGIC) | (1L << CLASS_BS_MAGIC) | (1L << CLASS_NL_MAGIC) | (1L << LEFT_PAREN_NOMAGIC) | (1L << LITERAL_CHAR_NOMAGIC) | (1L << DOT_NOMAGIC) | (1L << COLLECTION_START_NOMAGIC) | (1L << CLASS_IDENTIFIER_NOMAGIC) | (1L << CLASS_IDENTIFIER_D_NOMAGIC) | (1L << CLASS_KEYWORD_NOMAGIC) | (1L << CLASS_KEYWORD_D_NOMAGIC) | (1L << CLASS_FILENAME_NOMAGIC) | (1L << CLASS_FILENAME_D_NOMAGIC))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (CLASS_PRINTABLE_NOMAGIC - 64)) | (1L << (CLASS_PRINTABLE_D_NOMAGIC - 64)) | (1L << (CLASS_WS_NOMAGIC - 64)) | (1L << (CLASS_NOT_WS_NOMAGIC - 64)) | (1L << (CLASS_DIGIT_NOMAGIC - 64)) | (1L << (CLASS_NOT_DIGIT_NOMAGIC - 64)) | (1L << (CLASS_HEX_NOMAGIC - 64)) | (1L << (CLASS_NOT_HEX_NOMAGIC - 64)) | (1L << (CLASS_OCTAL_NOMAGIC - 64)) | (1L << (CLASS_NOT_OCTAL_NOMAGIC - 64)) | (1L << (CLASS_WORD_NOMAGIC - 64)) | (1L << (CLASS_NOT_WORD_NOMAGIC - 64)) | (1L << (CLASS_HEADWORD_NOMAGIC - 64)) | (1L << (CLASS_NOT_HEADWORD_NOMAGIC - 64)) | (1L << (CLASS_ALPHA_NOMAGIC - 64)) | (1L << (CLASS_NOT_ALPHA_NOMAGIC - 64)) | (1L << (CLASS_LCASE_NOMAGIC - 64)) | (1L << (CLASS_NOT_LCASE_NOMAGIC - 64)) | (1L << (CLASS_UCASE_NOMAGIC - 64)) | (1L << (CLASS_NOT_UCASE_NOMAGIC - 64)) | (1L << (CLASS_ESC_NOMAGIC - 64)) | (1L << (CLASS_TAB_NOMAGIC - 64)) | (1L << (CLASS_CR_NOMAGIC - 64)) | (1L << (CLASS_BS_NOMAGIC - 64)) | (1L << (CLASS_NL_NOMAGIC - 64)))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PieceContext extends ParserRuleContext {
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public MultiContext multi() {
			return getRuleContext(MultiContext.class,0);
		}
		public PieceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_piece; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterPiece(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitPiece(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitPiece(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PieceContext piece() throws RecognitionException {
		PieceContext _localctx = new PieceContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_piece);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(47);
			atom();
			setState(49);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STAR_MAGIC) | (1L << PLUS_MAGIC) | (1L << OPTIONAL_MAGIC) | (1L << RANGE_START_MAGIC) | (1L << STAR_NOMAGIC) | (1L << PLUS_NOMAGIC) | (1L << OPTIONAL_NOMAGIC) | (1L << RANGE_START_NOMAGIC))) != 0)) {
				{
				setState(48);
				multi();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AtomContext extends ParserRuleContext {
		public AtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atom; }
	 
		public AtomContext() { }
		public void copyFrom(AtomContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class OrdinaryAtomContext extends AtomContext {
		public Ordinary_atomContext ordinary_atom() {
			return getRuleContext(Ordinary_atomContext.class,0);
		}
		public OrdinaryAtomContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterOrdinaryAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitOrdinaryAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitOrdinaryAtom(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GroupingContext extends AtomContext {
		public TerminalNode LEFT_PAREN_MAGIC() { return getToken(RegexParser.LEFT_PAREN_MAGIC, 0); }
		public TerminalNode LEFT_PAREN_NOMAGIC() { return getToken(RegexParser.LEFT_PAREN_NOMAGIC, 0); }
		public TerminalNode RIGHT_PAREN_MAGIC() { return getToken(RegexParser.RIGHT_PAREN_MAGIC, 0); }
		public TerminalNode RIGHT_PAREN_NOMAGIC() { return getToken(RegexParser.RIGHT_PAREN_NOMAGIC, 0); }
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public GroupingContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterGrouping(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitGrouping(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitGrouping(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomContext atom() throws RecognitionException {
		AtomContext _localctx = new AtomContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_atom);
		int _la;
		try {
			setState(57);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LITERAL_CHAR_MAGIC:
			case DOT_MAGIC:
			case COLLECTION_START_MAGIC:
			case CLASS_IDENTIFIER_MAGIC:
			case CLASS_IDENTIFIER_D_MAGIC:
			case CLASS_KEYWORD_MAGIC:
			case CLASS_KEYWORD_D_MAGIC:
			case CLASS_FILENAME_MAGIC:
			case CLASS_FILENAME_D_MAGIC:
			case CLASS_PRINTABLE_MAGIC:
			case CLASS_PRINTABLE_D_MAGIC:
			case CLASS_WS_MAGIC:
			case CLASS_NOT_WS_MAGIC:
			case CLASS_DIGIT_MAGIC:
			case CLASS_NOT_DIGIT_MAGIC:
			case CLASS_HEX_MAGIC:
			case CLASS_NOT_HEX_MAGIC:
			case CLASS_OCTAL_MAGIC:
			case CLASS_NOT_OCTAL_MAGIC:
			case CLASS_WORD_MAGIC:
			case CLASS_NOT_WORD_MAGIC:
			case CLASS_HEADWORD_MAGIC:
			case CLASS_NOT_HEADWORD_MAGIC:
			case CLASS_ALPHA_MAGIC:
			case CLASS_NOT_ALPHA_MAGIC:
			case CLASS_LCASE_MAGIC:
			case CLASS_NOT_LCASE_MAGIC:
			case CLASS_UCASE_MAGIC:
			case CLASS_NOT_UCASE_MAGIC:
			case CLASS_ESC_MAGIC:
			case CLASS_TAB_MAGIC:
			case CLASS_CR_MAGIC:
			case CLASS_BS_MAGIC:
			case CLASS_NL_MAGIC:
			case LITERAL_CHAR_NOMAGIC:
			case DOT_NOMAGIC:
			case COLLECTION_START_NOMAGIC:
			case CLASS_IDENTIFIER_NOMAGIC:
			case CLASS_IDENTIFIER_D_NOMAGIC:
			case CLASS_KEYWORD_NOMAGIC:
			case CLASS_KEYWORD_D_NOMAGIC:
			case CLASS_FILENAME_NOMAGIC:
			case CLASS_FILENAME_D_NOMAGIC:
			case CLASS_PRINTABLE_NOMAGIC:
			case CLASS_PRINTABLE_D_NOMAGIC:
			case CLASS_WS_NOMAGIC:
			case CLASS_NOT_WS_NOMAGIC:
			case CLASS_DIGIT_NOMAGIC:
			case CLASS_NOT_DIGIT_NOMAGIC:
			case CLASS_HEX_NOMAGIC:
			case CLASS_NOT_HEX_NOMAGIC:
			case CLASS_OCTAL_NOMAGIC:
			case CLASS_NOT_OCTAL_NOMAGIC:
			case CLASS_WORD_NOMAGIC:
			case CLASS_NOT_WORD_NOMAGIC:
			case CLASS_HEADWORD_NOMAGIC:
			case CLASS_NOT_HEADWORD_NOMAGIC:
			case CLASS_ALPHA_NOMAGIC:
			case CLASS_NOT_ALPHA_NOMAGIC:
			case CLASS_LCASE_NOMAGIC:
			case CLASS_NOT_LCASE_NOMAGIC:
			case CLASS_UCASE_NOMAGIC:
			case CLASS_NOT_UCASE_NOMAGIC:
			case CLASS_ESC_NOMAGIC:
			case CLASS_TAB_NOMAGIC:
			case CLASS_CR_NOMAGIC:
			case CLASS_BS_NOMAGIC:
			case CLASS_NL_NOMAGIC:
				_localctx = new OrdinaryAtomContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(51);
				ordinary_atom();
				}
				break;
			case LEFT_PAREN_MAGIC:
			case LEFT_PAREN_NOMAGIC:
				_localctx = new GroupingContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(52);
				_la = _input.LA(1);
				if ( !(_la==LEFT_PAREN_MAGIC || _la==LEFT_PAREN_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(54);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LEFT_PAREN_MAGIC) | (1L << LITERAL_CHAR_MAGIC) | (1L << DOT_MAGIC) | (1L << COLLECTION_START_MAGIC) | (1L << CLASS_IDENTIFIER_MAGIC) | (1L << CLASS_IDENTIFIER_D_MAGIC) | (1L << CLASS_KEYWORD_MAGIC) | (1L << CLASS_KEYWORD_D_MAGIC) | (1L << CLASS_FILENAME_MAGIC) | (1L << CLASS_FILENAME_D_MAGIC) | (1L << CLASS_PRINTABLE_MAGIC) | (1L << CLASS_PRINTABLE_D_MAGIC) | (1L << CLASS_WS_MAGIC) | (1L << CLASS_NOT_WS_MAGIC) | (1L << CLASS_DIGIT_MAGIC) | (1L << CLASS_NOT_DIGIT_MAGIC) | (1L << CLASS_HEX_MAGIC) | (1L << CLASS_NOT_HEX_MAGIC) | (1L << CLASS_OCTAL_MAGIC) | (1L << CLASS_NOT_OCTAL_MAGIC) | (1L << CLASS_WORD_MAGIC) | (1L << CLASS_NOT_WORD_MAGIC) | (1L << CLASS_HEADWORD_MAGIC) | (1L << CLASS_NOT_HEADWORD_MAGIC) | (1L << CLASS_ALPHA_MAGIC) | (1L << CLASS_NOT_ALPHA_MAGIC) | (1L << CLASS_LCASE_MAGIC) | (1L << CLASS_NOT_LCASE_MAGIC) | (1L << CLASS_UCASE_MAGIC) | (1L << CLASS_NOT_UCASE_MAGIC) | (1L << CLASS_ESC_MAGIC) | (1L << CLASS_TAB_MAGIC) | (1L << CLASS_CR_MAGIC) | (1L << CLASS_BS_MAGIC) | (1L << CLASS_NL_MAGIC) | (1L << LEFT_PAREN_NOMAGIC) | (1L << LITERAL_CHAR_NOMAGIC) | (1L << DOT_NOMAGIC) | (1L << COLLECTION_START_NOMAGIC) | (1L << CLASS_IDENTIFIER_NOMAGIC) | (1L << CLASS_IDENTIFIER_D_NOMAGIC) | (1L << CLASS_KEYWORD_NOMAGIC) | (1L << CLASS_KEYWORD_D_NOMAGIC) | (1L << CLASS_FILENAME_NOMAGIC) | (1L << CLASS_FILENAME_D_NOMAGIC))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (CLASS_PRINTABLE_NOMAGIC - 64)) | (1L << (CLASS_PRINTABLE_D_NOMAGIC - 64)) | (1L << (CLASS_WS_NOMAGIC - 64)) | (1L << (CLASS_NOT_WS_NOMAGIC - 64)) | (1L << (CLASS_DIGIT_NOMAGIC - 64)) | (1L << (CLASS_NOT_DIGIT_NOMAGIC - 64)) | (1L << (CLASS_HEX_NOMAGIC - 64)) | (1L << (CLASS_NOT_HEX_NOMAGIC - 64)) | (1L << (CLASS_OCTAL_NOMAGIC - 64)) | (1L << (CLASS_NOT_OCTAL_NOMAGIC - 64)) | (1L << (CLASS_WORD_NOMAGIC - 64)) | (1L << (CLASS_NOT_WORD_NOMAGIC - 64)) | (1L << (CLASS_HEADWORD_NOMAGIC - 64)) | (1L << (CLASS_NOT_HEADWORD_NOMAGIC - 64)) | (1L << (CLASS_ALPHA_NOMAGIC - 64)) | (1L << (CLASS_NOT_ALPHA_NOMAGIC - 64)) | (1L << (CLASS_LCASE_NOMAGIC - 64)) | (1L << (CLASS_NOT_LCASE_NOMAGIC - 64)) | (1L << (CLASS_UCASE_NOMAGIC - 64)) | (1L << (CLASS_NOT_UCASE_NOMAGIC - 64)) | (1L << (CLASS_ESC_NOMAGIC - 64)) | (1L << (CLASS_TAB_NOMAGIC - 64)) | (1L << (CLASS_CR_NOMAGIC - 64)) | (1L << (CLASS_BS_NOMAGIC - 64)) | (1L << (CLASS_NL_NOMAGIC - 64)))) != 0)) {
					{
					setState(53);
					pattern();
					}
				}

				setState(56);
				_la = _input.LA(1);
				if ( !(_la==RIGHT_PAREN_MAGIC || _la==RIGHT_PAREN_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiContext extends ParserRuleContext {
		public MultiContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multi; }
	 
		public MultiContext() { }
		public void copyFrom(MultiContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class OneOrMoreContext extends MultiContext {
		public TerminalNode PLUS_MAGIC() { return getToken(RegexParser.PLUS_MAGIC, 0); }
		public TerminalNode PLUS_NOMAGIC() { return getToken(RegexParser.PLUS_NOMAGIC, 0); }
		public OneOrMoreContext(MultiContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterOneOrMore(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitOneOrMore(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitOneOrMore(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ZeroOrMoreContext extends MultiContext {
		public TerminalNode STAR_MAGIC() { return getToken(RegexParser.STAR_MAGIC, 0); }
		public TerminalNode STAR_NOMAGIC() { return getToken(RegexParser.STAR_NOMAGIC, 0); }
		public ZeroOrMoreContext(MultiContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterZeroOrMore(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitZeroOrMore(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitZeroOrMore(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RangeQuantifierContext extends MultiContext {
		public RangeContext range() {
			return getRuleContext(RangeContext.class,0);
		}
		public RangeQuantifierContext(MultiContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterRangeQuantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitRangeQuantifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitRangeQuantifier(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ZeroOrOneContext extends MultiContext {
		public TerminalNode OPTIONAL_MAGIC() { return getToken(RegexParser.OPTIONAL_MAGIC, 0); }
		public TerminalNode OPTIONAL_NOMAGIC() { return getToken(RegexParser.OPTIONAL_NOMAGIC, 0); }
		public ZeroOrOneContext(MultiContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterZeroOrOne(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitZeroOrOne(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitZeroOrOne(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiContext multi() throws RecognitionException {
		MultiContext _localctx = new MultiContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_multi);
		int _la;
		try {
			setState(63);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STAR_MAGIC:
			case STAR_NOMAGIC:
				_localctx = new ZeroOrMoreContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(59);
				_la = _input.LA(1);
				if ( !(_la==STAR_MAGIC || _la==STAR_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case PLUS_MAGIC:
			case PLUS_NOMAGIC:
				_localctx = new OneOrMoreContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(60);
				_la = _input.LA(1);
				if ( !(_la==PLUS_MAGIC || _la==PLUS_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case OPTIONAL_MAGIC:
			case OPTIONAL_NOMAGIC:
				_localctx = new ZeroOrOneContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(61);
				_la = _input.LA(1);
				if ( !(_la==OPTIONAL_MAGIC || _la==OPTIONAL_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case RANGE_START_MAGIC:
			case RANGE_START_NOMAGIC:
				_localctx = new RangeQuantifierContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(62);
				range();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Ordinary_atomContext extends ParserRuleContext {
		public Ordinary_atomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ordinary_atom; }
	 
		public Ordinary_atomContext() { }
		public void copyFrom(Ordinary_atomContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class AnyCharContext extends Ordinary_atomContext {
		public TerminalNode DOT_MAGIC() { return getToken(RegexParser.DOT_MAGIC, 0); }
		public TerminalNode DOT_NOMAGIC() { return getToken(RegexParser.DOT_NOMAGIC, 0); }
		public AnyCharContext(Ordinary_atomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterAnyChar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitAnyChar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitAnyChar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CharClassContext extends Ordinary_atomContext {
		public Char_classContext char_class() {
			return getRuleContext(Char_classContext.class,0);
		}
		public CharClassContext(Ordinary_atomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterCharClass(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitCharClass(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitCharClass(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CollecContext extends Ordinary_atomContext {
		public CollectionContext collection() {
			return getRuleContext(CollectionContext.class,0);
		}
		public CollecContext(Ordinary_atomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterCollec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitCollec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitCollec(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LiteralCharContext extends Ordinary_atomContext {
		public TerminalNode LITERAL_CHAR_MAGIC() { return getToken(RegexParser.LITERAL_CHAR_MAGIC, 0); }
		public TerminalNode LITERAL_CHAR_NOMAGIC() { return getToken(RegexParser.LITERAL_CHAR_NOMAGIC, 0); }
		public LiteralCharContext(Ordinary_atomContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterLiteralChar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitLiteralChar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitLiteralChar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Ordinary_atomContext ordinary_atom() throws RecognitionException {
		Ordinary_atomContext _localctx = new Ordinary_atomContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_ordinary_atom);
		int _la;
		try {
			setState(70);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LITERAL_CHAR_MAGIC:
				_localctx = new LiteralCharContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(65);
				match(LITERAL_CHAR_MAGIC);
				}
				break;
			case LITERAL_CHAR_NOMAGIC:
				_localctx = new LiteralCharContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(66);
				match(LITERAL_CHAR_NOMAGIC);
				}
				break;
			case DOT_MAGIC:
			case DOT_NOMAGIC:
				_localctx = new AnyCharContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(67);
				_la = _input.LA(1);
				if ( !(_la==DOT_MAGIC || _la==DOT_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_IDENTIFIER_MAGIC:
			case CLASS_IDENTIFIER_D_MAGIC:
			case CLASS_KEYWORD_MAGIC:
			case CLASS_KEYWORD_D_MAGIC:
			case CLASS_FILENAME_MAGIC:
			case CLASS_FILENAME_D_MAGIC:
			case CLASS_PRINTABLE_MAGIC:
			case CLASS_PRINTABLE_D_MAGIC:
			case CLASS_WS_MAGIC:
			case CLASS_NOT_WS_MAGIC:
			case CLASS_DIGIT_MAGIC:
			case CLASS_NOT_DIGIT_MAGIC:
			case CLASS_HEX_MAGIC:
			case CLASS_NOT_HEX_MAGIC:
			case CLASS_OCTAL_MAGIC:
			case CLASS_NOT_OCTAL_MAGIC:
			case CLASS_WORD_MAGIC:
			case CLASS_NOT_WORD_MAGIC:
			case CLASS_HEADWORD_MAGIC:
			case CLASS_NOT_HEADWORD_MAGIC:
			case CLASS_ALPHA_MAGIC:
			case CLASS_NOT_ALPHA_MAGIC:
			case CLASS_LCASE_MAGIC:
			case CLASS_NOT_LCASE_MAGIC:
			case CLASS_UCASE_MAGIC:
			case CLASS_NOT_UCASE_MAGIC:
			case CLASS_ESC_MAGIC:
			case CLASS_TAB_MAGIC:
			case CLASS_CR_MAGIC:
			case CLASS_BS_MAGIC:
			case CLASS_NL_MAGIC:
			case CLASS_IDENTIFIER_NOMAGIC:
			case CLASS_IDENTIFIER_D_NOMAGIC:
			case CLASS_KEYWORD_NOMAGIC:
			case CLASS_KEYWORD_D_NOMAGIC:
			case CLASS_FILENAME_NOMAGIC:
			case CLASS_FILENAME_D_NOMAGIC:
			case CLASS_PRINTABLE_NOMAGIC:
			case CLASS_PRINTABLE_D_NOMAGIC:
			case CLASS_WS_NOMAGIC:
			case CLASS_NOT_WS_NOMAGIC:
			case CLASS_DIGIT_NOMAGIC:
			case CLASS_NOT_DIGIT_NOMAGIC:
			case CLASS_HEX_NOMAGIC:
			case CLASS_NOT_HEX_NOMAGIC:
			case CLASS_OCTAL_NOMAGIC:
			case CLASS_NOT_OCTAL_NOMAGIC:
			case CLASS_WORD_NOMAGIC:
			case CLASS_NOT_WORD_NOMAGIC:
			case CLASS_HEADWORD_NOMAGIC:
			case CLASS_NOT_HEADWORD_NOMAGIC:
			case CLASS_ALPHA_NOMAGIC:
			case CLASS_NOT_ALPHA_NOMAGIC:
			case CLASS_LCASE_NOMAGIC:
			case CLASS_NOT_LCASE_NOMAGIC:
			case CLASS_UCASE_NOMAGIC:
			case CLASS_NOT_UCASE_NOMAGIC:
			case CLASS_ESC_NOMAGIC:
			case CLASS_TAB_NOMAGIC:
			case CLASS_CR_NOMAGIC:
			case CLASS_BS_NOMAGIC:
			case CLASS_NL_NOMAGIC:
				_localctx = new CharClassContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(68);
				char_class();
				}
				break;
			case COLLECTION_START_MAGIC:
			case COLLECTION_START_NOMAGIC:
				_localctx = new CollecContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(69);
				collection();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RangeContext extends ParserRuleContext {
		public TerminalNode RANGE_END() { return getToken(RegexParser.RANGE_END, 0); }
		public TerminalNode RANGE_START_MAGIC() { return getToken(RegexParser.RANGE_START_MAGIC, 0); }
		public TerminalNode RANGE_START_NOMAGIC() { return getToken(RegexParser.RANGE_START_NOMAGIC, 0); }
		public List<TerminalNode> INT() { return getTokens(RegexParser.INT); }
		public TerminalNode INT(int i) {
			return getToken(RegexParser.INT, i);
		}
		public TerminalNode COMMA() { return getToken(RegexParser.COMMA, 0); }
		public RangeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_range; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterRange(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitRange(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitRange(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RangeContext range() throws RecognitionException {
		RangeContext _localctx = new RangeContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_range);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(72);
			_la = _input.LA(1);
			if ( !(_la==RANGE_START_MAGIC || _la==RANGE_START_NOMAGIC) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(74);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==INT) {
				{
				setState(73);
				match(INT);
				}
			}

			setState(80);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(76);
				match(COMMA);
				setState(78);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==INT) {
					{
					setState(77);
					match(INT);
					}
				}

				}
			}

			setState(82);
			match(RANGE_END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Char_classContext extends ParserRuleContext {
		public Char_classContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_char_class; }
	 
		public Char_classContext() { }
		public void copyFrom(Char_classContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class NotAlphaContext extends Char_classContext {
		public TerminalNode CLASS_NOT_ALPHA_MAGIC() { return getToken(RegexParser.CLASS_NOT_ALPHA_MAGIC, 0); }
		public TerminalNode CLASS_NOT_ALPHA_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_ALPHA_NOMAGIC, 0); }
		public NotAlphaContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotAlpha(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotAlpha(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotAlpha(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotHexContext extends Char_classContext {
		public TerminalNode CLASS_NOT_HEX_MAGIC() { return getToken(RegexParser.CLASS_NOT_HEX_MAGIC, 0); }
		public TerminalNode CLASS_NOT_HEX_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_HEX_NOMAGIC, 0); }
		public NotHexContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotHex(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotHex(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotHex(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotHeadOfWordContext extends Char_classContext {
		public TerminalNode CLASS_NOT_HEADWORD_MAGIC() { return getToken(RegexParser.CLASS_NOT_HEADWORD_MAGIC, 0); }
		public TerminalNode CLASS_NOT_HEADWORD_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_HEADWORD_NOMAGIC, 0); }
		public NotHeadOfWordContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotHeadOfWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotHeadOfWord(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotHeadOfWord(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class HeadofwordContext extends Char_classContext {
		public TerminalNode CLASS_HEADWORD_MAGIC() { return getToken(RegexParser.CLASS_HEADWORD_MAGIC, 0); }
		public TerminalNode CLASS_HEADWORD_NOMAGIC() { return getToken(RegexParser.CLASS_HEADWORD_NOMAGIC, 0); }
		public HeadofwordContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterHeadofword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitHeadofword(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitHeadofword(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WhitespaceContext extends Char_classContext {
		public TerminalNode CLASS_WS_MAGIC() { return getToken(RegexParser.CLASS_WS_MAGIC, 0); }
		public TerminalNode CLASS_WS_NOMAGIC() { return getToken(RegexParser.CLASS_WS_NOMAGIC, 0); }
		public WhitespaceContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterWhitespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitWhitespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitWhitespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotUcaseContext extends Char_classContext {
		public TerminalNode CLASS_NOT_UCASE_MAGIC() { return getToken(RegexParser.CLASS_NOT_UCASE_MAGIC, 0); }
		public TerminalNode CLASS_NOT_UCASE_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_UCASE_NOMAGIC, 0); }
		public NotUcaseContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotUcase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotUcase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotUcase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BSContext extends Char_classContext {
		public TerminalNode CLASS_BS_MAGIC() { return getToken(RegexParser.CLASS_BS_MAGIC, 0); }
		public TerminalNode CLASS_BS_NOMAGIC() { return getToken(RegexParser.CLASS_BS_NOMAGIC, 0); }
		public BSContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterBS(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitBS(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitBS(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WordcharContext extends Char_classContext {
		public TerminalNode CLASS_WORD_MAGIC() { return getToken(RegexParser.CLASS_WORD_MAGIC, 0); }
		public TerminalNode CLASS_WORD_NOMAGIC() { return getToken(RegexParser.CLASS_WORD_NOMAGIC, 0); }
		public WordcharContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterWordchar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitWordchar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitWordchar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IdentifierContext extends Char_classContext {
		public TerminalNode CLASS_IDENTIFIER_MAGIC() { return getToken(RegexParser.CLASS_IDENTIFIER_MAGIC, 0); }
		public TerminalNode CLASS_IDENTIFIER_NOMAGIC() { return getToken(RegexParser.CLASS_IDENTIFIER_NOMAGIC, 0); }
		public IdentifierContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotwordcharContext extends Char_classContext {
		public TerminalNode CLASS_NOT_WORD_MAGIC() { return getToken(RegexParser.CLASS_NOT_WORD_MAGIC, 0); }
		public TerminalNode CLASS_NOT_WORD_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_WORD_NOMAGIC, 0); }
		public NotwordcharContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotwordchar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotwordchar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotwordchar(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class EscContext extends Char_classContext {
		public TerminalNode CLASS_ESC_MAGIC() { return getToken(RegexParser.CLASS_ESC_MAGIC, 0); }
		public TerminalNode CLASS_ESC_NOMAGIC() { return getToken(RegexParser.CLASS_ESC_NOMAGIC, 0); }
		public EscContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterEsc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitEsc(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitEsc(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FilenameNotDigitContext extends Char_classContext {
		public TerminalNode CLASS_FILENAME_D_MAGIC() { return getToken(RegexParser.CLASS_FILENAME_D_MAGIC, 0); }
		public TerminalNode CLASS_FILENAME_D_NOMAGIC() { return getToken(RegexParser.CLASS_FILENAME_D_NOMAGIC, 0); }
		public FilenameNotDigitContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterFilenameNotDigit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitFilenameNotDigit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitFilenameNotDigit(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotLcaseContext extends Char_classContext {
		public TerminalNode CLASS_NOT_LCASE_MAGIC() { return getToken(RegexParser.CLASS_NOT_LCASE_MAGIC, 0); }
		public TerminalNode CLASS_NOT_LCASE_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_LCASE_NOMAGIC, 0); }
		public NotLcaseContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotLcase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotLcase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotLcase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UcaseContext extends Char_classContext {
		public TerminalNode CLASS_UCASE_MAGIC() { return getToken(RegexParser.CLASS_UCASE_MAGIC, 0); }
		public TerminalNode CLASS_UCASE_NOMAGIC() { return getToken(RegexParser.CLASS_UCASE_NOMAGIC, 0); }
		public UcaseContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterUcase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitUcase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitUcase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IdentifierNotDigitContext extends Char_classContext {
		public TerminalNode CLASS_IDENTIFIER_D_MAGIC() { return getToken(RegexParser.CLASS_IDENTIFIER_D_MAGIC, 0); }
		public TerminalNode CLASS_IDENTIFIER_D_NOMAGIC() { return getToken(RegexParser.CLASS_IDENTIFIER_D_NOMAGIC, 0); }
		public IdentifierNotDigitContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterIdentifierNotDigit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitIdentifierNotDigit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitIdentifierNotDigit(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LcaseContext extends Char_classContext {
		public TerminalNode CLASS_LCASE_MAGIC() { return getToken(RegexParser.CLASS_LCASE_MAGIC, 0); }
		public TerminalNode CLASS_LCASE_NOMAGIC() { return getToken(RegexParser.CLASS_LCASE_NOMAGIC, 0); }
		public LcaseContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterLcase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitLcase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitLcase(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DigitContext extends Char_classContext {
		public TerminalNode CLASS_DIGIT_MAGIC() { return getToken(RegexParser.CLASS_DIGIT_MAGIC, 0); }
		public TerminalNode CLASS_DIGIT_NOMAGIC() { return getToken(RegexParser.CLASS_DIGIT_NOMAGIC, 0); }
		public DigitContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterDigit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitDigit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitDigit(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class OctalContext extends Char_classContext {
		public TerminalNode CLASS_OCTAL_MAGIC() { return getToken(RegexParser.CLASS_OCTAL_MAGIC, 0); }
		public TerminalNode CLASS_OCTAL_NOMAGIC() { return getToken(RegexParser.CLASS_OCTAL_NOMAGIC, 0); }
		public OctalContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterOctal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitOctal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitOctal(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class KeywordContext extends Char_classContext {
		public TerminalNode CLASS_KEYWORD_MAGIC() { return getToken(RegexParser.CLASS_KEYWORD_MAGIC, 0); }
		public TerminalNode CLASS_KEYWORD_NOMAGIC() { return getToken(RegexParser.CLASS_KEYWORD_NOMAGIC, 0); }
		public KeywordContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterKeyword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitKeyword(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitKeyword(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FilenameContext extends Char_classContext {
		public TerminalNode CLASS_FILENAME_MAGIC() { return getToken(RegexParser.CLASS_FILENAME_MAGIC, 0); }
		public TerminalNode CLASS_FILENAME_NOMAGIC() { return getToken(RegexParser.CLASS_FILENAME_NOMAGIC, 0); }
		public FilenameContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterFilename(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitFilename(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitFilename(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotDigitContext extends Char_classContext {
		public TerminalNode CLASS_NOT_DIGIT_MAGIC() { return getToken(RegexParser.CLASS_NOT_DIGIT_MAGIC, 0); }
		public TerminalNode CLASS_NOT_DIGIT_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_DIGIT_NOMAGIC, 0); }
		public NotDigitContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotDigit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotDigit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotDigit(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CRContext extends Char_classContext {
		public TerminalNode CLASS_CR_MAGIC() { return getToken(RegexParser.CLASS_CR_MAGIC, 0); }
		public TerminalNode CLASS_CR_NOMAGIC() { return getToken(RegexParser.CLASS_CR_NOMAGIC, 0); }
		public CRContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterCR(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitCR(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitCR(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotOctalContext extends Char_classContext {
		public TerminalNode CLASS_NOT_OCTAL_MAGIC() { return getToken(RegexParser.CLASS_NOT_OCTAL_MAGIC, 0); }
		public TerminalNode CLASS_NOT_OCTAL_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_OCTAL_NOMAGIC, 0); }
		public NotOctalContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotOctal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotOctal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotOctal(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TabContext extends Char_classContext {
		public TerminalNode CLASS_TAB_MAGIC() { return getToken(RegexParser.CLASS_TAB_MAGIC, 0); }
		public TerminalNode CLASS_TAB_NOMAGIC() { return getToken(RegexParser.CLASS_TAB_NOMAGIC, 0); }
		public TabContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterTab(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitTab(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitTab(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotWhitespaceContext extends Char_classContext {
		public TerminalNode CLASS_NOT_WS_MAGIC() { return getToken(RegexParser.CLASS_NOT_WS_MAGIC, 0); }
		public TerminalNode CLASS_NOT_WS_NOMAGIC() { return getToken(RegexParser.CLASS_NOT_WS_NOMAGIC, 0); }
		public NotWhitespaceContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNotWhitespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNotWhitespace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNotWhitespace(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class KeywordNotDigitContext extends Char_classContext {
		public TerminalNode CLASS_KEYWORD_D_MAGIC() { return getToken(RegexParser.CLASS_KEYWORD_D_MAGIC, 0); }
		public TerminalNode CLASS_KEYWORD_D_NOMAGIC() { return getToken(RegexParser.CLASS_KEYWORD_D_NOMAGIC, 0); }
		public KeywordNotDigitContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterKeywordNotDigit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitKeywordNotDigit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitKeywordNotDigit(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AlphaContext extends Char_classContext {
		public TerminalNode CLASS_ALPHA_MAGIC() { return getToken(RegexParser.CLASS_ALPHA_MAGIC, 0); }
		public TerminalNode CLASS_ALPHA_NOMAGIC() { return getToken(RegexParser.CLASS_ALPHA_NOMAGIC, 0); }
		public AlphaContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterAlpha(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitAlpha(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitAlpha(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class HexContext extends Char_classContext {
		public TerminalNode CLASS_HEX_MAGIC() { return getToken(RegexParser.CLASS_HEX_MAGIC, 0); }
		public TerminalNode CLASS_HEX_NOMAGIC() { return getToken(RegexParser.CLASS_HEX_NOMAGIC, 0); }
		public HexContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterHex(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitHex(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitHex(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrintableContext extends Char_classContext {
		public TerminalNode CLASS_PRINTABLE_MAGIC() { return getToken(RegexParser.CLASS_PRINTABLE_MAGIC, 0); }
		public TerminalNode CLASS_PRINTABLE_NOMAGIC() { return getToken(RegexParser.CLASS_PRINTABLE_NOMAGIC, 0); }
		public PrintableContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterPrintable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitPrintable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitPrintable(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrintableNotDigitContext extends Char_classContext {
		public TerminalNode CLASS_PRINTABLE_D_MAGIC() { return getToken(RegexParser.CLASS_PRINTABLE_D_MAGIC, 0); }
		public TerminalNode CLASS_PRINTABLE_D_NOMAGIC() { return getToken(RegexParser.CLASS_PRINTABLE_D_NOMAGIC, 0); }
		public PrintableNotDigitContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterPrintableNotDigit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitPrintableNotDigit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitPrintableNotDigit(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NLContext extends Char_classContext {
		public TerminalNode CLASS_NL_MAGIC() { return getToken(RegexParser.CLASS_NL_MAGIC, 0); }
		public TerminalNode CLASS_NL_NOMAGIC() { return getToken(RegexParser.CLASS_NL_NOMAGIC, 0); }
		public NLContext(Char_classContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterNL(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitNL(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitNL(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Char_classContext char_class() throws RecognitionException {
		Char_classContext _localctx = new Char_classContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_char_class);
		int _la;
		try {
			setState(115);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CLASS_IDENTIFIER_MAGIC:
			case CLASS_IDENTIFIER_NOMAGIC:
				_localctx = new IdentifierContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(84);
				_la = _input.LA(1);
				if ( !(_la==CLASS_IDENTIFIER_MAGIC || _la==CLASS_IDENTIFIER_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_IDENTIFIER_D_MAGIC:
			case CLASS_IDENTIFIER_D_NOMAGIC:
				_localctx = new IdentifierNotDigitContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(85);
				_la = _input.LA(1);
				if ( !(_la==CLASS_IDENTIFIER_D_MAGIC || _la==CLASS_IDENTIFIER_D_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_KEYWORD_MAGIC:
			case CLASS_KEYWORD_NOMAGIC:
				_localctx = new KeywordContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(86);
				_la = _input.LA(1);
				if ( !(_la==CLASS_KEYWORD_MAGIC || _la==CLASS_KEYWORD_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_KEYWORD_D_MAGIC:
			case CLASS_KEYWORD_D_NOMAGIC:
				_localctx = new KeywordNotDigitContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(87);
				_la = _input.LA(1);
				if ( !(_la==CLASS_KEYWORD_D_MAGIC || _la==CLASS_KEYWORD_D_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_FILENAME_MAGIC:
			case CLASS_FILENAME_NOMAGIC:
				_localctx = new FilenameContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(88);
				_la = _input.LA(1);
				if ( !(_la==CLASS_FILENAME_MAGIC || _la==CLASS_FILENAME_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_FILENAME_D_MAGIC:
			case CLASS_FILENAME_D_NOMAGIC:
				_localctx = new FilenameNotDigitContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(89);
				_la = _input.LA(1);
				if ( !(_la==CLASS_FILENAME_D_MAGIC || _la==CLASS_FILENAME_D_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_PRINTABLE_MAGIC:
			case CLASS_PRINTABLE_NOMAGIC:
				_localctx = new PrintableContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(90);
				_la = _input.LA(1);
				if ( !(_la==CLASS_PRINTABLE_MAGIC || _la==CLASS_PRINTABLE_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_PRINTABLE_D_MAGIC:
			case CLASS_PRINTABLE_D_NOMAGIC:
				_localctx = new PrintableNotDigitContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(91);
				_la = _input.LA(1);
				if ( !(_la==CLASS_PRINTABLE_D_MAGIC || _la==CLASS_PRINTABLE_D_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_WS_MAGIC:
			case CLASS_WS_NOMAGIC:
				_localctx = new WhitespaceContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(92);
				_la = _input.LA(1);
				if ( !(_la==CLASS_WS_MAGIC || _la==CLASS_WS_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_WS_MAGIC:
			case CLASS_NOT_WS_NOMAGIC:
				_localctx = new NotWhitespaceContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(93);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_WS_MAGIC || _la==CLASS_NOT_WS_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_DIGIT_MAGIC:
			case CLASS_DIGIT_NOMAGIC:
				_localctx = new DigitContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(94);
				_la = _input.LA(1);
				if ( !(_la==CLASS_DIGIT_MAGIC || _la==CLASS_DIGIT_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_DIGIT_MAGIC:
			case CLASS_NOT_DIGIT_NOMAGIC:
				_localctx = new NotDigitContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(95);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_DIGIT_MAGIC || _la==CLASS_NOT_DIGIT_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_HEX_MAGIC:
			case CLASS_HEX_NOMAGIC:
				_localctx = new HexContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(96);
				_la = _input.LA(1);
				if ( !(_la==CLASS_HEX_MAGIC || _la==CLASS_HEX_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_HEX_MAGIC:
			case CLASS_NOT_HEX_NOMAGIC:
				_localctx = new NotHexContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(97);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_HEX_MAGIC || _la==CLASS_NOT_HEX_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_OCTAL_MAGIC:
			case CLASS_OCTAL_NOMAGIC:
				_localctx = new OctalContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(98);
				_la = _input.LA(1);
				if ( !(_la==CLASS_OCTAL_MAGIC || _la==CLASS_OCTAL_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_OCTAL_MAGIC:
			case CLASS_NOT_OCTAL_NOMAGIC:
				_localctx = new NotOctalContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(99);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_OCTAL_MAGIC || _la==CLASS_NOT_OCTAL_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_WORD_MAGIC:
			case CLASS_WORD_NOMAGIC:
				_localctx = new WordcharContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(100);
				_la = _input.LA(1);
				if ( !(_la==CLASS_WORD_MAGIC || _la==CLASS_WORD_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_WORD_MAGIC:
			case CLASS_NOT_WORD_NOMAGIC:
				_localctx = new NotwordcharContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(101);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_WORD_MAGIC || _la==CLASS_NOT_WORD_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_HEADWORD_MAGIC:
			case CLASS_HEADWORD_NOMAGIC:
				_localctx = new HeadofwordContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(102);
				_la = _input.LA(1);
				if ( !(_la==CLASS_HEADWORD_MAGIC || _la==CLASS_HEADWORD_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_HEADWORD_MAGIC:
			case CLASS_NOT_HEADWORD_NOMAGIC:
				_localctx = new NotHeadOfWordContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(103);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_HEADWORD_MAGIC || _la==CLASS_NOT_HEADWORD_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_ALPHA_MAGIC:
			case CLASS_ALPHA_NOMAGIC:
				_localctx = new AlphaContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(104);
				_la = _input.LA(1);
				if ( !(_la==CLASS_ALPHA_MAGIC || _la==CLASS_ALPHA_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_ALPHA_MAGIC:
			case CLASS_NOT_ALPHA_NOMAGIC:
				_localctx = new NotAlphaContext(_localctx);
				enterOuterAlt(_localctx, 22);
				{
				setState(105);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_ALPHA_MAGIC || _la==CLASS_NOT_ALPHA_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_LCASE_MAGIC:
			case CLASS_LCASE_NOMAGIC:
				_localctx = new LcaseContext(_localctx);
				enterOuterAlt(_localctx, 23);
				{
				setState(106);
				_la = _input.LA(1);
				if ( !(_la==CLASS_LCASE_MAGIC || _la==CLASS_LCASE_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_LCASE_MAGIC:
			case CLASS_NOT_LCASE_NOMAGIC:
				_localctx = new NotLcaseContext(_localctx);
				enterOuterAlt(_localctx, 24);
				{
				setState(107);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_LCASE_MAGIC || _la==CLASS_NOT_LCASE_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_UCASE_MAGIC:
			case CLASS_UCASE_NOMAGIC:
				_localctx = new UcaseContext(_localctx);
				enterOuterAlt(_localctx, 25);
				{
				setState(108);
				_la = _input.LA(1);
				if ( !(_la==CLASS_UCASE_MAGIC || _la==CLASS_UCASE_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NOT_UCASE_MAGIC:
			case CLASS_NOT_UCASE_NOMAGIC:
				_localctx = new NotUcaseContext(_localctx);
				enterOuterAlt(_localctx, 26);
				{
				setState(109);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NOT_UCASE_MAGIC || _la==CLASS_NOT_UCASE_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_ESC_MAGIC:
			case CLASS_ESC_NOMAGIC:
				_localctx = new EscContext(_localctx);
				enterOuterAlt(_localctx, 27);
				{
				setState(110);
				_la = _input.LA(1);
				if ( !(_la==CLASS_ESC_MAGIC || _la==CLASS_ESC_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_TAB_MAGIC:
			case CLASS_TAB_NOMAGIC:
				_localctx = new TabContext(_localctx);
				enterOuterAlt(_localctx, 28);
				{
				setState(111);
				_la = _input.LA(1);
				if ( !(_la==CLASS_TAB_MAGIC || _la==CLASS_TAB_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_CR_MAGIC:
			case CLASS_CR_NOMAGIC:
				_localctx = new CRContext(_localctx);
				enterOuterAlt(_localctx, 29);
				{
				setState(112);
				_la = _input.LA(1);
				if ( !(_la==CLASS_CR_MAGIC || _la==CLASS_CR_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_BS_MAGIC:
			case CLASS_BS_NOMAGIC:
				_localctx = new BSContext(_localctx);
				enterOuterAlt(_localctx, 30);
				{
				setState(113);
				_la = _input.LA(1);
				if ( !(_la==CLASS_BS_MAGIC || _la==CLASS_BS_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case CLASS_NL_MAGIC:
			case CLASS_NL_NOMAGIC:
				_localctx = new NLContext(_localctx);
				enterOuterAlt(_localctx, 31);
				{
				setState(114);
				_la = _input.LA(1);
				if ( !(_la==CLASS_NL_MAGIC || _la==CLASS_NL_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CollectionContext extends ParserRuleContext {
		public CollectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_collection; }
	 
		public CollectionContext() { }
		public void copyFrom(CollectionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class CollectionNegContext extends CollectionContext {
		public TerminalNode CARET() { return getToken(RegexParser.CARET, 0); }
		public TerminalNode COLLECTION_END() { return getToken(RegexParser.COLLECTION_END, 0); }
		public TerminalNode COLLECTION_START_MAGIC() { return getToken(RegexParser.COLLECTION_START_MAGIC, 0); }
		public TerminalNode COLLECTION_START_NOMAGIC() { return getToken(RegexParser.COLLECTION_START_NOMAGIC, 0); }
		public List<Collection_elemContext> collection_elem() {
			return getRuleContexts(Collection_elemContext.class);
		}
		public Collection_elemContext collection_elem(int i) {
			return getRuleContext(Collection_elemContext.class,i);
		}
		public CollectionNegContext(CollectionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterCollectionNeg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitCollectionNeg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitCollectionNeg(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CollectionPosContext extends CollectionContext {
		public TerminalNode COLLECTION_END() { return getToken(RegexParser.COLLECTION_END, 0); }
		public TerminalNode COLLECTION_START_MAGIC() { return getToken(RegexParser.COLLECTION_START_MAGIC, 0); }
		public TerminalNode COLLECTION_START_NOMAGIC() { return getToken(RegexParser.COLLECTION_START_NOMAGIC, 0); }
		public List<Collection_elemContext> collection_elem() {
			return getRuleContexts(Collection_elemContext.class);
		}
		public Collection_elemContext collection_elem(int i) {
			return getRuleContext(Collection_elemContext.class,i);
		}
		public CollectionPosContext(CollectionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterCollectionPos(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitCollectionPos(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitCollectionPos(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CollectionContext collection() throws RecognitionException {
		CollectionContext _localctx = new CollectionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_collection);
		int _la;
		try {
			setState(134);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				_localctx = new CollectionPosContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(117);
				_la = _input.LA(1);
				if ( !(_la==COLLECTION_START_MAGIC || _la==COLLECTION_START_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(121);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 98)) & ~0x3f) == 0 && ((1L << (_la - 98)) & ((1L << (DASH - 98)) | (1L << (ESCAPED_CHAR - 98)) | (1L << (COLLECTION_CHAR - 98)))) != 0)) {
					{
					{
					setState(118);
					collection_elem();
					}
					}
					setState(123);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(124);
				match(COLLECTION_END);
				}
				break;
			case 2:
				_localctx = new CollectionNegContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(125);
				_la = _input.LA(1);
				if ( !(_la==COLLECTION_START_MAGIC || _la==COLLECTION_START_NOMAGIC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(126);
				match(CARET);
				setState(130);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 98)) & ~0x3f) == 0 && ((1L << (_la - 98)) & ((1L << (DASH - 98)) | (1L << (ESCAPED_CHAR - 98)) | (1L << (COLLECTION_CHAR - 98)))) != 0)) {
					{
					{
					setState(127);
					collection_elem();
					}
					}
					setState(132);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(133);
				match(COLLECTION_END);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Collection_elemContext extends ParserRuleContext {
		public Collection_elemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_collection_elem; }
	 
		public Collection_elemContext() { }
		public void copyFrom(Collection_elemContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class RangeColElemContext extends Collection_elemContext {
		public List<TerminalNode> DASH() { return getTokens(RegexParser.DASH); }
		public TerminalNode DASH(int i) {
			return getToken(RegexParser.DASH, i);
		}
		public List<TerminalNode> COLLECTION_CHAR() { return getTokens(RegexParser.COLLECTION_CHAR); }
		public TerminalNode COLLECTION_CHAR(int i) {
			return getToken(RegexParser.COLLECTION_CHAR, i);
		}
		public List<TerminalNode> ESCAPED_CHAR() { return getTokens(RegexParser.ESCAPED_CHAR); }
		public TerminalNode ESCAPED_CHAR(int i) {
			return getToken(RegexParser.ESCAPED_CHAR, i);
		}
		public RangeColElemContext(Collection_elemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterRangeColElem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitRangeColElem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitRangeColElem(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SingleColElemContext extends Collection_elemContext {
		public TerminalNode COLLECTION_CHAR() { return getToken(RegexParser.COLLECTION_CHAR, 0); }
		public TerminalNode DASH() { return getToken(RegexParser.DASH, 0); }
		public TerminalNode ESCAPED_CHAR() { return getToken(RegexParser.ESCAPED_CHAR, 0); }
		public SingleColElemContext(Collection_elemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).enterSingleColElem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegexParserListener ) ((RegexParserListener)listener).exitSingleColElem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegexParserVisitor ) return ((RegexParserVisitor<? extends T>)visitor).visitSingleColElem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Collection_elemContext collection_elem() throws RecognitionException {
		Collection_elemContext _localctx = new Collection_elemContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_collection_elem);
		int _la;
		try {
			setState(140);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				_localctx = new RangeColElemContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(136);
				_la = _input.LA(1);
				if ( !(((((_la - 98)) & ~0x3f) == 0 && ((1L << (_la - 98)) & ((1L << (DASH - 98)) | (1L << (ESCAPED_CHAR - 98)) | (1L << (COLLECTION_CHAR - 98)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(137);
				match(DASH);
				setState(138);
				_la = _input.LA(1);
				if ( !(((((_la - 98)) & ~0x3f) == 0 && ((1L << (_la - 98)) & ((1L << (DASH - 98)) | (1L << (ESCAPED_CHAR - 98)) | (1L << (COLLECTION_CHAR - 98)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 2:
				_localctx = new SingleColElemContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(139);
				_la = _input.LA(1);
				if ( !(((((_la - 98)) & ~0x3f) == 0 && ((1L << (_la - 98)) & ((1L << (DASH - 98)) | (1L << (ESCAPED_CHAR - 98)) | (1L << (COLLECTION_CHAR - 98)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001d\u008f\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0005\u0000\u001a\b\u0000\n\u0000\f\u0000\u001d\t\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0005\u0001$\b\u0001"+
		"\n\u0001\f\u0001\'\t\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0004\u0002"+
		",\b\u0002\u000b\u0002\f\u0002-\u0001\u0003\u0001\u0003\u0003\u00032\b"+
		"\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u00047\b\u0004\u0001"+
		"\u0004\u0003\u0004:\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0003\u0005@\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0003\u0006G\b\u0006\u0001\u0007\u0001\u0007\u0003"+
		"\u0007K\b\u0007\u0001\u0007\u0001\u0007\u0003\u0007O\b\u0007\u0003\u0007"+
		"Q\b\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003"+
		"\bt\b\b\u0001\t\u0001\t\u0005\tx\b\t\n\t\f\t{\t\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0005\t\u0081\b\t\n\t\f\t\u0084\t\t\u0001\t\u0003\t\u0087\b"+
		"\t\u0001\n\u0001\n\u0001\n\u0001\n\u0003\n\u008d\b\n\u0001\n\u0000\u0000"+
		"\u000b\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0000*\u0002"+
		"\u0000\u0001\u0001//\u0002\u0000\u0002\u000200\u0002\u0000\u0003\u0003"+
		"11\u0002\u0000\u0004\u000422\u0002\u0000\u0007\u000755\u0002\u0000\b\b"+
		"66\u0002\u0000\t\t77\u0002\u0000\u0006\u000644\u0002\u0000\n\n88\u0002"+
		"\u0000\f\f::\u0002\u0000\r\r;;\u0002\u0000\u000e\u000e<<\u0002\u0000\u000f"+
		"\u000f==\u0002\u0000\u0010\u0010>>\u0002\u0000\u0011\u0011??\u0002\u0000"+
		"\u0012\u0012@@\u0002\u0000\u0013\u0013AA\u0002\u0000\u0014\u0014BB\u0002"+
		"\u0000\u0015\u0015CC\u0002\u0000\u0016\u0016DD\u0002\u0000\u0017\u0017"+
		"EE\u0002\u0000\u0018\u0018FF\u0002\u0000\u0019\u0019GG\u0002\u0000\u001a"+
		"\u001aHH\u0002\u0000\u001b\u001bII\u0002\u0000\u001c\u001cJJ\u0002\u0000"+
		"\u001d\u001dKK\u0002\u0000\u001e\u001eLL\u0002\u0000\u001f\u001fMM\u0002"+
		"\u0000  NN\u0002\u0000!!OO\u0002\u0000\"\"PP\u0002\u0000##QQ\u0002\u0000"+
		"$$RR\u0002\u0000%%SS\u0002\u0000&&TT\u0002\u0000\'\'UU\u0002\u0000((V"+
		"V\u0002\u0000))WW\u0002\u0000**XX\u0002\u0000\u000b\u000b99\u0001\u0000"+
		"bd\u00b5\u0000\u001b\u0001\u0000\u0000\u0000\u0002%\u0001\u0000\u0000"+
		"\u0000\u0004+\u0001\u0000\u0000\u0000\u0006/\u0001\u0000\u0000\u0000\b"+
		"9\u0001\u0000\u0000\u0000\n?\u0001\u0000\u0000\u0000\fF\u0001\u0000\u0000"+
		"\u0000\u000eH\u0001\u0000\u0000\u0000\u0010s\u0001\u0000\u0000\u0000\u0012"+
		"\u0086\u0001\u0000\u0000\u0000\u0014\u008c\u0001\u0000\u0000\u0000\u0016"+
		"\u0017\u0003\u0002\u0001\u0000\u0017\u0018\u0007\u0000\u0000\u0000\u0018"+
		"\u001a\u0001\u0000\u0000\u0000\u0019\u0016\u0001\u0000\u0000\u0000\u001a"+
		"\u001d\u0001\u0000\u0000\u0000\u001b\u0019\u0001\u0000\u0000\u0000\u001b"+
		"\u001c\u0001\u0000\u0000\u0000\u001c\u001e\u0001\u0000\u0000\u0000\u001d"+
		"\u001b\u0001\u0000\u0000\u0000\u001e\u001f\u0003\u0002\u0001\u0000\u001f"+
		"\u0001\u0001\u0000\u0000\u0000 !\u0003\u0004\u0002\u0000!\"\u0007\u0001"+
		"\u0000\u0000\"$\u0001\u0000\u0000\u0000# \u0001\u0000\u0000\u0000$\'\u0001"+
		"\u0000\u0000\u0000%#\u0001\u0000\u0000\u0000%&\u0001\u0000\u0000\u0000"+
		"&(\u0001\u0000\u0000\u0000\'%\u0001\u0000\u0000\u0000()\u0003\u0004\u0002"+
		"\u0000)\u0003\u0001\u0000\u0000\u0000*,\u0003\u0006\u0003\u0000+*\u0001"+
		"\u0000\u0000\u0000,-\u0001\u0000\u0000\u0000-+\u0001\u0000\u0000\u0000"+
		"-.\u0001\u0000\u0000\u0000.\u0005\u0001\u0000\u0000\u0000/1\u0003\b\u0004"+
		"\u000002\u0003\n\u0005\u000010\u0001\u0000\u0000\u000012\u0001\u0000\u0000"+
		"\u00002\u0007\u0001\u0000\u0000\u00003:\u0003\f\u0006\u000046\u0007\u0002"+
		"\u0000\u000057\u0003\u0000\u0000\u000065\u0001\u0000\u0000\u000067\u0001"+
		"\u0000\u0000\u000078\u0001\u0000\u0000\u00008:\u0007\u0003\u0000\u0000"+
		"93\u0001\u0000\u0000\u000094\u0001\u0000\u0000\u0000:\t\u0001\u0000\u0000"+
		"\u0000;@\u0007\u0004\u0000\u0000<@\u0007\u0005\u0000\u0000=@\u0007\u0006"+
		"\u0000\u0000>@\u0003\u000e\u0007\u0000?;\u0001\u0000\u0000\u0000?<\u0001"+
		"\u0000\u0000\u0000?=\u0001\u0000\u0000\u0000?>\u0001\u0000\u0000\u0000"+
		"@\u000b\u0001\u0000\u0000\u0000AG\u0005\u0005\u0000\u0000BG\u00053\u0000"+
		"\u0000CG\u0007\u0007\u0000\u0000DG\u0003\u0010\b\u0000EG\u0003\u0012\t"+
		"\u0000FA\u0001\u0000\u0000\u0000FB\u0001\u0000\u0000\u0000FC\u0001\u0000"+
		"\u0000\u0000FD\u0001\u0000\u0000\u0000FE\u0001\u0000\u0000\u0000G\r\u0001"+
		"\u0000\u0000\u0000HJ\u0007\b\u0000\u0000IK\u0005^\u0000\u0000JI\u0001"+
		"\u0000\u0000\u0000JK\u0001\u0000\u0000\u0000KP\u0001\u0000\u0000\u0000"+
		"LN\u0005_\u0000\u0000MO\u0005^\u0000\u0000NM\u0001\u0000\u0000\u0000N"+
		"O\u0001\u0000\u0000\u0000OQ\u0001\u0000\u0000\u0000PL\u0001\u0000\u0000"+
		"\u0000PQ\u0001\u0000\u0000\u0000QR\u0001\u0000\u0000\u0000RS\u0005]\u0000"+
		"\u0000S\u000f\u0001\u0000\u0000\u0000Tt\u0007\t\u0000\u0000Ut\u0007\n"+
		"\u0000\u0000Vt\u0007\u000b\u0000\u0000Wt\u0007\f\u0000\u0000Xt\u0007\r"+
		"\u0000\u0000Yt\u0007\u000e\u0000\u0000Zt\u0007\u000f\u0000\u0000[t\u0007"+
		"\u0010\u0000\u0000\\t\u0007\u0011\u0000\u0000]t\u0007\u0012\u0000\u0000"+
		"^t\u0007\u0013\u0000\u0000_t\u0007\u0014\u0000\u0000`t\u0007\u0015\u0000"+
		"\u0000at\u0007\u0016\u0000\u0000bt\u0007\u0017\u0000\u0000ct\u0007\u0018"+
		"\u0000\u0000dt\u0007\u0019\u0000\u0000et\u0007\u001a\u0000\u0000ft\u0007"+
		"\u001b\u0000\u0000gt\u0007\u001c\u0000\u0000ht\u0007\u001d\u0000\u0000"+
		"it\u0007\u001e\u0000\u0000jt\u0007\u001f\u0000\u0000kt\u0007 \u0000\u0000"+
		"lt\u0007!\u0000\u0000mt\u0007\"\u0000\u0000nt\u0007#\u0000\u0000ot\u0007"+
		"$\u0000\u0000pt\u0007%\u0000\u0000qt\u0007&\u0000\u0000rt\u0007\'\u0000"+
		"\u0000sT\u0001\u0000\u0000\u0000sU\u0001\u0000\u0000\u0000sV\u0001\u0000"+
		"\u0000\u0000sW\u0001\u0000\u0000\u0000sX\u0001\u0000\u0000\u0000sY\u0001"+
		"\u0000\u0000\u0000sZ\u0001\u0000\u0000\u0000s[\u0001\u0000\u0000\u0000"+
		"s\\\u0001\u0000\u0000\u0000s]\u0001\u0000\u0000\u0000s^\u0001\u0000\u0000"+
		"\u0000s_\u0001\u0000\u0000\u0000s`\u0001\u0000\u0000\u0000sa\u0001\u0000"+
		"\u0000\u0000sb\u0001\u0000\u0000\u0000sc\u0001\u0000\u0000\u0000sd\u0001"+
		"\u0000\u0000\u0000se\u0001\u0000\u0000\u0000sf\u0001\u0000\u0000\u0000"+
		"sg\u0001\u0000\u0000\u0000sh\u0001\u0000\u0000\u0000si\u0001\u0000\u0000"+
		"\u0000sj\u0001\u0000\u0000\u0000sk\u0001\u0000\u0000\u0000sl\u0001\u0000"+
		"\u0000\u0000sm\u0001\u0000\u0000\u0000sn\u0001\u0000\u0000\u0000so\u0001"+
		"\u0000\u0000\u0000sp\u0001\u0000\u0000\u0000sq\u0001\u0000\u0000\u0000"+
		"sr\u0001\u0000\u0000\u0000t\u0011\u0001\u0000\u0000\u0000uy\u0007(\u0000"+
		"\u0000vx\u0003\u0014\n\u0000wv\u0001\u0000\u0000\u0000x{\u0001\u0000\u0000"+
		"\u0000yw\u0001\u0000\u0000\u0000yz\u0001\u0000\u0000\u0000z|\u0001\u0000"+
		"\u0000\u0000{y\u0001\u0000\u0000\u0000|\u0087\u0005`\u0000\u0000}~\u0007"+
		"(\u0000\u0000~\u0082\u0005a\u0000\u0000\u007f\u0081\u0003\u0014\n\u0000"+
		"\u0080\u007f\u0001\u0000\u0000\u0000\u0081\u0084\u0001\u0000\u0000\u0000"+
		"\u0082\u0080\u0001\u0000\u0000\u0000\u0082\u0083\u0001\u0000\u0000\u0000"+
		"\u0083\u0085\u0001\u0000\u0000\u0000\u0084\u0082\u0001\u0000\u0000\u0000"+
		"\u0085\u0087\u0005`\u0000\u0000\u0086u\u0001\u0000\u0000\u0000\u0086}"+
		"\u0001\u0000\u0000\u0000\u0087\u0013\u0001\u0000\u0000\u0000\u0088\u0089"+
		"\u0007)\u0000\u0000\u0089\u008a\u0005b\u0000\u0000\u008a\u008d\u0007)"+
		"\u0000\u0000\u008b\u008d\u0007)\u0000\u0000\u008c\u0088\u0001\u0000\u0000"+
		"\u0000\u008c\u008b\u0001\u0000\u0000\u0000\u008d\u0015\u0001\u0000\u0000"+
		"\u0000\u0010\u001b%-169?FJNPsy\u0082\u0086\u008c";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}