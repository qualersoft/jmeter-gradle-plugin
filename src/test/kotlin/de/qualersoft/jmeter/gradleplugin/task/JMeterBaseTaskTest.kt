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
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.match
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.io.File

class JMeterBaseTaskTest : JMeterTaskTestBase() {

  //<editor-fold desc="Sys-prop-file">
  @Test
  fun systemPropertyFileDefaultsToEmpty() {
    val task = createTask<JMeterBaseTask> {}.get()
    task.jmSystemPropertyFiles.isEmpty shouldBe true
  }

  @Test
  fun taskMustInheritSystemPropertyFile() {
    val task = createTask<JMeterBaseTask> {
      systemPropertyFiles.from(project.file("testSysProps.properties"))
    }.get()

    task.jmSystemPropertyFiles shouldContain project.file("testSysProps.properties")
  }

  @Test
  fun systemPropertyFileMustPutToRunArguments() {
    val task = createTask<JMeterBaseTask> {
      systemPropertyFiles.from(project.file("sysPropsForArgs.properties"))
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

    assertAll(
      { result shouldContain "-q" },
      { result shouldHave entryEndsWith("Extension.file") }
    )
  }
  //</editor-fold>

  //<editor-fold desc="jmeter Properties">
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
    result should contain("-JargKey=argVal")
  }
  //</editor-fold>

  //<editor-fold desc="log config">
  @Test
  fun noLogConfigByDefault() {
    val task = createTask<JMeterBaseTask> { }.get()

    val args = task.createRunArguments()

    args shouldNot contain("-i")
  }

  @Test
  fun taskMustRespectLogConfig() {
    val task = createTask<JMeterBaseTask> {
      logConfig.set(project.file("myCustomLogConf.xml"))
    }.get()

    val args = task.createRunArguments()

    assertAll(
      { withClue("logConfig flag exists") { args should contain("-i") } },
      { args shouldHave matchingEntry(".*myCustomLogConf.xml".toRegex()) }
    )
  }
  //</editor-fold>

  //<editor-fold desc="logOutputFile">
  @Test
  fun logOutputHasDefault() {
    val task = createTask<JMeterBaseTask> { }.get()

    assertAll(
      { withClue("Log file should exist") { task.logOutputFile.isPresent shouldBe true } },
      { task.logOutputFile.get().asFile.name shouldBe "jmeter.log" }
    )
  }

  @Test
  fun taskMustInheritLogOutputFile() {
    val task = createTask<JMeterBaseTask> {
      logOutputFile.set(project.file("customExt.log"))
    }.get()

    task.logOutputFile.get().asFile.name shouldBe "customExt.log"
  }

  @Test
  fun logOutputFileCanBeOverriddenByTask() {
    val task = createTaskWithConfig<JMeterBaseTask>({ }, {
      logOutputFile.set(project.file("task.log"))
    }).get()
    task.logOutputFile.get().asFile.name shouldBe "task.log"
  }

  @Test
  fun logOutputFileMustPutToRunArguments() {
    val task = createTask<JMeterBaseTask> {
      logOutputFile.set(project.file("args.log"))
    }.get()

    val result = task.createRunArguments()
    assertAll(
      { withClue("logOutput flag") { result should contain("-j") } },
      { result shouldHave matchingEntry(".*args\\.log".toRegex()) }
    )
  }

  @Test
  fun unsetLogOutputIsNotInRunArguments() {
    val task = createTaskWithConfig<JMeterBaseTask>({ }, {
      logOutputFile.set(null as File?)
    }).get()

    val result = task.createRunArguments()
    withClue("logOutput flag") { result shouldNot contain("-j") }
  }
  //</editor-fold>

  //<editor-fold desc="JMX-file">
  @Test
  fun noDefaultJmxFile() {
    val task = createTask<JMeterBaseTask> { }.get()
    task.jmxFile.isPresent shouldBe false
  }

  @Test
  fun jmxFileIsMandatoryOnResolution() {
    val task = createTask<JMeterBaseTask> { }.get()

    assertThrows<IllegalStateException> {
      task.sourceFile.get()
    }
  }

  @Test
  fun relativeJmxFileGetsResolvedAgainstRoot() {
    val task = createTaskWithConfig<JMeterBaseTask>({ }, {
      jmxFile.set("relative.jmx")
    }).get()

    task.sourceFile.get().asFile shouldBe project.file("src/test/jmeter/relative.jmx")
  }

  @Test
  fun absoluteJmxFileWillNotResolvedAgainstRoot() {
    val task = createTaskWithConfig<JMeterBaseTask>({ }, {
      jmxFile.set(project.projectDir.resolve("/absolute.jmx").absolutePath)
    }).get()

    assertAll(
      { task.sourceFile.get().asFile shouldBe project.projectDir.resolve("/absolute.jmx").absoluteFile },
      { task.sourceFile.get().asFile.absolutePath shouldNot match(".*src[/\\\\]test[/\\\\]jmeter".toRegex()) }
    )
  }
  //</editor-fold>

  @Test
  fun taskWithNoConfigShouldInheritDefaultConfig() {
    val task = createTask<JMeterBaseTask> { }.get()

    assertAll(
      { withClue("jmxFile should not be present") { task.jmxFile.isPresent shouldBe false } },
      {
        withClue("No default max heap size") { task.maxHeap.isPresent shouldBe false }
      }
    )
  }
}