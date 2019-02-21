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

In order to contribute to IdeaVim you should have some understanding of Java or [Kotlin](https://kotlinlang.org/).

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

3. Run your IdeaVim plugin within IntelliJ via a Gradle task

    * Select "View | Tool Windows | Gradle" tool window
    
    * Launch "ideavim | intellij | runIde" from the tool window

4. Run IdeaVim tests via a Gradle task

    * Select "View | Tool Windows | Gradle" tool window
    
    * Launch "ideavim | verification | test" from the tool window

5. Build the plugin distribution by running `./gradlew clean buildPlugin` in the
   terminal in your project root.

    * The resulting distribution file is build/distributions/IdeaVim-VERSION.zip

    * You can install this file using "Settings | Plugins | Install plugin
      from disk"