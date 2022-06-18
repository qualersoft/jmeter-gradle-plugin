plugins {
  id("de.qualersoft.jmeter")
}

jmeter {
  tool {
    version = project.findProperty("toolVersion") as String? ?: "5.4.3"
  }
}

repositories {
  mavenCentral()
}
