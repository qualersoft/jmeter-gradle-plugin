package de.qualersoft.jmeter.gradleplugin.task

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
