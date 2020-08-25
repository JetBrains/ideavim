Emulated Vim Plugins
--------------------

IdeaVim extensions emulate plugins of the original Vim. In order to use
IdeaVim extensions, you have to enable them via this command in your `~/.ideavimrc`:

    set <extension-name>

Available extensions:

##  easymotion

* Setup:  
    * Install [IdeaVim-EasyMotion](https://plugins.jetbrains.com/plugin/13360-ideavim-easymotion/)
    and [AceJump](https://plugins.jetbrains.com/plugin/7086-acejump/) plugins.
    * `set easymotion`
* Emulates [vim-easymotion](https://github.com/easymotion/vim-easymotion)
* Commands: All commands with the mappings are supported. See the [full list of supported commands](https://github.com/AlexPl292/IdeaVim-EasyMotion#supported-commands).

##  surround

* Setup: `set surround`
* Emulates [vim-surround](https://github.com/tpope/vim-surround)
* Commands: `ys`, `cs`, `ds`, `S`

## multiple-cursors

* Setup: `set multiple-cursors`
* Emulates [vim-multiple-cursors](https://github.com/terryma/vim-multiple-cursors)
* Commands: `<A-n>`, `<A-x>`, `<A-p>`, `g<A-n>`

## commentary

* Setup: `set commentary`
* Emulates [commentary.vim](https://github.com/tpope/vim-commentary)
* Commands: `gcc`, `gc + motion`, `v_gc`
* By [Daniel Leong](https://github.com/dhleong)

## ReplaceWithRegister

* Setup: `set ReplaceWithRegister`
* Emulates [ReplaceWithRegister](https://github.com/vim-scripts/ReplaceWithRegister)
* Commands: `gr`, `grr`
* By [igrekster](https://github.com/igrekster)

## argtextobj

* Setup:
    * `set argtextobj`
    * By default, only the arguments inside parenthesis are considered. To extend the functionality
      to other types of brackets, set `g:argtextobj_pairs` variable to a comma-separated
      list of colon-separated pairs (same as VIM's `matchpairs` option), like
      `let g:argtextobj_pairs="(:),{:},<:>"`. The order of pairs matters when
      handling symbols that can also be operators: `func(x << 5, 20) >> 17`. To handle
      this syntax parenthesis, must come before angle brackets in the list.
* Emulates [argtextobj.vim](https://www.vim.org/scripts/script.php?script_id=2699)
* Additional text objects: `aa`, `ia`

## exchange

* Setup: `set exchange`
* Emulates [vim-exchange](https://github.com/tommcdo/vim-exchange)
* Commands: `cx`, `cxx`, `X`, `cxc`
* By [fan-tom](https://github.com/fan-tom)

## textobj-entire

* Setup: `set textobj-entire`
* Emulates [vim-textobj-entire](https://github.com/kana/vim-textobj-entire)
* Additional text objects: `ae`, `ie`
* By [Alexandre Grison](https://github.com/agrison)

## highlightedyank

* Setup: 
    * `set highlightedyank`
    * if you want to optimize highlight duration, assign a time in milliseconds:  
       `let g:highlightedyank_highlight_duration = "1000"`  
       A negative number makes the highlight persistent.  
       `let g:highlightedyank_highlight_duration = "-1"`
     * if you want to change background color of highlight you can provide the rgba of the color you want e.g.  
       `let g:highlightedyank_highlight_color = "rgba(160, 160, 160, 155)"`
* Emulates [vim-highlightedyank](https://github.com/machakann/vim-highlightedyank)
* By [KostkaBrukowa](https://github.com/KostkaBrukowa)
