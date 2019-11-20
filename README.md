<img src="resources/META-INF/pluginIcon.svg" width="80" height="80" alt="icon"  align="left"/>

IdeaVim
===

<div>
  <a href="https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub">
    <img src="https://jb.gg/badges/official.svg" alt="official JetBrains project"/>
  </a>
  <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20191&guest=1">
    <img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20191)/statusIcon.svg?guest=1"/>
  </a>
</div>

IdeaVim is a Vim emulation plugin for IDEs based on the IntelliJ Platform.
IdeaVim can be used with IntelliJ IDEA, PyCharm, CLion, PhpStorm, WebStorm,
RubyMine, AppCode, DataGrip, GoLand, Rider, Cursive, and Android Studio.

Resources:

* [Plugin homepage](https://plugins.jetbrains.com/plugin/164-ideavim)
* [Changelog](CHANGES.md)
* [Bug tracker](https://youtrack.jetbrains.com/issues/VIM)
* [Continuous integration builds](https://teamcity.jetbrains.com/project.html?projectId=IdeaVim&guest=1)
* [@IdeaVim](https://twitter.com/ideavim) in Twitter


Installation
------------

Use the IDE's plugin manager to install the latest version of the plugin.
Start the IDE normally and enable the Vim emulation using "Tools | Vim
Emulator" menu item. At this point you must use Vim keystrokes in all editors.

If you wish to disable the plugin, select the "Tools | Vim Emulator" menu so
it is unchecked. At this point your IDE will work with its regular keyboard
shortcuts.

Keyboard shortcut conflicts between the Vim emulation and the IDE can be
resolved via "File | Settings | Editor | Vim Emulation", "File | Settings |
Keymap" on Linux & Windows, and via "Preferences | Editor | Vim Emulation",
"Preferences | Keymap" on macOS. They can also be resolved by key-mapping
commands in your ~/.ideavimrc file.


Get Early Access
-------------------

Would you like to try new features and fixes? Join the Early Access Program and
receive EAP builds as updates!  

1. Click the IdeaVim icon in the status bar | `EAP` | `Get Early Access...`

Or subscribe to EAP updates manually:

1. Open `Settings | Plugins`
2. Click the gear icon :gear:, select `Manage Plugin Repositories`, and add the following url:
 `https://plugins.jetbrains.com/plugins/eap/ideavim`

See [the changelog](CHANGES.md) for the list of hot unreleased features.

It is important to distinguish EAP builds from traditional pre-release software.
Please note that the quality of EAP versions may at times be way below even
usual beta standards.

You can always leave your feedback with:
* [@IdeaVim](https://twitter.com/ideavim) in Twitter
* [Bug tracker](https://youtrack.jetbrains.com/issues/VIM)


Summary of Supported Vim Features
---------------------------------

Supported:

* Motion keys
* Deletion/changing
* Insert mode commands
* Marks
* Registers
* Undo/redo
* Visual mode commands
* Some Ex commands
* Some [:set options](doc/set-commands.md)
* Full Vim regexps for search and search/replace
* Key mappings
* Macros
* Digraphs
* Command line and search history
* Window commands
* Vim web help
* Select mode

Emulated Vim plugins:

* vim-surround
* vim-multiple-cursors
* vim-commentary

Not supported (yet):

* Jump lists
* Various less-used commands

See also:

* [The list of all supported commands](src/com/maddyhome/idea/vim/package-info.java)
* [Top features and bugs](https://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved+sort+by%3A+votes)


Files
-----

* ~/.ideavimrc
    * Your IdeaVim-specific Vim initialization commands

You can read your ~/.vimrc file from ~/.ideavimrc with this command:

    source ~/.vimrc

Note, that IdeaVim currently parses ~/.ideavimrc file via simple pattern matching.
See [VIM-669](https://youtrack.jetbrains.com/issue/VIM-669) for proper parsing
of VimL files.

Also note that if you have overridden the `user.home` JVM option, this
will affect where IdeaVim looks for your .ideavimrc file. For example, if you
have `-Duser.home=/my/alternate/home` then IdeaVim will source
`/my/alternate/home/.ideavimrc` instead of `~/.ideavimrc`.

Alternatively, you can set up initialization commands using [XDG](https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html) standard.
Put your settings to `$XDG_CONFIG_HOME$/ideavim/ideavimrc` file.     [To Be Released]


Emulated Vim Plugins
--------------------

IdeaVim extensions emulate some plugins of the original Vim. In order to use
IdeaVim extensions, you have to enable them via this command in your `~/.ideavimrc`:

    set <extension-name>

Available extensions:

* easymotion
    * Setup:  
        * Install [IdeaVim-EasyMotion](https://plugins.jetbrains.com/plugin/13360-ideavim-easymotion/)
        and [AceJump](https://plugins.jetbrains.com/plugin/7086-acejump/) plugins.
        * `set easymotion`
    * Emulates [vim-easymotion](https://github.com/easymotion/vim-easymotion)
    * Commands: All commands with the mappings are supported. See the [full list of supported commands](https://github.com/AlexPl292/IdeaVim-EasyMotion#supported-commands).

* surround
    * Setup: `set surround`
    * Emulates [vim-surround](https://github.com/tpope/vim-surround)
    * Commands: `ys`, `cs`, `ds`, `S`

* multiple-cursors
    * Setup: `set multiple-cursors`
    * Emulates [vim-multiple-cursors](https://github.com/terryma/vim-multiple-cursors)
    * Commands: `<A-n>`, `<A-x>`, `<A-p>`, `g<A-n>`

* commentary
    * Setup: `set commentary`
    * Emulates [commentary.vim](https://github.com/tpope/vim-commentary)
    * Commands: `gcc`, `gc + motion`, `v_gc`


Changes to the IDE
------------------

### Undo/Redo

The IdeaVim plugin uses the undo/redo functionality of the IntelliJ Platform,
so the behavior of the `u` and `<C-R>` commands may differ from the original
Vim. Vim compatibility of undo/redo may be improved in future releases.

See also [unresolved undo issues](https://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved+Help+topic%3A+u).

### Escape

Using `<Esc>` in dialog windows remains problematic. For most dialog windows,
the Vim emulator is put into insert mode with `<Esc>` not working. You
should use `<C-c>` or `<C-[>` instead. In some dialog windows, the normal mode is
switched by default. The usage of the Vim emulator in dialog windows is an area for
improvement.

See also [unresolved escape issues](https://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved+Help+topic%3A+i_Esc).

### Executing IDE Actions

IdeaVim adds two commands for listing and executing arbitrary IDE actions as
Ex commands or via `:map` command mappings:

* `:actionlist [pattern]`
    * Find IDE actions by name or keymap pattern (E.g. `:actionlist extract`, `:actionlist <C-D`)
* `:action {name}`
    * Execute an action named `NAME`

For example, here `\r` is mapped to the Reformat Code action:

    :map \r :action ReformatCode<CR>


Contributing
------------

See [CONTRIBUTING.md](CONTRIBUTING.md)

Authors
-------

See [AUTHORS.md](AUTHORS.md)
for a list of authors and contributors.


License
-------

IdeaVim is licensed under the terms of the GNU Public License version 2
or any later version.
