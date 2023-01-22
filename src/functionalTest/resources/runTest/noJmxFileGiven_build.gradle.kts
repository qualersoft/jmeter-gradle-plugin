import de.qualersoft.jmeter.gradleplugin.task.JMeterRunTask

plugins {
  id("de.qualersoft.jmeter")
}

jmeter {
  resultDir.set(file("jmeter-results"))
}

repositories {
  mavenCentral()
}

tasks {
  register<JMeterRunTask>("runTest") {
  }
}
