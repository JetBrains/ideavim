# IdeaVim Builtin Functions Status

This document tracks the progress of implementing Vim's builtin functions. There are a lot of functions, and it will 
take some time to implement them all. 

The builtin functions are listed below, grouped by functionality, as in `:help function-list`, with implementation
status.

* ✅ Indicates that the function has been fully implemented.
* ☑️ Indicates that the function has been partially implemented. Perhaps some optional parameters are not currently
supported.
* 🅾️ Indicates that the function is not applicable to IdeaVim and there are no current plans to implement it. This is
usually because it is related to a feature that is either not implemented by or not relevant to IdeaVim. For example,
`lispindent()`, or quick fix window, terminal or syntax highlighting related functions.

If there is no status icon, then the function is waiting to be implemented. Contributions are welcome!

## String manipulation

See `:help string-functions`.

| Status | Function            | Description                                                      |
|--------|---------------------|------------------------------------------------------------------|
| ✅      | `nr2char()`         | get a character by its number value                              |
|        | `list2str()`        | get a character string from a list of numbers                    |
| ✅      | `char2nr()`         | get number value of a character                                  |
|        | `str2list()`        | get list of numbers from a string                                |
|        | `str2nr()`          | convert a string to a Number                                     |
|        | `str2float()`       | convert a string to a Float                                      |
|        | `printf()`          | format a string according to % items                             |
| ✅      | `escape()`          | escape characters in a string with a '\'                         |
|        | `shellescape()`     | escape a string for use with a shell command                     |
|        | `fnameescape()`     | escape a file name for use with a Vim command                    |
|        | `tr()`              | translate characters from one set to another                     |
|        | `strtrans()`        | translate a string to make it printable                          |
|        | `keytrans()`        | translate internal keycodes to a form that can be used by `:map` | 
| ✅      | `tolower()`         | turn a string to lowercase                                       |
| ✅      | `toupper()`         | turn a string to uppercase                                       |
|        | `charclass()`       | class of a character                                             |
|        | `match()`           | position where a pattern matches in a string                     |
|        | `matchbufline()`    | all the matches of a pattern in a buffer                         |
|        | `matchend()`        | position where a pattern match ends in a string                  |
|        | `matchfuzzy()`      | fuzzy matches a string in a list of strings                      |
|        | `matchfuzzypos()`   | fuzzy matches a string in a list of strings                      |
|        | `matchstr()`        | match of a pattern in a string                                   |
|        | `matchstrlist()`    | all the matches of a pattern in a List of strings                |
|        | `matchstrpos()`     | match and positions of a pattern in a string                     |
|        | `matchlist()`       | like `matchstr()` and also return submatches                     |
|        | `stridx()`          | first index of a short string in a long string                   |
|        | `strridx()`         | last index of a short string in a long string                    |
|        | `strlen()`          | length of a string in bytes                                      |
|        | `strcharlen()`      | length of a string in characters                                 |
|        | `strchars()`        | number of characters in a string                                 |
|        | `strutf16len()`     | number of UTF-16 code units in a string                          |
|        | `strwidth()`        | size of string when displayed                                    |
|        | `strdisplaywidth()` | size of string when displayed, deals with tabs                   |
|        | `setcellwidths()`   | set character cell width overrides                               |
|        | `getcellwidths()`   | get character cell width overrides                               |
|        | `getcellpixels()`   | get character cell pixel size                                    |
| ✅      | `reverse()`         | reverse the order of characters in a string                      |
|        | `substitute()`      | substitute a pattern match with a string                         |
| ☑️     | `submatch()`        | get a specific match in ":s" and substitute()                    |
|        | `strpart()`         | get part of a string using byte index                            |
|        | `strcharpart()`     | get part of a string using char index                            |
| ✅      | `slice()`           | take a slice of a string, using char index in Vim9 script        |
|        | `strgetchar()`      | get character from a string using char index                     |
|        | `expand()`          | expand special keywords                                          |
|        | `expandcmd()`       | expand a command like done for `:edit`                           |
|        | `iconv()`           | convert text from one encoding to another                        |
|        | `byteidx()`         | byte index of a character in a string                            |
|        | `byteidxcomp()`     | like byteidx() but count composing characters                    |
|        | `charidx()`         | character index of a byte in a string                            |
|        | `utf16idx()`        | UTF-16 index of a byte in a string                               |
| ✅      | `repeat()`          | repeat a string multiple times                                   |
|        | `eval()`            | evaluate a string expression                                     |
|        | `execute()`         | execute an Ex command and get the output                         |
|        | `win_execute()`     | like execute() but in a specified window                         |
| ✅      | `trim()`            | trim characters from a string                                    |
|        | `bindtextdomain()`  | set message lookup translation base path                         |
|        | `gettext()`         | lookup message translation                                       |
|        | `ngettext()`        | lookup single/plural message translation                         |
|        | `str2blob()`        | convert a list of strings into a blob                            |
|        | `blob2str()`        | convert a blob into a list of strings                            |

## List manipulation

See `:help list-functions`.

