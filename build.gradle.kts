import java.util.Properties

plugins {
  // implementation
  `java-gradle-plugin`
  kotlin("jvm") version "1.5.30"

  // quality
  jacoco
  id("pl.droidsonroids.jacoco.testkit") version "1.0.9"
  id("org.unbroken-dome.test-sets") version "4.0.0"
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

testSets {
  "functionalTest" {
    description = "Runs the functional tests"
  }
}

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

// Setup functional test sets
val functionalTestTask = tasks.named<Test>("functionalTest") {
  description = "Run the functional tests"
  group = "verification"
  testClassesDirs = sourceSets.named("functionalTest").get().output.classesDirs
  classpath = sourceSets.named("functionalTest").get().runtimeClasspath

  useJUnitPlatform()
  shouldRunAfter(tasks.test)
  dependsOn(tasks.jar, tasks.named("generateJacocoFunctionalTestKitProperties"))
  applyJacocoWorkaround()
}

jacocoTestKit.applyTo("functionalTestRuntimeOnly", functionalTestTask as TaskProvider<Task>)

tasks.check {
  dependsOn(functionalTestTask)
}

tasks {

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
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
  }

  pluginUnderTestMetadata {
    // https://discuss.gradle.org/t/how-to-make-gradle-testkit-depend-on-output-jar-rather-than-just-classes/18940/2
    val gradlePlgExt = project.extensions.getByName<GradlePluginDevelopmentExtension>("gradlePlugin")
    val additionalResources = pluginClasspath.files - files(
      gradlePlgExt.pluginSourceSet.output.classesDirs,
      gradlePlgExt.pluginSourceSet.output.resourcesDir
    )
    pluginClasspath.setFrom(
      files(jar) + additionalResources
    )
    mustRunAfter(jar)
  }

  withType<JacocoReport>().configureEach {
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
  register<JacocoReport>("jacocoMergedReports") {
    group = "verification"
    additionalClassDirs(sourceSets.main.get().output.classesDirs)
    additionalSourceDirs(sourceSets.main.get().allSource.sourceDirectories)
    withType<Test>().map { executionData(it) }
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

//https://github.com/koral--/jacoco-gradle-testkit-plugin/issues/9
fun Test.applyJacocoWorkaround() {
  // Workaround on gradle/jacoco keeping *.exec file locked
  if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
    logger.lifecycle("Running on Windows -> applying jacoco-lock workaround")
    this.doLast("JacocoLockWorkaround") {
      fun File.isLocked() = !renameTo(this)
      logger.lifecycle("Execute workaround")
      val jacocoTestExec = checkNotNull(extensions.getByType(JacocoTaskExtension::class)).destinationFile
      if (null == jacocoTestExec) {
        logger.lifecycle("No exec file ô.Ô?")
        return@doLast
      }
      logger.lifecycle("Waiting for $jacocoTestExec to become unlocked")
      val waitMillis = 100L
      var tries = 0
      while ((!jacocoTestExec.exists() || jacocoTestExec.isLocked()) && (tries++ < 100)) {
        logger.lifecycle("Waiting $waitMillis ms (${jacocoTestExec.name} is locked)...")
        Thread.sleep(waitMillis)
      }
      logger.lifecycle("Done waiting (${jacocoTestExec.name} is unlocked).")
    }
  }
}