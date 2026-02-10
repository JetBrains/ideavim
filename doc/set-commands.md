List of Supported Options
=========================

The following options can be set with the `:set`, `:setglobal` and `:setlocal` commands.
They can be added to the `~/.ideavimrc` file, or set manually in Command-line mode.
For more details of each option, please see the Vim documentation.
Every effort is made to make these options compatible with Vim behaviour.
However, some differences are inevitable.

```
'clipboard'     'cb'    Defines clipboard behavior
        A comma-separated list of words to control clipboard behaviour:
           unnamed      The clipboard register '*' is used instead of the
                        unnamed register
           unnamedplus  The clipboard register '+' is used instead of the
                        unnamed register
           ideaput      Uses the IDEs own paste implementation for put
                        operations rather than simply inserting the text

'digraph'       'dg'    Enable using <BS> to enter digraphs in Insert mode
'gdefault'      'gd'    The ":substitute" flag 'g' is by default
'guicursor'     'gcr'   Controls the shape of the cursor for different modes
'history'       'hi'    Number of command-lines that are remembered
'hlsearch'      'hls'   Highlight matches with the last search pattern
'ignorecase'    'ic'    Ignore case in search patterns
'incsearch'     'is'    Show where search pattern typed so far matches
'iskeyword'     'isk'   Defines keywords for commands like 'w', '*', etc.
'keymodel'      'km'    Controls selection behaviour with special keys
        List of comma separated words, which enable special things that keys
        can do. These values can be used:
           startsel     Using a shifted special key starts selection (either
                        Select mode or Visual mode, depending on "key" being
                        present in 'selectmode')
           stopsel      Using a NOT-shifted special key stops selection.
                        Automatically enables `stopselect` and `stopvisual`
           stopselect   Using a NOT-shifted special key stops select mode
                        and removes selection - IdeaVim ONLY
           stopvisual   Using a NOT-shifted special key stops visual mode
                        and removes selection - IdeaVim ONLY
           continueselect   Using a shifted arrow key doesn't start selection,
                        but in select mode acts like startsel is enabled
                        - IdeaVim ONLY
           continuevisual   Using a shifted arrow key doesn't start selection,
                        but in visual mode acts like startsel is enabled
                        - IdeaVim ONLY
                                
        Special keys in this context are the cursor keys, <End>, <Home>,
        <PageUp> and <PageDown>.

'matchpairs'    'mps'   Pairs of characters that "%" can match
'maxmapdepth'   'mmd'   Maximum depth of mappings
'more'          'more'  When on, listings pause when the whole screen is filled
'nrformats'     'nf'    Number formats recognized for CTRL-A command
'operatorfunc'  'opfunc'    Name of a function to call with the g@ operator
'scroll'        'scr'   Number of lines to scroll with CTRL-U and CTRL-D
'selection'     'sel'   What type of selection to use
'selectmode'    'slm'   Controls when to start Select mode instead of Visual
        This is a comma-separated list of words:
                        
           mouse        When using the mouse
           key          When using shifted special[1] keys
           cmd          When using "v", "V", or <C-V>
           ideaselection    When IDE sets a selection - IdeaVim ONLY
                        (e.g.: extend selection, wrap with while, etc.)

'shell'         'sh'    The shell to use to execute commands with ! and :!
'shellcmdflag'  'shcf'  The command flag passed to the shell
'shellxescape'  'sxe'   The characters to be escaped when calling a shell
'shellxquote'   'sxq'   The quote character to use in a shell command
'showcmd'       'sc'    Show (partial) command in the status bar
'showmode'      'smd'   Show the current mode in the status bar
'smartcase'     'scs'   Use case sensitive search if any character in the
                        pattern is uppercase
'startofline'   'sol'   When on, some commands move the cursor to the first
                        non-blank of the line
                        When off, the cursor is kept in the same column
                        (if possible)
'timeout'       'to'    Use timeout for mapped key sequences
'timeoutlen'    'tm'    Timeout duration for a mapped key sequence
'viminfo'       'vi'    Information to remember after restart
'virtualedit'   've'    Placement of the cursor where there is no actual text
        A comma-separated list of these words:
           block        Allow virtual editing in Visual mode (not supported)
           insert       Allow virtual editing in Insert mode (not supported)
           all          Allow virtual editing in all modes (not supported)
           onemore      Allow the cursor to move just past the end of the line

'visualbell'    'vb'    When on, prevents beeping on error
'whichwrap'     'ww'    Which keys that move the cursor left/right can wrap to
                        other lines
        A comma-separated list of these flags:
           char key     modes
           b    <BS>    Normal and Visual
           s    <Space> Normal and Visual
           h    "h"     Normal and Visual
           l    "l"     Normal and Visual
           <    <Left>  Normal and Visual
           >    <Right> Normal and Visual
           ~    "~"     Normal
           [    <Left>  Insert and Replace
           ]    <Right> Insert and Replace

'wrapscan'      'ws'    Search will wrap around the end of file
```

