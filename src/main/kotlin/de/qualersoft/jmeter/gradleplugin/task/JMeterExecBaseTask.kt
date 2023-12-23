package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

/**
 * This class is an intermediate class to avoid doubling
 * properties and functionality of run- and report-tasks.
 * It is not meant for instantiation nor direct usage.
 */
@DisableCachingByDefault(because = "Dedicated tasks has to decide")
abstract class JMeterExecBaseTask : JMeterBaseTask() {

  /**
   * Path to a custom report-template folder used by report generator.
   *
   * Inherited from [JMeterExtension.customReportTemplateDirectory].
   */
  @InputDirectory
  @Optional
  @PathSensitive(PathSensitivity.ABSOLUTE)
  val customReportTemplateDirectory: DirectoryProperty = objectFactory.directoryProperty()
    .value(jmExt.customReportTemplateDirectory)

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
   * Force jmeter to delete/override any existing output.
   * If `false` but output exists, jmeter fails!
   *
   * Defaults to `false`
   */
  @Input
  var deleteResults: Boolean = false

  protected fun addResultFile(args: MutableList<String>, forReport: Boolean) = args.apply {
    val file = sourceFile.get().asFile
    // result file
    if (forReport) {
      add("-g")
    } else {
      add("-l")
    }
    add(resultDirectory.file("${file.nameWithoutExtension}.jtl").get().asFile.absolutePath)
  }

  protected fun addReport(args: MutableList<String>) {
    if (customReportTemplateDirectory.isPresent) {
      args.add(
        "-J$REPORT_TEMPLATE_DIR_KEY=${customReportTemplateDirectory.get().asFile.absolutePath}"
      )
    }

    val file = sourceFile.get().asFile
    args.add("-o")
    args.add(reportDir.file(file.nameWithoutExtension).get().asFile.absolutePath)
  }

  protected fun addDelete(args: MutableList<String>) {
    if (deleteResults) {
      args.add("-f")
    }
  }

  companion object {
    private const val REPORT_TEMPLATE_DIR_KEY = "jmeter.reportgenerator.exporter.html.property.template_dir"
  }
}
