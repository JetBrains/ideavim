# API Reference

## VimApi

The `VimApi` class is the main entry point for interacting with the Vim editor. It provides access to various functionalities like variable management, window operations, and text manipulation.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `mode` | `Mode` | The current mode of the Vim editor. |
| `tabCount` | `Int` | Gets the number of tabs in the current window. |
| `currentTabIndex` | `Int?` | The index of the current tab or null if there is no tab selected or no tabs are open. |

### Methods

#### Variable Management

| Method | Description                                                                                             | Return Value |
|--------|---------------------------------------------------------------------------------------------------------|--------------|
| `getVariable<T : Any>(name: String): T?` | Gets a variable with the specified name and scope.                                                      | The variable value or null if not found. |
| `setVariable<T : Any>(name: String, value: T)` | Sets a variable with the specified name and value. In Vim, this is equivalent to `let varname = value`. | None |

#### Operator Functions

| Method                                                                         | Description | Return Value |
|--------------------------------------------------------------------------------|-------------|--------------|
| `exportOperatorFunction(name: String, function: suspend VimApi.() -> Boolean)` | Exports a function as an operator function. | None |
| `setOperatorFunction(name: String)`                                            | Sets the operator function to use. | None |
| `normal(command: String)`                                                      | Executes a normal mode command. | None |

#### Editor Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `editor<T>(block: EditorScope.() -> T): T` | Executes a block of code in the context of the current editor. | The result of the block. |
| `forEachEditor<T>(block: EditorScope.() -> T): List<T>` | Executes a block of code for each open editor. | A list of results from each editor. |

#### Scope Access

| Method | Description | Return Value |
|--------|-------------|--------------|
| `mappings(block: MappingScope.() -> Unit)` | Executes a block of code in the mapping scope. | None |
| `listeners(block: ListenersScope.() -> Unit)` | Executes a block of code in the listeners scope. | None |
| `outputPanel(block: OutputPanelScope.() -> Unit)` | Executes a block of code in the output panel scope. | None |
| `modalInput(): ModalInput` | Gets the modal input scope. | The modal input scope. |
| `commandLine(block: CommandLineScope.() -> Unit)` | Executes a block of code in the command line scope. | None |
| `option<T>(block: OptionScope.() -> T): T` | Executes a block of code in the option scope. | The result of the block execution. |
| `digraph(block: DigraphScope.() -> Unit)` | Executes a block of code in the digraph scope. | None |

#### Tab Management

| Method | Description | Return Value |
|--------|-------------|--------------|
| `removeTabAt(indexToDelete: Int, indexToSelect: Int)` | Removes a tab at the specified index and selects another tab. | None |
| `moveCurrentTabToIndex(index: Int)` | Moves the current tab to the specified index. | None |
| `closeAllExceptCurrentTab()` | Closes all tabs except the current one. | None |

#### Pattern Matching

| Method | Description | Return Value |
|--------|-------------|--------------|
| `matches(pattern: String, text: String, ignoreCase: Boolean = false): Boolean` | Checks if a pattern matches a text. | True if the pattern matches the text, false otherwise. |
| `getAllMatches(text: String, pattern: String): List<Pair<Int, Int>>` | Finds all matches of a pattern in a text. | A list of pairs representing the start and end offsets of each match. |

#### Window Management

| Method | Description | Return Value |
|--------|-------------|--------------|
| `selectNextWindow()` | Selects the next window in the editor. | None |
| `selectPreviousWindow()` | Selects the previous window in the editor. | None |
| `selectWindow(index: Int)` | Selects a window by its index. | None |
| `splitWindowVertically(filename: String? = null)` | Splits the current window vertically and optionally opens a file in the new window. | None |
| `splitWindowHorizontally(filename: String? = null)` | Splits the current window horizontally and optionally opens a file in the new window. | None |
| `closeAllExceptCurrentWindow()` | Closes all windows except the current one. | None |
| `closeCurrentWindow()` | Closes the current window. | None |
| `closeAllWindows()` | Closes all windows in the editor. | None |

#### Script Execution

| Method                                                     | Description | Return Value |
|------------------------------------------------------------|-------------|--------------|
| `execute(script: String): Boolean`                         | Parses and executes the given Vimscript string. It can be used to execute ex commands, such as `:set`, `:map`, etc. | The result of the execution, which can be Success or Error. |
| `command(command: String, block: VimApi.(String) -> Unit)` | Defines a new command. | None |