| Status | Function       | Description                                                   |
|--------|----------------|---------------------------------------------------------------|
| ✅      | `get()`        | get an item without error for wrong index                     |
| ✅      | `len()`        | number of items in a List                                     |
| ✅      | `empty()`      | check if List is empty                                        |
| ✅      | `insert()`     | insert an item somewhere in a List                            |
| ✅      | `add()`        | append an item to a List                                      |
| ✅      | `extend()`     | append a List to a List                                       |
| ✅      | `extendnew()`  | make a new List and append items                              |
| ✅      | `remove()`     | remove one or more items from a List                          |
| ✅      | `copy()`       | make a shallow copy of a List                                 |
| ✅      | `deepcopy()`   | make a full copy of a List                                    |
|        | `filter()`     | remove selected items from a List                             |
|        | `map()`        | change each List item                                         |
|        | `mapnew()`     | make a new List with changed items                            |
|        | `foreach()`    | apply function to List items                                  |
|        | `reduce()`     | reduce a List to a value                                      |
| ✅      | `slice()`      | take a slice of a List                                        |
|        | `sort()`       | sort a List                                                   |
| ✅      | `reverse()`    | reverse the order of items in a List                          |
|        | `uniq()`       | remove copies of repeated adjacent items                      |
| ✅      | `split()`      | split a String into a List                                    |
| ✅      | `join()`       | join List items into a String                                 |
| ✅      | `range()`      | return a List with a sequence of numbers                      |
| ✅      | `string()`     | String representation of a List                               |
|        | `call()`       | call a function with List as arguments                        |
| ✅      | `index()`      | index of a value in a List or Blob                            |
|        | `indexof()`    | index in a List or Blob where an expression evaluates to true |
| ✅      | `max()`        | maximum value in a List                                       |
| ✅      | `min()`        | minimum value in a List                                       |
| ✅      | `count()`      | count number of times a value appears in a List               |
| ✅      | `repeat()`     | repeat a List multiple times                                  |
| ✅      | `flatten()`    | flatten a List                                                |
| ✅      | `flattennew()` | flatten a copy of a List                                      |
| ✅      | `items()`      | get List of List index-value pairs                            |

## Dictionary manipulation

See `:help dict-functions`.

| Status | Function      | Description                                   |
|--------|---------------|-----------------------------------------------|
| ✅      | `get()`       | get an entry without an error for a wrong key |
| ✅      | `len()`       | number of entries in a Dictionary             |
| ✅      | `has_key()`   | check whether a key appears in a Dictionary   |
| ✅      | `empty()`     | check if Dictionary is empty                  |
| ✅      | `remove()`    | remove an entry from a Dictionary             |
| ✅      | `extend()`    | add entries from one Dictionary to another    |
| ✅      | `extendnew()` | make a new Dictionary and append items        |
|        | `filter()`    | remove selected entries from a Dictionary     |
|        | `map()`       | change each Dictionary entry                  |
|        | `mapnew()`    | make a new Dictionary with changed items      |
|        | `foreach()`   | apply function to Dictionary items            |
| ✅      | `keys()`      | get List of Dictionary keys                   |
| ✅      | `values()`    | get List of Dictionary values                 |
| ✅      | `items()`     | get List of Dictionary key-value pairs        |
| ✅      | `copy()`      | make a shallow copy of a Dictionary           |
| ✅      | `deepcopy()`  | make a full copy of a Dictionary              |
| ✅      | `string()`    | String representation of a Dictionary         |
| ✅      | `max()`       | maximum value in a Dictionary                 |
| ✅      | `min()`       | minimum value in a Dictionary                 |
| ✅      | `count()`     | count number of times a value appears         |

## Floating point computation

See `:help float-functions`.

| Status | Function     | Description                              |
|--------|--------------|------------------------------------------|
| ✅      | `float2nr()` | 	convert Float to Number                 |
| ✅      | `abs()`      | 	absolute value (also works for Number)  |
| ✅      | `round()`    | 	round off                               |
| ✅      | `ceil()`     | 	round up                                |
| ✅      | `floor()`    | 	round down                              |
| ✅      | `trunc()`    | 	remove value after decimal point        |
| ✅      | `fmod()`     | 	remainder of division                   |
| ✅      | `exp()`      | 	exponential                             |
| ✅      | `log()`      | 	natural logarithm (logarithm to base e) |
| ✅      | `log10()`    | 	logarithm to base 10                    |
| ✅      | `pow()`      | 	value of x to the exponent y            |
| ✅      | `sqrt()`     | 	square root                             |
| ✅      | `sin()`      | 	sine                                    |
| ✅      | `cos()`      | 	cosine                                  |
| ✅      | `tan()`      | 	tangent                                 |
| ✅      | `asin()`     | 	arc sine                                |
| ✅      | `acos()`     | 	arc cosine                              |
| ✅      | `atan()`     | 	arc tangent                             |
| ✅      | `atan2()`    | 	arc tangent                             |
| ✅      | `sinh()`     | 	hyperbolic sine                         |
| ✅      | `cosh()`     | 	hyperbolic cosine                       |
| ✅      | `tanh()`     | 	hyperbolic tangent                      |
| ✅      | `isinf()`    | 	check for infinity                      |
| ✅      | `isnan()`    | 	check for not a number                  |

## Blob manipulation

See `help blob-functions`.

| Status | Function      | Description                            |
|--------|---------------|----------------------------------------|
|        | `blob2list()` | get a list of numbers from a blob      |
|        | `list2blob()` | get a blob from a list of numbers      |
|        | `reverse()`   | reverse the order of numbers in a blob |

## Other computation

See `:help bitwise-function`.

| Status | Function   | Description                    |
|--------|------------|--------------------------------|
| ✅      | `and()`    | bitwise AND                    |
| ✅      | `invert()` | bitwise invert                 |
| ✅      | `or()`     | bitwise OR                     |
| ✅      | `xor()`    | bitwise XOR                    |
|        | `sha256()` | SHA-256 hash                   |
|        | `rand()`   | get a pseudo-random number     |
|        | `srand()`  | initialize seed used by rand() |

