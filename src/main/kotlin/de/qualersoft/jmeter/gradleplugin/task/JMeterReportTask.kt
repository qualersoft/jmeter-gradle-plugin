package de.qualersoft.jmeter.gradleplugin.task

import org.gradle.api.tasks.CacheableTask

/**
 * Task to create jmeter reports.
 */
@CacheableTask
open class JMeterReportTask : JMeterExecBaseTask() {

  override fun createRunArguments(): MutableList<String> = mutableListOf<String>().apply {
    addAll(super.createRunArguments())

    addJmxFile(this)

    addResultFile(this, true)

    addReport(this)

    addDelete(this)
  }
}
