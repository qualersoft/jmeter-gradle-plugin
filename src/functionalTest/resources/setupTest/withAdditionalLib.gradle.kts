plugins {
  id("de.qualersoft.jmeter")
}

repositories {
  mavenCentral()
}

dependencies {
  jmeterLibrary(group = "javax.annotation", name = "javax.annotation-api", version = "1.3.2")
}
