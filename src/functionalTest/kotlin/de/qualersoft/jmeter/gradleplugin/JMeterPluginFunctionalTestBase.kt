package de.qualersoft.jmeter.gradleplugin

import io.kotest.matchers.string.shouldContain
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Tag
import java.io.File
import java.io.InputStream


private const val EXT_GR = ".gradle"
private const val EXT_KT = "$EXT_GR.kts"

open class JMeterPluginFunctionalTestBase {

  @Tag("groovy")
  annotation class GroovyTag

  @Tag("kotlin")
  annotation class KotlinTag

  protected val testProjectDir: TemporaryFolder = TemporaryFolder.builder()
    .parentFolder(File("./build/tmp/functionalTest").absoluteFile)
    .assureDeletion()
    .build()

  /**
   * Path to the jmxFile.
   * Will be copied to default location if 
   */
  protected var jmxFile = { "Test.jmx" }
  
  /**
   * Meant to be overridden if required.
   * If not `null` the whole folder will be copied.
   * @see setupGroovyTest
   * @see setupKotlinTest
   */
  protected var rootFolder: () -> String? = { null }

  protected fun setupKotlinTest(baseFileName: String): GradleRunner {
    copyTestFileToTemp(baseFileName, EXT_KT)
    return createRunner()
  }

  protected fun setupGroovyTest(baseFileName: String): GradleRunner {
    copyTestFileToTemp(baseFileName, EXT_GR)
    return createRunner()
  }

  protected fun runShouldSucceed(result: BuildResult) {
    result.output shouldContain "BUILD SUCCESSFUL"
  }

  private fun createRunner() = GradleRunner.create()
    .withProjectDir(testProjectDir.root)
    // Attention: do not enable debug! Details see https://github.com/gradle/gradle/issues/6862
    .withPluginClasspath()
    .withTestKitDir(testProjectDir.newFolder())
    .withJaCoCo()

  /**
   * Copies a jmx-file from `resource` to the default location.
   * @param srcJmx The path to the jmx-file. Defaults to [jmxFile]
   */
  fun copyJmxToDefaultLocation(srcJmx: String = jmxFile()) {
    val destDir = testProjectDir.newFolder("./src/test/jmeter")
    destDir.mkdirs()
    val resource = File(javaClass.classLoader.getResource(srcJmx)!!.file)
    val destFile = destDir.resolve(resource.name)
    resource.copyTo(destFile)
  }

  /**
   * Copies a result-file (*.jtl) from `resource` to the default location.
   * @param source The path to the resource file.
   */
  fun copyResultToDefaultLocation(source: String) {
    val destDir = testProjectDir.newFolder("./build/test-results/jmeter")
    destDir.mkdirs()
    val resource = File(javaClass.classLoader.getResource(source)!!.file)
    val destFile = destDir.resolve(resource.name)
    resource.copyTo(destFile)
  }

  private fun copyTestFileToTemp(resource: String, ext: String): File {
    var res = resource + ext
    // if we have a root folder
    rootFolder()?.also {
      res = "$it/$res"
    }

    val file = File(JMeterPluginFunctionalTestBase::class.java.classLoader.getResource(res)!!.file)
    testProjectDir.create()
    val result = testProjectDir.newFile("build$ext")
    file.inputStream().use { input ->
      result.outputStream().use { output -> input.copyTo(output) }
    }

    val settings = testProjectDir.newFile("settings$ext")
    settings.writeText("")

    return result
  }


  protected fun File.copyTo(file: File) {
    this.inputStream().toFile(file)
  }

  protected fun InputStream.toFile(file: File) {
    use { input ->
      file.outputStream().use { input.copyTo(it) }
    }
  }

  private fun GradleRunner.withJaCoCo(): GradleRunner {
    javaClass.classLoader.getResourceAsStream("testkit-gradle.properties")
      ?.toFile(File(projectDir, "gradle.properties"))
    return this
  }
}
