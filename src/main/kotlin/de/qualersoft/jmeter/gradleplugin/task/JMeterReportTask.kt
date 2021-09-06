package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

/**
 * Task to create jmeter reports.
 */
@CacheableTask
open class JMeterReportTask : JMeterBaseTask() {

  /**
   * Path to a user defined report template folder.
   * Must have name `report-template`.
   *
   * Defaults to `jmeter.tool.reportTemplateFolder`. (If not set uses the bundled)
   */
  @InputDirectory
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @Optional
  val reportTemplate: DirectoryProperty = objectFactory.directoryProperty().value(
    jmExt.tool.reportTemplateDirectory
  )

  override fun processResources(jmBinDir: File) {
    super.processResources(jmBinDir)
    copyReportTemplate(reportTemplate, jmBinDir)
  }

  override fun createRunArguments(): MutableList<String> = mutableListOf<String>().apply {
    val src = sourceFile.asFile.get()
    // result file
    add("-g")
    add(resultDirectory.file("${src.nameWithoutExtension}.jtl").get().asFile.absolutePath)
    // output dir
    add("-o")
    add(reportDir.file(src.nameWithoutExtension).get().asFile.absolutePath)

    // log file
    add("-j")
    add(resultDirectory.file("${src.nameWithoutExtension}.log").get().asFile.absolutePath)

    // user properties file goes first to allow override by dedicated user properties
    if (globalPropertiesFile.isPresent) {
      add("-G${globalPropertiesFile.get().asFile.absolutePath}")
    }

    globalProperties.get().forEach { (k, v) ->
      add("-G$k=$v")
    }

    if (deleteResults) {
      add("-f")
    }
  }
}
