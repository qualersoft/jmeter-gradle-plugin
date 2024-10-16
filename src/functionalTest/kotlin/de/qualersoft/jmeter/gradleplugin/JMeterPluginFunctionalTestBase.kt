package de.qualersoft.jmeter.gradleplugin

import io.kotest.matchers.string.shouldContain
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.io.InputStream
import java.lang.management.ManagementFactory
import java.util.zip.ZipFile

open class JMeterPluginFunctionalTestBase {

  protected val testProjectDir: TemporaryFolder = TemporaryFolder.builder()
    .parentFolder(File("./build/tmp/functionalTest").absoluteFile)
    .assureDeletion()
    .build()

  /**
   * Path to the jmxFile.
   * Will be copied to default location.
   * Defaults to `Test.jmx`
   */
  protected var jmxFile = { "Test.jmx" }

  /**
   * Meant to be overridden if required.
   * If not `null` the whole folder will be copied.
   * @see copyTestFileToTemp
   */
  protected var rootFolder: () -> String? = { null }

  protected fun setupTest(baseFileName: String, ext: String = "gradle.kts"): GradleRunner {
    copyTestFileToTemp(baseFileName, ext)
    return createRunner()
  }

  protected fun runShouldFail(result: BuildResult, reason: String = "") {
    result.output shouldContain "FAILURE: $reason"
  }

  private fun createRunner() = GradleRunner.create()
    .withProjectDir(testProjectDir.root)
    .withPluginClasspath()
    .withDebug(ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf("-agentlib:jdwp") > 0)
    .withTestKitDir(testProjectDir.newFolder())
    .withJaCoCo()
    .also {
      if (it.isDebug) {
        it.forwardOutput()
      }
    }

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

  fun copyZipResourceTo(resource: String, targetDir: File) {
    val tmp = testProjectDir.newFolder()
    val tmpZip = tmp.resolve(resource)

    checkNotNull(javaClass.classLoader.getResourceAsStream(resource)) { "Resource '$resource' not found!" }
      .copyTo(tmpZip.outputStream())
    val zip = ZipFile(tmpZip)
    zip.use { zf ->
      zf.stream().forEach { ze ->
        val targetEntry = targetDir.resolve(ze.name)
        if (ze.isDirectory) {
          targetEntry.mkdirs()
        } else {
          zf.getInputStream(ze).copyTo(targetEntry.outputStream())
        }
      }
    }
  }

  private fun copyTestFileToTemp(resource: String, ext: String): File {
    var res = "$resource.$ext"
    // if we have a root folder
    rootFolder()?.also {
      res = "$it/$res"
    }

    val file = File(JMeterPluginFunctionalTestBase::class.java.classLoader.getResource(res)!!.file)
    testProjectDir.create()
    val result = testProjectDir.newFile("build.$ext")
    file.inputStream().use { input ->
      result.outputStream().use { output -> input.copyTo(output) }
    }

    val settings = testProjectDir.newFile("settings.$ext")
    settings.writeText("""rootProject.name = "$resource"""")

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
