import de.qualersoft.jmeter.gradleplugin.task.*

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
    jmxFile.set("Test.jmx")
  }
}
