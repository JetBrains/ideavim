plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij")
}

repositories {
  mavenCentral()
  maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

val kotlinVersion: String by project
val ideaVersion: String by project
val javaVersion: String by project
val remoteRobotVersion: String by project

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation(testFixtures(project(":"))) // The root project
  testImplementation(testFixtures(project(":tests:ui-fixtures"))) // The root project

  testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
  testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
  testImplementation("com.intellij.remoterobot:ide-launcher:$remoteRobotVersion")
  testImplementation("com.automation-remarks:video-recorder-junit5:2.0")
}

tasks {
  // This task is disabled because it should be excluded from `gradle test` run (because it's slow)
  // I didn't find a better way to exclude except disabling and defining a new task with a different name
  test {
    useJUnitPlatform()
    enabled = false
  }

  register<Test>("testUi") {
    group = "verification"
    useJUnitPlatform()
  }

  downloadRobotServerPlugin {
    version.set(remoteRobotVersion)
  }

  runIdeForUiTests {
    systemProperty("robot-server.port", "8082")
    systemProperty("ide.mac.message.dialogs.as.sheets", "false")
    systemProperty("jb.privacy.policy.text", "<!--999.999-->")
    systemProperty("jb.consents.confirmation.enabled", "false")
    systemProperty("ide.show.tips.on.startup.default.value", "false")
  }

  verifyPlugin {
    enabled = false
  }

  publishPlugin {
    enabled = false
  }
}

intellij {
  version.set(ideaVersion)
  type.set("IC")
  plugins.set(listOf("java"))
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}
