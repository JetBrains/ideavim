IdeaVim Plugins
--------------------

IdeaVim plugins work like the original Vim plugins. If you want to turn any of them on, you have to enable it via this command in your `~/.ideavimrc`:

```
Plug '<plugin-github-reference>'
```

If you reuse your existing `.vimrc` file using `source ~/.vimrc`, IdeaVim can parse and enable plugins that are defined
using [vim-plug](https://github.com/junegunn/vim-plug) or [vundle](https://github.com/VundleVim/Vundle.vim).
No additional set commands in `~/.ideavimrc` are required.  
If you'd like to disable some plugin that's enabled in `.vimrc`, you can use `set no<extension-name>`
in `~/.ideavimrc`. E.g. `set nosurround`.

Available plugins:

<details>
<summary><h2>easymotion</h2></summary>
   
Original plugin: [vim-easymotion](https://github.com/easymotion/vim-easymotion).
   
### Setup:
- Install [IdeaVim-EasyMotion](https://plugins.jetbrains.com/plugin/13360-ideavim-easymotion/)
      and [AceJump](https://plugins.jetbrains.com/plugin/7086-acejump/) plugins.
- Add the following command to `~/.ideavimrc`: `Plug 'easymotion/vim-easymotion'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'easymotion/vim-easymotion'</code>
      <br/>
      <code>Plug 'https://github.com/easymotion/vim-easymotion'</code>
      <br/>
      <code>Plug 'vim-easymotion'</code>
      <br/>
      <code>set easymotion</code>
      </details>
   
### Instructions
   
All commands with the mappings are supported. See the [full list of supported commands](https://github.com/AlexPl292/IdeaVim-EasyMotion#supported-commands).

</details>


<details>
<summary><h2>sneak</h2></summary>

<img src="images/sneakIcon.svg" width="80" height="80" alt="icon"/>  

By [Mikhail Levchenko](https://github.com/Mishkun)  
Original repository with the plugin: https://github.com/Mishkun/ideavim-sneak  
Original plugin: [vim-sneak](https://github.com/justinmk/vim-sneak).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'justinmk/vim-sneak'`
   
### Instructions

* Type `s` and two chars to start sneaking in forward direction
* Type `S` and two chars to start sneaking in backward direction
* Type `;` or `,` to proceed with sneaking just as if you were using `f` or `t` commands

</details>

<details>
<summary><h2>NERDTree</h2></summary>
   
Original plugin: [NERDTree](https://github.com/preservim/nerdtree).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'preservim/nerdtree'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'preservim/nerdtree'</code>
      <br/>
      <code>Plug 'https://github.com/preservim/nerdtree'</code>
      <br/>
      <code>Plug 'nerdtree'</code>
      <br/>
      <code>set NERDTree</code>
      </details>
   
### Instructions
   
[See here](NERDTree-support.md).

</details>

<details>
<summary><h2>surround</h2></summary>
   
Original plugin: [vim-surround](https://github.com/tpope/vim-surround).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'tpope/vim-surround'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'tpope/vim-surround'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=1697'</code>
      <br/>
      <code>Plug 'vim-surround'</code>
      <br/>
      <code>set surround</code>
      </details>
   
### Instructions
   
https://github.com/tpope/vim-surround/blob/master/doc/surround.txt

</details>

<details>
<summary><h2>multiple-cursors</h2></summary>
   
Original plugin: [vim-multiple-cursors](https://github.com/terryma/vim-multiple-cursors).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'terryma/vim-multiple-cursors'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'terryma/vim-multiple-cursors'</code>
      <br/>
      <code>Plug 'https://github.com/terryma/vim-multiple-cursors'</code>
      <br/>
      <code>Plug 'vim-multiple-cursors'</code>
      <br/>
      <code>set multiple-cursors</code>
      </details>
   
### Instructions

At the moment, the default key binds for this plugin do not get mapped correctly in IdeaVim (see [VIM-2178](https://youtrack.jetbrains.com/issue/VIM-2178)). To enable the default key binds, add the following to your `.ideavimrc` file...

```
" Remap multiple-cursors shortcuts to match terryma/vim-multiple-cursors
nmap <C-n> <Plug>NextWholeOccurrence
xmap <C-n> <Plug>NextWholeOccurrence
nmap g<C-n> <Plug>NextOccurrence
xmap g<C-n> <Plug>NextOccurrence
xmap <C-x> <Plug>SkipOccurrence
xmap <C-p> <Plug>RemoveOccurrence

" Note that the default <A-n> and g<A-n> shortcuts don't work on Mac due to dead keys.
" <A-n> is used to enter accented text e.g. ñ
" Feel free to pick your own mappings that are not affected. I like to use <leader>
nmap <leader><C-n> <Plug>AllWholeOccurrences
xmap <leader><C-n> <Plug>AllWholeOccurrences
nmap <leader>g<C-n> <Plug>AllOccurrences
xmap <leader>g<C-n> <Plug>AllOccurrences
```

</details>

<details>
<summary><h2>commentary</h2></summary>

By [Daniel Leong](https://github.com/dhleong)  
Original plugin: [commentary.vim](https://github.com/tpope/vim-commentary).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'tpope/vim-commentary'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'tpope/vim-commentary'</code>
      <br/>
      <code>Plug 'https://github.com/tpope/vim-commentary'</code>
      <br/>
      <code>Plug 'vim-commentary'</code>
      <br/>
      <code>Plug 'tcomment_vim'</code>
      <br/>
      <code>set commentary</code>
      </details>
   
### Instructions
   
https://github.com/tpope/vim-commentary/blob/master/doc/commentary.txt

</details>

<details>
<summary><h2>ReplaceWithRegister</h2></summary>
   
By [igrekster](https://github.com/igrekster)  
Original plugin: [ReplaceWithRegister](https://github.com/vim-scripts/ReplaceWithRegister).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'vim-scripts/ReplaceWithRegister'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'vim-scripts/ReplaceWithRegister'</code>
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
   
### Instructions
   
https://github.com/vim-scripts/ReplaceWithRegister/blob/master/doc/ReplaceWithRegister.txt

</details>

<details>
<summary><h2>argtextobj</h2></summary>

Original plugin: [argtextobj.vim](https://www.vim.org/scripts/script.php?script_id=2699).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'vim-scripts/argtextobj.vim'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'vim-scripts/argtextobj.vim'</code>
      <br/>
      <code>Plug 'https://github.com/vim-scripts/argtextobj.vim'</code>
      <br/>
      <code>Plug 'argtextobj.vim'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=2699'</code>
      <br/>
      <code>set argtextobj</code>
      </details>
   
### Instructions
   
By default, only the arguments inside parenthesis are considered. To extend the functionality
      to other types of brackets, set `g:argtextobj_pairs` variable to a comma-separated
      list of colon-separated pairs (same as VIM's `matchpairs` option), like
      `let g:argtextobj_pairs="(:),{:},<:>"`. The order of pairs matters when
      handling symbols that can also be operators: `func(x << 5, 20) >> 17`. To handle
      this syntax parenthesis, must come before angle brackets in the list.
   
https://www.vim.org/scripts/script.php?script_id=2699

</details>
   

<details>
<summary><h2>exchange</h2></summary>

By [fan-tom](https://github.com/fan-tom)  
Original plugin: [vim-exchange](https://github.com/tommcdo/vim-exchange).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'tommcdo/vim-exchange'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'tommcdo/vim-exchange'</code>
      <br/>
      <code>Plug 'https://github.com/tommcdo/vim-exchange'</code>
      <br/>
      <code>Plug 'vim-exchange'</code>
      <br/>
      <code>set exchange</code>
      </details>
   
### Instructions
   
https://github.com/tommcdo/vim-exchange/blob/master/doc/exchange.txt

</details>
   
<details>
<summary><h2>textobj-entire</h2></summary>

By [Alexandre Grison](https://github.com/agrison)  
Original plugin: [vim-textobj-entire](https://github.com/kana/vim-textobj-entire).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'kana/vim-textobj-entire'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'kana/vim-textobj-entire'</code>
      <br/>
      <code>Plug 'vim-textobj-entire'</code>
      <br/>
      <code>Plug 'https://www.vim.org/scripts/script.php?script_id=2610'</code>
      <br/>
      <code>set textobj-entire</code>
      </details>
   
### Instructions
   
https://github.com/kana/vim-textobj-entire/blob/master/doc/textobj-entire.txt

</details>
   
<details>
<summary><h2>highlightedyank</h2></summary>

By [KostkaBrukowa](https://github.com/KostkaBrukowa)  
Original plugin: [vim-highlightedyank](https://github.com/machakann/vim-highlightedyank).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'machakann/vim-highlightedyank'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'machakann/vim-highlightedyank'</code>
      <br/>
      <code>Plug 'https://github.com/machakann/vim-highlightedyank'</code>
      <br/>
      <code>Plug 'vim-highlightedyank'</code>
      <br/>
      <code>set highlightedyank</code>
      </details>
   
### Instructions
   
If you want to optimize highlight duration, assign a time in milliseconds:  
      `let g:highlightedyank_highlight_duration = "1000"`  
      A negative number makes the highlight persistent.  
   
If you want to change background color of highlight you can provide the rgba of the color you want e.g.  
      `let g:highlightedyank_highlight_color = "rgba(160, 160, 160, 155)"`

If you want to change text color of highlight you can provide the rgba of the color you want e.g.  
`let g:highlightedyank_highlight_foreground_color = "rgba(0, 0, 0, 255)"`

https://github.com/machakann/vim-highlightedyank/blob/master/doc/highlightedyank.txt

</details>

<details>
<summary><h2>vim-paragraph-motion</h2></summary>

Original plugin: [vim-paragraph-motion](https://github.com/dbakker/vim-paragraph-motion).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'dbakker/vim-paragraph-motion'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'dbakker/vim-paragraph-motion'</code>
      <br/>
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
   
### Instructions
   
https://github.com/dbakker/vim-paragraph-motion#vim-paragraph-motion

</details>
   
<details>
<summary><h2>vim-indent-object</h2></summary>

By [Shrikant Sharat Kandula](https://github.com/sharat87)  
Original plugin: [vim-indent-object](https://github.com/michaeljsmith/vim-indent-object).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `Plug 'michaeljsmith/vim-indent-object'`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plugin 'michaeljsmith/vim-indent-object'</code>
      <br/>
      <code>Plug 'https://github.com/michaeljsmith/vim-indent-object'</code>
      <br/>
      <code>Plug 'vim-indent-object'</code>
      <br/>
      <code>set textobj-indent</code>
      </details>
   
### Instructions
   
https://github.com/michaeljsmith/vim-indent-object/blob/master/doc/indent-object.txt

</details>
   
   
<details>
<summary><h2>matchit.vim</h2></summary>

By [Martin Yzeiri](https://github.com/myzeiri)
Original plugin: [matchit.vim](https://github.com/chrisbra/matchit).
   
### Setup:
- Add the following command to `~/.ideavimrc`: `packadd matchit`
    <details>
      <summary>Alternative syntax</summary>
      <code>Plug 'vim-matchit'</code>
      <br/>
      <code>Plug 'chrisbra/matchit'</code>
      <br/>
      <code>set matchit</code>
      </details>
   
### Instructions
   
https://github.com/adelarsq/vim-matchit/blob/master/doc/matchit.txt

</details>

<details>
<summary><h2>IdeaVim-Quickscope</h2></summary>

Original plugin: [quick-scope](https://github.com/unblevable/quick-scope).

### Setup:
- Install [IdeaVim-Quickscope](https://plugins.jetbrains.com/plugin/19417-ideavim-quickscope) plugin.
- Add the following command to `~/.ideavimrc`: `set quickscope`

### Instructions

https://plugins.jetbrains.com/plugin/19417-ideavim-quickscope

</details>

<details>
<summary><h2>Mini.ai: Extend and create a/i textobjects (IMPORTANT: The plugin is not related with artificial intelligence)</h2></summary>

### Features: 
Provides additional text object motions for handling quotes and brackets. The following motions are included:

- aq: Around any quotes.
- iq: Inside any quotes.
- ab: Around any parentheses, curly braces, and square brackets.
- ib: Inside any parentheses, curly braces, and square brackets.

Original plugin: [mini.ai](https://github.com/echasnovski/mini.ai).

### Setup:
- Add the following command to `~/.ideavimrc`: `set mini-ai`

</details>


<details>
<summary><h2>Which-Key</h2></summary>

Original plugin: [vim-which-key](https://github.com/liuchengxu/vim-which-key).

### Setup:
- Install [Which-Key](https://plugins.jetbrains.com/plugin/15976-which-key) plugin.
- Add the following command to `~/.ideavimrc`: `set which-key`

### Instructions

https://github.com/TheBlob42/idea-which-key?tab=readme-ov-file#installation

</details>
<details>
<summary><h2>Vim Peekaboo</h2></summary>

By Julien Phalip  
Original plugin: [vim-peekaboo](https://github.com/junegunn/vim-peekaboo).

### Setup

Add `set peekaboo` to your `~/.ideavimrc` file, then run `:source ~/.ideavimrc`
or restart the IDE.

### Instructions

https://plugins.jetbrains.com/plugin/25776-vim-peekaboo
</details>

<details>
<summary><h2>FunctionTextObj</h2></summary>

By Julien Phalip  

### Setup

Add `set functiontextobj` to your `~/.ideavimrc` file, then run `:source ~/.ideavimrc`
or restart the IDE.

### Instructions

https://plugins.jetbrains.com/plugin/25897-vim-functiontextobj
</details>

<details>
<summary><h2>Switch</h2></summary>

By Julien Phalip  
Original plugin: [switch.vim](https://github.com/AndrewRadev/switch.vim).

### Setup

Add `set switch` to your `~/.ideavimrc` file, then run `:source ~/.ideavimrc`
or restart the IDE.

### Instructions

https://plugins.jetbrains.com/plugin/25899-vim-switch

