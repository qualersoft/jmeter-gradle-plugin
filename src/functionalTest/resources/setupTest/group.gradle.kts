plugins {
  id("de.qualersoft.jmeter")
}

jmeter {
  tool {
    group = "com.example.dummy"
  }
}

repositories {
  mavenCentral()
}