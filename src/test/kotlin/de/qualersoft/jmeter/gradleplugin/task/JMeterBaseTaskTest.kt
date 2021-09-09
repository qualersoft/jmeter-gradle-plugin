package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.entryEndsWith
import de.qualersoft.jmeter.gradleplugin.matchingEntry
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.contain
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.beEmpty
import io.kotest.matchers.maps.haveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class JMeterBaseTaskTest : JMeterTaskTestBase() {

  //<editor-fold desc="Sys-prop-file">
  @Test
  fun systemPropertyFileDefaultsToUnset() {
    val task = createTask<JMeterBaseTask> {}.get()
    task.jmSystemPropertyFile.isPresent shouldBe false
  }

  @Test
  fun taskMustInheritSystemPropertyFile() {
    val task = createTask<JMeterBaseTask> {
      sysPropertyFile.set(project.file("testSysProps.properties"))
    }.get()

    task.jmSystemPropertyFile.get().asFile shouldBe project.file("testSysProps.properties")
  }

  @Test
  fun systemPropertyFileMustPutToRunArguments() {
    val task = createTask<JMeterBaseTask> {
      sysPropertyFile.set(project.file("sysPropsForArgs.properties"))
    }.get()

    val result = task.createRunArguments()
    result shouldHave entryEndsWith("sysPropsForArgs.properties")
  }
  //</editor-fold>

  //<editor-fold desc="Sys prop key value">
  @Test
  fun systemPropertyDefaultsToEmpty() {
    val task = createTask<JMeterBaseTask> {}.get()
    task.jmSystemProperties.get() should beEmpty()
  }

  @Test
  fun taskMustInheritSystemProperty() {
    val task = createTask<JMeterBaseTask> {
      systemProperties.put("sysKey", "sysVal")
    }.get()

    assertAll(
      { task.jmSystemProperties.get() shouldHaveSize 1 },
      { task.jmSystemProperties.get() should io.kotest.matchers.maps.contain("sysKey", "sysVal") }
    )
  }

  @Test
  fun inheritSystemPropertyCanBeExtended() {
    val task = createTaskWithConfig<JMeterBaseTask>({
      systemProperties.put("sysKey1", "sysVal1")
    }, {
      jmSystemProperties.put("sysKey2", "sysVal2")
    }).get()

    assertAll(
      { task.jmSystemProperties.get() shouldHaveSize 2 },
      { task.jmSystemProperties.get() should io.kotest.matchers.maps.contain("sysKey1", "sysVal1") },
      { task.jmSystemProperties.get() should io.kotest.matchers.maps.contain("sysKey2", "sysVal2") }
    )
  }

  @Test
  fun systemPropertiesMustPutToRunArguments() {
    val task = createTask<JMeterBaseTask> {
      systemProperties.put("aKey", "aValue")
    }.get()

    val result = task.createRunArguments()
    result should contain("-DaKey=aValue")
  }

  @Test
  fun multipleSystemPropertiesMustPutToRunArguments() {
    val task = createTaskWithConfig<JMeterBaseTask>({ }, {
      jmSystemProperties.put("bKey1", "bValue1")
      jmSystemProperties.put("bKey2", "bValue2")
    }).get()

    val result = task.createRunArguments()
    assertAll(
      { result should contain("-DbKey1=bValue1") },
      { result should contain("-DbKey2=bValue2") }
    )
  }
  //</editor-fold>

  //<editor-fold desc="Main property file">
  @Test
  fun mainPropertyFileDefaultsToUnset() {
    val task = createTask<JMeterBaseTask> { }.get()

    task.mainPropertyFile.isPresent shouldBe false
  }

  @Test
  fun taskMustInheritMainPropertyFile() {
    val task = createTask<JMeterBaseTask> {
      mainPropertyFile.set(project.file("main.properties"))
    }.get()

    task.mainPropertyFile.get().asFile shouldBe project.file("main.properties")
  }

  @Test
  fun mainPropertyFileMustPutToRunArguments() {
    val task = createTask<JMeterBaseTask> {
      mainPropertyFile.set(project.file("otherMain.properties"))
    }.get()

    val result = task.createRunArguments()
    result shouldHave entryEndsWith("otherMain.properties")
  }
  //</editor-fold>

  //<editor-fold desc="Additional prop files">
  @Test
  fun additionalPropertyFilesDefaultsToEmpty() {
    val task = createTask<JMeterBaseTask> { }.get()

    withClue("Should be empty") { task.additionalPropertyFiles.isEmpty shouldBe true }
  }

  @Test
  fun taskMustInheritAdditionalPropertyFiles() {
    val task = createTask<JMeterBaseTask> {
      additionalPropertyFiles.from(project.file("addProp.properties"))
    }.get()

    assertAll(
      { task.additionalPropertyFiles.files shouldHaveSize (1) },
      { task.additionalPropertyFiles.files should contain(project.file("addProp.properties")) }
    )
  }

  @Test
  fun extendAdditionalPropertyFilesInTaskDoesNotAffectExtension() {
    val task = createTaskWithConfig<JMeterBaseTask>({
      additionalPropertyFiles.from(
        "Extension.file"
      )
    }, {
      additionalPropertyFiles.from("Task.file")
    }).get()

    val proj = task.project
    val ext = proj.extensions.getByType(JMeterExtension::class.java)
    assertAll(
      { withClue("Ext: ") { ext.additionalPropertyFiles shouldContain proj.file("Extension.file") } },
      { withClue("Ext: ") { ext.additionalPropertyFiles shouldNotContain proj.file("Task.file") } },

      { withClue("Task: ") { task.additionalPropertyFiles shouldContain proj.file("Extension.file") } },
      { withClue("Task: ") { task.additionalPropertyFiles shouldContain proj.file("Task.file") } }
    )
  }

  @Test
  fun additionalPropertyFilesMustPutToRunArguments() {
    val task = createTask<JMeterBaseTask> {
      additionalPropertyFiles.from(
        "Extension.file"
      )
    }.get()

    val result = task.createRunArguments()
    result shouldHave matchingEntry("-q.*Extension\\.file".toRegex())
  }
  //</editor-fold>

  @Test
  fun jmeterPropertiesDefaultsToEmpty() {
    val task = createTask<JMeterBaseTask> { }.get()

    task.jmeterProperties.get() should beEmpty()
  }

  @Test
  fun taskMustInheritJmeterProperties() {
    val task = createTask<JMeterBaseTask> {
      jmeterProperties.put("jmpExtKey", "jmpExtVal")
    }.get()

    assertAll(
      { task.jmeterProperties.get() should haveSize(1) },
      { task.jmeterProperties.get() should io.kotest.matchers.maps.contain("jmpExtKey", "jmpExtVal") }
    )
  }

  @Test
  fun inheritedJmeterPropertiesCanBeExtended() {
    val task = createTaskWithConfig<JMeterBaseTask>({
      jmeterProperties.put("extKey", "extVal")
    }, {
      jmeterProperties.put("taskKey", "taskVal")
    }).get()

    assertAll(
      { task.jmeterProperties.get() should haveSize(2) },
      { task.jmeterProperties.get() should io.kotest.matchers.maps.contain("extKey", "extVal") },
      { task.jmeterProperties.get() should io.kotest.matchers.maps.contain("taskKey", "taskVal") }
    )
  }

  @Test
  fun jmeterPropertiesMustPutToRunArguments() {
    val task = createTask<JMeterBaseTask> { 
      jmeterProperties.put("argKey", "argVal")
    }.get()
    
    val result = task.createRunArguments()
    result should contain("-jargKey=argVal")
  }
  
  // logConfig

  // logOutputFile (default)

  @Test
  fun taskWithNoConfigShouldInheritDefaultConfig() {
    val task = createTask<JMeterBaseTask> { }.get()

    assertAll(
      { withClue("jmxFile should not be present") { task.jmxFile.isPresent shouldBe false } },
      {
        assertAll(
          { withClue("jmeter properties should be present") { task.jmeterProperties.isPresent shouldBe true } },
          { task.jmeterProperties.get() should beEmpty() }
        )
      },
      {
        val repDir = task.reportDir
        assertAll("Report dir",
          withClue("must be present") { { repDir.isPresent shouldBe true } },
          { repDir.get().asFile.path shouldEndWith "jmeter" },
          { repDir.get().asFile.path shouldContain "[\\\\/]reports[\\\\/]".toRegex() }
        )
      },
      {
        val resDir = task.resultDirectory
        assertAll("Result dir",
          { withClue("must be present") { resDir.isPresent shouldBe true } },
          { resDir.get().asFile.path shouldContain "[\\\\/]test-results[\\\\/]".toRegex() },
          { resDir.get().asFile.path shouldEndWith "jmeter" }
        )
      },
      {
        withClue("No default max heap size") { task.maxHeap.isPresent shouldBe false }
      }
    )
  }
}