#### Data Storage

| Method | Description | Return Value |
|--------|-------------|--------------|
| `getDataFromWindow<T>(key: String): T?` | Gets keyed data from a Vim window. | The data associated with the key, or null if no data is found. |
| `putDataToWindow<T>(key: String, data: T)` | Stores keyed user data in a Vim window. | None |
| `getDataFromBuffer<T>(key: String): T?` | Gets data from buffer. Vim stores there buffer scoped (`b:`) variables and local options. | The data associated with the key, or null if no data is found. |
| `putDataToBuffer<T>(key: String, data: T)` | Puts data to buffer. Vim stores there buffer scoped (`b:`) variables and local options. | None |
| `getDataFromTab<T>(key: String): T?` | Gets data from tab (group of windows). Vim stores there tab page scoped (`t:`) variables. | The data associated with the key, or null if no data is found. |
| `putDataToTab<T>(key: String, data: T)` | Puts data to tab (group of windows). Vim stores there tab page scoped (`t:`) variables. | None |
| `getOrPutWindowData<T>(key: String, provider: () -> T): T` | Gets data from window or puts it if it doesn't exist. | The existing data or the newly created data. |
| `getOrPutBufferData<T>(key: String, provider: () -> T): T` | Gets data from buffer or puts it if it doesn't exist. | The existing data or the newly created data. |
| `getOrPutTabData<T>(key: String, provider: () -> T): T` | Gets data from tab or puts it if it doesn't exist. | The existing data or the newly created data. |

#### File Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `saveFile()` | Saves the current file. In Vim, this is equivalent to the `:w` command. | None |
| `closeFile()` | Closes the current file. In Vim, this is equivalent to the `:q` command. | None |

#### Text Navigation

| Method | Description | Return Value |
|--------|-------------|--------------|
| `getNextCamelStartOffset(chars: CharSequence, startIndex: Int, count: Int = 1): Int?` | Finds the start offset of the next word in camel case or snake case text. | The offset of the next word start, or null if not found. |
| `getPreviousCamelStartOffset(chars: CharSequence, endIndex: Int, count: Int = 1): Int?` | Finds the start offset of the previous word in camel case or snake case text. | The offset of the previous word start, or null if not found. |
| `getNextCamelEndOffset(chars: CharSequence, startIndex: Int, count: Int = 1): Int?` | Finds the end offset of the next word in camel case or snake case text. | The offset of the next word end, or null if not found. |
| `getPreviousCamelEndOffset(chars: CharSequence, endIndex: Int, count: Int = 1): Int?` | Finds the end offset of the previous word in camel case or snake case text. | The offset of the previous word end, or null if not found. |

## EditorScope

The `EditorScope` class provides access to read and write operations on the editor. It serves as a bridge between the read-only and transaction-based operations.

### Methods

| Method | Description | Return Value |
|--------|-------------|--------------|
| `read<T>(block: suspend Read.() -> T): Deferred<T>` | Executes a block of code in the context of read operations. This allows for reading the editor state without modifying it. | A Deferred result of the block execution. |
| `change(block: suspend Transaction.() -> Unit): Job` | Executes a block of code in the context of transaction operations. This allows for modifying the editor state. | A Job representing the asynchronous operation. |

## ReadScope

The `ReadScope` interface provides read-only access to the editor content and state. It includes methods for navigating text, working with carets, and querying editor information.

### Properties

| Property           | Type              | Description                                     |
|--------------------|-------------------|-------------------------------------------------|
| `textLength`       | `Long`            | The total length of the text in the editor.     |
| `text`             | `CharSequence`    | The entire text content of the editor.          |
| `lineCount`        | `Int`             | The number of lines in the editor.              |
| `filePath`         | `Path`            | File path of the editor.                        |
| `caretData`        | `List<CaretData>` | Information about all carets in the editor.     |
| `caretIds`         | `List<CaretId>`   | The IDs of all carets in the editor.            |
| `globalMarks`      | `Set<Mark>`       | All global marks defined in the editor.         |
| `jumps`            | `List<Jump>`      | All jumps in the jump list.                     |
| `currentJumpIndex` | `Int`             | Index of the current position in the jump list. |

### Methods

#### Caret Operations

