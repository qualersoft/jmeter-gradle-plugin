package de.qualersoft.jmeter.gradleplugin.task

import org.gradle.work.DisableCachingByDefault

@Suppress("UnstableApiUsage")
@DisableCachingByDefault(because = "Gui can be started always")
open class JMeterGuiTask : JMeterBaseTask() {

  init {
    outputs.upToDateWhen {
      false
    }
  }

  override fun createRunArguments() = mutableListOf<String>().also {
    it.addAll(super.createRunArguments())
  }
}
