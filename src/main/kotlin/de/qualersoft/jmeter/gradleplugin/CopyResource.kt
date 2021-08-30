package de.qualersoft.jmeter.gradleplugin

import org.gradle.api.logging.Logging
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.UnsupportedOperationException
import java.net.JarURLConnection
import java.net.URL
import kotlin.jvm.Throws


object CopyResource {

  val logger = Logging.getLogger(CopyResource::class.java)

  fun copyJarEntriesToFolder(resourceName: String, targetDir: File) {
    if (!targetDir.isDirectory) {
      throw IllegalArgumentException("Given file '$targetDir' is not a directory!")
    }

    val res = getResource(resourceName)
    val jarConnection = res.openConnection() as JarURLConnection
    val jarSrcPath = jarConnection.entryName
    val jarFile = jarConnection.jarFile

    jarFile.entries().toList().filter {
      it.name.startsWith(jarSrcPath)
    }.forEach {
      val filename = it.name.removePrefix(jarSrcPath)
      logger.info("Going to copy: {}", filename)
      val destFile = File(targetDir, filename)
      if (it.isDirectory) {
        logger.info("{} is directory ->", filename)
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

  /**
   * Gets a resource from the classloader.
   * @param resourceName Path to the resource
   */
  @Throws(IllegalArgumentException::class)
  private fun getResource(resourceName: String) =
    javaClass.classLoader.getResource(resourceName)
      ?: throw IllegalArgumentException("Resource '$resourceName' not found!")


  private fun getRelativeResourcePath(url: URL, itemOnly: Boolean = true): String {
    val entry = (url.openConnection() as JarURLConnection).jarEntry
    return if (itemOnly) {
      File(entry.name).name
    } else {
      entry.name
    }
  }

  /**
   * Util function that copies a bundled resource **file** to a target **folder**.
   * Uses classloader.getResourceAsStream
   *
   * @param resourceName The absolute path to the resource.
   * @param fileOnly Copy just the file or the whole path. Default: `true`
   */
  fun File.copyFromResourceFile(resourceName: String, fileOnly: Boolean = true): File {
    if (this.isDirectory) {
      val res = getResource(resourceName)
      val target = getRelativeResourcePath(res, fileOnly)
      val destFile = this.resolve(target)
      val srcStream = res.openStream()
      copyStream(srcStream, destFile.outputStream())
      return destFile
    } else {
      throw UnsupportedOperationException("Source is not a directory")
    }
  }
}


/**
 * Copies this file to a target directory
 */
fun File.copyToDir(destDir: File): File {
  val destFile = destDir.resolve(this.name)
  if (!destFile.exists()) {
    this.copyTo(destFile)
  }
  return destFile
}