## Variables

See `:help var-functions`.

| Status | Function           | Description                                         |
|--------|--------------------|-----------------------------------------------------|
|        | `instanceof()`     | check if a variable is an instance of a given class |
|        | `type()`           | type of a variable as a number                      |
|        | `typename()`       | type of a variable as text                          |
|        | `islocked()`       | check if a variable is locked                       |
| ✅      | `funcref()`        | get a Funcref for a function reference              |
| ✅      | `function()`       | get a Funcref for a function name                   |
|        | `getbufvar()`      | get a variable value from a specific buffer         |
|        | `setbufvar()`      | set a variable in a specific buffer                 |
|        | `getwinvar()`      | get a variable from specific window                 |
|        | `gettabvar()`      | get a variable from specific tab page               |
|        | `gettabwinvar()`   | get a variable from specific window & tab page      |
|        | `setwinvar()`      | set a variable in a specific window                 |
|        | `settabvar()`      | set a variable in a specific tab page               |
|        | `settabwinvar()`   | set a variable in a specific window & tab page      |
|        | `garbagecollect()` | possibly free memory                                |

## Cursor and mark position

See `:help cursor-functions` and `:help mark-functions`.

| Status | Function             | Description                                   |
|--------|----------------------|-----------------------------------------------|
| ✅      | `col()`              | column number of the cursor or a mark         |
|        | `virtcol()`          | screen column of the cursor or a mark         |
| ✅      | `line()`             | line number of the cursor or mark             |
|        | `wincol()`           | window column number of the cursor            |
|        | `winline()`          | window line number of the cursor              |
|        | `cursor()`           | position the cursor at a line/column          |
|        | `screencol()`        | get screen column of the cursor               |
|        | `screenrow()`        | get screen row of the cursor                  |
|        | `screenpos()`        | screen row and col of a text character        |
|        | `virtcol2col()`      | byte index of a text character on screen      |
|        | `getcurpos()`        | get position of the cursor                    |
|        | `getpos()`           | get position of cursor, mark, etc.            |
|        | `setpos()`           | set position of cursor, mark, etc.            |
|        | `getmarklist()`      | list of global/local marks                    |
|        | `byte2line()`        | get line number at a specific byte count      |
|        | `line2byte()`        | byte count at a specific line                 |
|        | `diff_filler()`      | get the number of filler lines above a line   |
|        | `screenattr()`       | get attribute at a screen line/row            |
|        | `screenchar()`       | get character code at a screen line/row       |
|        | `screenchars()`      | get character codes at a screen line/row      |
|        | `screenstring()`     | get string of characters at a screen line/row |
|        | `charcol()`          | character number of the cursor or a mark      |
|        | `getcharpos()`       | get character position of cursor, mark, etc.  |
|        | `setcharpos()`       | set character position of cursor, mark, etc.  |
|        | `getcursorcharpos()` | get character position of the cursor          |
|        | `setcursorcharpos()` | set character position of the cursor          |

## Working with text in the current buffer

See `:help text-functions`.

| Status | Function          | Description                                   |
|--------|-------------------|-----------------------------------------------|
| ✅      | `getline()`       | get a line or list of lines from the buffer   |
|        | `getregion()`     | get a region of text from the buffer          |
|        | `getregionpos()`  | get a list of positions for a region          |
|        | `setline()`       | replace a line in the buffer                  |
|        | `append()`        | append line or list of lines in the buffer    |
|        | `indent()`        | indent of a specific line                     |
|        | `cindent()`       | indent according to C indenting               |
|        | `lispindent()`    | indent according to Lisp indenting            |
|        | `nextnonblank()`  | find next non-blank line                      |
|        | `prevnonblank()`  | find previous non-blank line                  |
|        | `search()`        | find a match for a pattern                    |
|        | `searchpos()`     | find a match for a pattern                    |
|        | `searchcount()`   | get number of matches before/after the cursor |
|        | `searchpair()`    | find the other end of a start/skip/end        |
|        | `searchpairpos()` | find the other end of a start/skip/end        |
|        | `searchdecl()`    | search for the declaration of a name          |
|        | `getcharsearch()` | return character search information           |
|        | `setcharsearch()` | set character search information              |

## Working with text in another buffer

| Status | Function          | Description                                    |
|--------|-------------------|------------------------------------------------|
|        | `getbufline()`    | get a list of lines from the specified buffer  |
|        | `getbufoneline()` | get a one line from the specified buffer       |
|        | `setbufline()`    | replace a line in the specified buffer         |
|        | `appendbufline()` | append a list of lines in the specified buffer |
|        | `deletebufline()` | delete lines from a specified buffer           |

## System functions and manipulation of files

See `:help system-functions` and `:help file-functions`.

