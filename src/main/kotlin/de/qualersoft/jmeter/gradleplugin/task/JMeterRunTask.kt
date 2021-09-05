package de.qualersoft.jmeter.gradleplugin.task

import org.gradle.api.tasks.Input
import org.gradle.work.DisableCachingByDefault

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
