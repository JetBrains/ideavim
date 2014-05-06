IdeaVim
=======

<div>
  <a href="http://teamcity.jetbrains.com/viewType.html?buildTypeId=bt299&guest=1">
    <img src="http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:bt299)/statusIcon"/>
  </a>
  <span>Build<span>
</div>

<div>
  <a href="http://teamcity.jetbrains.com/viewType.html?buildTypeId=bt453&guest=1">
    <img src="http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:bt453)/statusIcon"/>
  </a>
  <span>Tests</span>
</div>

IdeaVim is a Vim emulation plug-in for IDEs based on the IntelliJ platform.
IdeaVim can be used with IntelliJ IDEA, RubyMine, PyCharm, PhpStorm, WebStorm,
AppCode and Android Studio.

Resources:

* [Plugin homepage](http://plugins.jetbrains.com/plugin/164)
* [Changelog](https://github.com/JetBrains/ideavim/blob/master/CHANGES.md)
* [Bug tracker](http://youtrack.jetbrains.com/issues/VIM)
* [Continuous integration builds](http://teamcity.jetbrains.com/project.html?projectId=IdeaVim&guest=1)
* [@IdeaVim](http://twitter.com/ideavim) in Twitter


Installation
------------

Use the IDE's plugin manager to install the latest version of the plugin.
Start the IDE normally and enable the Vim emulation using "Tools | Vim
Emulator" menu item. At this point you must use Vim keystrokes in all editors.

If you wish to disable the plugin, select the "Tools | Vim Emulator" menu so
it is unchecked. At this point IDE will work with it's regular keyboard
shortcuts.

Keyboard shortcut conflicts between the Vim emulation and the IDE can be
resolved via "File | Settings | Vim Emulation", "File | Settings | Keymap"
and key mapping commands in your ~/.ideavimrc file.


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
* Some [:set options](https://github.com/JetBrains/ideavim/blob/master/doc/set-commands.md)
* Full Vim regexps for search and search/replace
* Key mappings
* Macros
* Digraphs
* Command line and search history
* Vim web help

Not supported (yet):

* Window commands
* Jump lists
* Various less used commands

See also:

* [List of recently added commands](https://github.com/JetBrains/ideavim/blob/master/src/com/maddyhome/idea/vim/package-info.java)
* [List of commands covered with tests](https://github.com/JetBrains/ideavim/blob/master/index.txt)
* [Top features and bugs](http://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved+sort+by%3A+votes)


Files
-----

* ~/.ideavimrc
    * Your IdeaVim-specific Vim initialization commands

You can read your ~/.vimrc file from ~/.ideavimrc using this command:

    source ~/.vimrc

Note, that IdeaVim currently parses ~/.ideavimrc file via simple pattern matching,
see [VIM-669](http://youtrack.jetbrains.com/issue/VIM-669) for proper parsing
of VimL files.


Changes to the IDE
------------------

### Undo/Redo

The IdeaVim plugin uses the undo/redo functionality of the IntelliJ platform,
so the behaviour of the `u` and `<C-R>` commands may differ from the original
Vim. Vim compatibility of undo/redo may be improved in the future releases.

See also [unresolved undo issues](http://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved+Help+topic%3A+u).

### Escape

Using `<Esc>` in dialog windows remains problematic. For most dialog windows
the Vim emulator is put into the insert mode without the possibility to switch to
the normal mode. In some dialog windows the normal mode is on by default. The
usage of the Vim emulator in dialog windows is an area for improvements.

See also [unresolved escape issues](http://youtrack.jetbrains.com/issues/VIM?q=%23Unresolved+Help+topic%3A+i_Esc).


Contributing
------------

### Where to Start

In order to contribute to IdeaVim you should have some understanding of Java.

See also these docs on the IntelliJ API:

* [IntelliJ architectural overview](http://confluence.jetbrains.com/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview)
* [IntelliJ plugin development resources](http://confluence.jetbrains.com/display/IDEADEV/PluginDevelopment)

You can start by picking relatively simple tasks that are tagged with
[#patch_welcome](http://youtrack.jetbrains.com/issues/VIM?q=%23patch_welcome)
in the issue tracker.


### Development Environment

1. Fork IdeaVim on GitHub and clone the repository on your local machine.

2. Open the project in IntelliJ IDEA 12+ (Community or Ultimate) using "File |
   Open... | /path/to/ideavim".

3. Set up a JDK if you haven't got it yet. Use "File | Project Structure | SDKs
   | Add new JDK".

4. Set up an IntelliJ plugin SDK using "File | Project Structure | SDKs | Add
   new IntelliJ IDEA Plugin SDK". The correct path to your current installation
   of IntelliJ will be suggested automatically. You will be prompted to select a
   JDK for your plugin SDK. Select the JDK from the previous step. You
   **should** name your plugin SDK `IntelliJ Plugin SDK` in order to match the
   name in the project settings stored in the Git repository.

5. Select a project SDK for your project using "File | Project Structure |
   Project | Project SDK". Choose the plugin SDK you have created at the
   previous step.

6. Build IdeaVim and run IntelliJ with IdeaVim enabled using the "IdeaVim" run
   configuration (use "Run | Run... | IdeaVim").

7. In order to be able to run tests in your IntelliJ edition uncomment the
   appropriate lines in the constructor of the `VimTestCase` class.


Authors
-------

See [AUTHORS.md](https://github.com/JetBrains/ideavim/blob/master/AUTHORS.md)
for a list of authors and contributors.


License
-------

IdeaVim is licensed under the terms of the GNU Public license version 2.

