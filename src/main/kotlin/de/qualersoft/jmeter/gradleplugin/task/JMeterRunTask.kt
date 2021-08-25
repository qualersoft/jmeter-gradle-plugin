package de.qualersoft.jmeter.gradleplugin.task

import org.gradle.api.tasks.Input

open class JMeterRunTask : JMeterBaseTask() {

  @Input
  val generateReport: Boolean = false

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