| Status | Function           | Description                                   |
|--------|--------------------|-----------------------------------------------|
|        | `glob()`           | expand wildcards                              |
|        | `globpath()`       | expand wildcards in a number of directories   |
|        | `glob2regpat()`    | convert a glob pattern into a search pattern  |
|        | `findfile()`       | find a file in a list of directories          |
|        | `finddir()`        | find a directory in a list of directories     |
|        | `resolve()`        | find out where a shortcut points to           |
|        | `fnamemodify()`    | modify a file name                            |
|        | `pathshorten()`    | shorten directory names in a path             |
|        | `simplify()`       | simplify a path without changing its meaning  |
|        | `executable()`     | check if an executable program exists         |
|        | `exepath()`        | full path of an executable program            |
|        | `filereadable()`   | check if a file can be read                   |
|        | `filewritable()`   | check if a file can be written to             |
|        | `getfperm()`       | get the permissions of a file                 |
|        | `setfperm()`       | set the permissions of a file                 |
|        | `getftype()`       | get the kind of a file                        |
|        | `isabsolutepath()` | check if a path is absolute                   |
|        | `isdirectory()`    | check if a directory exists                   |
|        | `getfsize()`       | get the size of a file                        |
|        | `getcwd()`         | get the current working directory             |
|        | `haslocaldir()`    | check if current window used                  |
|        | `tempname()`       | get the name of a temporary file              |
|        | `mkdir()`          | create a new directory                        |
|        | `chdir()`          | change current working directory              |
|        | `delete()`         | delete a file                                 |
|        | `rename()`         | rename a file                                 |
|        | `system()`         | get the result of a shell command as a string |
|        | `systemlist()`     | get the result of a shell command as a list   |
|        | `environ()`        | get all environment variables                 |
|        | `getenv()`         | get one environment variable                  |
|        | `setenv()`         | set an environment variable                   |
|        | `hostname()`       | name of the system                            |
|        | `readfile()`       | read a file into a List of lines              |
|        | `readblob()`       | read a file into a Blob                       |
|        | `readdir()`        | get a List of file names in a directory       |
|        | `readdirex()`      | get a List of file information in a directory |
|        | `writefile()`      | write a List of lines or Blob into a file     |
|        | `filecopy()`       | copy a file {from} to {to}                    |

## Date and Time

See `:help date-functions` and `:help time-functions`.

| Status | Function         | Description                                 |
|--------|------------------|---------------------------------------------|
|        | `getftime()`     | 	get last modification time of a file       |
|        | `localtime()`    | 	get current time in seconds                |
|        | `strftime()`     | 	convert time to a string                   |
|        | `strptime()`     | 	convert a date/time string to time         |
|        | `reltime()`      | 	get the current or elapsed time accurately |
|        | `reltimestr()`   | 	convert reltime() result to a string       |
|        | `reltimefloat()` | 	convert reltime() result to a Float        |

## Autocmds

See `:help autocmd-functions`.

| Status | Function           | Description                          |
|--------|--------------------|--------------------------------------|
|        | `autocmd_add()`    | add a list of autocmds and groups    |
|        | `autocmd_delete()` | delete a list of autocmds and groups |
|        | `autocmd_get()`    | return a list of autocmds            |

## Buffers, windows and the argument list

See `:help buffer-functions`, `:help window-functions` and `:help arg-functions`.

| Status | Function                | Description                                  |
|--------|-------------------------|----------------------------------------------|
|        | `argc()`                | number of entries in the argument list       |
|        | `argidx()`              | current position in the argument list        |
|        | `arglistid()`           | get id of the argument list                  |
|        | `argv()`                | get one entry from the argument list         |
|        | `bufadd()`              | add a file to the list of buffers            |
|        | `bufexists()`           | check if a buffer exists                     |
|        | `buflisted()`           | if a buffer exists and is listed             |
|        | `bufload()`             | ensure a buffer is loaded                    |
|        | `bufloaded()`           | check if a buffer exists and is loaded       |
|        | `bufname()`             | get the name of a specific buffer            |
|        | `bufnr()`               | get the buffer number of a specific buffer   |
|        | `tabpagebuflist()`      | return List of buffers in a tab page         |
|        | `tabpagenr()`           | get the number of a tab page                 |
|        | `tabpagewinnr()`        | like `winnr()` for a specified tab page      |
|        | `winnr()`               | get the window number for the current window |
|        | `bufwinid()`            | get the window ID of a specific buffer       |
|        | `bufwinnr()`            | get the window number of a specific buffer   |
|        | `winbufnr()`            | get the buffer number of a specific window   |
|        | `listener_add()`        | add a callback to listen to changes          |
|        | `listener_flush()`      | invoke listener callbacks                    |
|        | `listener_remove()`     | remove a listener callback                   |
|        | `win_findbuf()`         | find windows containing a buffer             |
|        | `win_getid()`           | get window ID of a window                    |
|        | `win_gettype()`         | get type of window                           |
|        | `win_gotoid()`          | go to window with ID                         |
|        | `win_id2tabwin()`       | get tab and window nr from window ID         |
|        | `win_id2win()`          | get window nr from window ID                 |
|        | `win_move_separator()`  | move window vertical separator               |
|        | `win_move_statusline()` | move window status line                      |
|        | `win_splitmove()`       | move window to a split of another window     |
|        | `getbufinfo()`          | get a list with buffer information           |
|        | `gettabinfo()`          | get a list with tab page information         |
|        | `getwininfo()`          | get a list with window information           |
|        | `getchangelist()`       | get a list of change list entries            |
|        | `getjumplist()`         | get a list of jump list entries              |
|        | `swapfilelist()`        | list of existing swap files in 'directory'   |
|        | `swapinfo()`            | information about a swap file                |
|        | `swapname()`            | get the swap file path of a buffer           |

## Command line:

See `:help command-line-functions`.

