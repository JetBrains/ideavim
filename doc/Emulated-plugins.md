Emulated Vim Plugins
--------------------

IdeaVim extensions emulate plugins of the original Vim. In order to use
IdeaVim extensions, you have to enable them via this command in your `~/.ideavimrc`:

```
Plug '<extension-github-reference>'
```

If you reuse your existing `.vimrc` file using `source ~/.vimrc`, IdeaVim can parse and enable plugins that are defined
using [vim-plug](https://github.com/junegunn/vim-plug) or [vundle](https://github.com/VundleVim/Vundle.vim).
No additional set commands in `~/.ideavimrc` are required.  
If you'd like to disable some plugin that's enabled in `.vimrc`, you can use `set no<extension-name>`
in `~/.ideavimrc`. E.g. `set nosurround`.

Available extensions:

## easymotion

* Setup:
    * Install [IdeaVim-EasyMotion](https://plugins.jetbrains.com/plugin/13360-ideavim-easymotion/)
      and [AceJump](https://plugins.jetbrains.com/plugin/7086-acejump/) plugins.
    * `Plug 'easymotion/vim-easymotion'`
    * <details>
      <summary>Alternative syntax</summary>
      <code>Plug 'https://github.com/easymotion/vim-easymotion'</code>
      <br/>
      <code>Plug 'vim-easymotion'</code>
      <br/>
      <code>set easymotion</code>
      </details>
* Emulates [vim-easymotion](https://github.com/easymotion/vim-easymotion)
* Commands: All commands with the mappings are supported. See the [full list of supported commands](https://github.com/AlexPl292/IdeaVim-EasyMotion#supported-commands).

## NERDTree
* Setup: `Plug 'preservim/nerdtree'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/preservim/nerdtree'</code>
      <br/>
      <code>Plug 'nerdtree'</code>
      <br/>
      <code>set NERDTree</code>
      </details>
* Emulates [NERDTree](https://github.com/preservim/nerdtree)
* Commands: [[see here|NERDTree-support]]

## surround

* Setup: `Plug 'tpope/vim-surround'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/tpope/vim-surround'</code>
      <br/>
      <code>Plug 'vim-surround'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=1697'</code>
      <br/>
      <code>set surround</code>
      </details>
* Emulates [vim-surround](https://github.com/tpope/vim-surround)
* Commands: `ys`, `cs`, `ds`, `S`

## multiple-cursors

* Setup: `Plug 'terryma/vim-multiple-cursors'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/terryma/vim-multiple-cursors'</code>
      <br/>
      <code>Plug 'vim-multiple-cursors'</code>
      <br/>
      <code>set multiple-cursors</code>
      </details>
* Emulates [vim-multiple-cursors](https://github.com/terryma/vim-multiple-cursors)
* Commands: `<A-n>`, `<A-x>`, `<A-p>`, `g<A-n>`

## commentary

* Setup: `Plug 'tpope/vim-commentary'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/tpope/vim-commentary'</code>
      <br/>
      <code>Plug 'vim-commentary'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=3695'</code>
      <br/>
      <code>Plug 'tomtom/tcomment_vim'</code>
      <br/>
      <code>Plug 'tcomment_vim'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=1173'</code>
      <br/>
      <code>set commentary</code>
      </details>
* Emulates [commentary.vim](https://github.com/tpope/vim-commentary)
* Commands: `gcc`, `gc + motion`, `v_gc`
* By [Daniel Leong](https://github.com/dhleong)

## ReplaceWithRegister

* Setup: `Plug 'vim-scripts/ReplaceWithRegister'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/vim-scripts/ReplaceWithRegister'</code>
      <br/>
      <code>Plug 'ReplaceWithRegister'</code>
      <br/>
      <code>Plug 'https://github.com/inkarkat/vim-ReplaceWithRegister'</code>
      <br/>
      <code>Plug 'inkarkat/vim-ReplaceWithRegister'</code>
      <br/>
      <code>Plug 'vim-ReplaceWithRegister'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=2703'</code>
      <br/>
      <code>set ReplaceWithRegister</code>
      </details>
* Emulates [ReplaceWithRegister](https://github.com/vim-scripts/ReplaceWithRegister)
* Commands: `gr`, `grr`
* By [igrekster](https://github.com/igrekster)

## argtextobj

* Setup:
    * `Plug 'vim-scripts/argtextobj.vim'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/vim-scripts/argtextobj.vim'</code>
      <br/>
      <code>Plug 'argtextobj.vim'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=2699'</code>
      <br/>
      <code>set argtextobj</code>
      </details>
    * By default, only the arguments inside parenthesis are considered. To extend the functionality
      to other types of brackets, set `g:argtextobj_pairs` variable to a comma-separated
      list of colon-separated pairs (same as VIM's `matchpairs` option), like
      `let g:argtextobj_pairs="(:),{:},<:>"`. The order of pairs matters when
      handling symbols that can also be operators: `func(x << 5, 20) >> 17`. To handle
      this syntax parenthesis, must come before angle brackets in the list.
* Emulates [argtextobj.vim](https://www.vim.org/scripts/script.php?script_id=2699)
* Additional text objects: `aa`, `ia`

## exchange

* Setup: `Plug 'tommcdo/vim-exchange'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/tommcdo/vim-exchange'</code>
      <br/>
      <code>Plug 'vim-exchange'</code>
      <br/>
      <code>set exchange</code>
      </details>
* Emulates [vim-exchange](https://github.com/tommcdo/vim-exchange)
* Commands: `cx`, `cxx`, `X`, `cxc`
* By [fan-tom](https://github.com/fan-tom)

## textobj-entire

* Setup: `Plug 'kana/vim-textobj-entire'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/kana/vim-textobj-entire'</code>
      <br/>
      <code>Plug 'vim-textobj-entire'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=2610'</code>
      <br/>
      <code>set textobj-entire</code>
      </details>
* Emulates [vim-textobj-entire](https://github.com/kana/vim-textobj-entire)
* Additional text objects: `ae`, `ie`
* By [Alexandre Grison](https://github.com/agrison)

## highlightedyank

* Setup:
    * `Plug 'machakann/vim-highlightedyank'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/machakann/vim-highlightedyank'</code>
      <br/>
      <code>Plug 'vim-highlightedyank'</code>
      <br/>
      <code>set highlightedyank</code>
      </details>
    * if you want to optimize highlight duration, assign a time in milliseconds:  
      `let g:highlightedyank_highlight_duration = "1000"`  
      A negative number makes the highlight persistent.  
      `let g:highlightedyank_highlight_duration = "-1"`
    * if you want to change background color of highlight you can provide the rgba of the color you want e.g.  
      `let g:highlightedyank_highlight_color = "rgba(160, 160, 160, 155)"`
* Emulates [vim-highlightedyank](https://github.com/machakann/vim-highlightedyank)
* By [KostkaBrukowa](https://github.com/KostkaBrukowa)

## vim-paragraph-motion

* Setup: `Plug 'dbakker/vim-paragraph-motion'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/dbakker/vim-paragraph-motion'</code>
      <br/>
      <code>Plug 'vim-paragraph-motion'</code>
      <br/>
      <code>Plug 'https://github.com/vim-scripts/Improved-paragraph-motion'</code>
      <br/>
      <code>Plug 'vim-scripts/Improved-paragraph-motion'</code>
      <br/>
      <code>Plug 'Improved-paragraph-motion'</code>
      <br/>
      <code>set vim-paragraph-motion</code>
      </details>
* Emulates [vim-paragraph-motion](https://github.com/dbakker/vim-paragraph-motion)

## vim-indent-object

* Setup: `Plug 'michaeljsmith/vim-indent-object'`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'https://github.com/michaeljsmith/vim-indent-object'</code>
      <br/>
      <code>Plug 'vim-indent-object'</code>
      <br/>
      <code>set textobj-indent</code>
      </details>
* Emulates [vim-indent-object](https://github.com/michaeljsmith/vim-indent-object)
* Additional text objects: `ai`, `ii`, `aI`
* By [Shrikant Sharat Kandula](https://github.com/sharat87)

## matchit.vim

* Setup: `packadd matchit`
    * <details>
      <summary>Alternative vim-plug / vundle syntax</summary>
      <code>Plug 'vim-matchit'</code>
      <br/>
      <code>Plug 'chrisbra/matchit'</code>
      <br/>
      <code>set matchit</code>
      </details>
* Emulates [matchit.vim](https://github.com/chrisbra/matchit)
* Currently works for HTML/XML and ruby
* By [Martin Yzeiri](https://github.com/myzeiri)
