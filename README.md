<img src="resources/META-INF/pluginIcon.svg" width="80" height="80" alt="icon" align="left"/>

IdeaVim
===

<div>
  <a href="https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub">
    <img src="https://jb.gg/badges/official.svg" alt="official JetBrains project"/>
  </a>
  <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20201&guest=1">
    <img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20201)/statusIcon.svg?guest=1" alt="TeamCity Build"/>
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


Setup
------------

- IdeaVim can be installed via `Settings | Plugins`.
See [detailed instructions](https://www.jetbrains.com/help/idea/managing-plugins.html#).

- Use `Tools | Vim Emulator` to enable or disable emulation.

- Use `~/.ideavimrc` file as an analog of `~/.vimrc` ([details](#Files)). XGD standard is supported as well.

- Shortcut conflicts can be resolved using:
     - Linux & Windows: `File | Settings | Editor | Vim Emulation` & `File | Settings | Keymap`,
     - macOS: `Preferences | Editor | Vim Emulation` & `Preferences | Keymap`,
     - regular vim mappings in the  `~/.ideavimrc` file.

Get Early Access
-------------------

Would you like to try new features and fixes? Join the Early Access Program and
receive EAP builds as updates!  

1. Click the <img src="resources/META-INF/pluginIcon_noBorders.svg" width="16" height="16" alt="icon"/> IdeaVim
icon in the status bar  | `EAP` | `Get Early Access...`


Or subscribe to EAP updates manually:

1. Open `Settings | Plugins`
2. Click the gear icon :gear:, select `Manage Plugin Repositories`, and add the following url:
 `https://plugins.jetbrains.com/plugins/eap/ideavim`

See [the changelog](CHANGES.md) for the list of unreleased features.

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

[Emulated Vim plugins](doc/emulated-plugins.md):

* vim-easymotion
* vim-surround
* vim-multiple-cursors
* vim-commentary
* argtextobj.vim
* vim-textobj-entire
* ReplaceWithRegister

Not supported (yet):

* Jump lists
* Various less-used commands

See also:

* [The list of all supported commands](src/com/maddyhome/idea/vim/package-info.java)
* [Top features and bugs](https://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved+sort+by%3A+votes)


Files
-----

* `~/.ideavimrc`
    * Your IdeaVim-specific Vim initialization commands

You can read your `~/.vimrc` file from `~/.ideavimrc` with this command:

    source ~/.vimrc

Note, that IdeaVim currently parses `~/.ideavimrc` file via simple pattern matching.
See [VIM-669](https://youtrack.jetbrains.com/issue/VIM-669) for proper parsing
of VimL files.

Also note that if you have overridden the `user.home` JVM option, this
will affect where IdeaVim looks for your `.ideavimrc` file. For example, if you
have `-Duser.home=/my/alternate/home` then IdeaVim will source
`/my/alternate/home/.ideavimrc` instead of `~/.ideavimrc`.

Alternatively, you can set up initialization commands using [XDG](https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html) standard.
Put your settings to `$XDG_CONFIG_HOME$/ideavim/ideavimrc` file.


Emulated Vim Plugins
--------------------

See [doc/emulated-plugins.md](doc/emulated-plugins.md)

Changes to the IDE
------------------

### Executing IDE Actions

IdeaVim adds two commands for listing and executing arbitrary IDE actions as
Ex commands or via `:map` command mappings:

* `:actionlist [pattern]`
    * Find IDE actions by name or keymap pattern (E.g. `:actionlist extract`, `:actionlist <C-D`)
* `:action {name}`
    * Execute an action named `NAME`

For example, here `\r` is mapped to the Reformat Code action:

    :map \r :action ReformatCode<CR>

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
