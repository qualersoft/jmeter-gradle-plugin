package de.qualersoft.jmeter.gradleplugin

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.jar.JarFile

object CopyResource {

  private val log: Logger = Logging.getLogger(CopyResource::class.java)

  fun extractJarToDir(jarFile: JarFile, targetDir: File) {
    jarFile.entries().toList().forEach {
      val filename = it.name
      log.info("Going to copy: {}", filename)
      val destFile = File(targetDir, filename)
      if (it.isDirectory) {
        log.info("{} is directory -> creating it", filename)
        destFile.mkdirs()
      } else {
        copyStream(jarFile.getInputStream(it), destFile.outputStream())
      }
    }
  }

  fun copyStream(srcStream: InputStream, destStream: OutputStream) {
    srcStream.use { src ->
      destStream.use { dest -> src.copyTo(dest) }
    }
  }
}

/**
 * Copies this file to a target directory
 *
 * @return The target file
 */
fun File.copyToDir(destDir: File): File {
  val destFile = destDir.resolve(this.name)
  if (!destFile.exists()) {
    this.copyTo(destFile)
  }
  return destFile
}
