IdeaVim
=======

IdeaVim is a Vim emulation plug-in for IDEs based on the IntelliJ platform.
IdeaVim can be used with IntelliJ IDEA, RubyMine, PyCharm, PhpStorm, WebStorm,
AppCode and Android Studio.

Resources:

* [Plugin homepage](http://plugins.jetbrains.com/plugin/164)
* [Changelog](https://github.com/JetBrains/ideavim/blob/master/CHANGES.md)
* [Bug tracker](http://youtrack.jetbrains.com/issues/VIM)
* [Continuous integration builds](http://teamcity.jetbrains.com/project.html?projectId=IdeaVim)
* [@IdeaVim](http://twitter.com/ideavim) in Twitter


Installation
------------

Use the IDE's plugin manager to install the latest version of the plugin.
Start the IDE normally and enable the Vim emulation using "Tools | Vim
Emulator" menu item. At this point you must use Vim keystrokes in all editors.

If you wish to disable the plugin, select the "Tools | Vim Emulator" menu so
it is unchecked. At this point IDE will work with it's regular keyboard
shortcuts.


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
* Macros
* Digraphs
* Command line and search history
* Vim web help

Not supported (yet):

* Key mappings
* Window commands
* Jump lists
* Various less used commands

Please see the file [index.txt](https://github.com/JetBrains/ideavim/blob/master/index.txt)
for a list of commands covered with tests.


Changes to the IDE
------------------

### Undo/Redo

The IdeaVim plugin uses the undo/redo functionality of the IntelliJ platform,
so the behaviour of the `u` and `<C-R>` commands may differ from the original
Vim. Vim compatibility of undo/redo may be improved in the future releases.

### Escape

Using `<Esc>` in dialog windows remains problematic. For most dialog windows
the Vim emulator is put into the insert mode without the possibility to switch to
the normal mode. In some dialog windows the normal mode is on by default. The
usage of the Vim emulator in dialog windows is an area for improvements.

### Menu Changes

In order to emulate the keystrokes used by Vim, several of the default hotkeys
used by the IDE had to be changed. Below is a list of IDE menus, their default
keyboard shortcuts, and their new VIM keystrokes.

    File
         Save All                 Ctrl-S              :w

    Edit
         Undo                     Ctrl-Z              u
         Redo                     Ctrl-Shift-Z        Ctrl-R
         Cut                      Ctrl-X              "+x
         Copy                     Ctrl-C              "+y
         Paste                    Ctrl-V              "+P
         Select All               Ctrl-A              ggVG

    Search
         Find                     Ctrl-F              /
         Replace                  Ctrl-R              :s
         Find Next                F3                  n
         Find Previous            Shift-F3            N

    View
         Quick JavaDoc            Ctrl-Q              K
         Parameter Info           Ctrl-P              Ctrl-Shift-P
         Swap Panels              Ctrl-U              <None>
         Recent Files...          Ctrl-E              <None>
         Type Hierarchy           Ctrl-H              Ctrl-Alt-Shift-H

    Goto
         Class...                 Ctrl-N              Alt-Shift-N
         Line...                  Ctrl-G              G
         Declaration              Ctrl-B              gd
         Super Method             Ctrl-U              Ctrl-Shift-U

    Code
         Override Methods...      Ctrl-O              Ctrl-Shift-O
         Implement Methods...     Ctrl-I              Ctrl-Shift-I
         Complete Code                                (Only in Insert mode)
              Basic               Ctrl-Space          Ctrl-Space or Ctrl-N or Ctrl-P
              Smart Type          Ctrl-Shift-Space    Ctrl-Shift-Space
              Class Name          Ctrl-Alt-Space      Ctrl-Alt-Space
         Insert Live Template     Ctrl-J              Ctrl-]

    Tools
         Version Control
              Check In Project    Ctrl-K              <None>


Development
-----------

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