| Status | Function            | Description                                           |
|--------|---------------------|-------------------------------------------------------|
|        | `getcmdcomplpat()`  | get completion pattern of the current command         |
|        | `getcmdcompltype()` | get the type of the current command line completion   |
|        | `getcmdline()`      | get the current command line                          |
|        | `getcmdpos()`       | get position of the cursor in the command line        |
|        | `getcmdscreenpos()` | get screen position of the cursor in the command line |
|        | `setcmdline()`      | set the current command line                          |
|        | `setcmdpos()`       | set position of the cursor in the command line        |
| ✅      | `getcmdtype()`      | return the current command-line type                  |
|        | `getcmdwintype()`   | return the current command-line window type           |
|        | `getcompletion()`   | list of command-line completion matches               |
|        | `fullcommand()`     | get full command name                                 |

## Quickfix and location lists

See `:help quickfix-functions`.

| Status | Function       | Description                 |
|--------|----------------|-----------------------------|
|        | `getqflist()`  | list of quickfix errors     |
|        | `setqflist()`  | modify a quickfix list      |
|        | `getloclist()` | list of location list items |
|        | `setloclist()` | modify a location list      |

## Insert mode completion:

See `:help completion-functions`.

| Status | Function           | Description                                            |
|--------|--------------------|--------------------------------------------------------|
|        | `complete()`       | set found matches                                      |
|        | `complete_add()`   | add to found matches                                   |
|        | `complete_check()` | check if completion should be aborted                  |
|        | `complete_info()`  | get current completion information                     |
|        | `complete_match()` | get insert completion start match col and trigger text |
| ✅      | `pumvisible()`     | check if the popup menu is displayed                   |
|        | `pum_getpos()`     | position and size of popup menu if visible             |

## Folding

See `:help folding-functions`.

| Status | Function           | Description                                   |
|--------|--------------------|-----------------------------------------------|
|        | `foldclosed()`     | check for a closed fold at a specific line    |
|        | `foldclosedend()`  | like `foldclosed()` but return the last line  |
|        | `foldlevel()`      | check for the fold level at a specific line   |
|        | `foldtext()`       | generate the line displayed for a closed fold |
|        | `foldtextresult()` | get the text displayed for a closed fold      |

## Syntax and highlighting:

See `:help syntax-functions` and `:help highlighting-functions`.

| Status | Function         | Description                                                         |
|--------|------------------|---------------------------------------------------------------------|
|        | `clearmatches()` | clear all matches defined by `matchadd()` and the `:match` commands |
|        | `getmatches()`   | get all matches defined by `matchadd()` and the `:match` commands   |
|        | `hlexists()`     | check if a highlight group exists                                   |              |
|        | `hlget()`        | get highlight group attributes                                      |              |
|        | `hlset()`        | set highlight group attributes                                      |              |
|        | `hlID()`         | get ID of a highlight group                                         |              |
|        | `synID()`        | get syntax ID at a specific position                                |              |
|        | `synIDattr()`    | get a specific attribute of a syntax ID                             |              |
|        | `synIDtrans()`   | get translated syntax ID                                            |              |
|        | `synstack()`     | get list of syntax IDs at a specific position                       |              |
|        | `synconcealed()` | get info about (syntax) concealing                                  |              |
|        | `diff_hlID()`    | get highlight ID for diff mode at a position                        |              |
|        | `matchadd()`     | define a pattern to highlight (a "match")                           |              |
|        | `matchaddpos()`  | define a list of positions to highlight                             |              |
|        | `matcharg()`     | get info about `:match` arguments                                   |
|        | `matchdelete()`  | delete a match defined by `matchadd()` or a `:match` command        |
|        | `setmatches()`   | restore a list of matches saved by `getmatches()`                   |

## Spelling

See `:help spell-functions`.

| Status | Function         | Description                                  |
|--------|------------------|----------------------------------------------|
|        | `spellbadword()` | locate badly spelled word at or after cursor |
|        | `spellsuggest()` | return suggested spelling corrections        |
|        | `soundfold()`    | return the sound-a-like equivalent of a word |

## History

See `:help history-functions`.

| Status | Function    | Description                         |
|--------|-------------|-------------------------------------|
|        | `histadd()` | add an item to a history            |
|        | `histdel()` | delete an item from a history       |
|        | `histget()` | get an item from a history          |
|        | `histnr()`  | get highest index of a history list |

## Interactive

See `:help interactive-functions`

| Status | Function          | Description                                 |
|--------|-------------------|---------------------------------------------|
|        | `browse()`        | put up a file requester                     |
|        | `browsedir()`     | put up a directory requester                |
|        | `confirm()`       | let the user make a choice                  |
|        | `getchar()`       | get a character from the user               |
|        | `getcharstr()`    | get a character from the user as a string   |
|        | `getcharmod()`    | get modifiers for the last typed character  |
|        | `getmousepos()`   | get last known mouse position               |
|        | `getmouseshape()` | get name of the current mouse shape         |
|        | `echoraw()`       | output characters as-is                     |
|        | `feedkeys()`      | put characters in the typeahead queue       |
|        | `input()`         | get a line from the user                    |
|        | `inputlist()`     | let the user pick an entry from a list      |
|        | `inputsecret()`   | get a line from the user without showing it |
|        | `inputdialog()`   | get a line from the user in a dialog        |
|        | `inputsave()`     | save and clear typeahead                    |
|        | `inputrestore()`  | restore typeahead                           |

## GUI:

See `:help gui-functions`.

| Status | Function            | Description                         |
|--------|---------------------|-------------------------------------|
|        | `getfontname()`     | get name of current font being used |
|        | `getwinpos()`       | position of the Vim window          |
|        | `getwinposx()`      | X position of the Vim window        |
|        | `getwinposy()`      | Y position of the Vim window        |
|        | `balloon_show()`    | set the balloon content             |
|        | `balloon_split()`   | split a message for a balloon       |
|        | `balloon_gettext()` | get the text in the balloon         |

