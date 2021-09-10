package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.CopyResource
import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * This class is an intermediate class to avoid doubling
 * properties and functionality of run- and report-tasks.
 * It is not meant for instantiation nor direct usage.
 */
@Suppress("UnstableApiUsage")
@DisableCachingByDefault(because = "Dedicated tasks has to decide")
abstract class JMeterExecBaseTask: JMeterBaseTask() {

  /**
   * Directory where to store the results.
   *
   * Inherited from [JMeterExtension.resultDir]
   */
  @OutputDirectory
  val resultDirectory: DirectoryProperty = objectFactory.directoryProperty()
    .value(jmExt.resultDir)

  /**
   * Directory where to create the report.
   *
   * Inherited from [JMeterExtension.reportDir]
   */
  @OutputDirectory
  val reportDir: DirectoryProperty = objectFactory.directoryProperty()
    .value(jmExt.reportDir)

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

  /**
   * Copies the report-template either from given directory property if present or
   * from bundled to the jmeter-bin directory.
   *
   * @param reportTemplate The property pointing to the custom report template.
   * @param jmBinDir The target directory to which to copy the template.
   */
  protected fun copyReportTemplate(reportTemplate: DirectoryProperty, jmBinDir: File) {
    copyRespectProperty(jmExt.tool.reportGeneratorPropertyFile, "reportgenerator.properties", jmBinDir)
    // copy report-template dir
    val destReportTempDir = jmBinDir.resolve("report-template")
    if (reportTemplate.isPresent) {
      reportTemplate.asFile.get().copyRecursively(destReportTempDir, true)
    } else {
      // copy from jar
      // ensure directory exists
      if (destReportTempDir.exists()) {
        destReportTempDir.delete()
      }
      destReportTempDir.mkdirs()
      CopyResource.copyJarEntriesToFolder("report-template", destReportTempDir)
    }
  }

  protected fun addResultFile(args: MutableList<String>, forReport: Boolean) = args.apply {
    val file = sourceFile.get().asFile
    // result file
    if(forReport) {
      add("-g")
    } else {
      add("-l")
    }
    add(resultDirectory.file("${file.nameWithoutExtension}.jtl").get().asFile.absolutePath)
  }

  protected fun addReport(args: MutableList<String>) {
    val file = sourceFile.get().asFile
    args.add("-o")
    args.add(reportDir.file(file.nameWithoutExtension).get().asFile.absolutePath)
  }

  protected fun addDelete(args: MutableList<String>) {
    if (deleteResults) {
      args.add("-f")
    }
  }
}