| Method                                                           | Description | Return Value                       |
|------------------------------------------------------------------|-------------|------------------------------------|
| `forEachCaret<T>(block: suspend CaretRead.() -> T): List<T>`     | Executes a block of code for each caret in the editor. | A list of results from each caret. |
| `with<T>(caretId: CaretId, block: suspend CaretRead.() -> T): T` | Executes a block of code with a specific caret. | Result from caret.                 |
| `withPrimaryCaret<T>(block: suspend CaretRead.() -> T): T`       | Executes a block of code with the primary caret. | Result from caret.                 |

#### Line Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `getLineStartOffset(line: Int): Int` | Gets the offset of the start of a line. | The offset of the line start. |
| `getLineEndOffset(line: Int, allowEnd: Boolean): Int` | Gets the offset of the end of a line. | The offset of the line end. |
| `getLine(offset: Int): Line` | Gets the line at the specified offset. | The Line object. |

#### Mark and Jump Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `getGlobalMark(char: Char): Mark?` | Gets a global mark by its character key. | The mark, or null if the mark doesn't exist. |
| `getJump(count: Int = 0): Jump?` | Gets a jump from the jump list. | The jump, or null if there is no jump at the specified position. |

#### Scrolling Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `scrollCaretIntoView()` | Scrolls the caret into view. | None |
| `scrollVertically(lines: Int): Boolean` | Scrolls the editor by a specified number of lines. | True if the scroll was successful, false otherwise. |
| `scrollLineToTop(line: Int, start: Boolean): Boolean` | Scrolls the current line to the top of the display. | True if the scroll was successful, false otherwise. |
| `scrollLineToMiddle(line: Int, start: Boolean): Boolean` | Scrolls the current line to the middle of the display. | True if the scroll was successful, false otherwise. |
| `scrollLineToBottom(line: Int, start: Boolean): Boolean` | Scrolls the current line to the bottom of the display. | True if the scroll was successful, false otherwise. |
| `scrollHorizontally(columns: Int): Boolean` | Scrolls the editor horizontally by a specified number of columns. | True if the scroll was successful, false otherwise. |
| `scrollCaretToLeftEdge(): Boolean` | Scrolls the editor to position the caret column at the left edge of the display. | True if the scroll was successful, false otherwise. |
| `scrollCaretToRightEdge(): Boolean` | Scrolls the editor to position the caret column at the right edge of the display. | True if the scroll was successful, false otherwise. |

#### Text Navigation

| Method | Description | Return Value |
|--------|-------------|--------------|
| `getNextParagraphBoundOffset(startLine: Int, count: Int = 1, includeWhitespaceLines: Boolean = true): Int?` | Find the next paragraph-bound offset in the editor. | The offset of the next paragraph bound, or null if not found. |
| `getNextSentenceStart(startOffset: Int, count: Int = 1, includeCurrent: Boolean, requireAll: Boolean = true): Int?` | Finds the next sentence start in the editor from the given offset. | The offset of the next sentence start, or null if not found. |
| `getNextSectionStart(startLine: Int, marker: Char, count: Int = 1): Int` | Find the next section in the editor. | The offset of the next section. |
| `getPreviousSectionStart(startLine: Int, marker: Char, count: Int = 1): Int` | Find the start of the previous section in the editor. | The offset of the previous section. |
| `getNextSentenceEnd(startOffset: Int, count: Int = 1, includeCurrent: Boolean, requireAll: Boolean = true): Int?` | Find the next sentence end from the given offset. | The offset of the next sentence end, or null if not found. |
| `getNextWordStartOffset(startOffset: Int, count: Int = 1, isBigWord: Boolean): Int?` | Find the next word in the editor's document, from the given starting point. | The offset of the next word, or null if not found. |
| `getNextWordEndOffset(startOffset: Int, count: Int = 1, isBigWord: Boolean, stopOnEmptyLine: Boolean = true): Int` | Find the end offset of the next word in the editor's document. | The offset of the next word end. |
| `getNextCharOnLineOffset(startOffset: Int, count: Int = 1, char: Char): Int` | Find the next character on the current line. | The offset of the next character, or -1 if not found. |
| `getNearestWordOffset(startOffset: Int): Range?` | Find the word at or nearest to the given offset. | The range of the word, or null if not found. |
| `getParagraphRange(line: Int, count: Int = 1, isOuter: Boolean): Range?` | Returns range of a paragraph containing the given line. | The paragraph text range, or null if not found. |
| `getBlockQuoteInLineRange(startOffset: Int, quote: Char, isOuter: Boolean): Range?` | Find a block quote in the current line. | The range of the block quote, or null if not found. |

