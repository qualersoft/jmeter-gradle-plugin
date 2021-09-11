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
    generateReport = true
    customReportTemplateDirectory.set(file("custom-template/report-template"))
    deleteResults = true
  }
}
