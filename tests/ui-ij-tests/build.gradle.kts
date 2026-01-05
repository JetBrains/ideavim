plugins {
  java
  kotlin("jvm")
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

    // Gradle 9+ requires explicit test source set configuration for custom Test tasks
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    // This is needed for the robot to access the message of the exception
    // Usually these opens are provided by the intellij gradle plugin
    // https://github.com/JetBrains/gradle-intellij-plugin/blob/b21e3f382e9885948a6427001d5e64234c602613/src/main/kotlin/org/jetbrains/intellij/utils/OpenedPackages.kt#L26
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
  }
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
