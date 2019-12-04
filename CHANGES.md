The Changelog
=============

History of changes in IdeaVim for the IntelliJ platform.


Get an Early Access
-------------------

Would you like to try new features and fixes? Join the Early Access Program and
receive EAP builds as updates! Use the `EAP` option in the status bar or
add this URL to "Settings | Plugins | Manage Plugin Repositories":
`https://plugins.jetbrains.com/plugins/eap/ideavim`

It is important to distinguish EAP from traditional pre-release software.
Please note that the quality of EAP versions may at times be way below even
usual beta standards.

[To Be Released]
--------------

**Features:**
* Surround and Commentary extensions can be repeated with a dot command ([VIM-1118](https://youtrack.jetbrains.com/issue/VIM-1118))
* Support XDG settings standard ([VIM-664](https://youtrack.jetbrains.com/issue/VIM-664))
* Add option to remove the status bar icon ([VIM-1847](https://youtrack.jetbrains.com/issue/VIM-1847))

**Fixes:**
* [VIM-1823](https://youtrack.jetbrains.com/issue/VIM-1823) Fix multiple carets with ignorecase
* [VIM-1053](https://youtrack.jetbrains.com/issue/VIM-1053)
  [VIM-1038](https://youtrack.jetbrains.com/issue/VIM-1038)
  Implement gq+motion. Support some double `g` commands (`guu`, `gUU`, `g~~`).
* [VIM-1325](https://youtrack.jetbrains.com/issue/VIM-1325)
  [VIM-1050](https://youtrack.jetbrains.com/issue/VIM-1050)
  [VIM-1627](https://youtrack.jetbrains.com/issue/VIM-1627)
  Fix bindings for active lookup
* [VIM-1845](https://youtrack.jetbrains.com/issue/VIM-1845) Show ActionGroup popups
* [VIM-1424](https://youtrack.jetbrains.com/issue/VIM-1424) CTRL-A doesn't have any restrictions now
* [VIM-1454](https://youtrack.jetbrains.com/issue/VIM-1454) Fix CTRL-W with the autocompletion lookup
* [VIM-1855](https://youtrack.jetbrains.com/issue/VIM-1855) Fix initialization error
* [VIM-1853](https://youtrack.jetbrains.com/issue/VIM-1853) Fix marks for disposed projects
* [VIM-1858](https://youtrack.jetbrains.com/issue/VIM-1858) Fix imap for autocomplete
* [VIM-1362](https://youtrack.jetbrains.com/issue/VIM-1362) Search with confirm doesn't scroll down far enough


0.54, 2019-11-20
--------------

**Features:**
* EasyMotion plugin emulation ([VIM-820](https://youtrack.jetbrains.com/issue/VIM-820) | [Instructions](https://github.com/JetBrains/ideavim#emulated-vim-plugins))
* Support surrounding with a function name ([link](https://github.com/tpope/vim-surround/blob/master/doc/surround.txt#L138))
* Add `:delmarks` command ([VIM-1720](https://youtrack.jetbrains.com/issue/VIM-1720))
* Add IdeaVim icon to the status bar ([VIM-943](https://youtrack.jetbrains.com/issue/VIM-943))

**Changes:**
* Remove the default IdeaVim toggle shortcut (<kbd>CTRL</kbd><kbd>ALT</kbd><kbd>V</kbd>)
* Rename `refactoring` value of `selectmode` option to `ideaselection`
* Remove `template` value of `selectmode` option and replace it with `idearefactormode` option.

**Fixes:**
* [VIM-1766](https://youtrack.jetbrains.com/issue/VIM-1766) Fix disappearing caret in ex entry on Linux
* [VIM-1032](https://youtrack.jetbrains.com/issue/VIM-1032) Annotations work well with relative line numbers enabled
* [VIM-1762](https://youtrack.jetbrains.com/issue/VIM-1762) Relative line numbers respect line number theme
* [VIM-1717](https://youtrack.jetbrains.com/issue/VIM-1717) Fix incorrect scroll location if no match with `incsearch`
* [VIM-1757](https://youtrack.jetbrains.com/issue/VIM-1757) Fix incorrect search results when specifying offset as part of search command
* Fix search highlights not showing after deleting last result
* Update current line highlight during incsearch and replace operation
* [VIM-1773](https://youtrack.jetbrains.com/issue/VIM-1773) Provide fallback error stripe colour if not defined
* [VIM-1785](https://youtrack.jetbrains.com/issue/VIM-1785)
  [VIM-1731](https://youtrack.jetbrains.com/issue/VIM-1731)
  Fix some problems with yanking to clipboard
* [VIM-1781](https://youtrack.jetbrains.com/issue/VIM-1781) Fix yanking with dollar motion
* [VIM-1772](https://youtrack.jetbrains.com/issue/VIM-1772) Fix yanking with `:y` command
* [VIM-1685](https://youtrack.jetbrains.com/issue/VIM-1685) Fix `ESC` for insert mode
* [VIM-1752](https://youtrack.jetbrains.com/issue/VIM-1752) Fix `ESC` for insert mode
* [VIM-1189](https://youtrack.jetbrains.com/issue/VIM-1189)
  [VIM-927](https://youtrack.jetbrains.com/issue/VIM-927)
  Fix mappings to black hole register
* [VIM-1804](https://youtrack.jetbrains.com/issue/VIM-1804) Exit insert move after toggling IdeaVim
* [VIM-1749](https://youtrack.jetbrains.com/issue/VIM-1749) Tag surround is finished on `>`
* [VIM-1801](https://youtrack.jetbrains.com/issue/VIM-1801) Fix j/k motions with inline hints
* [VIM-1800](https://youtrack.jetbrains.com/issue/VIM-1800) Improve ideamarks option
* [VIM-1819](https://youtrack.jetbrains.com/issue/VIM-1819) Fix execution of some rider actions
* [VIM-1604](https://youtrack.jetbrains.com/issue/VIM-1604) Add IdeaVim logo


0.53, 2019-08-07
--------------
* [VIM-1711](https://youtrack.jetbrains.com/issue/VIM-1711) Search is not triggered during surround action
* [VIM-1712](https://youtrack.jetbrains.com/issue/VIM-1712) Fix `Y` command for visual mode
* [VIM-1713](https://youtrack.jetbrains.com/issue/VIM-1713) Surround in visual mode put caret in correct position
* [VIM-1732](https://youtrack.jetbrains.com/issue/VIM-1732) Fix SO after enabling vim mode
* [VIM-1710](https://youtrack.jetbrains.com/issue/VIM-1710) Fix opening empty file with "relative number" enabled
* [VIM-1725](https://youtrack.jetbrains.com/issue/VIM-1725) Fix problems with Japanese language
* [VIM-1648](https://youtrack.jetbrains.com/issue/VIM-1648) Fix exception while substitute with conformation
* [VIM-1736](https://youtrack.jetbrains.com/issue/VIM-1736) Fix `` for ex panel
* [VIM-1739](https://youtrack.jetbrains.com/issue/VIM-1739) Fix full-width characters for ex pane

0.52, 2019-07-23
--------------

* Introduce [Select Mode](https://github.com/JetBrains/ideavim/wiki/Select-mode).


* Fixed `:only` command
* [VIM-1586](https://youtrack.jetbrains.com/issue/VIM-1586) Support `:shell` command
* [VIM-801](https://youtrack.jetbrains.com/issue/VIM-801) Support `:tabnext` and `:tabprevious` commands
* [VIM-1570](https://youtrack.jetbrains.com/issue/VIM-1570) Support `g<C-A>` and `g<C-X>` commands for visual mode
* [VIM-1119](https://youtrack.jetbrains.com/issue/VIM-1119) Fixed 'e' search offset
* [VIM-1587](https://youtrack.jetbrains.com/issue/VIM-1587) Fixed end-of-line multi line percent match in visual mode
* [VIM-1303](https://youtrack.jetbrains.com/issue/VIM-1303) Fixed "Changing color schemes doesn't change find input"
* [VIM-944](https://youtrack.jetbrains.com/issue/VIM-944) Fixed navigation with keypad arrows
* [VIM-1569](https://youtrack.jetbrains.com/issue/VIM-1569) Fixed surround plugin bug by `S<tag attr="attr">`
* [VIM-1012](https://youtrack.jetbrains.com/issue/VIM-1012) Fixed wrong tab selection after`:q`
* [VIM-1245](https://youtrack.jetbrains.com/issue/VIM-1245) Clear switcher list after`:q`
* [VIM-1425](https://youtrack.jetbrains.com/issue/VIM-1425) Correct `%` command
* [VIM-1521](https://youtrack.jetbrains.com/issue/VIM-1521) Support `commentary` extension
* [VIM-907](https://youtrack.jetbrains.com/issue/VIM-907) Fix `va(` command
* [VIM-1067](https://youtrack.jetbrains.com/issue/VIM-1067) Fix repeating of `A` command
* [VIM-1615](https://youtrack.jetbrains.com/issue/VIM-1615) Fix `set so=999` command and line "bouncing" with inlays
* [VIM-1630](https://youtrack.jetbrains.com/issue/VIM-1630) Support `:tabonly` command
* [VIM-607](https://youtrack.jetbrains.com/issue/VIM-607) Fix memory leaks
* [VIM-1546](https://youtrack.jetbrains.com/issue/VIM-1546) Storing TAB key as input
* [VIM-1231](https://youtrack.jetbrains.com/issue/VIM-1231) Get indent from PsiFile
* [VIM-1633](https://youtrack.jetbrains.com/issue/VIM-1633) Fixed sequential text object commands in visual mode
* [VIM-1105](https://youtrack.jetbrains.com/issue/VIM-1105) Added the `:command` command
* [VIM-1090](https://youtrack.jetbrains.com/issue/VIM-1090) Fixed tag motion with duplicate tags
* [VIM-1644](https://youtrack.jetbrains.com/issue/VIM-1644) Fixed repeat with visual mode
* Fixed invoking IDE actions instead of command line actions with same shortcuts
* [VIM-1550](https://youtrack.jetbrains.com/issue/VIM-1550) Fixed leaving command line mode on backspace
* Fix insert position of `<C-R>` in ex commands
* Command line editing caret shape and insert digraph/register feedback
* [VIM-1419](https://youtrack.jetbrains.com/issue/VIM-1419),
  [VIM-1493](https://youtrack.jetbrains.com/issue/VIM-1493) Correctly set focus when handling cmode mapping
* Fix incorrect handling of subsequent key strokes after ex command line loses focus
* [VIM-1240](https://youtrack.jetbrains.com/issue/VIM-1240) Improve UI of ex command line and output panel
* [VIM-1485](https://youtrack.jetbrains.com/issue/VIM-1485) Remove incorrect gap between ex command line label and text
* [VIM-1496](https://youtrack.jetbrains.com/issue/VIM-1496) Fix focus for Recent Files action
* [VIM-1275](https://youtrack.jetbrains.com/issue/VIM-1275) "Change In Brackets" for string
* [VIM-941](https://youtrack.jetbrains.com/issue/VIM-941) Fix tab for visual block mode
* [VIM-1002](https://youtrack.jetbrains.com/issue/VIM-1002) Fix dot command for tab
* [VIM-1426](https://youtrack.jetbrains.com/issue/VIM-1426) Correct `%` command
* [VIM-1655](https://youtrack.jetbrains.com/issue/VIM-1655) Deleted word should is not yanked with Ctrl-W in insert mode
* [VIM-1031](https://youtrack.jetbrains.com/issue/VIM-1031),
  [VIM-1389](https://youtrack.jetbrains.com/issue/VIM-1389),
  [VIM-1666](https://youtrack.jetbrains.com/issue/VIM-1666) Fix `<BS>` for digraphs
* [VIM-1628](https://youtrack.jetbrains.com/issue/VIM-1628) Fix dead keys for JBR11
* [VIM-1061](https://youtrack.jetbrains.com/issue/VIM-1061) Fix `^K` for digraphs
* [VIM-437](https://youtrack.jetbrains.com/issue/VIM-437) Support `keymode` option
* [VIM-274](https://youtrack.jetbrains.com/issue/VIM-274) Enter select mode for refactoring
* [VIM-510](https://youtrack.jetbrains.com/issue/VIM-510) Support `Extend Selection` for visual mode
* [VIM-606](https://youtrack.jetbrains.com/issue/VIM-606) Fix select text with mouse in insert mode
* [VIM-800](https://youtrack.jetbrains.com/issue/VIM-800) Fix surround with live template
* [VIM-1013](https://youtrack.jetbrains.com/issue/VIM-1013) Fix reformat code on selection
* [VIM-1214](https://youtrack.jetbrains.com/issue/VIM-1214) Fix insert text to empty row
* [VIM-1452](https://youtrack.jetbrains.com/issue/VIM-1452) Fix reselect visual block
* [VIM-1497](https://youtrack.jetbrains.com/issue/VIM-1497) Fix rename variable action
* [VIM-1541](https://youtrack.jetbrains.com/issue/VIM-1541) Fix visual block mode problems
* [VIM-1619](https://youtrack.jetbrains.com/issue/VIM-1619) Extract method for visual mode
* [VIM-1616](https://youtrack.jetbrains.com/issue/VIM-1616) `I` with multicaret works correctly
* [VIM-1631](https://youtrack.jetbrains.com/issue/VIM-1631) Fix visual block for tab character
* [VIM-1649](https://youtrack.jetbrains.com/issue/VIM-1649) Type variable for surround live template
* [VIM-1654](https://youtrack.jetbrains.com/issue/VIM-1654) Fix NPE while indent in visual block mode
* [VIM-1657](https://youtrack.jetbrains.com/issue/VIM-1657) Fix vim repeat in visual block
* [VIM-1659](https://youtrack.jetbrains.com/issue/VIM-1658) Fix selection on empty line
* [VIM-1473](https://youtrack.jetbrains.com/issue/VIM-1473) Yanked lines are not handled as block
  selection when clipboard is used
* [VIM-714](https://youtrack.jetbrains.com/issue/VIM-714) Fixed problems with caret position by vertical movement
* [VIM-635](https://youtrack.jetbrains.com/issue/VIM-635) Supported `gn` commands
* [VIM-1535](https://youtrack.jetbrains.com/issue/VIM-1535) Use same text attributes and highlight layer as IntelliJ's own Find command
* [VIM-1413](https://youtrack.jetbrains.com/issue/VIM-1413) Fix `smartcase` option being ignored in incremental search
* Fix incremental search not matching with trailing options, e.g. `/Foo/+1`
* Move the current line as well as scrolling during incremental search
* [VIM-128](https://youtrack.jetbrains.com/issue/VIM-128) Fix `:substitute` not respecting `ignorecase` and `smartcase` options
* Fix next/previous search commands not respecting `smartcase` override
* Search highlights are updated when `ignorecase`, `smartcase` and `hlsearch` options are updated, and when plugin is disabled
* Incremental search highlights all matches in file, not just first
* Added incremental search highlights for `:substitute` command
* Fix exception when trying to highlight last CR in file
* Improve behavior of `<BS>` in command line entry
* [VIM-1626](https://youtrack.jetbrains.com/issue/VIM-1626) Add `ideajoin` option
* [VIM-959](https://youtrack.jetbrains.com/issue/VIM-959) Add `ideamarks` option
* [VIM-608](https://youtrack.jetbrains.com/issue/VIM-608) Automatic upload files on explicit save
* [VIM-1548](https://youtrack.jetbrains.com/issue/VIM-1548) Respect editor settings about tabs and spaces
* [VIM-1682](https://youtrack.jetbrains.com/issue/VIM-1682) Fix backward search with OR
* [VIM-752](https://youtrack.jetbrains.com/issue/VIM-752) Enter finishes template in normal mode
* [VIM-1668](https://youtrack.jetbrains.com/issue/VIM-1668) Fix smart step into
* [VIM-1697](https://youtrack.jetbrains.com/issue/VIM-1697) Fix wrong search with tab characters
* [VIM-1700](https://youtrack.jetbrains.com/issue/VIM-1700) Fix wrong search with tab characters
* [VIM-1698](https://youtrack.jetbrains.com/issue/VIM-1698) Paste doesn't clear clipboard
* [VIM-1359](https://youtrack.jetbrains.com/issue/VIM-1359) Fix behavior of i_CTRL-W action

0.51, 2019-02-12
----------------

* [VIM-1558](https://youtrack.jetbrains.com/issue/VIM-1558) Fixed scrolling for code with block inlays in Rider 2018.3 
* [VIM-1187](https://youtrack.jetbrains.com/issue/VIM-1187) Improved performance of `set relativelinenumber` on large files
* [VIM-620](https://youtrack.jetbrains.com/issue/VIM-620) Fixed handling `<C-O>` and `<Esc>` in Insert and Replace modes
* [VIM-798](https://youtrack.jetbrains.com/issue/VIM-798) Allow arrow keys for window navigation commands


0.50, 2018-10-18
----------------

Moved "Vim Emulation" settings into "File | Settings | Vim Emulation". Support
for vim-multiple-cursors commands `<A-n>`, `<A-x>`, `<A-p>`, `g<A-n>` (put `set
multiple-cursors` into your ~/.ideavimrc to enable it). Support for running
Vim commands for multiple cursors. Various bug fixes.

* [VIM-634](https://youtrack.jetbrains.com/issue/VIM-634) Support for vim-multiple-cursors commands `<A-n>`, `<A-x>`, `<A-p>`, `g<A-n>`
* [VIM-780](https://youtrack.jetbrains.com/issue/VIM-780) Support for running Vim commands for multiple cursors
* [VIM-176](https://youtrack.jetbrains.com/issue/VIM-176) Fixed arrow key navigation in Run/Debug tool windows
* [VIM-339](https://youtrack.jetbrains.com/issue/VIM-339) Fixed `<Esc>` in diff windows
* [VIM-862](https://youtrack.jetbrains.com/issue/VIM-862) Allow `:action` to work in visual mode
* [VIM-1110](https://youtrack.jetbrains.com/issue/VIM-1110) Put the caret in correct place after `I` in visual block mode
* [VIM-1329](https://youtrack.jetbrains.com/issue/VIM-1329) Request focus reliably for Ex entry and output panels
* [VIM-1368](https://youtrack.jetbrains.com/issue/VIM-1368) Wait for focus reliably before running an `:action`
* [VIM-1379](https://youtrack.jetbrains.com/issue/VIM-1379) Fixed `I` for short lines in visual block mode
* [VIM-1380](https://youtrack.jetbrains.com/issue/VIM-1380) Fixed `cw` with count at the end of a word
* [VIM-1404](https://youtrack.jetbrains.com/issue/VIM-1404) Fixed the ability to use `:e#` when editor tabs are hidden
* [VIM-1431](https://youtrack.jetbrains.com/issue/VIM-1431) Fixed pasting text into the empty document
* [VIM-1427](https://youtrack.jetbrains.com/issue/VIM-1427) Added the support for count to the `it` and `at` motions
* [VIM-1287](https://youtrack.jetbrains.com/issue/VIM-1287) Fixed `i(` actions inside string literals
* [VIM-1317](https://youtrack.jetbrains.com/issue/VIM-1317) Don't run Undo/Redo inside write actions
* [VIM-1366](https://youtrack.jetbrains.com/issue/VIM-1366) Don't wrap a secondary event loop for `input()` into a write action
* [VIM-1274](https://youtrack.jetbrains.com/issue/VIM-1274) Correctly process escaping when `smartcase` is on


0.49, 2017-12-12
----------------

Enabled zero-latency typing for Vim emulation. Added support for `iskeyword` option. Various bug fixes.

* [VIM-1254](https://youtrack.jetbrains.com/issue/VIM-1254) Enable zero-latency typing for Vim emulation
* [VIM-1367](https://youtrack.jetbrains.com/issue/VIM-1367) Support `iskeyword` option
* [VIM-523](https://youtrack.jetbrains.com/issue/VIM-523) Fixed global mark remembering only the line number

0.48, 2017-01-15
----------------

A bugfix release.

Bug fixes:

* [VIM-1205](https://youtrack.jetbrains.com/issue/VIM-1205) Don't move key handling into separate event for raw handlers
* [VIM-1216](https://youtrack.jetbrains.com/issue/VIM-1216) Fixed `.` resetting the last find movement while repeating change that also uses movement

Features:

* Support for zero-latency rendering


0.47, 2016-10-19
----------------

A bugfix release.

Bug fixes:

* VIM-1098 Don't start visual selection when mouse click was actually drag over single character
* VIM-1190 Fixed exception "Write access is allowed from write-safe contexts only"


0.46, 2016-07-07
----------------

Added `incsearch` option for incremental search. Added support for `it` and
`at` tag block selection. Added `vim-surround` commands `ys`, `cs`, `ds`,
`S`. Various bug fixes.

Features:

* VIM-769 Added `vim-surround` commands `ys`, `cs`, `ds`, `S`
* VIM-264 Added tag block selection
* VIM-271 Added `incsearch` option for showing search results while typing
* VIM-217 Added support for `={motion}` formatting command

Bug fixes:

* VIM-796 Fixed focus issues with `:action` command
* VIM-581 Fixed use of special registers `0`-`9` and `-` in delete commands
* VIM-965 Fixed exception in `[m` in some file types
* VIM-564 Fixed `g_` move to go to the current line
* VIM-964 Fixed marks behavior when the whole line got deleted
* VIM-259 Move caret to the line beginning after `==`
* VIM-246 Fixed `{count}==` formatting
* VIM-287 Fixed insert new line before and after folds
* VIM-139 Focus on current search and use modal confirmation for `:s///gc`
* VIM-843 Don't highlight search results after restart
* VIM-1126 Fixed warning about modifying shortcuts of global actions for 2016.2


0.44, 2015-11-02
----------------

A bugfix release.


* VIM-1040 Fixed typing keys in completion menus and typing with the
  plugin disabled


0.43, 2015-11-02
----------------

A bugfix release.

* VIM-1039 Fixed running the plugin with Java 6


0.42, 2015-11-01
----------------

This release is compatible with IntelliJ 15+ and other IDEs based on the
IntelliJ platform branch 143+.

* VIM-970 Fixed move commands in read-only files


0.41, 2015-06-10
----------------

A bugfix release.

* VIM-957 Fixed plugin version 0.40 is not compatible with IDEs other than
  IntelliJ


0.40, 2015-06-09
----------------

Added support for `mapleader`. Support comments in `%` brace matching. Various
bug fixes.

Features:

* VIM-650 Added support for `mapleader`
* VIM-932 Support comments in `%` brace matching

Bug fixes:

* VIM-586 Invoke Vim shortcuts handler later to restore the sequence of input
  events
* VIM-838 `J` shouldn't add whitespace if there is a trailing space
* VIM-855 Fixed regexp character class problem
* VIM-210 Fix focus issues with the Ex panel and splits
* VIM-575 Don't change cursor position of other splits in visual mode
* VIM-864 Fixed visual marks getting changed during visual substitute
* VIM-856 Fixed regex look-behind problem
* VIM-868 Allow count on `gt` and `gT`
* VIM-700 Remapping `0` should still allow it to be entered in command count
* VIM-781 Fixed expanding visual block selection past empty lines
* VIM-845 Fixed `c` and `x` functionality for visual block selections
* VIM-930 Fixed editor focus issues after closing Ex entry box on Oracle Java 6


0.39, 2014-12-03
----------------

A bugfix release.

Bug fixes:

* VIM-848 Show line numbers if they are enabled in the settings and there is
  no `set number`
* VIM-702 Fix infinite loop on `s/$/\r/g`
* EA-63022 Don't update line numbers in the caret movement event listener


0.38, 2014-12-01
----------------

Added support for `number` and `relativenumber` options, `clipboard=unnamed`
option. Added `:action` and `:actionlist` commands for executing arbitrary
IDE actions. Various bug fixes.

Features:

* VIM-476 Added support for `clipboard=unnamed` option
* VIM-410 Added support for `relativenumber` option
* VIM-483 Added support for `number` option
* VIM-652 Added `:action` and `:actionlist` commands for executing arbitrary
  IDE actions

Bug fixes:

* VIM-818 Enable key repeat on Mac OS X every time it gets reset by the OS
* VIM-624 Deselect visual selection range on opening the Ex entry field
* VIM-511 Fixed editing offset after `<BS>` for `.` command
* VIM-792 Fixed line-wise and block-wise paste commands for `*` and `+`
  registers
* VIM-501 Fixed off-by-1 error in visual block-wise selection
* VIM-613 Fixed repeat after `d$`
* VIM-705 Fixed repeated multiline indent
* VIM-567 Fixed `:!` to allow running non-filter commands
* VIM-536 Fixed `cc` on the second-to-last line
* VIM-515 Fixed `cW` command detecting end-of-word incorrectly
* VIM-794 Fixed NCDFE related to 'number' in IDEs other than IntelliJ
* VIM-771 Fix semicolon repeat for 'till char' motion
* VIM-723 Fix pasting to an empty line


0.37, 2014-10-15
----------------

A bugfix release.

Bug fixes:

* VIM-784 Fixed visual line selection where the start of the selection range
  was greater than its end
* VIM-407 Fixed `>>` to work if a line contains only one character


0.36, 2014-10-14
----------------

Added support for common window splitting and navigation commands. Various bug
fixes.

Features:

* VIM-171 Window `<C-W>` commands: split, close, next/previous windows,
  left/right/up/down windows
* VIM-265 Window `:split` and `:vsplit` commands

Bug fixes:

* VIM-632 Restored visual block mode that was broken due to multiple carets support
* VIM-770 Close the current tab on `:quit` instead of all tabs with the current
  file
* VIM-569 Fixed `<C-W>` when the caret is at the end of a line


0.35, 2014-05-15
----------------

The `~/.vimrc` initialization file is no longer read by default, use
`~/.ideavimrc` instead.

Features:

* VIM-690 Read initialization commands only from `~/.ideavimrc`

Bug fixes:

* VIM-676 Handle control characters in `.ideavimrc` as pressed, not typed
  keystrokes
* VIM-679 Parse characters less than U+0020 as `<C-$CHAR>`
* VIM-683 Allow `<C-PageUp>`/`<C-PagDown>` to be used outside of Vim emulation
* VIM-646 Don't update the visual selection if a command moves the caret and exits
  the visual mode
* VIM-213 Use `'<` and `'>` marks for saving and restoring the last visual
  selection


0.34, 2014-04-29
----------------

A bugfix release.

Bug fixes:

* VIM-674 Don't handle `<Tab>` in Insert mode in Vim emulation
* VIM-672 Ignore mappings that contain `<Plug>` and `<SID>`
* VIM-670 First character of a recursive mapping shouldn't be mapped again
* VIM-666 Support `<Bar>` in Vim key notation
* VIM-666 Ignore characters after `|` in `:map` commands
* VIM-667 Ignore potentially nested lines of .vimrc based on leading whitespace


0.33, 2014-04-28
----------------

Added support for `:map` key mapping commands. New keyboard shortcuts handler
that doesn't require a separate keymap for Vim emulation. Added support for
`:source` and `:sort` commands.

Features:

* VIM-288 Support for `:map` key mapping commands
* VIM-543 Allow granular enable/disable of Vim shortcut keys
* VIM-643 Support for `:source` command
* VIM-439 Support for `:sort` command

Bug fixes:

* VIM-528 Search and replace with grouping no longer works
* VIM-281 Don't disable global reformat code action for Vim emulation


0.32, 2013-11-15
----------------

Fixed API compatibility with IntelliJ platform builds 132.1052+.


0.31, 2013-11-12
----------------

A bugfix release.

Bug fixes:

* VIM-582 Fixed line comment and reformat commands with no visual selection


0.30, 2013-11-11
----------------

Added support for a separate `.ideavimrc` config file. Fixed long-standing
issues with merged undo/redo commands and `<Esc>` during code completion.
Various bug fixes.

Features:

* VIM-425 Read config from .ideavimrc if available

Bug fixes:

* VIM-98 Invoke actions in separate commands for better undo/redo
* VIM-193 Launch Vim action handler for `<Esc>` in completion windows
* VIM-440 Fixed `:e` open file dialog
* VIM-550 `:put` creates a new line
* VIM-551 Argument of `:put` is optional
* Fixed several reported exceptions


0.29, 2013-05-15
----------------

A bugfix release.

Bug fixes:

* VIM-482 Fixed repeat buffer limits
* VIM-91 Enable normal `<Enter>` handling for one-line editors
* VIM-121 Don't move cursor while scrolling


0.28, 2013-04-06
----------------

A bugfix release.

Bug fixes:

* VIM-478 Fixed reconfigure Vim keymap for user-defined base keymaps
* VIM-479 Don't try to activate insert mode for diff view


0.27, 2013-04-03
----------------

New Vim keymap generator creates better keymaps, especially for Mac OS X.
Restart after reconfiguring the keymap is no longer required.

Features:

* VIM-92 Better Vim keymaps for Mac OS X
* VIM-286 Ask if the plugin should enable repeating keys in Mac OS X

Bug fixes:

* VIM-42 Fixed code completion for the `.` command
* VIM-421 Fixed `cw` on the last character in line
* VIM-419 Fixed resetting cursor position after 'gt' and 'gT'
* VIM-233 Fixed code completion for edits in visual block mode
* VIM-404 Fixed `O` command at the first line
* VIM-472 Fixed right selection in visual mode to be one char more
* Fixed command window font size to match editor font size


0.26, 2012-12-26
----------------

Added support for paste in the command mode: from a register using `<C-R>`,
from the clipboard using `<S-Insert>` or `<M-V>`. Added support for the last
change position mark (the dot `.` mark). New shortcuts for Go to declaration
`<C-]>` and Navigate back `<C-T>`. Various bug fixes.

Features:

* VIM-262 Support for paste from register in command mode
* VIM-214 Key bindings for paste into command line
* VIM-43 Added support for the last change position mark
* VIM-177 Added `<C-]>` and `<C-T>` to the keymap

Bug fixes:

* VIM-302 Fixed tab switching order for `gt` and `gT`


0.25, 2012-12-19
----------------

A bugfix release.

* VIM-400 Fixed saving characters with key modifiers in plugin settings
* VIM-319 Fixed saving plugin settings when registers contain the null
  character


0.24, 2012-12-03
----------------

Added Vim string object selection motions (see help topics `v_i"`, `v_a"`).
Various bug fixes.

Features:

* VIM-132 String object selection motions

Bug fixes:

* VIM-393 Fixed restoring editor state after invalid arguments with pending
  operators
* VIM-244 Fixed `dl` for the last character in line
* VIM-394 Fixed `daw` for first and last words with no space at the right/left
  in the current line
* VIM-296 Fixed `cc` at the last line
* VIM-392 Fixed change action at the last char in word for non-word motions
* VIM-390 Fixed paste a single line at the last line
* VIM-325 External web help for Vim
* VIM-300 Fixed `cw` at the last char of a word before next word without
  whitespace
* VIM-200 Fixed `cw` at the last character of a word
* VIM-105 Fixed `w` motion for the last word in line
* VIM-223 Fixed AE: BaseCodeCompletionAction.actionPerformed
* VIM-331 Fixed word bounds in `w` motion for extended latin letters
* Fixed `w` motion to stop at empty line
* VIM-312 Fixed range and caret position after `dw` on the last single-word
  line, `w` command argument for the last word in file
* Fixed `w` motion at the last word
* VIM-85 Bug fix for gi behavior
* Always move cursor at the beginning of the deleted range
* VIM-275 Fixed edge cases for `i{` motion
* VIM-314 Made `i{` motion characterwise, not linewise/characterwise in visual
  mode
* VIM-326 Fixed IOOBE in delete inner block motion inside string literals
* VIM-157 Fixed regression in moving the cursor after `~`


0.23.115, 2012-11-14
--------------------

A bugfix release.

* VIM-318 Fixed executing editor commands for editors not bound to a project
* VIM-321 Fixed IOOBE in delete empty range
* VIM-112 Delete a single previous word with <C-W> in insert mode, not all inserted words


0.23.111, 2012-11-12
--------------------

A bugfix release.

* Register action for 'iW' selection
* Vim compatible regexp substitutions for '\n' and '\r'
* Index of supported commands covered with tests
* VIM-276 T and F motions are exclusive, not inclusive
* VIM-289 Fixed regexp substitute when the substitution contained newlines
* VIM-185 Fixed NPE in KeyHandler.handleKey()
* VIM-226 Added tests for the bug fixed together with VIM-146
* VIM-146 Fixed handling of '$' in search and substitute commands
* VIM-198 Fixed indexing bug in offset normalization
* VIM-311 Test for single command sub-mode of insert mode
* EA-33193 Fixed access to context data from different Swing events
* Fixed command handling when motion expected, but another type of argument
  found


0.23.93, 2012-03-21
-------------------

A bugfix release. Vim.xml was fixed to use Command+C, Command+V on Mac OS.
Unfortunately you need to update Vim.xml manually this time.

...


Previous Releases
-----------------

... from 0.8.4
Bug Fixes
- The Escape key is passed up to IDEA if not used by VIM first. This fix solves
  minor issues such as not being able to clear highlighted text using the
  Ctrl-Shift-F7, for example.
- :quit command now works in all forms (e.g. :q, :qu, :qui, :quit).
- Performing a change command while in visual mode now properly terminates
  visual mode.
- Fixed internal error caused when trying to use visual mode with IDEA's
  "column mode".

0.8.3 from 0.8.2
Bug Fixes
- After a fresh install it is possible to get a NPE when loading the first
  project. Now fixed.
- Fixed :class and :find. These were also broken by the focus fix in 0.7.2.
- Fixed * and # commands on one letter words.
- Fixed * and # commands on last word of line with trailing punctuation.
- Fixed b and e commands when trying to move to first or last word of file and
  the first or last character of the file was punctuation.
- Fixed NullPointerException appearing in system log when viewing a .form file.
- Fixed extraneous characters getting added to a register during recording.
- Use file type's indent size instead of tab size for indenting text.
- Restore caret if plugin is disabled.
- Fixed ability to delete blank line at end of files.

0.8.2 from 0.8.1
- Fixed typo in plugin.xml for new 'since-build'

0.8.1 from 0.8.0
Bug Fixes
- Updated to show up in the plugin list for Pallada.
- Fixed NullPointerException when using the :qall, :q, or :wq commands.
- Fixed the :edit, :next, :previous, :argument, :first, and :last commands.
  These were broken by the focus fix in 0.7.2.

0.8.0 from 0.7.3
New Features
- Support for Pallada (IDEA 4.5)
- Support for the hlsearch option and the :nohlsearch command. Now when a
  search is done, all matching text is highlighted. The highlight attributes
  are based on the "General|Search result" color setting. (Not for 4.0.x)
- Support for aw, aW, iw, and iW while in visual mode or as arguments to the
  y, gu, gU, g~, c, d, and ! commands.
  
Bug Fixes
- e and E while on the last word of a file didn't work.
- b and B while on the first word of a file didn't work.

0.7.3 from 0.7.2

New Features
- Added support for digraphs. Currently you can use Ctrl-k {char1}{char2} to
  enter a special character. This works while in Insert/Replace mode, as an
  argument to the r, F, T, f, and t commands, or while entering an ex command.
  The :digraphs command has been added to display the currently supported
  digraphs. Certain special cases of the Ctrl-K {char1}{char2} sequence are not
  supported. This supports all two character digraphs as listed in RFC1345.
  This amounts to 1,338 digraphs! Ensure the file encoding can handle the
  characters you enter.
- Added support for the 'digraph' option. If set, digraphs may be entered using
  {char1} [BackSpace] {char2}. This works in Insert/Replace mode or while
  entering an ex command.
- Added support for Ctrl-v {digits}. This works while in Insert/Replace mode,
  as an argument to the r, F, T, f, and t commands, or while entering an ex
  command.
- A new Vim.xml keymap needs to be installed or Ctrl-K needs to be removed
  from all source control menus.

Bug Fixes
- Fixed backspace in ex entry. Deleting the first character was closing the ex
  entry window.

0.7.2 from 0.7.1

Bug Fixes
- Fixed c<motion and d<motion> exception if the motion was invalid.
- Fixed focus problem with : and / and ? commands.
- Fixed word motion on strings such as 1/2/3.
- Fixed <count><word motion> which didn't always match doing <count>
  independent <word motion> commands.
- Fixed <count>cw on strings such as 1/2/3.
- Fixed <count>dw which could delete <count> lines instead.
- The results of the :registers, :marks, and :set commands are now displayed
  properly. This "more" window hadn't been working for a while.
- Fixed cursor position when issuing the O command on the first line.
- The confirmation dialog used with the :s//c command now has a default button
  and mnemonics for all buttons.
- A space is now properly allowed between the range and the command in a :
  command such as :1,2 co 4

0.7.1 from 0.7.0

Bug Fixes
- Opening a non-text file resulted in some exceptions. This could happen when
  opening an image with the ImageViewer plugin.
- Better handling of trying to edit a VCS controlled read-only file.
- Properly handle multiple < or > in the :> and :< commands.
- Fixed an exception and assertions when reopening a project.
- Fixed cursor position problem when issuing a c<motion> command that changed
  text up to the end of line.
- Using the C command on an empty file caused an error.
- Changing the last word on the last line leaves cursor correctly.

0.7.0 from 0.6.5

New Features
- Highlighting lines of code by clicking and/or dragging in the line number area
  now leaves you in visual line mode.
- Undoing all changes in a file now correctly marks the file as unchanged if it
  hasn't been saved in the meantime.
- All the :write commands (:w :wn :wN :wq) save just the one file now. To save
  all files use the :wall command.
- Enhanced the :e command. Support for :e# and :e <filename> have been added.
  :e# selects the previous tab. :e <filename> will search the entire project
  and open the first matching file. Relative paths are supported too. :e with
  no argument will still bring up the File Open dialog.
- Added support for the gP and gp commands.
- Added support for the z+ and z^ commands.
- Added :class command to bring up "Go To Class" dialog. If followed by a
  classname, open the corresponding Java file. Not in VIM.
- Added :symbol command to bring up "Go To Symbol" dialog. Not in VIM.
- Editors use block cursor for command mode and a vertical bar for
  insert/replace mode.
- Better support for split view editing.
- Text selection is reflected in all editors of a file.

Bug Fixes
- Under certain conditions, highlighting text left you in multiple layers of
  visual mode. This is now fixed.
- The gv command resulted in too much text being selected in many cases.
- The gv command now properly restores the cursor position.
- Fixed exception caused by using the :undo and :redo commands.
- Re-enabled all the :write related commands. Hopefully the deadlock has been
  solved.
- Fixed error referencing unknown class FileCloseAction.
- Fixed several exceptions related to edit fields in dialog boxes.
- Fixed some exceptions related to the undo manager.
- Status was not always show proper mode.
- r<Enter> now works as expected.
- Toggling between insert and replace mode wasn't working properly.
- The cursor wasn't always restored correctly after an undo.
- Yanking the last line and then putting it elsewhere sometimes resulted in the
  old and new line not having a newline between them.
- Repeating of o and O commands, including with count, works properly now.
- dw, dW, and d[w now act like d$ if deleting the last word of a line.
- cW now properly behaves like cE.

0.6.5 from 0.6.4

Support for Aurora build #1050 and higher.

0.6.4 from 0.6.3

Support for Aurora build #1035 and higher.

New Features
- Added support for {, }, '{, '}, `{, and `} commands (previous and next
  paragraph).

Bug Fixes
- Fixed cursor movement problem introduced in version 0.6.3.
- Fixed issues with visual ranges introduced in version 0.6.3.
- Fixed bug with cursor placement when clicking on a blank line.
- Triple clicking text now properly puts the user in Visual Line mode.

Clean-up
- Removed use of newly deprecated methods in Open API.
- Some basic code cleanup.

0.6.3 from 0.6.2

Support for Aurora build #992 and higher.

0.6.2 from 0.6.1

Repackaged to install as a zip directory instead of a jar file. This provides
better support for the new plugins repository.

0.6.1 from 0.6.0

Support for Aurora build #963.

Changed Features
- Removed the VIM icon from the toolbar. This was only used to display status
  messages. Now display status messages in the IDEA status bar.

Bug Unfixes
- Removed, again, support for :w related ex commands

0.6.0 from 0.4.1

Support for Aurora build #939. It may work with slightly older versions too.
This version will not work with IDEA 3.x. For IDEA 3.x you must use IdeaVim
versions prior to 0.6.0.

Bug Fixes
- Put back file saving for the :w related ex commands

0.4.1 from 0.4.0

Bug Fixes
- Fixed vim.xml for Windows. This file was getting deleted by Idea on Windows
  due to a mismatch in the case of the name.
- The O command now properly indents the new line.
- Fixed Null Pointer Exception when editing file templates.

Temporary Work-arounds
- Disabled the saving of files when using any of the :w related commands until
  a fix can be found for the dead-lock bug. ZZ and ZQ still save files.

0.4.0 from 0.3.2

New Features
- A VIM tool window has been added. This is used to show the current mode (if
  :set showmode is set) and any messages normally shown on the last line in
  VIM. To make this useful you should do the following:
  - Show the VIM tool window.
  - Make the VIM tool window docked.
  - Turn off auto-hide for the VIM tool window.
  - Shrink the tool window so just the window title is visible.
  - Do not move the VIM tool window to the left or right - leave on the bottom
    or top.
- Various error messages are now displayed in the new status bar.
- Added support for : register
- Added support for / register

New Commands
- Added support for q{register} command - macro recording.
- Added support for @{register} and @@ commands - playback register contents.
- Added support for :@{register}, :@@, and :@: - run register as command or
  repeat last :@ command.

New :set Options
- showmode is now supported.

Bug Fixes
- Hitting escape while entering a search string resulted in a search for the
  previous search string instead of doing nothing.
- The :registers command didn't display trailing newlines in a register
- Fixed focus problem if user hits escape in the find dialog after entering
  the :find command.
- All the search and substitute commands acted strangely if the text contained
  real tab characters. Tabs are now properly handled.
- gd and gD weren't working in read-only files.

0.3.2 from 0.3.1

New Commands
- Added support for {visual}!{filter}, !{motion}{filter}, and !!{filter}.
- Added support for [p, ]p, [P, ]P - put text but do not autoindent. Note -
  This plugin's support for putting text with or without proper indenting is
  reversed from VIM.

New Features
- Entering a count before the v or V command to start Visual mode is now
  supported.
- Repeating Visual commands is now supported.

Bug Fixes
- Now properly handle :0, :1, and :<negative> commands
- More problems with Visual mode - none of the visual change commands left
  you in insert mode after deleting the text and undo wouldn't put the text
  back.
- Fixed the handling of undo/redo with regard to text added to a file while not
  in insert mode. This can happen with the use of the Generate... menu for
  adding constructors, getters, etc. This also can happen when IDEA adds an
  import statement automatically. This also allows you to undo import
  optimizations!
- Doing a cw or cW when the cursor is already on the end of a word/WORD now
  correctly deletes only the last character of the word/WORD and not the next
  word too.
- gd and gD now properly save the jump location before moving to the
  declaration.
- z<Enter>, z-, and z. now move the cursor to the start of the line.
- Visual mode is now exited after issuing the = or gq command.
- The commands c% and d% where not removing the closing match as expected.
- The d{motion} command now becomes linewise when the motion covers more than
  one line and there is just whitespace before the start and after the end.
- Renamed some source files to avoid problems on case insensitive platforms.
- Entering a count for the . (repeat) command sometimes results in an internal
  error.
- Repeating an R command resulted in the text being inserted instead of
  overwriting old text. This now works properly.
- Issuing a p command when on the last line of a file pasted the line before
  the last line instead of after the last line.

0.3.1 from 0.3.0

New Commands
- Added support for [w, ]w, [b, and ]b. These move the cursor forward to start
  of next camel word, forward to end of next camel word, backward to start of
  previous camel word, and backward to end of previous camel word respectively.
  These are NOT in VIM but are very useful when working with mixed case method
  and variable names. Like their normal w, e, b, and ge counterparts, these may
  be used as operators for the c and d commands.
- :qall, :quitall, :wqall, and :xall - closes all editors.
- :wall - same as :write - save all files.
- :xit and :exit - same :wq - save files and close current editor.
- :close and :hide - same as :quit - close current editor.
- :only - Close all editors except the current editor.
- :display - same as :registers - display register contents.
- :undo - same as u - undo last change.
- :redo - same as Ctrl-R - redo last undone change.
- :wnext - save files and move to next editor.
- :wNext and :wprevious - save files and move to previous editor.

Changed Commands
- ZZ, ZQ, :q and :wq will not exit IDEA anymore

Bug Fixes
- Visual mode became unusable without doing a Ctrl-\ Ctrl-n command after each
  visual mode command. This unstable mode also caused undo to work improperly
  resulting in garbled text.
- Ex commands that take a count instead of a range (:next, :Next, etc.) were
  getting the current line number as the count instead of one.
- The == command can now be repeated with the . command.
- Backslashes (\) in the replace text of a :substitute command was not being
  handled correctly in all cases.

0.3.0 from 0.2.0

Redone Commands
- / and ? and all their variants are now supported properly, including offsets.
- n and N are now supported properly
- Ex command ranges now fully support searches
- :substitute command now fully supports all Vim style search and replaces

New Commands
- Added support for :set. Only a small number of options are supported.
- Load .vimrc or _vimrc from user's home directory. Only set commands are
  honored.
- Added support for *, #, g*, and g# - search for word under cursor.

Supported Set Options
- gdefault - Indicates of the global flag in on by default for the :substitute
  command.
- ignorecase - The default case sensitivity for searchs and substitutes.
- matchpairs - Specify the character pairs used by the % command
- more - Indicates whether 'more' is used or not for display windows
- scroll - Specifies the number of lines scrolled by the Ctrl-D and Ctrl-U
  commands. Set to zero for half page.
- selection - Specifies how the cursor can be moved at the ends of lines in
  Visual mode. It also controls whether the Visual mode selection is inclusive
  or exclusive.
- smartcase - Overrides ignorecase if set and pattern has uppercase characters.
- undolevels - Set to 0 for Vi style undo (1 level). Set to other number to
  specify how many levels of undo are supported per editor.
- visualbell - controls whether the plugin beeps or not.
- wrapscan - Determines is searches wrap around the start or end of the file.

0.2.0 from 0.1.1

New Commands
- Added support for Ctrl-U while in insert mode
- Added support for 0 Ctrl-D to remove all indent in current line
- Added support for F1 while in insert - exit insert and bring up help topics
- Added support for F1 while in normal mode - bring up help topics
- Added support for :promptfind - Brings up Search Dialog
- Added support for :promptrepl - Brings up Search/Replace Dialog
- Added real support for :substitute - search and replace
- Added support for :& - search and replace
- Added support for :~ - search and replace
- Added support for @: - repeat last Ex command
- Added support for :{range}!{filter} [arg] command - filter text
- Added support for :{range}!! command - repeat filter
- Added support for :marks - display marks
- Added support for :registers - display registers

Bug Fixes
- Using the '%' range in Ex commands ignored the first line
- Selecting a register for a command after using Ctrl-O in insert mode sent
  the user back to insert mode before they could enter the command.
- Many commands caused exceptions if the current file is empty.
- Let mouse clicks move cursor to end-of-line while in Insert or Replace mode.

Keymappings - Install new plugin.xml or manually add keymapping
- F1

0.1.1 from 0.1.0
- Fixed text of README file (NAME and VERSION weren't properly substituted)
- Fixed issue with pressing Enter when entering an Ex command (no more beep or
  IDEA error message)
- Fixed issue with pressing Backspace in the Ex command entry window when there
  are no characters. The beep is gone and the command entry is properly exited.
