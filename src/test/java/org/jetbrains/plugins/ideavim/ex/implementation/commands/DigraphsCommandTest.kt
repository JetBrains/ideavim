/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.DigraphCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class DigraphsCommandTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test digraphs is parsed correctly`() {
    val exCommand = injector.vimscriptParser.parseCommand("digraphs")
    assertTrue(exCommand is DigraphCommand)
  }

  @Test
  fun `test digraph output`() {
    assertCommandOutput("digraphs",
      """
        |NU ^@ 0000  SH ^A 0001  SX ^B 0002  EX ^C 0003  ET ^D 0004  EQ ^E 0005  
        |AK ^F 0006  BL ^G 0007  BS ^H 0008  HT ^I 0009  LF ^J 000a  VT ^K 000b  
        |FF ^L 000c  CR ^M 000d  SO ^N 000e  SI ^O 000f  DL ^P 0010  D1 ^Q 0011  
        |D2 ^R 0012  D3 ^S 0013  D4 ^T 0014  NK ^U 0015  SY ^V 0016  EB ^W 0017  
        |CN ^X 0018  EM ^Y 0019  SB ^Z 001a  EC ^[ 001b  FS ^\ 001c  GS ^] 001d  
        |RS ^^ 001e  US ^_ 001f  SP    0020  Nb #  0023  DO ${'$'}  0024  At @  0040  
        |<( [  005b  // \  005c  )> ]  005d  '> ^  005e  '! `  0060  (! {  007b  
        |!! |  007c  !) }  007d  '? ~  007e  DT   007f  PA ~@ 0080  HO ~A 0081  
        |BH ~B 0082  NH ~C 0083  IN ~D 0084  NL ~E 0085  SA ~F 0086  ES ~G 0087  
        |HS ~H 0088  HJ ~I 0089  VS ~J 008a  PD ~K 008b  PU ~L 008c  RI ~M 008d  
        |S2 ~N 008e  S3 ~O 008f  DC ~P 0090  P1 ~Q 0091  P2 ~R 0092  TS ~S 0093  
        |CC ~T 0094  MW ~U 0095  SG ~V 0096  EG ~W 0097  SS ~X 0098  GC ~Y 0099  
        |SC ~Z 009a  CI ~[ 009b  ST ~\ 009c  OC ~] 009d  PM ~^ 009e  AC ~_ 009f  
        |NS    00a0  !I ¡  00a1  Ct ¢  00a2  Pd £  00a3  Cu ¤  00a4  Ye ¥  00a5  
        |BB ¦  00a6  SE §  00a7  ': ¨  00a8  Co ©  00a9  -a ª  00aa  << «  00ab  
        |NO ¬  00ac  -- ­  00ad  Rg ®  00ae  'm ¯  00af  DG °  00b0  +- ±  00b1  
        |2S ²  00b2  3S ³  00b3  '' ´  00b4  My µ  00b5  PI ¶  00b6  .M ·  00b7  
        |', ¸  00b8  1S ¹  00b9  -o º  00ba  >> »  00bb  14 ¼  00bc  12 ½  00bd  
        |34 ¾  00be  ?I ¿  00bf  A! À  00c0  A' Á  00c1  A> Â  00c2  A? Ã  00c3  
        |A: Ä  00c4  AA Å  00c5  AE Æ  00c6  C, Ç  00c7  E! È  00c8  E' É  00c9  
        |E> Ê  00ca  E: Ë  00cb  I! Ì  00cc  I' Í  00cd  I> Î  00ce  I: Ï  00cf  
        |D- Ð  00d0  N? Ñ  00d1  O! Ò  00d2  O' Ó  00d3  O> Ô  00d4  O? Õ  00d5  
        |O: Ö  00d6  *X ×  00d7  O/ Ø  00d8  U! Ù  00d9  U' Ú  00da  U> Û  00db  
        |U: Ü  00dc  Y' Ý  00dd  TH Þ  00de  ss ß  00df  a! à  00e0  a' á  00e1  
        |a> â  00e2  a? ã  00e3  a: ä  00e4  aa å  00e5  ae æ  00e6  c, ç  00e7  
        |e! è  00e8  e' é  00e9  e> ê  00ea  e: ë  00eb  i! ì  00ec  i' í  00ed  
        |i> î  00ee  i: ï  00ef  d- ð  00f0  n? ñ  00f1  o! ò  00f2  o' ó  00f3  
        |o> ô  00f4  o? õ  00f5  o: ö  00f6  -: ÷  00f7  o/ ø  00f8  u! ù  00f9  
        |u' ú  00fa  u> û  00fb  u: ü  00fc  y' ý  00fd  th þ  00fe  y: ÿ  00ff  
        |A- Ā  0100  a- ā  0101  A( Ă  0102  a( ă  0103  A; Ą  0104  a; ą  0105  
        |C' Ć  0106  c' ć  0107  C> Ĉ  0108  c> ĉ  0109  C. Ċ  010a  c. ċ  010b  
        |C< Č  010c  c< č  010d  D< Ď  010e  d< ď  010f  D/ Đ  0110  d/ đ  0111  
        |E- Ē  0112  e- ē  0113  E( Ĕ  0114  e( ĕ  0115  E. Ė  0116  e. ė  0117  
        |E; Ę  0118  e; ę  0119  E< Ě  011a  e< ě  011b  G> Ĝ  011c  g> ĝ  011d  
        |G( Ğ  011e  g( ğ  011f  G. Ġ  0120  g. ġ  0121  G, Ģ  0122  g, ģ  0123  
        |H> Ĥ  0124  h> ĥ  0125  H/ Ħ  0126  h/ ħ  0127  I? Ĩ  0128  i? ĩ  0129  
        |I- Ī  012a  i- ī  012b  I( Ĭ  012c  i( ĭ  012d  I; Į  012e  i; į  012f  
        |I. İ  0130  i. ı  0131  IJ Ĳ  0132  ij ĳ  0133  J> Ĵ  0134  j> ĵ  0135  
        |K, Ķ  0136  k, ķ  0137  kk ĸ  0138  L' Ĺ  0139  l' ĺ  013a  L, Ļ  013b  
        |l, ļ  013c  L< Ľ  013d  l< ľ  013e  L. Ŀ  013f  l. ŀ  0140  L/ Ł  0141  
        |l/ ł  0142  N' Ń  0143  n' ń  0144  N, Ņ  0145  n, ņ  0146  N< Ň  0147  
        |n< ň  0148  'n ŉ  0149  NG Ŋ  014a  ng ŋ  014b  O- Ō  014c  o- ō  014d  
        |O( Ŏ  014e  o( ŏ  014f  O" Ő  0150  o" ő  0151  OE Œ  0152  oe œ  0153  
        |R' Ŕ  0154  r' ŕ  0155  R, Ŗ  0156  r, ŗ  0157  R< Ř  0158  r< ř  0159  
        |S' Ś  015a  s' ś  015b  S> Ŝ  015c  s> ŝ  015d  S, Ş  015e  s, ş  015f  
        |S< Š  0160  s< š  0161  T, Ţ  0162  t, ţ  0163  T< Ť  0164  t< ť  0165  
        |T/ Ŧ  0166  t/ ŧ  0167  U? Ũ  0168  u? ũ  0169  U- Ū  016a  u- ū  016b  
        |U( Ŭ  016c  u( ŭ  016d  U0 Ů  016e  u0 ů  016f  U" Ű  0170  u" ű  0171  
        |U; Ų  0172  u; ų  0173  W> Ŵ  0174  w> ŵ  0175  Y> Ŷ  0176  y> ŷ  0177  
        |Y: Ÿ  0178  Z' Ź  0179  z' ź  017a  Z. Ż  017b  z. ż  017c  Z< Ž  017d  
        |z< ž  017e  O9 Ơ  01a0  o9 ơ  01a1  OI Ƣ  01a2  oi ƣ  01a3  yr Ʀ  01a6  
        |U9 Ư  01af  u9 ư  01b0  Z/ Ƶ  01b5  z/ ƶ  01b6  ED Ʒ  01b7  A< Ǎ  01cd  
        |a< ǎ  01ce  I< Ǐ  01cf  i< ǐ  01d0  O< Ǒ  01d1  o< ǒ  01d2  U< Ǔ  01d3  
        |u< ǔ  01d4  A1 Ǟ  01de  a1 ǟ  01df  A7 Ǡ  01e0  a7 ǡ  01e1  A3 Ǣ  01e2  
        |a3 ǣ  01e3  G/ Ǥ  01e4  g/ ǥ  01e5  G< Ǧ  01e6  g< ǧ  01e7  K< Ǩ  01e8  
        |k< ǩ  01e9  O; Ǫ  01ea  o; ǫ  01eb  O1 Ǭ  01ec  o1 ǭ  01ed  EZ Ǯ  01ee  
        |ez ǯ  01ef  j< ǰ  01f0  G' Ǵ  01f4  g' ǵ  01f5  ;S ʿ  02bf  '< ˇ  02c7  
        |'( ˘  02d8  '. ˙  02d9  '0 ˚  02da  '; ˛  02db  '" ˝  02dd  A% Ά  0386  
        |E% Έ  0388  Y% Ή  0389  I% Ί  038a  O% Ό  038c  U% Ύ  038e  W% Ώ  038f  
        |i3 ΐ  0390  A* Α  0391  B* Β  0392  G* Γ  0393  D* Δ  0394  E* Ε  0395  
        |Z* Ζ  0396  Y* Η  0397  H* Θ  0398  I* Ι  0399  K* Κ  039a  L* Λ  039b  
        |M* Μ  039c  N* Ν  039d  C* Ξ  039e  O* Ο  039f  P* Π  03a0  R* Ρ  03a1  
        |S* Σ  03a3  T* Τ  03a4  U* Υ  03a5  F* Φ  03a6  X* Χ  03a7  Q* Ψ  03a8  
        |W* Ω  03a9  J* Ϊ  03aa  V* Ϋ  03ab  a% ά  03ac  e% έ  03ad  y% ή  03ae  
        |i% ί  03af  u3 ΰ  03b0  a* α  03b1  b* β  03b2  g* γ  03b3  d* δ  03b4  
        |e* ε  03b5  z* ζ  03b6  y* η  03b7  h* θ  03b8  i* ι  03b9  k* κ  03ba  
        |l* λ  03bb  m* μ  03bc  n* ν  03bd  c* ξ  03be  o* ο  03bf  p* π  03c0  
        |r* ρ  03c1  *s ς  03c2  s* σ  03c3  t* τ  03c4  u* υ  03c5  f* φ  03c6  
        |x* χ  03c7  q* ψ  03c8  w* ω  03c9  j* ϊ  03ca  v* ϋ  03cb  o% ό  03cc  
        |u% ύ  03cd  w% ώ  03ce  'G Ϙ  03d8  ,G ϙ  03d9  T3 Ϛ  03da  t3 ϛ  03db  
        |M3 Ϝ  03dc  m3 ϝ  03dd  K3 Ϟ  03de  k3 ϟ  03df  P3 Ϡ  03e0  p3 ϡ  03e1  
        |'% ϴ  03f4  j3 ϵ  03f5  IO Ё  0401  D% Ђ  0402  G% Ѓ  0403  IE Є  0404  
        |DS Ѕ  0405  II І  0406  YI Ї  0407  J% Ј  0408  LJ Љ  0409  NJ Њ  040a  
        |Ts Ћ  040b  KJ Ќ  040c  V% Ў  040e  DZ Џ  040f  A= А  0410  B= Б  0411  
        |V= В  0412  G= Г  0413  D= Д  0414  E= Е  0415  Z% Ж  0416  Z= З  0417  
        |I= И  0418  J= Й  0419  K= К  041a  L= Л  041b  M= М  041c  N= Н  041d  
        |O= О  041e  P= П  041f  R= Р  0420  S= С  0421  T= Т  0422  U= У  0423  
        |F= Ф  0424  H= Х  0425  C= Ц  0426  C% Ч  0427  S% Ш  0428  Sc Щ  0429  
        |=" Ъ  042a  Y= Ы  042b  %" Ь  042c  JE Э  042d  JU Ю  042e  JA Я  042f  
        |a= а  0430  b= б  0431  v= в  0432  g= г  0433  d= д  0434  e= е  0435  
        |z% ж  0436  z= з  0437  i= и  0438  j= й  0439  k= к  043a  l= л  043b  
        |m= м  043c  n= н  043d  o= о  043e  p= п  043f  r= р  0440  s= с  0441  
        |t= т  0442  u= у  0443  f= ф  0444  h= х  0445  c= ц  0446  c% ч  0447  
        |s% ш  0448  sc щ  0449  =' ъ  044a  y= ы  044b  %' ь  044c  je э  044d  
        |ju ю  044e  ja я  044f  io ё  0451  d% ђ  0452  g% ѓ  0453  ie є  0454  
        |ds ѕ  0455  ii і  0456  yi ї  0457  j% ј  0458  lj љ  0459  nj њ  045a  
        |ts ћ  045b  kj ќ  045c  v% ў  045e  dz џ  045f  Y3 Ѣ  0462  y3 ѣ  0463  
        |O3 Ѫ  046a  o3 ѫ  046b  F3 Ѳ  0472  f3 ѳ  0473  V3 Ѵ  0474  v3 ѵ  0475  
        |C3 Ҁ  0480  c3 ҁ  0481  G3 Ґ  0490  g3 ґ  0491  A+ א  05d0  B+ ב  05d1  
        |G+ ג  05d2  D+ ד  05d3  H+ ה  05d4  W+ ו  05d5  Z+ ז  05d6  X+ ח  05d7  
        |Tj ט  05d8  J+ י  05d9  K% ך  05da  K+ כ  05db  L+ ל  05dc  M% ם  05dd  
        |M+ מ  05de  N% ן  05df  N+ נ  05e0  S+ ס  05e1  E+ ע  05e2  P% ף  05e3  
        |P+ פ  05e4  Zj ץ  05e5  ZJ צ  05e6  Q+ ק  05e7  R+ ר  05e8  Sh ש  05e9  
        |T+ ת  05ea  ,+ ،  060c  ;+ ؛  061b  ?+ ؟  061f  H' ء  0621  aM آ  0622  
        |aH أ  0623  wH ؤ  0624  ah إ  0625  yH ئ  0626  a+ ا  0627  b+ ب  0628  
        |tm ة  0629  t+ ت  062a  tk ث  062b  g+ ج  062c  hk ح  062d  x+ خ  062e  
        |d+ د  062f  dk ذ  0630  r+ ر  0631  z+ ز  0632  s+ س  0633  sn ش  0634  
        |c+ ص  0635  dd ض  0636  tj ط  0637  zH ظ  0638  e+ ع  0639  i+ غ  063a  
        |++ ـ  0640  f+ ف  0641  q+ ق  0642  k+ ك  0643  l+ ل  0644  m+ م  0645  
        |n+ ن  0646  h+ ه  0647  w+ و  0648  j+ ى  0649  y+ ي  064a  :+ ً  064b  
        |"+ ٌ  064c  =+ ٍ  064d  /+ َ  064e  '+ ُ  064f  1+ ِ  0650  3+ ّ  0651  
        |0+ ْ  0652  aS ٰ  0670  p+ پ  067e  v+ ڤ  06a4  gf گ  06af  0a ۰  06f0  
        |1a ۱  06f1  2a ۲  06f2  3a ۳  06f3  4a ۴  06f4  5a ۵  06f5  6a ۶  06f6  
        |7a ۷  06f7  8a ۸  06f8  9a ۹  06f9  B. Ḃ  1e02  b. ḃ  1e03  B_ Ḇ  1e06  
        |b_ ḇ  1e07  D. Ḋ  1e0a  d. ḋ  1e0b  D_ Ḏ  1e0e  d_ ḏ  1e0f  D, Ḑ  1e10  
        |d, ḑ  1e11  F. Ḟ  1e1e  f. ḟ  1e1f  G- Ḡ  1e20  g- ḡ  1e21  H. Ḣ  1e22  
        |h. ḣ  1e23  H: Ḧ  1e26  h: ḧ  1e27  H, Ḩ  1e28  h, ḩ  1e29  K' Ḱ  1e30  
        |k' ḱ  1e31  K_ Ḵ  1e34  k_ ḵ  1e35  L_ Ḻ  1e3a  l_ ḻ  1e3b  M' Ḿ  1e3e  
        |m' ḿ  1e3f  M. Ṁ  1e40  m. ṁ  1e41  N. Ṅ  1e44  n. ṅ  1e45  N_ Ṉ  1e48  
        |n_ ṉ  1e49  P' Ṕ  1e54  p' ṕ  1e55  P. Ṗ  1e56  p. ṗ  1e57  R. Ṙ  1e58  
        |r. ṙ  1e59  R_ Ṟ  1e5e  r_ ṟ  1e5f  S. Ṡ  1e60  s. ṡ  1e61  T. Ṫ  1e6a  
        |t. ṫ  1e6b  T_ Ṯ  1e6e  t_ ṯ  1e6f  V? Ṽ  1e7c  v? ṽ  1e7d  W! Ẁ  1e80  
        |w! ẁ  1e81  W' Ẃ  1e82  w' ẃ  1e83  W: Ẅ  1e84  w: ẅ  1e85  W. Ẇ  1e86  
        |w. ẇ  1e87  X. Ẋ  1e8a  x. ẋ  1e8b  X: Ẍ  1e8c  x: ẍ  1e8d  Y. Ẏ  1e8e  
        |y. ẏ  1e8f  Z> Ẑ  1e90  z> ẑ  1e91  Z_ Ẕ  1e94  z_ ẕ  1e95  h_ ẖ  1e96  
        |t: ẗ  1e97  w0 ẘ  1e98  y0 ẙ  1e99  A2 Ả  1ea2  a2 ả  1ea3  E2 Ẻ  1eba  
        |e2 ẻ  1ebb  E? Ẽ  1ebc  e? ẽ  1ebd  I2 Ỉ  1ec8  i2 ỉ  1ec9  O2 Ỏ  1ece  
        |o2 ỏ  1ecf  U2 Ủ  1ee6  u2 ủ  1ee7  Y! Ỳ  1ef2  y! ỳ  1ef3  Y2 Ỷ  1ef6  
        |y2 ỷ  1ef7  Y? Ỹ  1ef8  y? ỹ  1ef9  ;' ἀ  1f00  ,' ἁ  1f01  ;! ἂ  1f02  
        |,! ἃ  1f03  ?; ἄ  1f04  ?, ἅ  1f05  !: ἆ  1f06  ?: ἇ  1f07  1N    2002  
        |1M    2003  3M    2004  4M    2005  6M    2006  1T    2009  1H    200a  
        |-1 ‐  2010  -N –  2013  -M —  2014  -3 ―  2015  !2 ‖  2016  =2 ‗  2017  
        |'6 ‘  2018  '9 ’  2019  .9 ‚  201a  9' ‛  201b  "6 “  201c  "9 ”  201d  
        |:9 „  201e  9" ‟  201f  /- †  2020  /= ‡  2021  .. ‥  2025  ,. …  2026  
        |%0 ‰  2030  1' ′  2032  2' ″  2033  3' ‴  2034  1" ‵  2035  2" ‶  2036  
        |3" ‷  2037  Ca ‸  2038  <1 ‹  2039  >1 ›  203a  :X ※  203b  '- ‾  203e  
        |/f ⁄  2044  0S ⁰  2070  4S ⁴  2074  5S ⁵  2075  6S ⁶  2076  7S ⁷  2077  
        |8S ⁸  2078  9S ⁹  2079  +S ⁺  207a  -S ⁻  207b  =S ⁼  207c  (S ⁽  207d  
        |)S ⁾  207e  nS ⁿ  207f  0s ₀  2080  1s ₁  2081  2s ₂  2082  3s ₃  2083  
        |4s ₄  2084  5s ₅  2085  6s ₆  2086  7s ₇  2087  8s ₈  2088  9s ₉  2089  
        |+s ₊  208a  -s ₋  208b  =s ₌  208c  (s ₍  208d  )s ₎  208e  Li ₤  20a4  
        |Pt ₧  20a7  W= ₩  20a9  oC ℃  2103  co ℅  2105  oF ℉  2109  N0 №  2116  
        |PO ℗  2117  Rx ℞  211e  SM ℠  2120  TM ™  2122  Om Ω  2126  AO Å  212b  
        |13 ⅓  2153  23 ⅔  2154  15 ⅕  2155  25 ⅖  2156  35 ⅗  2157  45 ⅘  2158  
        |16 ⅙  2159  56 ⅚  215a  18 ⅛  215b  38 ⅜  215c  58 ⅝  215d  78 ⅞  215e  
        |1R Ⅰ  2160  2R Ⅱ  2161  3R Ⅲ  2162  4R Ⅳ  2163  5R Ⅴ  2164  6R Ⅵ  2165  
        |7R Ⅶ  2166  8R Ⅷ  2167  9R Ⅸ  2168  aR Ⅹ  2169  bR Ⅺ  216a  cR Ⅻ  216b  
        |1r ⅰ  2170  2r ⅱ  2171  3r ⅲ  2172  4r ⅳ  2173  5r ⅴ  2174  6r ⅵ  2175  
        |7r ⅶ  2176  8r ⅷ  2177  9r ⅸ  2178  ar ⅹ  2179  br ⅺ  217a  cr ⅻ  217b  
        |<- ←  2190  -! ↑  2191  -> →  2192  -v ↓  2193  <> ↔  2194  UD ↕  2195  
        |<= ⇐  21d0  => ⇒  21d2  == ⇔  21d4  FA ∀  2200  dP ∂  2202  TE ∃  2203  
        |/0 ∅  2205  DE ∆  2206  NB ∇  2207  (- ∈  2208  -) ∋  220b  *P ∏  220f  
        |+Z ∑  2211  -2 −  2212  -+ ∓  2213  *- ∗  2217  Ob ∘  2218  Sb ∙  2219  
        |RT √  221a  0( ∝  221d  00 ∞  221e  -L ∟  221f  -V ∠  2220  PP ∥  2225  
        |AN ∧  2227  OR ∨  2228  (U ∩  2229  )U ∪  222a  In ∫  222b  DI ∬  222c  
        |Io ∮  222e  .: ∴  2234  :. ∵  2235  :R ∶  2236  :: ∷  2237  ?1 ∼  223c  
        |CG ∾  223e  ?- ≃  2243  ?= ≅  2245  ?2 ≈  2248  =? ≌  224c  HI ≓  2253  
        |!= ≠  2260  =3 ≡  2261  =< ≤  2264  >= ≥  2265  <* ≪  226a  *> ≫  226b  
        |!< ≮  226e  !> ≯  226f  (C ⊂  2282  )C ⊃  2283  (_ ⊆  2286  )_ ⊇  2287  
        |0. ⊙  2299  02 ⊚  229a  -T ⊥  22a5  .P ⋅  22c5  :3 ⋮  22ee  .3 ⋯  22ef  
        |Eh ⌂  2302  <7 ⌈  2308  >7 ⌉  2309  7< ⌊  230a  7> ⌋  230b  NI ⌐  2310  
        |(A ⌒  2312  TR ⌕  2315  Iu ⌠  2320  Il ⌡  2321  </ 〈  2329  /> 〉  232a  
        |Vs ␣  2423  1h ⑀  2440  3h ⑁  2441  2h ⑂  2442  4h ⑃  2443  1j ⑆  2446  
        |2j ⑇  2447  3j ⑈  2448  4j ⑉  2449  1. ⒈  2488  2. ⒉  2489  3. ⒊  248a  
        |4. ⒋  248b  5. ⒌  248c  6. ⒍  248d  7. ⒎  248e  8. ⒏  248f  9. ⒐  2490  
        |hh ─  2500  HH ━  2501  vv │  2502  VV ┃  2503  3- ┄  2504  3_ ┅  2505  
        |3! ┆  2506  3/ ┇  2507  4- ┈  2508  4_ ┉  2509  4! ┊  250a  4/ ┋  250b  
        |dr ┌  250c  dR ┍  250d  Dr ┎  250e  DR ┏  250f  dl ┐  2510  dL ┑  2511  
        |Dl ┒  2512  LD ┓  2513  ur └  2514  uR ┕  2515  Ur ┖  2516  UR ┗  2517  
        |ul ┘  2518  uL ┙  2519  Ul ┚  251a  UL ┛  251b  vr ├  251c  vR ┝  251d  
        |Vr ┠  2520  VR ┣  2523  vl ┤  2524  vL ┥  2525  Vl ┨  2528  VL ┫  252b  
        |dh ┬  252c  dH ┯  252f  Dh ┰  2530  DH ┳  2533  uh ┴  2534  uH ┷  2537  
        |Uh ┸  2538  UH ┻  253b  vh ┼  253c  vH ┿  253f  Vh ╂  2542  VH ╋  254b  
        |FD ╱  2571  BD ╲  2572  TB ▀  2580  LB ▄  2584  FB █  2588  lB ▌  258c  
        |RB ▐  2590  .S ░  2591  :S ▒  2592  ?S ▓  2593  fS ■  25a0  OS □  25a1  
        |RO ▢  25a2  Rr ▣  25a3  RF ▤  25a4  RY ▥  25a5  RH ▦  25a6  RZ ▧  25a7  
        |RK ▨  25a8  RX ▩  25a9  sB ▪  25aa  SR ▬  25ac  Or ▭  25ad  UT ▲  25b2  
        |uT △  25b3  PR ▶  25b6  Tr ▷  25b7  Dt ▼  25bc  dT ▽  25bd  PL ◀  25c0  
        |Tl ◁  25c1  Db ◆  25c6  Dw ◇  25c7  LZ ◊  25ca  0m ○  25cb  0o ◎  25ce  
        |0M ●  25cf  0L ◐  25d0  0R ◑  25d1  Sn ◘  25d8  Ic ◙  25d9  Fd ◢  25e2  
        |Bd ◣  25e3  *2 ★  2605  *1 ☆  2606  <H ☜  261c  >H ☞  261e  0u ☺  263a  
        |0U ☻  263b  SU ☼  263c  Fm ♀  2640  Ml ♂  2642  cS ♠  2660  cH ♡  2661  
        |cD ♢  2662  cC ♣  2663  Md ♩  2669  M8 ♪  266a  M2 ♫  266b  Mb ♭  266d  
        |Mx ♮  266e  MX ♯  266f  OK ✓  2713  XX ✗  2717  -X ✠  2720  IS 　  3000  
        |,_ 、  3001  ._ 。  3002  +" 〃  3003  +_ 〄  3004  *_ 々  3005  ;_ 〆  3006  
        |0_ 〇  3007  <+ 《  300a  >+ 》  300b  <' 「  300c  >' 」  300d  <" 『  300e  
        |>" 』  300f  (" 【  3010  )" 】  3011  =T 〒  3012  =_ 〓  3013  (' 〔  3014  
        |)' 〕  3015  (I 〖  3016  )I 〗  3017  -? 〜  301c  A5 ぁ  3041  a5 あ  3042  
        |I5 ぃ  3043  i5 い  3044  U5 ぅ  3045  u5 う  3046  E5 ぇ  3047  e5 え  3048  
        |O5 ぉ  3049  o5 お  304a  ka か  304b  ga が  304c  ki き  304d  gi ぎ  304e  
        |ku く  304f  gu ぐ  3050  ke け  3051  ge げ  3052  ko こ  3053  go ご  3054  
        |sa さ  3055  za ざ  3056  si し  3057  zi じ  3058  su す  3059  zu ず  305a  
        |se せ  305b  ze ぜ  305c  so そ  305d  zo ぞ  305e  ta た  305f  da だ  3060  
        |ti ち  3061  di ぢ  3062  tU っ  3063  tu つ  3064  du づ  3065  te て  3066  
        |de で  3067  to と  3068  do ど  3069  na な  306a  ni に  306b  nu ぬ  306c  
        |ne ね  306d  no の  306e  ha は  306f  ba ば  3070  pa ぱ  3071  hi ひ  3072  
        |bi び  3073  pi ぴ  3074  hu ふ  3075  bu ぶ  3076  pu ぷ  3077  he へ  3078  
        |be べ  3079  pe ぺ  307a  ho ほ  307b  bo ぼ  307c  po ぽ  307d  ma ま  307e  
        |mi み  307f  mu む  3080  me め  3081  mo も  3082  yA ゃ  3083  ya や  3084  
        |yU ゅ  3085  yu ゆ  3086  yO ょ  3087  yo よ  3088  ra ら  3089  ri り  308a  
        |ru る  308b  re れ  308c  ro ろ  308d  wA ゎ  308e  wa わ  308f  wi ゐ  3090  
        |we ゑ  3091  wo を  3092  n5 ん  3093  vu ゔ  3094  "5 ゛  309b  05 ゜  309c  
        |*5 ゝ  309d  +5 ゞ  309e  a6 ァ  30a1  A6 ア  30a2  i6 ィ  30a3  I6 イ  30a4  
        |u6 ゥ  30a5  U6 ウ  30a6  e6 ェ  30a7  E6 エ  30a8  o6 ォ  30a9  O6 オ  30aa  
        |Ka カ  30ab  Ga ガ  30ac  Ki キ  30ad  Gi ギ  30ae  Ku ク  30af  Gu グ  30b0  
        |Ke ケ  30b1  Ge ゲ  30b2  Ko コ  30b3  Go ゴ  30b4  Sa サ  30b5  Za ザ  30b6  
        |Si シ  30b7  Zi ジ  30b8  Su ス  30b9  Zu ズ  30ba  Se セ  30bb  Ze ゼ  30bc  
        |So ソ  30bd  Zo ゾ  30be  Ta タ  30bf  Da ダ  30c0  Ti チ  30c1  Di ヂ  30c2  
        |TU ッ  30c3  Tu ツ  30c4  Du ヅ  30c5  Te テ  30c6  De デ  30c7  To ト  30c8  
        |Do ド  30c9  Na ナ  30ca  Ni ニ  30cb  Nu ヌ  30cc  Ne ネ  30cd  No ノ  30ce  
        |Ha ハ  30cf  Ba バ  30d0  Pa パ  30d1  Hi ヒ  30d2  Bi ビ  30d3  Pi ピ  30d4  
        |Hu フ  30d5  Bu ブ  30d6  Pu プ  30d7  He ヘ  30d8  Be ベ  30d9  Pe ペ  30da  
        |Ho ホ  30db  Bo ボ  30dc  Po ポ  30dd  Ma マ  30de  Mi ミ  30df  Mu ム  30e0  
        |Me メ  30e1  Mo モ  30e2  YA ャ  30e3  Ya ヤ  30e4  YU ュ  30e5  Yu ユ  30e6  
        |YO ョ  30e7  Yo ヨ  30e8  Ra ラ  30e9  Ri リ  30ea  Ru ル  30eb  Re レ  30ec  
        |Ro ロ  30ed  WA ヮ  30ee  Wa ワ  30ef  Wi ヰ  30f0  We ヱ  30f1  Wo ヲ  30f2  
        |N6 ン  30f3  Vu ヴ  30f4  KA ヵ  30f5  KE ヶ  30f6  Va ヷ  30f7  Vi ヸ  30f8  
        |Ve ヹ  30f9  Vo ヺ  30fa  .6 ・  30fb  -6 ー  30fc  *6 ヽ  30fd  +6 ヾ  30fe  
        |b4 ㄅ  3105  p4 ㄆ  3106  m4 ㄇ  3107  f4 ㄈ  3108  d4 ㄉ  3109  t4 ㄊ  310a  
        |n4 ㄋ  310b  l4 ㄌ  310c  g4 ㄍ  310d  k4 ㄎ  310e  h4 ㄏ  310f  j4 ㄐ  3110  
        |q4 ㄑ  3111  x4 ㄒ  3112  zh ㄓ  3113  ch ㄔ  3114  sh ㄕ  3115  r4 ㄖ  3116  
        |z4 ㄗ  3117  c4 ㄘ  3118  s4 ㄙ  3119  a4 ㄚ  311a  o4 ㄛ  311b  e4 ㄜ  311c  
        |ai ㄞ  311e  ei ㄟ  311f  au ㄠ  3120  ou ㄡ  3121  an ㄢ  3122  en ㄣ  3123  
        |aN ㄤ  3124  eN ㄥ  3125  er ㄦ  3126  i4 ㄧ  3127  u4 ㄨ  3128  iu ㄩ  3129  
        |v4 ㄪ  312a  nG ㄫ  312b  gn ㄬ  312c  1c ㈠  3220  2c ㈡  3221  3c ㈢  3222  
        |4c ㈣  3223  5c ㈤  3224  6c ㈥  3225  7c ㈦  3226  8c ㈧  3227  9c ㈨  3228  
        |/c   e001  UA   e002  UB   e003  "3   e004  "1   e005  "!   e006  
        |"'   e007  ">   e008  "?   e009  "-   e00a  "(   e00b  ".   e00c  
        |":   e00d  "0   e00e  ""   e00f  "<   e010  ",   e011  ";   e012  
        |"_   e013  "=   e014  "/   e015  "i   e016  "d   e017  "p   e018  
        |;;   e019  ,,   e01a  b3   e01b  Ci   e01c  f(   e01d  ed   e01e  
        |am   e01f  pm   e020  Fl   e023  GF   e024  >V   e025  !*   e026  
        |?*   e027  J<   e028  ff ﬀ  fb00  fi ﬁ  fb01  fl ﬂ  fb02  ft ﬅ  fb05  
        |st ﬆ  fb06  
      """.trimMargin())
  }
}
