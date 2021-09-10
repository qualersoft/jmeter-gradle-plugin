package de.qualersoft.jmeter.gradleplugin.task

import org.gradle.api.tasks.CacheableTask
import java.io.File

/**
 * Task to create jmeter reports.
 */
@CacheableTask
open class JMeterReportTask : JMeterExecBaseTask() {


  override fun processResources(jmBinDir: File) {
    super.processResources(jmBinDir)
    copyReportTemplate(reportTemplate, jmBinDir)
  }

  override fun createRunArguments(): MutableList<String> = mutableListOf<String>().apply {
    addAll(super.createRunArguments())

    addJmxFile(this)

    addResultFile(this, true)

    addReport(this)

    addDelete(this)
  }
}
