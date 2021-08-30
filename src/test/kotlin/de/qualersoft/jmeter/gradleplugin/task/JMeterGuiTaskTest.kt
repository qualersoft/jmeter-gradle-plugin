package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.entryEndsWith
import de.qualersoft.jmeter.gradleplugin.entryStartsWith
import de.qualersoft.jmeter.gradleplugin.matchingEntry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.contain
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotHave
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File
import io.kotest.matchers.collections.beEmpty as beEmptyList
import io.kotest.matchers.maps.beEmpty as beEmptyMap

/**
 * Remarks: Because gui task is basically a JMeterBaseTask,
 * test are targeting on property chaining
 */
class JMeterGuiTaskTest : JMeterTaskTestBase() {

  @Test
  fun taskWithNoConfigShouldInheritDefaultConfig() {
    val task = createTask<JMeterGuiTask> { }.get()

    assertAll(
      { withClue("jmxFile should not be present") { task.jmxFile.isPresent shouldBe false } },
      {
        assertAll(
          { withClue("jmeter properties should be present") { task.jmeterProperties.isPresent shouldBe true } },
          { task.jmeterProperties.get() should beEmptyMap() }
        )
      },
      { withClue("global properties file must not be present") { task.globalPropertiesFile.isPresent shouldBe false } },
      {
        assertAll(
          "globale Properties",
          { withClue("must be present") { task.globalProperties.isPresent shouldBe true } },
          { withClue("must be empty") { task.globalProperties.get() should beEmptyMap() } },
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
        withClue("Delete results should be false") { task.deleteResults shouldBe false }
      },
      {
        withClue("Max heap size") { task.maxHeap.get() shouldBe "1024m" }
      }
    )
  }

  @Test
  fun taskShouldInheritGlobalPropertiesFromExtension() {
    val task = createTask<JMeterGuiTask> {
      globalProperties.put("usr", "prop")
    }

    assertAll(
      { task.get().globalProperties.get() shouldHaveSize 1 },
      { task.get().globalProperties.get() shouldContain ("usr" to "prop") }
    )
  }

  @Test
  fun taskShouldOverrideInheritedExtensionGlobalProperties() {
    val task = createTaskWithConfig<JMeterGuiTask>({
      globalProperties.put("usr", "prop")
    }, {
      globalProperties.set(mapOf("task" to "property"))
    })

    assertAll(
      { task.get().globalProperties.get() shouldHaveSize 1 },
      { task.get().globalProperties.get() shouldContain ("task" to "property") }
    )
  }

  @Test
  fun taskCanExtendInheritedExtensionGlobalProperties() {
    val task = createTaskWithConfig<JMeterGuiTask>({
      globalProperties.put("usr", "prop")
    }, {
      globalProperties.put("task", "property")
    })

    assertAll(
      { task.get().globalProperties.get() shouldHaveSize 2 },
      { task.get().globalProperties.get() shouldContain ("usr" to "prop") },
      { task.get().globalProperties.get() shouldContain ("task" to "property") }
    )
  }

  @Test
  fun taskMustHaveJmxFileConfigured() {
    val task = createTask<JMeterGuiTask> { }.get()
    shouldThrow<IllegalStateException> { task.createRunArguments() }
  }

  @Test
  fun taskWithJustJmxFileIsEnough() {
    val task = createTaskWithConfig<JMeterGuiTask>({}, {
      jmxFile.set("Test.jmx")
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { args shouldNot beEmptyList() },
      { withClue("test file flag") { args should contain("-t") } },
      { args shouldHave entryEndsWith("Test.jmx") },
      { withClue("result file flag") { args should contain("-l") } },
      { args shouldHave entryEndsWith("Test.jtl") },
      { withClue("log file flag") { args should contain("-j") } },
      { args shouldHave entryEndsWith("Test.log") },
      { withClue("jMeterProperty") { args shouldNotHave entryStartsWith("-J") } },
      { withClue("globalProperty") { args shouldNotHave entryStartsWith("-G") } },
      { withClue("delete flag") { args shouldNot contain("-f") } }
    )
  }

  @Test
  fun taskWithDeleteEnabledFlag() {
    val task = createTaskWithConfig<JMeterGuiTask>({}, {
      jmxFile.set("Test.jmx")
      deleteResults = true
    }).get()
    val args = task.createRunArguments()
    args should contain("-f")
  }

  @Test
  fun runArgsWithGlobalPropertyFileFromExtension() {
    val fileName = "globalExtPropFile.properties"
    val task = createTaskWithConfig<JMeterGuiTask>(
      { globalPropertiesFile.set(File(fileName)) },
      { jmxFile.set("Test.jmx") }
    ).get()

    val args = task.createRunArguments()
    args shouldHave matchingEntry("-G.*$fileName".toRegex())
  }

  @Test
  fun taskWithGlobalPropertyFileFromTask() {
    val fileName = "globalTaskPropFile.properties"
    val task = createTaskWithConfig<JMeterGuiTask>({ }, {
      globalPropertiesFile.set(File(fileName))
      jmxFile.set("Test.jmx")
    }).get()

    val args = task.createRunArguments()
    args shouldHave matchingEntry("-G.*$fileName".toRegex())
  }

  @Test
  fun taskWithGlobalPropertyFileFromTaskOverridesConfig() {
    val confFileName = "globalConfPropFile.properties"
    val taskFileName = "globalTaskPropFile.properties"
    val task = createTaskWithConfig<JMeterGuiTask>({
      globalPropertiesFile.set(File(confFileName))
    }, {
      globalPropertiesFile.set(File(taskFileName))
      jmxFile.set("Test.jmx")
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { args shouldNotHave matchingEntry("-G.*$confFileName".toRegex()) },
      { args shouldHave matchingEntry("-G.*$taskFileName".toRegex()) }
    )
  }

  @Test
  fun runArgsWithGlobalProperties() {
    val task = createTaskWithConfig<JMeterGuiTask>({
      globalProperties.put("conf", "prop1")
    }, {
      globalProperties.put("task", "prop2")
      jmxFile.set("Test.jmx")
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { args should contain("-Gconf=prop1") },
      { args should contain("-Gtask=prop2") }
    )
  }

  @Test
  fun runArgsWithJMeterProperties() {
    val task = createTaskWithConfig<JMeterGuiTask>({
      jmeterProperties.put("conf", "prop1")
    }, {
      jmeterProperties.put("task", "prop2")
      jmxFile.set("Test.jmx")
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { args should contain("-Jconf=prop1") },
      { args should contain("-Jtask=prop2") }
    )
  }
}