#### Pattern Matching

| Method | Description | Return Value |
|--------|-------------|--------------|
| `findAll(pattern: String, startLine: Int, endLine: Int, ignoreCase: Boolean = false): List<Range>` | Finds all occurrences of the given pattern within a specified line range. | A list of Ranges representing all matches found. |
| `findPattern(pattern: String, startOffset: Int, count: Int = 1, backwards: Boolean = false): Range?` | Finds text matching the given Vim-style regular expression pattern. | A Range representing the matched text, or null if no match is found. |

## Transaction

The `Transaction` interface provides methods for modifying the editor content and state. It includes operations for working with carets, highlights, marks, and jumps.

### Methods

#### Caret Operations

| Method                                                                  | Description | Return Value                       |
|-------------------------------------------------------------------------|-------------|------------------------------------|
| `forEachCaret<T>(block: suspend CaretTransaction.() -> T): List<T>`     | Executes a block of code for each caret in the editor. | A list of results from each caret. |
| `with<T>(caretId: CaretId, block: suspend CaretTransaction.() -> T): T` | Executes a block of code with a specific caret. | Result from caret.                 |
| `withPrimaryCaret<T>(block: suspend CaretTransaction.() -> T): T`       | Executes a block of code with the primary caret. | Result from caret.                               |
| `addCaret(offset: Int): CaretId`                                        | Adds a new caret at the specified offset. | The ID of the newly created caret. |
| `removeCaret(caretId: CaretId)`                                         | Removes a caret with the specified ID. | None                               |

#### Highlighting

| Method | Description | Return Value |
|--------|-------------|--------------|
| `addHighlight(startOffset: Int, endOffset: Int, backgroundColor: Color?, foregroundColor: Color?): HighlightId` | Adds a highlight to the editor. | The ID of the newly created highlight. |
| `removeHighlight(highlightId: HighlightId)` | Removes a highlight with the specified ID. | None |

#### Mark Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `setMark(char: Char): Boolean` | Sets a mark at the current position for each caret in the editor. | True if the mark was successfully set, false otherwise. |
| `removeMark(char: Char)` | Removes a mark for all carets in the editor. | None |
| `setGlobalMark(char: Char): Boolean` | Sets a global mark at the current position. | True if the mark was successfully set, false otherwise. |
| `removeGlobalMark(char: Char)` | Removes a global mark. | None |
| `setGlobalMark(char: Char, offset: Int): Boolean` | Sets a global mark at the specified offset. | True if the mark was successfully set, false otherwise. |
| `resetAllMarks()` | Resets all marks. | None |

#### Jump List Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `addJump(jump: Jump, reset: Boolean)` | Adds a specific jump to the jump list. | None |
| `removeJump(jump: Jump)` | Removes a jump from the jump list. | None |
| `dropLastJump()` | Removes the last jump from the jump list. | None |
| `clearJumps()` | Clears all jumps from the jump list. | None |

## CaretRead

The `CaretRead` interface provides read-only access to a caret in the editor. It includes methods for working with registers, marks, scrolling, and text navigation.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `caretId` | `CaretId` | The unique identifier for this caret. |
| `offset` | `Int` | The current offset (position) of the caret in the document. |
| `selection` | `Range` | The current selection range of the caret. |
| `line` | `Line` | Information about the current line where the caret is positioned. |
| `lastSelectedReg` | `Char` | The last register that was selected for operations. Example: After using `"ay` to yank into register 'a', this would return 'a'. In VimScript, variable `v:register` contains this value. |
| `defaultRegister` | `Char` | The default register used when no register is explicitly specified. In Vim, this is typically the unnamed register ("). |
| `isRegisterSpecifiedExplicitly` | `Boolean` | Indicates whether the current register was explicitly specified by the user. Example: After `"ay`, this would be true; after just `y`, this would be false. |
| `selectionMarks` | `Range?` | The marks for the current visual selection. In Vim, these are the '< and '> marks. |
| `changeMarks` | `Range?` | The marks for the last change. In Vim, these are the '[ and '] marks. |
| `localMarks` | `Set<Mark>` | All local marks for the current caret. |

### Methods

