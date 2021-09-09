package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
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

  /**
   * Force jmeter to delete/override any existing output.
   * If `false` but output exists, jmeter fails!
   *
   * Defaults to `false`
   */
  @Input
  var deleteResults: Boolean = false

  override fun processResources(jmBinDir: File) {
    super.processResources(jmBinDir)
    copyReportTemplate(reportTemplate, jmBinDir)
  }

  override fun createRunArguments(): MutableList<String> = mutableListOf<String>().apply {
    addAll(super.createRunArguments())

    addJmxFile(this)

    addResultFile(this, true)

    addReport(this)

    addDelete(this, deleteResults)
  }
}