## IdeaVim options mapped to IntelliJ-based IDE settings

IdeaVim provides its own implementation for handling scroll jump and offset, even though IntelliJ-based IDEs have similar functionality (there are differences in behaviour).
When IdeaVim is hosted in an IntelliJ-based IDE (but not JetBrains Fleet), the following options map to the equivalent IDE settings:

```
'scrolljump'    'sj'    Minimal number of lines to scroll
'scrolloff'     'so'    Minimal number of lines above and below the cursor
'sidescroll'    'ss'    Minimal number of columns to scroll horizontally
'sidescrolloff' 'siso'  Minimal number of columns to left and right of cursor
```

## IdeaVim options for IntelliJ-based IDE features

Some Vim features cannot be implemented by IdeaVim, and must be implemented by the host IDE, such as showing whitespace and line numbers, and enabling soft-wrap.
The following options modify equivalent settings and features implemented by IntelliJ-based IDEs.

There is some mismatch when trying to map Vim options, most of which are local options, to IDE settings, which are mostly global-local.
The Vim option will always reflect the effective value of the IDE setting for the current editor, and modifying the Vim option will update the local value of the IDE setting.
The default value of the Vim option set during startup is not passed to the IDE setting.

If the IDE setting has a way to modify the local value, such as entries in the _View | Active Editor_ menu, then changing this will update the current editor and be reflected in the Vim option value.
If the IDE setting can only modify its global setting in the main _Settings_ dialog, this change does not always update the current editor (because the local IDE setting has been modified and takes precedence).

IdeaVim tries to make this work more naturally by updating the editor and local Vim option when a global value changes unless the Vim option has been explicitly set in Command-line mode.

