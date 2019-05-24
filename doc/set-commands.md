List of Supported Set Commands
==============================

The following `:set` commands can appear in `~/.ideavimrc` or set manually in the command mode:

    'clipboard'      'cb'    clipboard options
    'digraph'        'dg'    enable the entering of digraphs in Insert mode
    'gdefault'       'gd'    the ":substitute" flag 'g' is default on
    'history'        'hi'    number of command-lines that are remembered
    'hlsearch'       'hls'   highlight matches with last search pattern
    'ignorecase'     'ic'    ignore case in search patterns
    'iskeyword'      'isk'   defines keywords for commands like 'w', '*', etc.
    'incsearch'      'is'    show where search pattern typed so far matches
    
    `keymodel`       `km`    String (default "continueselect,stopselect")   [To Be Released]

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
    'scrolloff'      'so'    minimum nr. of lines above and below cursor
    'selection'      'sel'   what type of selection to use

    `selectmode`     `slm`   String (default "template")     [To Be Released]

                 This is a comma separated list of words, which specifies when to start
                 Select mode instead of Visual mode, when a selection is started.
                 Possible values:
                   mouse           when using the mouse
                   key             when using shifted special[1] keys
                   cmd             when using "v", "V" or <C-V>
                   template        when template starting        - IdeaVim ONLY
                   refactoring     when refactoring without      - IdeaVim ONLY
                                         template performed

    'showmode'       'smd'   message on status line to show current mode
    'sidescroll'     'ss'    minimum number of columns to scroll horizontal
    'sidescrolloff'  'siso'  min. nr. of columns to left and right of cursor
    'smartcase'      'scs'   no ignore case when pattern has uppercase
    'timeout'        'to'    use timeout for mapped key sequences
    'timeoutlen'     'tm'    time that is waited for a mapped key sequence
    'undolevels'     'ul'    maximum number of changes that can be undone
    'viminfo'        'vi'    information to remember after restart
    'visualbell'     'vb'    use visual bell instead of beeping
    'wrapscan'       'ws'    searches wrap around the end of the file

    ----------
    [1] - cursor keys, <End>, <Home>, <PageUp> and <PageDown>