plugins {
  id("de.qualersoft.jmeter")
}

jmeter {
  tool {
    mainConfigureClosure = {
      logger.lifecycle("Reconfiguring tools dependencies")
    }
  }
}

repositories {
  mavenCentral()
}
