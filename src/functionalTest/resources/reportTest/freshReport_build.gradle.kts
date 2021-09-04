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
  }
  register<JMeterReportTask>("customReport") {
    jmxFile.set("Test.jmx")
    deleteResults = true
    reportTemplate.set(file("custom-template/report-template"))
  }
}
