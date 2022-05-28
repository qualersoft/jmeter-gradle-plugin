import de.qualersoft.jmeter.gradleplugin.task.JMeterSetupTask

plugins {
  id("de.qualersoft.jmeter")
}

repositories {
  mavenCentral()
}

tasks {
  register<JMeterSetupTask>("setup2")
}
