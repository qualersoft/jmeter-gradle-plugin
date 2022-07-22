import de.qualersoft.parseSemVer

plugins {
  // implementation
  `java-gradle-plugin`
  kotlin("jvm") version "1.6.10"

  // quality
  jacoco
  id("pl.droidsonroids.jacoco.testkit") version "1.0.9"
  id("io.gitlab.arturbosch.detekt") version "1.18.0"

  // documentation
  id("org.jetbrains.dokka") version "1.5.0"
  id("org.asciidoctor.jvm.convert") version "3.3.2"

  // publishing
  `maven-publish`
  id("com.gradle.plugin-publish") version "1.0.0"
  id("org.jetbrains.changelog") version "1.3.0"
}

group = "de.qualersoft"

repositories {
  mavenCentral()
}

dependencies {
  // Align versions of all Kotlin components
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

  testImplementation(platform("org.junit:junit-bom:5.7.2"))
  testImplementation(group = "org.junit.jupiter", name = "junit-jupiter")
  testImplementation(group = "io.kotest", name = "kotest-assertions-core", version = "4.6.4")

  testRuntimeOnly(kotlin("script-runtime"))
}

// Add a source set for the functional test suite
val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest")
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

gradlePlugin {
  // Define the plugin
  plugins.create("jmeter") {
    id = "de.qualersoft.jmeter"
    implementationClass = "de.qualersoft.jmeter.gradleplugin.JMeterPlugin"

    displayName = "jmeter gradle plugin"
    description = "Plugin to execute JMeter tests."
  }
  testSourceSets(sourceSets.test.get(), functionalTestSourceSet)
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

val javaVersion = JavaVersion.VERSION_1_8
java {
  targetCompatibility = javaVersion
  withSourcesJar()
  withJavadocJar()
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  version.set(project.version.toString())
  groups.set(emptyList())
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
    this.jvmTarget = javaVersion.toString()
  }

  // Setup functional test sets
  val functionalTest: TaskProvider<Test> by registering(Test::class) {
    description = "Run the functional tests"
    group = "verification"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = sourceSets.named("functionalTest").get().runtimeClasspath
    shouldRunAfter(test)
    dependsOn(jar)
    applyJacocoWorkaround()
  }
  jacocoTestKit.applyTo("functionalTestRuntimeOnly", functionalTest as TaskProvider<Task>)
  check {
    dependsOn(functionalTest)
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

  withType<Test>().configureEach {
    useJUnitPlatform()
  }

  withType<JacocoReport> {
    executionData(withType<Test>())
    reports {
      csv.required.set(false)
      html.required.set(true)
      xml.required.set(true)
    }
  }

  dokkaJavadoc.configure {
    outputDirectory.set(javadoc.get().destinationDir)
  }

  javadoc {
    dependsOn(dokkaJavadoc)
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

val KINDS = listOf("major", "minor", "patch", "snapshot")
tasks.register("nextVersion") {
  doLast {
    val kind = getKind(this)
    val semVer = parseSemVer(project.version.toString())
    semVer.updateByKind(kind)
    logger.lifecycle("NewVersion=$semVer")
  }
}

tasks.register("updateVersion") {
  description = """ONLY FOR CI/CD purposes!
    |
    |This task is meant to be used by CI/CD to generate new release versions.
    |Prerequists: a `gradle.properties` next to this build-script must exist.
    |   version must follow semver-schema: <number>'.'<number>'.'<number>('-'.*)?
    |Usage:
    |  > ./gradlew updateVersion -Pkind=${KINDS.joinToString("|", "[", "]")}
  """.trimMargin()

  doLast {
    val kind = getKind(this)

    val semVersion = parseSemVer(project.version.toString())
    semVersion.updateByKind(kind)
    semVersion.persist(getGradlePropsFile())
  }
}

fun getKind(task: Task) = (project.findProperty("kind") as String?)?.let { kind ->
  val cleanKind = kind.trim()
  KINDS.firstOrNull { it.equals(cleanKind, true) }
    ?: throw IllegalArgumentException("Given kind '$kind' is none of ${KINDS.joinToString("|", "[", "]")}")
} ?: throw IllegalArgumentException(
  "No `kind` specified! Usage: ./gradlew ${task.name} -Pkind=${KINDS.joinToString("|", "[", "]")}"
)

fun getGradlePropsFile(): File {
  val propsFile = files("./gradle.properties").singleFile
  if (!propsFile.exists()) {
    val msg = "This task requires version to be stored in gradle.properties file, which does not exist!"
    throw UnsupportedOperationException(msg)
  }
  return propsFile
}

object LOCK {
  const val waitMillis = 200L
  const val maxTries = 100
}

// https://github.com/koral--/jacoco-gradle-testkit-plugin/issues/9
fun Test.applyJacocoWorkaround() {
  // Workaround on gradle/jacoco keeping *.exec file locked
  if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
    this.doLast("JacocoLockWorkaround") {
      logger.lifecycle("Running on Windows -> using jacoco-lock workaround")
      fun File.isLocked() = !renameTo(this)
      logger.lifecycle("Execute workaround")
      val jacocoTestExec = checkNotNull(extensions.getByType(JacocoTaskExtension::class)).destinationFile
      if (null == jacocoTestExec) {
        logger.lifecycle("No exec file ô.Ô?")
        return@doLast
      }
      logger.lifecycle("Waiting for $jacocoTestExec to become unlocked")
      var tries = 0
      while ((!jacocoTestExec.exists() || jacocoTestExec.isLocked()) && (tries++ < LOCK.maxTries)) {
        logger.lifecycle("Waiting ${LOCK.waitMillis} ms (${jacocoTestExec.name} is locked) the ${tries}th time...")
        Thread.sleep(LOCK.waitMillis)
      }
      logger.lifecycle("Done waiting (${jacocoTestExec.name} is unlocked).")
    }
  }
}