## Vim server

See `:help server-functions`.

| Status | Function               | Description                                  |
|--------|------------------------|----------------------------------------------|
|        | `serverlist()`         | return the list of server names              |
|        | `remote_startserver()` | run a server                                 |
|        | `remote_send()`        | send command characters to a Vim server      |
|        | `remote_expr()`        | evaluate an expression in a Vim server       |
|        | `server2client()`      | send a reply to a client of a Vim server     |
|        | `remote_peek()`        | check if there is a reply from a Vim server  |
|        | `remote_read()`        | read a reply from a Vim server               |
|        | `foreground()`         | move the Vim window to the foreground        |
|        | `remote_foreground()`  | move the Vim server window to the foreground |

## Window size and position

See `:help window-size-functions`.

| Status | Function          | Description                            |
|--------|-------------------|----------------------------------------|
|        | `winheight()`     | get height of a specific window        |
|        | `winwidth()`      | get width of a specific window         |
|        | `win_screenpos()` | get screen position of a window        |
|        | `winlayout()`     | get layout of windows in a tab page    |
|        | `winrestcmd()`    | return command to restore window sizes |
|        | `winsaveview()`   | get view of current window             |
|        | `winrestview()`   | restore saved view of current window   |

## Mappings and Menus

See `:help mapping-functions`.

| Status | Function            | Description                        |
|--------|---------------------|------------------------------------|
|        | `digraph_get()`     | get `digraph`                      |
|        | `digraph_getlist()` | get all `digraph`s                 |
|        | `digraph_set()`     | register `digraph`                 |
|        | `digraph_setlist()` | register multiple `digraph`s       |
|        | `hasmapto()`        | check if a mapping exists          |
|        | `mapcheck()`        | check if a matching mapping exists |
|        | `maparg()`          | get rhs of a mapping               |
|        | `maplist()`         | get list of all mappings           |
|        | `mapset()`          | restore a mapping                  |
|        | `menu_info()`       | get information about a menu item  |
|        | `wildmenumode()`    | check if the wildmode is active    |

## Testing

See `:help test-functions`.

| Status | Function                     | Description                                      |
|--------|------------------------------|--------------------------------------------------|
|        | `assert_equal()`             | assert that two expressions values are equal     |
|        | `assert_equalfile()`         | assert that two file contents are equal          |
|        | `assert_notequal()`          | assert that two expressions values are not equal |
|        | `assert_inrange()`           | assert that an expression is inside a range      |
|        | `assert_match()`             | assert that a pattern matches the value          |
|        | `assert_notmatch()`          | assert that a pattern does not match the value   |
|        | `assert_false()`             | assert that an expression is false               |
|        | `assert_true()`              | assert that an expression is true                |
|        | `assert_exception()`         | assert that a command throws an exception        |
|        | `assert_beeps()`             | assert that a command beeps                      |
|        | `assert_nobeep()`            | assert that a command does not cause a beep      |
|        | `assert_fails()`             | assert that a command fails                      |
|        | `assert_report()`            | report a test failure                            |
|        | `test_alloc_fail()`          | make memory allocation fail                      |
|        | `test_autochdir()`           | enable `'autochdir'` during startup              |
|        | `test_override()`            | test with Vim internal overrides                 |
|        | `test_garbagecollect_now()`  | free memory right now                            |
|        | `test_garbagecollect_soon()` | set a flag to free memory soon                   |
|        | `test_getvalue()`            | get value of an internal variable                |
|        | `test_gui_event()`           | generate a GUI event for testing                 |
|        | `test_ignore_error()`        | ignore a specific error message                  |
|        | `test_mswin_event()`         | generate an MS-Windows event                     |
|        | `test_null_blob()`           | return a null Blob                               |
|        | `test_null_channel()`        | return a null Channel                            |
|        | `test_null_dict()`           | return a null Dict                               |
|        | `test_null_function()`       | return a null Funcref                            |
|        | `test_null_job()`            | return a null Job                                |
|        | `test_null_list()`           | return a null List                               |
|        | `test_null_partial()`        | return a null Partial function                   |
|        | `test_null_string()`         | return a null String                             |
|        | `test_null_tuple()`          | return a null Tuple                              |
|        | `test_settime()`             | set the time Vim uses internally                 |
|        | `test_setmouse()`            | set the mouse position                           |
|        | `test_feedinput()`           | add key sequence to input buffer                 |
|        | `test_option_not_set()`      | reset flag indicating option was set             |
|        | `test_refcount()`            | return an expression's reference count           |
|        | `test_srand_seed()`          | set the seed value for srand()                   |
|        | `test_unknown()`             | return a value with unknown type                 |
|        | `test_void()`                | return a value with void type                    |

## Inter-process communication:

See `:help channel-functions`.

