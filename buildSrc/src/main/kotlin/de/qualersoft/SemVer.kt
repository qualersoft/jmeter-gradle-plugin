package de.qualersoft

import java.io.File
import java.util.Properties

data class SemVer(
  var major: Int = 0,
  var minor: Int = 0,
  var patch: Int = 0,
  var preBuild: String? = null,
  var build: String? = null
) {

  fun nextMajor(build: String? = null): SemVer {
    major += 1
    minor = 0
    patch = 0
    preBuild = null
    this.build = build
    return this
  }

  fun nextMinor(build: String? = null): SemVer {
    minor += 1
    patch = 0
    preBuild = null
    this.build = build
    return this
  }

  fun nextPatch(build: String? = null): SemVer {
    if (null == preBuild) { // no snapshot -> increase patch
      patch += 1
    }
    preBuild = null
    this.build = build
    return this
  }

  fun nextSnapshot(build: String? = null): SemVer {
    nextPatch(build)
    preBuild = "SNAPSHOT"
    return this
  }

  fun updateByKind(kind: String) = when {
    kind.equals("major", true) -> nextMajor()
    kind.equals("minor", true) -> nextMinor()
    kind.equals("patch", true) -> nextPatch()
    kind.equals("snapshot", true) -> nextSnapshot()
    else -> throw IllegalArgumentException("Unknown kind '$kind', supported values: [major, minor, patch, snapshot].")
  }

  fun persist(properties: File, key: String = "version", comment: String? = null) {
    val props = Properties()
    props.load(properties.inputStream())
    props[key] = toString()
    props.store(properties.outputStream(), comment)
  }

  override fun toString(): String = "$major.$minor.$patch".let {
    var res = it
    if (null != preBuild) {
      res += "-$preBuild"
    }
    if (null != build) {
      res += "+$build"
    }
    res
  }
}

/**
 * Semantic versions consists of a minimum of three parts Major, Minor and Patch
 */
private const val MIN_SEMVER_PARTS = 3

fun parseSemVer(version: String?): SemVer {
  if (null == version) {
    return SemVer()
  }

  val versionParts = version.split(".").toMutableList()

  // fill up missing parts with 0
  if (MIN_SEMVER_PARTS > versionParts.size) {
    repeat(MIN_SEMVER_PARTS - versionParts.size) {
      versionParts.add("0")
    }
  }

  // normalize last entry
  val semVerPattern = "^(?<patch>[\\d]+)(-(?<pre>[\\d\\w-.]+))?(\\+(?<build>[\\d\\w-.]+))?\$".toRegex()
  // should never be null because at least we have a patch
  val matches = semVerPattern.matchEntire(versionParts[2])!!
  return SemVer(
    versionParts[0].toInt(),
    versionParts[1].toInt(),
    matches.groups["patch"]!!.value.toInt(),
    matches.groups["pre"]?.value,
    matches.groups["build"]?.value
  )
}
