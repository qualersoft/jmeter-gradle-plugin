import de.qualersoft.jmeter.gradleplugin.task.JMeterRunTask
import de.qualersoft.jmeter.gradleplugin.task.JMeterReportTask

plugins {
  id("de.qualersoft.jmeter")
}

repositories {
  mavenCentral()
}

jmeter {
  tool {
    customReportTemplateDirectory.set(file("custom-template/report-template"))
  }
}

tasks {
  register<JMeterRunTask>("runTest") {
    jmxFile.set("Test.jmx")
  }
  register<JMeterReportTask>("reportTest") {
    jmxFile.set("Test.jmx")
  }
}