| Status | Function          | Description                             |
|--------|-------------------|-----------------------------------------|
|        | `ch_canread()`    | check if there is something to read     |
|        | `ch_open()`       | open a channel                          |
|        | `ch_close()`      | close a channel                         |
|        | `ch_close_in()`   | close the in part of a channel          |
|        | `ch_read()`       | read a message from a channel           |
|        | `ch_readblob()`   | read a Blob from a channel              |
|        | `ch_readraw()`    | read a raw message from a channel       |
|        | `ch_sendexpr()`   | send a JSON message over a channel      |
|        | `ch_sendraw()`    | send a raw message over a channel       |
|        | `ch_evalexpr()`   | evaluate an expression over channel     |
|        | `ch_evalraw()`    | evaluate a raw string over channel      |
|        | `ch_status()`     | get status of a channel                 |
|        | `ch_getbufnr()`   | get the buffer number of a channel      |
|        | `ch_getjob()`     | get the job associated with a channel   |
|        | `ch_info()`       | get channel information                 |
|        | `ch_log()`        | write a message in the channel log file |
|        | `ch_logfile()`    | set the channel log file                |
|        | `ch_setoptions()` | set the options for a channel           |
|        | `json_encode()`   | encode an expression to a JSON string   |
|        | `json_decode()`   | decode a JSON string to Vim types       |
|        | `js_encode()`     | encode an expression to a JSON string   |
|        | `js_decode()`     | decode a JSON string to Vim types       |
|        | `base64_encode()` | encode a blob into a base64 string      |
|        | `base64_decode()` | decode a base64 string into a blob      |
|        | `err_teapot()`    | give error 418 or 503                   |

## Jobs

See `:help job-functions`.

| Status | Function           | Description                   |
|--------|--------------------|-------------------------------|
|        | `job_start()`      | start a job                   |
|        | `job_stop()`       | stop a job                    |
|        | `job_status()`     | get the status of a job       |
|        | `job_getchannel()` | get the channel used by a job |
|        | `job_info()`       | get information about a job   |
|        | `job_setoptions()` | set options for a job         |

## Signs

See `:help sign-functions`.

| Status | Function             | Description                 |
|--------|----------------------|-----------------------------|
|        | `sign_define()`      | define or update a sign     |
|        | `sign_getdefined()`  | get a list of defined signs |
|        | `sign_getplaced()`   | get a list of placed signs  |
|        | `sign_jump()`        | jump to a sign              |
|        | `sign_place()`       | place a sign                |
|        | `sign_placelist()`   | place a list of signs       |
|        | `sign_undefine()`    | undefine a sign             |
|        | `sign_unplace()`     | unplace a sign              |
|        | `sign_unplacelist()` | unplace a list of signs     |

## Terminal window

See `:help terminal-functions`.

| Status | Function               | Description                                  |
|--------|------------------------|----------------------------------------------|
|        | `term_start()`         | open a terminal window and run a job         |
|        | `term_list()`          | get the list of terminal buffers             |
|        | `term_sendkeys()`      | send keystrokes to a terminal                |
|        | `term_wait()`          | wait for screen to be updated                |
|        | `term_getjob()`        | get the job associated with a terminal       |
|        | `term_scrape()`        | get row of a terminal screen                 |
|        | `term_getline()`       | get a line of text from a terminal           |
|        | `term_getattr()`       | get the value of attribute {what}            |
|        | `term_getcursor()`     | get the cursor position of a terminal        |
|        | `term_getscrolled()`   | get the scroll count of a terminal           |
|        | `term_getaltscreen()`  | get the alternate screen flag                |
|        | `term_getsize()`       | get the size of a terminal                   |
|        | `term_getstatus()`     | get the status of a terminal                 |
|        | `term_gettitle()`      | get the title of a terminal                  |
|        | `term_gettty()`        | get the tty name of a terminal               |
|        | `term_setansicolors()` | set 16 ANSI colors, used for GUI             |
|        | `term_getansicolors()` | get 16 ANSI colors, used for GUI             |
|        | `term_dumpdiff()`      | display difference between two screen dumps  |
|        | `term_dumpload()`      | load a terminal screen dump in a window      |
|        | `term_dumpwrite()`     | dump contents of a terminal screen to a file |
|        | `term_setkill()`       | set signal to stop job in a terminal         |
|        | `term_setrestore()`    | set command to restore a terminal            |
|        | `term_setsize()`       | set the size of a terminal                   |
|        | `term_setapi()`        | set terminal JSON API function name prefix   |

## Popup window

See `:help popup-window-functions`.

| Status | Function               | Description                                                                       |
|--------|------------------------|-----------------------------------------------------------------------------------|
|        | `popup_create()`       | create popup centered in the screen                                               |
|        | `popup_atcursor()`     | create popup just above the cursor position, closes when the cursor moves away    |
|        | `popup_beval()`        | at the position indicated by v:beval_ variables, closes when the mouse moves away |
|        | `popup_notification()` | show a notification for three seconds                                             |
|        | `popup_dialog()`       | create popup centered with padding and border                                     |
|        | `popup_menu()`         | prompt for selecting an item from a list                                          |
|        | `popup_hide()`         | hide a popup temporarily                                                          |
|        | `popup_show()`         | show a previously hidden popup                                                    |
|        | `popup_move()`         | change the position and size of a popup                                           |
|        | `popup_setoptions()`   | override options of a popup                                                       |
|        | `popup_settext()`      | replace the popup buffer contents                                                 |
|        | `popup_setbuf()`       | set the popup buffer                                                              |
|        | `popup_close()`        | close one popup                                                                   |
|        | `popup_clear()`        | close all popups                                                                  |
|        | `popup_filter_menu()`  | select from a list of items                                                       |
|        | `popup_filter_yesno()` | block until 'y' or 'n' is pressed                                                 |
|        | `popup_getoptions()`   | get current options for a popup                                                   |
|        | `popup_getpos()`       | get actual position and size of a popup                                           |
|        | `popup_findecho()`     | get window ID for popup used for `:echowindow`                                    |
|        | `popup_findinfo()`     | get window ID for popup info window                                               |
|        | `popup_findpreview()`  | get window ID for popup preview window                                            |
|        | `popup_list()`         | get list of all popup window IDs                                                  |
|        | `popup_locate()`       | get popup window ID from its screen position                                      |

