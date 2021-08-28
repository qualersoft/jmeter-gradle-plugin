package de.qualersoft.jmeter.gradleplugin.task

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.net.JarURLConnection

@CacheableTask
open class JMeterReportTask : JMeterBaseTask() {

  /**
   * Path to a user defined report template folder.
   * Must have name `report-template`.
   *
   * Defaults to `jmeter.tool.reportTemplateFolder`. (If not set uses the bundled)
   */
  @InputDirectory
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @Optional
  val reportTemplate: DirectoryProperty = objectFactory.directoryProperty()

  override fun processResources(jmBinDir: File) {
    super.processResources(jmBinDir)
    copyRespectProperty(jmExt.tool.reportGeneratorPropertyFile, "reportgenerator.properties", jmBinDir)
    // copy report-template dir
    val destReportTempDir = jmBinDir.resolve("report-template")
    if (reportTemplate.isPresent) {
      reportTemplate.asFile.get().copyRecursively(destReportTempDir, true)
    } else if (jmExt.tool.reportTemplateFolder.isPresent) {
      jmExt.tool.reportTemplateFolder.asFile.get().copyRecursively(destReportTempDir, true)
    } else {
      // copy from jar
      val res = javaClass.getResource("/report-template")
      val jarCon = res.openConnection() as JarURLConnection
      val jarSrcPath = jarCon.entryName
      val jarFile = jarCon.jarFile
      for (entry in jarFile.entries()) {
        // only copy stuff that is under resources' path
        if (entry.name.startsWith(jarSrcPath)) {
          val filename = entry.name.removePrefix(jarSrcPath)
          val f = File(destReportTempDir, filename)
          if (!entry.isDirectory) {
            val srcStream = jarFile.getInputStream(entry)
            srcStream.use { src ->
              f.outputStream().use { dest ->
                src.copyTo(dest)
              }
            }
          } else {
            f.mkdirs()
          }
        }
      }
    }
  }

  override fun createRunArguments(): MutableList<String> = mutableListOf<String>().apply {
    val src = sourceFile.asFile.get()
    // protocol/log file
    add("-g")
    add(resultDirectory.file("${src.nameWithoutExtension}.jtl").get().asFile.absolutePath)
    // output dir
    add("-o")
    add(reportDir.file(src.nameWithoutExtension).get().asFile.absolutePath)

    // log file
    add("-j")
    add(resultDirectory.file("${src.nameWithoutExtension}.log").get().asFile.absolutePath)

    // user properties file goes first to allow override by dedicated user properties
    if (userPropertiesFile.isPresent) {
      add("-G${userPropertiesFile.get().asFile.absolutePath}")
    }

    userProperties.get().forEach { (k, v) ->
      add("-G$k=$v")
    }

    if (deleteResults) {
      add("-f")
    }
  }
}