In other words, if the local Vim value is explicitly set for a window or buffer, interactively, then it should not be reset.
If the Vim option was explicitly set in `~/.ideavimrc` however, then the value will be reset, because this can be viewed as a "global" value - set once and applied to subsequently opened windows.
(This should not be confused with Vim's concept of global options, which are mainly used to initialise new windows.)

The local Vim option can always be reset to the global IDE setting value by resetting the Vim option to default with the `:set {option}&` syntax.

```
'bomb'              'bomb'  Add or remove a byte order mark (BOM) to the
                            current file. Unlike Vim, the file is modified
                            immediately, and not when saved
'breakindent'       'bri'   Indent soft wrapped lines to match the first
                            line's indent
'colorcolumn'       'cc'    Maps to IntelliJ's visual guide columns
'cursorline'        'cul'   Highlight the line containing the cursor
'fileencoding'      'fenc'  Change the encoding of the current file. The file
                            is modified and written immediately, rather than
                            waiting to be saved
                            Note that the names of the encoding might not
                            match Vim's known names
'fileformat'        'ff'    Change the file format - dos, unix or mac
                            The file is modified immediately, rather than
                            when saved
'list'              'list'  Show whitespace. Maps to the editor's local
                            setting in the View | Active Editor menu
'number'            'nu'    Show line numbers. Maps to the editor's local
                            setting in the View | Active Editor menu
'relativenumber'    'rnu'   Show line numbers relative to the current line
'textwidth'         'tw'    Set the column at which text is automatically
                            wrapped
'wrap'              'wrap'  Enable soft-wraps. Maps to the editor's local
                            setting in the View | Active Editor menu
```

## IdeaVim only options

These options are IdeaVim only, and not supported by Vim.
They control integration with the host IDE.
Unless otherwise stated, these options do not have abbreviations.

```
'ideacopypreprocess'    boolean (default off)
                        global or local to buffer
        When enabled, the IDE will run custom copy pre-processors over text
        copied to registers. These pre-processors can perform transformations
        on the text, such as converting escape characters in a string literal
        into the actual control characters in a Java file.

        This is not usually the expected behaviour, so this option's default
        value is off. The equivalent processing for paste is controlled by the
        "ideaput" value to the 'clipboard' option.

'ideajoin'              boolean (default off)
                        global or local to buffer
        When enabled, join commands will be handled by the IDE's "smart join"
        feature. The IDE can change syntax when joining lines, such as merging
        string literals or if statements. See the wiki for more examples. Not
        all languages support smart join functionality.

'ideamarks'             boolean (default on)
                        global
        Maps Vim's global marks to IDE bookmarks.

'idearefactormode'      string  (default "select")
                        global or local to buffer
        Specifies the mode to be used when a refactoring selects text to be
        edited (e.g. renaming, live template fields, introduce variable, etc):
           keep         Keep the current mode
           select       Switch to Select mode
           visual       Switch to Visual mode

        This option is only used when the refactoring is started in Normal,
        Insert or Replace mode. Visual or Select modes are not changed.

'ideastatusicon'        string  (default "enabled")
                        global
        This option controls the behaviour and appearance of the IdeaVim icon
        in the status bar:
           enabled      Show the icon in the status bar
           gray         Show the gray version of the icon
           disabled     Hide the icon

'ideavimsupport'        string  (default "dialog")
                        global
        A comma-separated list of additional buffers or locations where
        IdeaVim should be enabled:
           dialog       Enable IdeaVim in editors hosted in dialogs
           singleline   Enable IdeaVim in single line editors (not recommended)

        The IDE's editor component can be used in many places, such as VCS
        commit tool window, or inside dialogs, and even as single line fields.

'ideawrite'             string  (default "all")
                        global
        This option defines the behaviour of the :w command:
           file         Save the current file only
           all          The :w command works like :wa and invokes the Save All
                        IDE action. This allows options such as "Prettier on
                        save" or "ESlint on save" to work with the :w command,
                        but means all files are saved.

'lookupkeys'            string  (default "<Tab>,<Down>,<Up>,<Enter>,
                                          <Left>,<Right>,<C-Down>,<C-Up>,
                                          <PageUp>,<PageDown>, <C-J>,<C-Q>")
                        global
        Comma-separated list of keys that should be processed by the IDE while
        a code completion lookup popup is active. For example, <Tab> and
        <Enter> are used by the IDE to complete the lookup and insert text,
        but <C-W> should be passed IdeaVim to continue editing the text.

'trackactionids'        boolean (default off)
                        global
        When on, IdeaVim will try to track the current IDE action and display
        the action name in a notification. This action ID can then be used in
        a mapping to the action in the form <Action>(...).

'visualdelay'           number  (default 100)
                        global
        This option specifies the delay, in milliseconds before converting an
        IDE selection into Visual mode.

        Some IDE features make a selection to help modify text (e.g. backspace
        in Python or Yaml selects an indent and invokes the "remove selection"
        action). IdeaVim listens for changes in selection to switch to Visual
        mode, and will return to Normal mode when the selection is removed,
        even if originally in Insert mode.

        By waiting before converting to Visual mode, temporary selections can
        be ignored and the current Vim mode maintained.

        It is not expected that this value will need to be changed.
```