#### Register Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `selectRegister(register: Char): Boolean` | Selects a register for subsequent operations. Example: In Vim, pressing `"a` before an operation selects register 'a'. | True if the register was successfully selected, false otherwise. |
| `resetRegisters()` | Resets all registers to their default state. | None |
| `isWritable(register: Char): Boolean` | Checks if a register is writable. Some registers in Vim are read-only. | True if the register is writable, false otherwise. |
| `isSystemClipboard(register: Char): Boolean` | Checks if a register is connected to the system clipboard. In Vim, registers '+' and '*' are connected to the system clipboard. | True if the register is connected to the system clipboard, false otherwise. |
| `isPrimaryRegisterSupported(): Boolean` | Checks if the primary selection register is supported. Example: On Linux, using `"*y` yanks text to the primary selection. | True if the primary selection register is supported, false otherwise. |
| `getReg(register: Char): String?` | Gets the text content of a register. | The text content of the register, or null if the register is empty or doesn't exist. |
| `getRegType(register: Char): TextType?` | Gets the type of text stored in a register (character-wise, line-wise, or block-wise). | The type of text in the register, or null if the register is empty or doesn't exist. |
| `setReg(register: Char, text: String, textType: TextType = TextType.CHARACTER_WISE): Boolean` | Sets the text content and type of a register. | True if the register was successfully set, false otherwise. |

#### Mark Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `getMark(char: Char): Mark?` | Gets a mark by its character key for the current caret. | The mark, or null if the mark doesn't exist. |
| `setMark(char: Char): Boolean` | Sets a mark at the current caret position. | True if the mark was successfully set, false otherwise. |
| `setMark(char: Char, offset: Int): Boolean` | Sets a mark at the specified offset. | True if the mark was successfully set, false otherwise. |
| `removeLocalMark(char: Char)` | Removes a local mark for the current caret. | None |
| `resetAllMarksForCaret()` | Resets all marks for the current caret. | None |

#### Scrolling Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `scrollFullPage(pages: Int): Boolean` | Scrolls a full page up or down. Positive values scroll down, negative values scroll up. | True if the scroll was successful, false otherwise. |
| `scrollHalfPageUp(lines: Int): Boolean` | Scrolls half a page up. | True if the scroll was successful, false otherwise. |
| `scrollHalfPageDown(lines: Int): Boolean` | Scrolls half a page down. | True if the scroll was successful, false otherwise. |
| `selectWindowHorizontally(relativePosition: Int)` | Selects a window in the same row as the current window. Positive values select windows to the right, negative values select windows to the left. | None |
| `selectWindowInVertically(relativePosition: Int)` | Selects a window in the same column as the current window. Positive values select the windows below, negative values select the windows above. | None |

#### Text Navigation

| Method | Description | Return Value |
|--------|-------------|--------------|
| `getNextParagraphBoundOffset(count: Int = 1, includeWhitespaceLines: Boolean = true): Int?` | Finds the offset of the next paragraph boundary. | The offset of the next paragraph bound, or null if not found. |
| `getNextSentenceStart(count: Int = 1, includeCurrent: Boolean, requireAll: Boolean = true): Int?` | Finds the next sentence start in the editor from the given offset. | The offset of the next sentence start, or null if not found. |
| `getNextSectionStart(marker: Char, count: Int = 1): Int` | Find the next section in the editor. | The offset of the next section. |
| `getPreviousSectionStart(marker: Char, count: Int = 1): Int` | Find the start of the previous section in the editor. | The offset of the previous section. |
| `getNextSentenceEnd(count: Int = 1, includeCurrent: Boolean, requireAll: Boolean = true): Int?` | Finds the end offset of the next sentence from the current caret position. | The offset of the next sentence end, or null if not found. |
| `getMethodEndOffset(count: Int = 1): Int` | Finds the end offset of the next method from the current caret position. | The offset of the end of the next method. |
| `getMethodStartOffset(count: Int = 1): Int` | Finds the start offset of the next method from the current caret position. | The offset of the start of the next method. |
| `getNextCharOnLineOffset(count: Int = 1, char: Char): Int` | Finds the next occurrence of a specific character on the current line. | The offset of the found character, or -1 if not found. |
| `getNearestWordOffset(): Range?` | Finds the word at or nearest to the current caret position. | A Range representing the found word, or null if no word is found. |
| `getWordTextObjectRange(count: Int = 1, isOuter: Boolean, isBigWord: Boolean): Range` | Find the range of the word text object at the location of the caret. | The range of the word text object. |
| `getSentenceRange(count: Int = 1, isOuter: Boolean): Range` | Find the range of the sentence text object at the location of the caret. | The range of the sentence text object. |
| `getParagraphRange(count: Int = 1, isOuter: Boolean): Range?` | Returns range of a paragraph containing the caret. | The paragraph text range, or null if not found. |
| `getBlockTagRange(count: Int = 1, isOuter: Boolean): Range?` | Find the range of a block tag at the location of the caret. | The range of the block tag, or null if not found. |
| `getBlockQuoteInLineRange(quote: Char, isOuter: Boolean): Range?` | Find a block quote in the current line at the location of the caret. | The range of the block quote, or null if not found. |
| `getNextMisspelledWordOffset(count: Int = 1): Int` | Finds the offset of the next misspelled word from the current caret position. | The offset of the next misspelled word. |

