import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.gradle.api.tasks.testing.logging.*

plugins {
  // implementation
  `java-gradle-plugin`
  kotlin("jvm") version "1.5.30"

  // quality
  id("pl.droidsonroids.jacoco.testkit") version "1.0.9"
  jacoco
  id("io.gitlab.arturbosch.detekt") version "1.18.0"

  // documentation
  id("org.jetbrains.dokka") version "1.5.0"
  id("org.asciidoctor.jvm.convert") version "3.3.2"

  // publishing
  `maven-publish`
  id("com.gradle.plugin-publish") version "0.15.0"
  id("com.github.ben-manes.versions") version "0.39.0"
}

group = "de.qualersoft"

repositories {
  mavenCentral()
}

val antJUnit: Configuration by configurations.creating
dependencies {
  // Align versions of all Kotlin components
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  // Use the Kotlin JDK 8 standard library.
  implementation(kotlin("stdlib-jdk8"))

  testImplementation(platform("org.junit:junit-bom:5.7.2"))
  testImplementation(group = "org.junit.jupiter", name = "junit-jupiter")

  testImplementation(kotlin("test-junit5"))

  testImplementation(group = "io.kotest", name = "kotest-runner-junit5", version = "4.6.2")
  testImplementation(group = "io.kotest", name = "kotest-assertions-core-jvm", version = "4.6.2")

  testRuntimeOnly(kotlin("script-runtime"))

  antJUnit(group = "org.apache.ant", name = "ant-junit", version = "1.10.5")
}

gradlePlugin {
  // Define the plugin
  plugins.create("jmeter") {
    id = "de.qualersoft.jmeter"
    implementationClass = "de.qualersoft.jmeter.gradleplugin.JMeterPlugin"

    displayName = "jmeter gradle plugin"
    description = "Plugin to execute JMeter tests."
  }
  testSourceSets(*sourceSets.filter { it.name.contains("test", true) }.toTypedArray())
}

pluginBundle {
  website = "https://github.com/qualersoft/jmeter-gradle-plugin"
  vcsUrl = "https://github.com/qualersoft/jmeter-gradle-plugin"
  tags = listOf("jmeter", "test", "performance")
}

jacoco {
  toolVersion = "0.8.7"
}

detekt {
  allRules = false
  buildUponDefaultConfig = true
  config = files("$projectDir/detekt.yml")
  source = files("src/main/kotlin")

  reports {
    html.enabled = true
    xml.enabled = true
    txt.enabled = false
  }
}

if (project.version.toString().endsWith("-SNAPSHOT", true)) {
  status = "snapshot"
}

val javaVersion = JavaVersion.VERSION_11
java {
  targetCompatibility = javaVersion
  withSourcesJar()
  withJavadocJar()
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
  
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

// Setup functional test sets
listOf("groovy", "kotlin").forEach {
  val testName = it.capitalizeAsciiOnly()
  val theTest = tasks.register<Test>("functional${testName}Test") {
    configureDefaultFuncTest(this)
    useJUnitPlatform {
      includeTags.add(it)
    }
    testLogging {
      events = setOf(TestLogEvent.FAILED)
      exceptionFormat = TestExceptionFormat.FULL
    }
    mustRunAfter(project.tasks.test)
  }

  tasks.check {
    dependsOn(theTest)
  }

  jacocoTestKit.applyTo("functionalTestRuntimeOnly", theTest as TaskProvider<Task>)

  project.tasks.register<JacocoReport>("jacoco${testName}Report") {
    group = "verification"
    additionalClassDirs(sourceSets.main.get().output.classesDirs)
    additionalSourceDirs(sourceSets.main.get().allSource.sourceDirectories)
    val jacocoExt = theTest.get().extensions.getByName<JacocoTaskExtension>("jacoco")
    executionData(jacocoExt.destinationFile)
    mustRunAfter(theTest)
  }
}

tasks {

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = javaVersion.toString()
    }
  }

  validatePlugins {
    enableStricterValidation.set(true)
  }

  this.detekt {
    // Target version of the generated JVM bytecode. It is used for type resolution.
    this.jvmTarget = JavaVersion.VERSION_11.toString()
  }
  
  test {
    useJUnitPlatform()
    testLogging {
      events = setOf(TestLogEvent.FAILED)
      exceptionFormat = TestExceptionFormat.SHORT
    }
  }

  withType<JacocoReport> {
    reports {
      xml.required.set(true)
      html.required.set(true)
    }
  }

  dokkaJavadoc.configure {
    outputDirectory.set(javadoc.get().destinationDir)
  }

  javadoc {
    dependsOn(dokkaJavadoc)
  }

  // the following merge tasks are only for local execution and not meant to be used pipeline!!!
  register<JacocoReport>("jacocoMergedReport") {
    group = "verification"
    additionalClassDirs(sourceSets.main.get().output.classesDirs)
    additionalSourceDirs(sourceSets.main.get().allSource.sourceDirectories)
    withType<Test>().map { executionData(it) }
  }

  register("mergedTestReport") {
    val resultsDir = file("$buildDir/test-results/merged").also {
      it.mkdirs()
    }
    val reportDir = file("$buildDir/reports/tests/merged")
    doLast {
      ant.withGroovyBuilder {
        "taskdef"(
          "name" to "junitreport",
          "classname" to "org.apache.tools.ant.taskdefs.optional.junit.XMLResultAggregator",
          "classpath" to antJUnit.asPath
        )

        // generates an XML report
        "junitreport"("todir" to resultsDir) {
          "fileset"(
            "dir" to resultsDir.parentFile,
            "includes" to "**/TEST-*.xml")
          "report"( // additionally generates an HTML report
            "todir" to reportDir,
            "format" to "frames")
        }
      }
    }
  }
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/qualersoft/jmeter-gradle-plugin")
      credentials {
        username = project.findProperty("gh.qualersoft.publish.gpr.usr") as String? ?: System.getenv("USERNAME")
        password = project.findProperty("gh.qualersoft.publish.gpr.key") as String? ?: System.getenv("TOKEN")
      }
    }
  }
}

fun configureDefaultFuncTest(test: Test) {
  test.group = "verification"
  test.testClassesDirs = functionalTestSourceSet.output.classesDirs
  test.classpath = functionalTestSourceSet.runtimeClasspath

  // Workaround on gradle/jacoco keeping *.exec file locked
  if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
    fun File.isLocked() = !renameTo(this)
    val waitUntilJacocoTestExecIsUnlocked = Action<Task> {
      val jacocoTestExec = checkNotNull(extensions.getByType(JacocoTaskExtension::class).destinationFile)
      val waitMillis = 100L
      var tries = 0
      while (jacocoTestExec.isLocked() && (tries++ < 100)) {
        logger.info("Waiting $waitMillis ms (${jacocoTestExec.name} is locked)...")
        Thread.sleep(waitMillis)
      }
      logger.info("Done waiting (${jacocoTestExec.name} is unlocked).")
    }
    test.doLast(waitUntilJacocoTestExecIsUnlocked)
  }
}