package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.PluginTestBase
import de.qualersoft.jmeter.gradleplugin.entryEndsWith
import de.qualersoft.jmeter.gradleplugin.entryStartsWith
import de.qualersoft.jmeter.gradleplugin.matchingEntry
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.contain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotHave
import io.kotest.matchers.types.beInstanceOf
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File
import io.kotest.matchers.collections.beEmpty as beEmptyList

/**
 * Remarks: Because gui task is basically a JMeterBaseTask,
 * test are targeting on property chaining
 */
class JMeterGuiTaskTest : JMeterTaskTestBase() {

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
  fun taskWithJustJmxFileIsEnough() {
    val task = createTaskWithConfig<JMeterGuiTask>({ }, {
      jmxFile.set("Test.jmx")
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { args shouldNot beEmptyList() },
      { withClue("test file flag") { args should contain("-t") } },
      { args shouldHave entryEndsWith("Test.jmx") },
      { withClue("log file flag") { args should contain("-j") } },
      { args shouldHave entryEndsWith("jmeter.log") },
      { withClue("jMeterProperty") { args shouldNotHave entryStartsWith("-J") } },
      { withClue("globalProperty") { args shouldNotHave entryStartsWith("-G") } },
      { withClue("delete flag") { args shouldNot contain("-f") } }
    )
  }

  @Test
  fun canRunWithoutJmxFile() {
    val task = createTask<JMeterGuiTask> { }.get()
    val args = task.createRunArguments()
    args shouldNot contain("-t")
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
  fun canCreateGuiTaskFromExtension() {
    val project = createProject()
    val ext = project.extensions.getByType(JMeterExtension::class.java)
    ext.withGuiTask("guiTestTask")

    val result = project.tasks.getByName("guiTestTask")
    assertAll(
      { result shouldNot beNull() },
      { result should beInstanceOf<JMeterGuiTask>() },
      { project.tasks.withType(JMeterGuiTask::class.java) shouldHaveSize 1 }
    )
  }

  @Test
  fun canCreateGuiTaskFromExtensionAndApplyConfig() {
    val project = createProject()
    val ext = project.extensions.getByType(JMeterExtension::class.java)
    ext.withGuiTask("configuredGuiTask") {
      it.jmxFile.set("test.jmx")
    }

    val result = project.tasks.getByName("configuredGuiTask")
    (result as JMeterGuiTask).jmxFile.get() shouldBe "test.jmx"
  }

  @Test
  fun `should never be up-to-date`() {
    val task = createTask<JMeterGuiTask>().get()

    val result = task.outputs.upToDateSpec.isSatisfiedBy(task)

    result shouldBe false
  }

  private fun createProject() = ProjectBuilder.builder().build()
    .also {
      it.plugins.apply(PluginTestBase.PLUGIN_ID)
    }
}