## CaretTransaction

The `CaretTransaction` interface extends `CaretRead` and provides methods for modifying the caret and text in the editor. It includes operations for updating the caret position, inserting text, replacing text, and deleting text.

### Methods

#### Caret Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `updateCaret(offset: Int, selection: Range.Simple? = null)` | Updates the caret position and optionally sets a selection. If a selection is provided, the caret will have this selection after moving to the new offset. If no selection is provided, any existing selection will be removed. | None |

#### Text Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `insertText(position: Int, text: String, caretAtEnd: Boolean = true, insertBeforeCaret: Boolean = false): Boolean` | Inserts text at the specified position in the document. | True if the insertion was successful, false otherwise. |
| `replaceText(startOffset: Int, endOffset: Int, text: String): Boolean` | Replaces text between the specified offsets with new text. | True if the replacement was successful, false otherwise. |
| `replaceTextBlockwise(range: Range.Block, text: List<String>)` | Replaces text in multiple ranges (blocks) with new text. | None |
| `deleteText(startOffset: Int, endOffset: Int): Boolean` | Deletes text between the specified offsets. | True if the deletion was successful, false otherwise. |

#### Jump Operations

| Method | Description | Return Value |
|--------|-------------|--------------|
| `addJump(reset: Boolean)` | Adds a jump with the current caret's position to the jump list. | None |
| `saveJumpLocation()` | Saves the location of the current caret to the jump list and sets the ' mark. | None |

## OptionScope

The `OptionScope` interface provides comprehensive methods for managing Vim options. It supports different scopes for options (global, local, and effective) and allows for type-safe access to option values. The `option` function returns a value, making it easy to retrieve option values directly.

### Core Methods

| Method                                 | Description                                                                                                                                                                      | Return Value                                                                                           |
|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `get<T>(name: String): T`              | Gets the value of an option with the specified type. In Vim, options can be accessed with the `&` prefix. Example: `&ignorecase` returns the value of the 'ignorecase' option.   | The value of the option. Throws IllegalArgumentException if the option doesn't exist or type is wrong. |
| `set<T>(name: String, value: T)`       | Sets the effective value of an option with the specified type. In Vim, this is equivalent to `:set option=value`. Example: `:set ignorecase` or `let &ignorecase = 1`            | None                                                                                                   |
| `setGlobal<T>(name: String, value: T)` | Sets the global value of an option with the specified type. In Vim, this is equivalent to `:setglobal option=value`. Example: `:setglobal ignorecase` or `let &g:ignorecase = 1` | None                                                                                                   |
| `setLocal<T>(name: String, value: T)`  | Sets the local value of an option with the specified type. In Vim, this is equivalent to `:setlocal option=value`. Example: `:setlocal ignorecase` or `let &l:ignorecase = 1`    | None                                                                                                   |
| `reset(name: String)`                  | Resets an option to its default value. In Vim, this is equivalent to `:set option&`. Example: `:set ignorecase&` resets the 'ignorecase' option to its default value.            | None                                                                                                   |

### List Option Methods

These extension functions provide convenient ways to manipulate comma-separated list options (like `virtualedit`, `whichwrap`, etc.):

| Method                                         | Description                                                 | Vim Equivalent       |
|------------------------------------------------|-------------------------------------------------------------|----------------------|
| `append(name: String, vararg values: String)`  | Appends values to a list option. Duplicates are not added.  | `:set option+=value` |
| `prepend(name: String, vararg values: String)` | Prepends values to a list option. Duplicates are not added. | `:set option^=value` |
| `remove(name: String, vararg values: String)`  | Removes values from a list option.                          | `:set option-=value` |

