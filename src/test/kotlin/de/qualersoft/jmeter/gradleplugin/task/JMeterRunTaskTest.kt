package de.qualersoft.jmeter.gradleplugin.task

import de.qualersoft.jmeter.gradleplugin.JMeterExtension
import de.qualersoft.jmeter.gradleplugin.entryEndsWith
import de.qualersoft.jmeter.gradleplugin.matchingEntry
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.contain
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotHave
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File

class JMeterRunTaskTest : JMeterTaskTestBase() {

  @Test
  fun withReportFlagEnabled() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      jmxFile.set("Test.jmx")
      generateReport = true
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { withClue("No gui flag") { args should contain("-n") } },
      { withClue("Enable report flag") { args should contain("-e") } },
      { withClue("Output param") { args should contain("-o") } },
      { args shouldHave entryEndsWith("reports${File.separatorChar}jmeter${File.separatorChar}Test") }
    )
  }

  @Test
  fun withReportFlagDisabled() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      jmxFile.set("Test.jmx")
      generateReport = false
    }).get()

    val args = task.createRunArguments()
    assertAll(
      { withClue("Enable report flag") { args shouldNot contain("-e") } },
      { withClue("Output param") { args shouldNot contain("-o") } },
      { args shouldNotHave entryEndsWith("reports${File.separatorChar}jmeter") }
    )
  }

  @Test
  fun argsWithGlobalPropertyFile() {
    val propFile = "GlobPropFile.properties"
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      globalPropertiesFile.set(File(propFile))
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args shouldHave matchingEntry("-G[^=]+$propFile".toRegex())
  }

  @Test
  fun argsWithGlobalProperties() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      globalProperties.put("Global", "property")
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args should contain("-GGlobal=property")
  }

  @Test
  fun withDeleteFlag() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      deleteResults = true
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args should contain("-f")
  }

  @Test
  fun withoutDeleteFlag() {
    val task = createTaskWithConfig<JMeterRunTask>({}, {
      deleteResults = false
      jmxFile.set("Report.jmx")
    }).get()

    val args = task.createRunArguments()
    args shouldNot contain("-f")
  }

  @Nested
  @DisplayName("Proxy settings tests")
  inner class ProxyTests {

    @Test
    fun withAllProxySettingsFromExtension() {
      val scheme = "https"
      val host = "127.0.0.1"
      val port = 8080
      val nonHosts = listOf("localhost", "localhorst")
      val task = createTaskWithConfig<JMeterRunTask>({
        proxyScheme.set(scheme)
        proxyHost.set(host)
        proxyPort.set(port)
        nonProxyHosts.addAll(nonHosts)
      }, {}).get()
      assertAll(
        { task.proxyScheme.get() shouldBe scheme },
        { task.proxyHost.get() shouldBe host },
        { task.proxyPort.get() shouldBe port },
        { task.nonProxyHosts.get() should containExactlyInAnyOrder(nonHosts) }
      )
    }

    @Test
    fun withProxySettingsFromExtensionOverridden() {
      val extScheme = "smtp"
      val extHost = "10.3.5.42"
      val extPort = 666
      val extNonHost = "example.com"

      val scheme = "https"
      val host = "127.0.0.1"
      val port = 8080
      val nonHosts = listOf("localhost", "localhorst")
      val task = createTaskWithConfig<JMeterRunTask>({
        proxyScheme.set(extScheme)
        proxyHost.set(extHost)
        proxyPort.set(extPort)
        nonProxyHosts.add(extNonHost)
      }, {
        proxyScheme.set(scheme)
        proxyHost.set(host)
        proxyPort.set(port)
        nonProxyHosts.addAll(nonHosts)
      }).get()

      val ext = getExtension()

      assertAll(
        { withClue("ext Scheme") { ext.proxyScheme.get() shouldBe extScheme } },
        { withClue("ext Host") { ext.proxyHost.get() shouldBe extHost } },
        { withClue("ext Port") { ext.proxyPort.get() shouldBe extPort } },
        { withClue("ext nonProxyHost size") { ext.nonProxyHosts.get() should haveSize(1) } },
        { withClue("ext nonProxyHosts") { ext.nonProxyHosts.get() should containExactly(extNonHost) } },
        { withClue("task Scheme") { task.proxyScheme.get() shouldBe scheme } },
        { withClue("task Host") { task.proxyHost.get() shouldBe host } },
        { withClue("task Port") { task.proxyPort.get() shouldBe port } },
        {
          withClue("task nonProxyHosts") {
            task.nonProxyHosts.get() should containExactlyInAnyOrder(nonHosts + extNonHost)
          }
        }
      )
    }

    @Test
    fun withSchemeOnly() {
      val task = createTaskWithConfig<JMeterRunTask>({}, {
        proxyScheme.set("smtp1234")
        jmxFile.set("Report.jmx")
      }).get()

      val args = task.createRunArguments()
      assertAll("Only Scheme arg",
        { withClue("Scheme") { args shouldContainInOrder listOf("-E", "smtp1234") } },
        { withClue("no Host") { args shouldNotContain "-H" } },
        { withClue("no Port") { args shouldNotContain "-P" } },
        { withClue("no NonProxy") { args shouldNotContain "-N" } },
        { withClue("no User") { args shouldNotContain "-u" } },
        { withClue("no Password") { args shouldNotContain "-a" } }
      )
    }

    @Test
    fun withHostOnly() {
      val task = createTaskWithConfig<JMeterRunTask>({}, {
        proxyHost.set("localhost")
        jmxFile.set("Report.jmx")
      }).get()

      val args = task.createRunArguments()
      assertAll("Only Scheme arg",
        { withClue("no Scheme") { args shouldNotContain "-E" } },
        { withClue("Host") { args shouldContainInOrder listOf("-H", "localhost") } },
        { withClue("no Port") { args shouldNotContain "-P" } },
        { withClue("no NonProxy") { args shouldNotContain "-N" } },
        { withClue("no User") { args shouldNotContain "-u" } },
        { withClue("no Password") { args shouldNotContain "-a" } }
      )
    }

    @Test
    fun withPortOnly() {
      val task = createTaskWithConfig<JMeterRunTask>({}, {
        proxyPort.set(42)
        jmxFile.set("Report.jmx")
      }).get()

      val args = task.createRunArguments()
      assertAll("Only Scheme arg",
        { withClue("no Scheme") { args shouldNotContain "-E" } },
        { withClue("no Host") { args shouldNotContain "-H" } },
        { withClue("Port") { args shouldContainInOrder listOf("-P", "42") } },
        { withClue("no NonProxy") { args shouldNotContain "-N" } },
        { withClue("no User") { args shouldNotContain "-u" } },
        { withClue("no Password") { args shouldNotContain "-a" } }
      )
    }

    @Test
    fun withNonProxy() {
      val task = createTaskWithConfig<JMeterRunTask>({}, {
        nonProxyHosts.add("localHorst")
        jmxFile.set("Report.jmx")
      }).get()

      val args = task.createRunArguments()
      assertAll("Only Scheme arg",
        { withClue("no Scheme") { args shouldNotContain "-E" } },
        { withClue("no Host") { args shouldNotContain "-H" } },
        { withClue("no Port") { args shouldNotContain "-P" } },
        { withClue("NonProxy") { args shouldContainInOrder listOf("-N", "localHorst") } },
        { withClue("no User") { args shouldNotContain "-u" } },
        { withClue("no Password") { args shouldNotContain "-a" } }
      )
    }

    @Test
    fun withMultipleNonProxy() {
      val task = createTaskWithConfig<JMeterRunTask>({}, {
        nonProxyHosts.addAll("localHorst", "127.0.0.42")
        jmxFile.set("Report.jmx")
      }).get()

      val args = task.createRunArguments()
      assertAll("Only Scheme arg",
        { withClue("no Scheme") { args shouldNotContain "-E" } },
        { withClue("no Host") { args shouldNotContain "-H" } },
        { withClue("no Port") { args shouldNotContain "-P" } },
        { withClue("NonProxy") { args shouldContainInOrder listOf("-N", "localHorst|127.0.0.42") } },
        { withClue("no User") { args shouldNotContain "-u" } },
        { withClue("no Password") { args shouldNotContain "-a" } }
      )
    }

    /**
     * Corner case
     * `Null` could be introduced through build script logic
     */
    @Test
    fun withNonProxyNulled() {
      val task = createTaskWithConfig<JMeterRunTask>({}, {
        nonProxyHosts.set(null as List<String>?)
        jmxFile.set("Report.jmx")
      }).get()
      val args = task.createRunArguments()
      assertAll("Only Scheme arg",
        { withClue("no Scheme") { args shouldNotContain "-E" } },
        { withClue("no Host") { args shouldNotContain "-H" } },
        { withClue("no Port") { args shouldNotContain "-P" } },
        { withClue("no NonProxy") { args shouldNotContain "-N" } },
        { withClue("no User") { args shouldNotContain "-u" } },
        { withClue("no Password") { args shouldNotContain "-a" } }
      )
    }

    @Test
    fun withUsername() {
      val task = createTaskWithConfig<JMeterRunTask>({}, {
        username.set("proxyUser")
        jmxFile.set("Report.jmx")
      }).get()

      val args = task.createRunArguments()
      assertAll("Only Scheme arg",
        { withClue("no Scheme") { args shouldNotContain "-E" } },
        { withClue("no Host") { args shouldNotContain "-H" } },
        { withClue("no Port") { args shouldNotContain "-P" } },
        { withClue("no NonProxy") { args shouldNotContain "-N" } },
        { withClue("User") { args shouldContainInOrder listOf("-u", "proxyUser") } },
        { withClue("no Password") { args shouldNotContain "-a" } }
      )
    }

    @Test
    fun withPassword() {
      val task = createTaskWithConfig<JMeterRunTask>({}, {
        password.set("Secret")
        jmxFile.set("Report.jmx")
      }).get()

      val args = task.createRunArguments()
      assertAll("Only Scheme arg",
        { withClue("no Scheme") { args shouldNotContain "-E" } },
        { withClue("no Host") { args shouldNotContain "-H" } },
        { withClue("no Port") { args shouldNotContain "-P" } },
        { withClue("no NonProxy") { args shouldNotContain "-N" } },
        { withClue("no User") { args shouldNotContain "-u" } },
        { withClue("Password") { args shouldContainInOrder listOf("-a", "Secret") } }
      )
    }
  }

  @Nested
  @DisplayName("Remote settings tests")
  inner class RemoteTests {

    @Test
    fun defaultNoRemoteArgsAreUsed() {
      val task = createTaskWithMandatoryArgs().get()

      val args = task.createRunArguments()
      checkForFlags(args, "No `-r` and no `-X` args", remoteFlag = false, exitFlag = false)
    }

    @Test
    fun withRemoteExecutionFlagFromExtension() {
      val task = createTaskWithMandatoryArgs(extConfig = {
        enableRemoteExecution.set(true)
      }).get()

      val args = task.createRunArguments()
      checkForFlags(args, "With `-r` no `-X` args", remoteFlag = true, exitFlag = false)
    }

    @Test
    fun withRemoteExecutionFlagOnTask() {
      val task = createTaskWithMandatoryArgs(taskConfig = {
        enableRemoteExecution.set(true)
      }).get()

      val args = task.createRunArguments()
      checkForFlags(args, "With `-r` no `-X` args", remoteFlag = true, exitFlag = false)
    }

    @Test
    fun withRemoteExecutionAndExitFlagFromExtension() {
      val task = createTaskWithMandatoryArgs(extConfig = {
        enableRemoteExecution.set(true)
        exitRemoteServers.set(true)
      }).get()

      val args = task.createRunArguments()
      checkForFlags(args, "With `-r` with `-X` args", remoteFlag = true, exitFlag = true)
    }

    @Test
    fun withRemoteExecutionAndExitFlagOnTask() {
      val task = createTaskWithMandatoryArgs(taskConfig = {
        enableRemoteExecution.set(true)
        exitRemoteServers.set(true)
      }).get()

      val args = task.createRunArguments()
      checkForFlags(args, "With `-r` with `-X` args", remoteFlag = true, exitFlag = true)
    }

    @Test
    fun exitFlagShouldBeIgnoredIfRemoteIsDisabled() {
      val task = createTaskWithMandatoryArgs(taskConfig = {
        exitRemoteServers.set(true)
      }).get()

      val args = task.createRunArguments()
      checkForFlags(args, "No `-r` no `-X` args", remoteFlag = false, exitFlag = false)
    }

    private fun createTaskWithMandatoryArgs(
      extConfig: JMeterExtension.() -> Unit = {},
      taskConfig: JMeterRunTask.() -> Unit = {}
    ) = createTaskWithConfig(extConfig, taskConfig).also {
      it.configure { tsk ->
        tsk.jmxFile.set("test.jmx")
      }
    }

    private fun checkForFlags(args: List<String>, title: String, remoteFlag: Boolean, exitFlag: Boolean) = assertAll(
      title,
      {
        withClue("Remote-Flag") {
          if (remoteFlag) {
            args shouldContain "-r"
          } else {
            args shouldNotContain "-r"
          }
        }
      },
      {
        withClue("Exit-Flag") {
          if (exitFlag) {
            args shouldContain "-X"
          } else {
            args shouldNotContain "-X"
          }
        }
      }
    )
  }
}
