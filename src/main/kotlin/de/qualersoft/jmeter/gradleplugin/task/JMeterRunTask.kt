package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Task to execute jmeter through cli mode (no gui).
 * 
 * This is the preferred way to run performance tests.
 */
@Suppress("UnstableApiUsage")
@DisableCachingByDefault(because = "Would love to execute jmeter tests more than once;)")
open class JMeterRunTask : JMeterBaseTask() {

  /**
   * If `true` the report will automatically be generated after executions.
   *
   * *Remark*: Consider to also enable [deleteResults] to avoid failures on rerun.
   *
   * Defaults to `false`
   */
  @Input
  var generateReport: Boolean = false

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
    if (generateReport) {
      copyReportTemplate(reportTemplate, jmBinDir)
    }
  }

  override fun createRunArguments() = mutableListOf<String>().also {
    it.add("-n") // no gui

    it.addAll(super.createRunArguments())

    if (generateReport) {
      it.add("-e")
      it.add("-o")
      it.add(reportDir.get().asFile.absolutePath)
    }
  }
}