### Utility Methods

| Method                         | Description                                                             | Return Value    |
|--------------------------------|-------------------------------------------------------------------------|-----------------|
| `toggle(name: String)`         | Toggles a boolean option value.                                         | None            |
| `String.split(): List<String>` | Extension function to split a comma-separated option value into a list. | List of strings |

### Usage Examples

```kotlin
// Getting option values
val history = myVimApi.option { get<Int>("history") }
val ignoreCase = myVimApi.option { get<Boolean>("ignorecase") }

// Setting options
myVimApi.option {
    set("number", true)          // Line numbers
    setGlobal("history", 100)    // Command history
    setLocal("tabstop", 4)       // Tab width for current buffer
}

// Working with list options
myVimApi.option {
    // Add values to a list option
    append("virtualedit", "block", "onemore")
    
    // Remove values from a list option
    remove("virtualedit", "block")
    
    // Prepend values to a list option
    prepend("whichwrap", "b", "s")
}

// Toggle boolean options
myVimApi.option {
    toggle("ignorecase")  // true → false or false → true
}

// Reset to default value
myVimApi.option {
    reset("tabstop")  // Reset to default value
}

// Process list options
myVimApi.option {
    val virtualEditModes = get<String>("virtualedit").split()
    // "block,all" → ["block", "all"]
}

// Complex operations with return value
val isIgnoreCaseEnabled = myVimApi.option {
    val current = get<Boolean>("ignorecase")
    if (!current) {
        set("ignorecase", true)
        set("smartcase", true)
    }
    current
}
```

## OutputPanelScope

The `OutputPanelScope` interface provides methods for interacting with the Vim output panel. The output panel is used to display text output from Vim commands and operations.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `text` | `String` | The text displayed in the output panel. |
| `label` | `String` | The label text displayed at the bottom of the output panel. This is used for status information like "-- MORE --" to indicate that there is more content to scroll through. |

### Methods

| Method | Description | Return Value |
|--------|-------------|--------------|
| `setText(text: String)` | Sets the text content of the output panel. This replaces any existing text in the panel with the provided text. | None |
| `appendText(text: String, startNewLine: Boolean = false)` | Appends text to the existing content of the output panel. If startNewLine is true and there is existing text, a newline character will be inserted before the appended text. | None |
| `setLabel(label: String)` | Sets the label text at the bottom of the output panel. | None |
| `clearText()` | Clears all text from the output panel. | None |

## ModalInput

The `ModalInput` interface provides methods for creating and managing modal input dialogs, which can be used to get user input in a Vim-like way.

### Methods

| Method                                                         | Description | Return Value |
|----------------------------------------------------------------|-------------|--------------|
| `updateLabel(block: (String) -> String): ModalInput`           | Updates the label of the modal input dialog using the provided function. | The ModalInput instance for method chaining. |
| `repeatWhile(condition: () -> Boolean): ModalInput`            | Repeats the modal input dialog while the provided condition is true. | The ModalInput instance for method chaining. |
| `repeat(count: Int): ModalInput`                               | Repeats the modal input dialog the specified number of times. | The ModalInput instance for method chaining. |
| `inputString(label: String, handler: VimApi.(String) -> Unit)` | Creates a modal input dialog with the given label and handler. The handler will be executed after the user presses ENTER. | None |
| `inputChar(label: String, handler: VimApi.(Char) -> Unit)`     | Creates a modal input dialog with the given label and handler. The handler will be executed after the user enters a character. | None |
| `closeCurrentInput(refocusEditor: Boolean = true): Boolean`    | Closes the current modal input dialog, if any. If refocusEditor is true, the editor will be refocused after closing the dialog. | True if a dialog was closed, false otherwise. |

## ListenerScope

The `ListenerScope` interface provides methods for registering callbacks for various events in the Vim editor, such as mode changes, yanking text, editor lifecycle events, and more.

### Methods

#### Mode and Action Listeners

| Method                                                                  | Description | Return Value |
|-------------------------------------------------------------------------|-------------|--------------|
| `onModeChange(callback: suspend VimApi.(Mode) -> Unit)`                 | Registers a callback that is invoked when the editor mode changes (e.g., from Normal to Insert). | None |
| `onYank(callback: suspend VimApi.(Map<CaretId, Range.Simple>) -> Unit)` | Registers a callback that is invoked when text is yanked. The callback receives a map of caret IDs to yanked text ranges. | None |

