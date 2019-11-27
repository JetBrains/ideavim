List of Supported Set Commands
==============================

The following `:set` commands can appear in `~/.ideavimrc` or be set manually in the command mode:

    'clipboard'      'cb'       clipboard options
                                Standard clipboard options plus 
                                
                                `ideaput`  (default on)   - IdeaVim ONLY
                                enable native idea paste action for put operations
    
    'digraph'        'dg'       enable the entering of digraphs in Insert mode
    'gdefault'       'gd'       the ":substitute" flag 'g' is by default
    'history'        'hi'       number of command-lines that are remembered
    'hlsearch'       'hls'      highlight matches with the last search pattern
    'ignorecase'     'ic'       ignore case in search patterns
    'iskeyword'      'isk'      defines keywords for commands like 'w', '*', etc.
    'incsearch'      'is'       show where search pattern typed so far matches
    
    `keymodel`       `km`       String (default "continueselect,stopselect")

                 List of comma separated words, which enable special things that keys
                 can do. These values can be used:
                   startsel           Using a shifted special[1] key starts selection (either
                                      Select mode or Visual mode, depending on "key" being
                                      present in 'selectmode').
                   stopsel            Using a NOT-shifted special[1] key stops selection.
                                      Automatically enables `stopselect` and `stopvisual`
                   stopselect         Using a NOT-shifted special[1] key stops     - IdeaVim ONLY
                                      select mode and removes selection.
                   stopvisual         Using a NOT-shifted special[1] key stops     - IdeaVim ONLY
                                      visual mode and removes selection.
                   continueselect     Using a shifted arrow key doesn't       - IdeaVim ONLY
                                      start selection, but in select mode
                                      acts like startsel is enabled
                   continuevisual     Using a shifted arrow key doesn't       - IdeaVim ONLY
                                      start selection, but in visual mode
                                      acts like startsel is enabled
    
    'matchpairs'     'mps'   pairs of characters that "%" can match
    'nrformats'      'nf'    number formats recognized for CTRL-A command
    'number'         'nu'    print the line number in front of each line
    'relativenumber' 'rnu'   show the line number relative to the line with
                             the cursor
    'scroll'         'scr'   lines to scroll with CTRL-U and CTRL-D
    'scrolljump'     'sj'    minimum number of lines to scroll
    'scrolloff'      'so'    minimum number of lines above and below the cursor
    'selection'      'sel'   what type of selection to use

    `selectmode`     `slm`   String (default "")

                 This is a comma-separated list of words, which specify when to start
                 Select mode instead of Visual mode, when a selection is started.
                 Possible values:
                   mouse           when using the mouse
                   key             when using shifted special[1] keys
                   cmd             when using "v", "V", or <C-V>
                   ideaselection   when IDE sets a selection      - IdeaVim ONLY
                                   (examples: extend selection, wrap with while, etc.)

                   Deprecated values:
                   template        ~~please use `idearefactormode` option~~
                                   when starting a template       - IdeaVim ONLY
                   refactoring     ~~please use ideaselection~~
                                   same as ideaselection          - IdeaVim ONLY

    'showmode'       'smd'       message on the status line to show current mode
    'sidescroll'     'ss'        minimum number of columns to scroll horizontally
    'sidescrolloff'  'siso'      min. number of columns to left and right of cursor
    'smartcase'      'scs'       no ignore case when pattern is uppercase
    'timeout'        'to'        use timeout for mapped key sequences
    'timeoutlen'     'tm'        timeout duration for a mapped key sequence
    'undolevels'     'ul'        maximum number of changes that can be undone
    'viminfo'        'vi'        information to remember after restart
    'visualbell'     'vb'        use visual bell instead of beeping
    'wrapscan'       'ws'        searches wrap around the end of file
    
    
    
    IdeaVim only commands:

    `ideamarks`      `ideamarks` Boolean (default true)
    
                     If true, creation of global mark will trigger creation of IDE's bookmark
                     and vice versa.
                     
    `idearefactormode` `idearefactormode` String(default "select")
    
                 Define the mode that would be enabled during
                 the refactoring (renaming, live template, introduce variable, etc)
                 
                 Use one of the following values:
                 - keep  - keep the mode that was enabled before starting a refactoring
                 - select - start refactoring in select mode
                 - visual - start refactoring in visual mode
                 
                 This option has effect if you are in normal, insert or replace mode before refactoring start.
                 Visual or select mode are not changed.
    
    
    `ideajoin`      `ideajoin` Boolean (default false)
    
                  If true, join command will be performed via IDE
                  See wiki/`ideajoin` examples

    `ideastatusbar`  `ideastatusbar` Boolean (default true)    [To Be Released]
    
                  If false, IdeaVim icon won't be shown in the status bar.
                  Works only from `~/.ideavimrc` after the IDE restart.

    ----------
    [1] - cursor keys, <End>, <Home>, <PageUp> and <PageDown>
