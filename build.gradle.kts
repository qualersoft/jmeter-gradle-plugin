import de.qualersoft.parseSemVer

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
  id("org.jetbrains.changelog") version "1.3.0"
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

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  version.set(project.version.toString())
  groups.set(emptyList())
}

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