## Timers

See `:help timer-functions`.

| Status | Function          | Description                  |
|--------|-------------------|------------------------------|
|        | `timer_start()`   | create a timer               |
|        | `timer_pause()`   | pause or unpause a timer     |
|        | `timer_stop()`    | stop a timer                 |
|        | `timer_stopall()` | stop all timers              |
|        | `timer_info()`    | get information about timers |

## Tags

See `:help tag-functions`.

| Status | Function        | Description                      |
|--------|-----------------|----------------------------------|
|        | `taglist()`     | get list of matching tags        |
|        | `tagfiles()`    | get a list of tags files         |
|        | `gettagstack()` | get the tag stack of a window    |
|        | `settagstack()` | modify the tag stack of a window |

## Prompt Buffer

See `:help promptbuffer-functions`.

| Status | Function                | Description                                |
|--------|-------------------------|--------------------------------------------|
|        | `prompt_getprompt()`    | get the effective prompt text for a buffer |
|        | `prompt_setcallback()`  | set prompt callback for a buffer           |
|        | `prompt_setinterrupt()` | set interrupt callback for a buffer        |
|        | `prompt_setprompt()`    | set the prompt text for a buffer           |

## Registers

See `:help register-functions`.

| Status | Function          | Description                                    |
|--------|-------------------|------------------------------------------------|
|        | `getreg()`        | get contents of a register                     |
|        | `getreginfo()`    | get information about a register               |
|        | `getregtype()`    | get type of a register                         |
|        | `setreg()`        | set contents and type of a register            |
|        | `reg_executing()` | return the name of the register being executed |
|        | `reg_recording()` | return the name of the register being recorded |

## Text Properties

See `:help text-property-functions`.

| Status | Function             | Description                                |
|--------|----------------------|--------------------------------------------|
|        | `prop_add()`         | attach a property at a position            |
|        | `prop_add_list()`    | attach a property at multiple positions    |
|        | `prop_clear()`       | remove all properties from a line or lines |
|        | `prop_find()`        | search for a property                      |
|        | `prop_list()`        | return a list of all properties in a line  |
|        | `prop_remove()`      | remove a property from a line              |
|        | `prop_type_add()`    | add/define a property type                 |
|        | `prop_type_change()` | change properties of a type                |
|        | `prop_type_delete()` | remove a text property type                |
|        | `prop_type_get()`    | return the properties of a type            |
|        | `prop_type_list()`   | return a list of all property types        |

## Sound

See `:help sound-functions`.

| Status | Function            | Description             |
|--------|---------------------|-------------------------|
|        | `sound_clear()`     | stop playing all sounds |
|        | `sound_playevent()` | play an event's sound   |
|        | `sound_playfile()`  | play a sound file       |
|        | `sound_stop()`      | stop playing a sound    |

## Various

See `:help various-functions`.

| Status | Function              | Description                                    |
|--------|-----------------------|------------------------------------------------|
|        | `mode()`              | get current editing mode                       |
|        | `state()`             | get current busy state                         |
|        | `visualmode()`        | last visual mode used                          |
| ☑️     | `exists()`            | check if a variable, function, etc. exists     |
|        | `exists_compiled()`   | like exists() but check at compile time        |
| ☑️     | `has()`               | check if a feature is supported in Vim         |
|        | `changenr()`          | return number of most recent change            |
|        | `cscope_connection()` | check if a cscope connection exists            |
|        | `did_filetype()`      | check if a FileType autocommand was used       |
|        | `diff()`              | diff two Lists of strings                      |
|        | `eventhandler()`      | check if invoked by an event handler           |
|        | `getcellpixels()`     | get List of cell pixel size                    |
|        | `getpid()`            | get process ID of Vim                          |
|        | `getscriptinfo()`     | get list of sourced vim scripts                |
|        | `getimstatus()`       | check if IME status is active                  |
|        | `interrupt()`         | interrupt script execution                     |
|        | `windowsversion()`    | get MS-Windows version                         |
|        | `terminalprops()`     | properties of the terminal                     |
|        | `libcall()`           | call a function in an external library         |
|        | `libcallnr()`         | idem, returning a number                       |
|        | `undofile()`          | get the name of the undo file                  |
|        | `undotree()`          | return the state of the undo tree for a buffer |
|        | `shiftwidth()`        | effective value of 'shiftwidth'                |
|        | `wordcount()`         | get byte/word/char count of buffer             |
|        | `id()`                | get unique string for item to use as a key     |
|        | `luaeval()`           | evaluate `Lua` expression                      |
|        | `mzeval()`            | evaluate `MzScheme` expression                 |
|        | `perleval()`          | evaluate Perl expression (`+perl`)             |
|        | `py3eval()`           | evaluate Python expression (`+python3`)        |
|        | `pyeval()`            | evaluate Python expression (`+python`)         |
|        | `pyxeval()`           | evaluate `python_x` expression                 |
|        | `rubyeval()`          | evaluate `Ruby` expression                     |
|        | `debugbreak()`        | interrupt a program being debugged             |

MacVim-specific functions

| Status | Function           | Description                                    |
|--------|--------------------|------------------------------------------------|
|        | `showdefinition()` | look up and show definition of provided string |
