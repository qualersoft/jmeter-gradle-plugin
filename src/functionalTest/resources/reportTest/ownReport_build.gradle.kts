import de.qualersoft.jmeter.gradleplugin.task.*

plugins {
  id("de.qualersoft.jmeter")
}

repositories {
  mavenCentral()
}

tasks {
  register<JMeterRunTask>("runTest") {
    jmxFile.set("Test.jmx")
  }
  register<JMeterReportTask>("reportTest") {
    jmxFile.set("Test.jmx")
    reportTemplate.set(file("custom-template/report-template"))
  }
}
