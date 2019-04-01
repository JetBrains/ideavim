<div>
  <a href="http://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20183&guest=1">
    <img src="http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20183)/statusIcon.svg?guest=1"/>
  </a>
  <span>2018.3 Tests</span>
</div>
<div>
  <a href="http://teamcity.jetbrains.com/viewType.html?buildTypeId=IdeaVim_TestsForIntelliJ20191&guest=1">
    <img src="http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:IdeaVim_TestsForIntelliJ20191)/statusIcon.svg?guest=1"/>
  </a>
  <span>2019.1 Tests</span>
</div>


### Where to Start

In order to contribute to IdeaVim, you should have some understanding of Java or [Kotlin](https://kotlinlang.org/).

See also these docs on the IntelliJ API:

* [IntelliJ architectural overview](http://confluence.jetbrains.com/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview)
* [IntelliJ plugin development resources](http://confluence.jetbrains.com/display/IDEADEV/PluginDevelopment)

You can start by picking relatively simple tasks that are tagged with
[#patch_welcome](https://youtrack.jetbrains.com/issues/VIM?q=%23patch_welcome%20%23Unresolved%20sort%20by:%20votes%20)
in the issue tracker.


### Development Environment

1. Fork IdeaVim on GitHub and clone the repository on your local machine.

2. Import the project from existing sources in IntelliJ IDEA 2018.1 or newer (Community or
   Ultimate) using "File | New | Project from Existing Sources..." or "Import
   Project" from the start window.

    * In the project wizard select "Import project from external model | Gradle"

    * Select your Java 8+ JDK as the Gradle JVM, leave other parameters unchanged

3. Set up [copyright](#copyright)

4. Read [testing](#testing) section

5. Run your IdeaVim plugin within IntelliJ via a Gradle task

    * Select "View | Tool Windows | Gradle" tool window
    
    * Launch "ideavim | intellij | runIde" from the tool window

6. Run IdeaVim tests via a Gradle task

    * Select "View | Tool Windows | Gradle" tool window
    
    * Launch "ideavim | verification | test" from the tool window

7. Build the plugin distribution by running `./gradlew clean buildPlugin` in the
   terminal in your project root.

    * The resulting distribution file is build/distributions/IdeaVim-VERSION.zip

    * You can install this file using "Settings | Plugins | Install plugin
      from disk"

### Copyright

1. Go to `Preferences | Appearance & Behavior | Scopes`, press "+" button, `local`.  
       Name: Copyright scope  
       Pattern: `file[IdeaVIM.main]:com//*||file[IdeaVIM.test]:*/`

2. Go to `Preferences | Editor | Copyright | Copyright Profiles`, press "+" button.  
       Name: IdeaVim  
       Text:  
       
       IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
       Copyright (C) 2003-$today.year The IdeaVim authors
       
       This program is free software: you can redistribute it and/or modify
       it under the terms of the GNU General Public License as published by
       the Free Software Foundation, either version 2 of the License, or
       (at your option) any later version.
       
       This program is distributed in the hope that it will be useful,
       but WITHOUT ANY WARRANTY; without even the implied warranty of
       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
       GNU General Public License for more details.
       
       You should have received a copy of the GNU General Public License
       along with this program. If not, see <http://www.gnu.org/licenses/>.
       
3. Go to `Preferences | Editor | Copyright`, press "+" button.  
       Scope: Copyright scope  
       Copyright: IdeaVim
       
### Testing

1. Read about `@VimBehaviourDiffers` annotation.

2. Please avoid senseless text like "dhjkwaldjwa", "asdasdasd",
"123 123 123 123", etc. Try to select a text that you can simply
read and find out what is wrong if the test fails.
For example, take a few lines from your favorite poem, or take
"Vladimir Nabokov – A Discovery" if you don't have one.

3. Test your functionality properly.
Especially check whether your command works with:
line start, line end, file start, file end, empty line, multiple carets.