#### Editor Lifecycle Listeners

| Method                                                   | Description | Return Value |
|----------------------------------------------------------|-------------|--------------|
| `onEditorCreate(callback: suspend VimApi.() -> Unit)`    | Registers a callback that is invoked when a new editor is created. | None |
| `onEditorRelease(callback: suspend VimApi.() -> Unit)`   | Registers a callback that is invoked when an editor is released (closed). | None |
| `onEditorFocusGain(callback: suspend VimApi.() -> Unit)` | Registers a callback that is invoked when an editor gains focus. | None |
| `onEditorFocusLost(callback: suspend VimApi.() -> Unit)` | Registers a callback that is invoked when an editor loses focus. | None |

#### Macro Recording Listeners

| Method                                                        | Description | Return Value |
|---------------------------------------------------------------|-------------|--------------|
| `onMacroRecordingStart(callback: suspend VimApi.() -> Unit)`  | Registers a callback that is invoked when macro recording starts. | None |
| `onMacroRecordingFinish(callback: suspend VimApi.() -> Unit)` | Registers a callback that is invoked when macro recording finishes. | None |

#### Plugin State Listeners

| Method                                                   | Description | Return Value |
|----------------------------------------------------------|-------------|--------------|
| `onIdeaVimEnabled(callback: suspend VimApi.() -> Unit)`  | Registers a callback that is invoked when IdeaVim is enabled. | None |
| `onIdeaVimDisabled(callback: suspend VimApi.() -> Unit)` | Registers a callback that is invoked when IdeaVim is disabled. | None |

## DigraphScope

The `DigraphScope` interface provides access to Vim's digraph functionality. Digraphs are special character combinations that produce a single character, often used for entering non-ASCII characters.

### Methods

| Method | Description | Return Value |
|--------|-------------|--------------|
| `getCharacter(ch1: Char, ch2: Char): Int` | Gets the character for a digraph. | The Unicode codepoint of the character represented by the digraph, or the codepoint of ch2 if no digraph is found. |
| `addDigraph(ch1: Char, ch2: Char, codepoint: Int)` | Adds a custom digraph. | None |
| `clearCustomDigraphs()` | Clears all custom digraphs. | None |

## CommandLineScope

The `CommandLineScope` class provides methods for interacting with the Vim command line. The command line is used for entering Ex commands, search patterns, and other input.

### Methods

| Method                                                                             | Description | Return Value |
|------------------------------------------------------------------------------------|-------------|--------------|
| `input(prompt: String, finishOn: Char? = null, callback: VimApi.(String) -> Unit)` | Reads input from the command line and processes it with the provided function. | None |
| `read<T>(block: suspend CommandLineRead.() -> T): Deferred<T>`                     | Executes a block of code in the context of read operations on the command line. This allows for reading the command line state without modifying it. | A Deferred result of the block execution. |
| `change(block: suspend CommandLineTransaction.() -> Unit): Job`                    | Executes a block of code in the context of transaction operations on the command line. This allows for modifying the command line state. | A Job representing the asynchronous operation. |

## CommandLineRead

The `CommandLineRead` interface provides read-only access to the command line state. It includes properties for accessing the current text, caret position, and active state of the command line.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `text` | `String` | The text currently displayed in the command line. |
| `caretPosition` | `Int` | The current position of the caret in the command line. |
| `isActive` | `Boolean` | True if the command line is currently active, false otherwise. |

## CommandLineTransaction

The `CommandLineTransaction` interface provides methods for modifying the command line state. It includes operations for setting text, inserting text, setting the caret position, and closing the command line.

### Methods

| Method | Description | Return Value |
|--------|-------------|--------------|
| `setText(text: String)` | Sets the text content of the command line. This replaces any existing text in the command line with the provided text. | None |
| `insertText(offset: Int, text: String)` | Inserts text at the specified position in the command line. | None |
| `setCaretPosition(position: Int)` | Sets the caret position in the command line. | None |
| `close(refocusEditor: Boolean = true): Boolean` | Closes the command line. If refocusEditor is true, the editor will be refocused after closing the command line. | True if the command line was closed, false if it was not active. |
