<!--
Types of changes
 - `Added` for new features.
 - `Changed` for changes in existing functionality.
 - `Deprecated` for soon-to-be removed features.
 - `Removed` for now removed features.
 - `Fixed` for any bug fixes.
 - `Security` in case of vulnerabilities.
Only add when needed
-->

# jmeter-gradle-plugin Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com),
and this project adheres to [Semantic Versioning](https://semver.org).

## [Unreleased]
### Changed
- ⚠️Default jmeter version is now 5.5⚠️  
  Please refer to https://jmeter.apache.org/changes.html for potential breaking changes

## [2.4.0]
### Added
- added support for properties `-r` and `-X` for remote test execution (#50)

### Fixed
- Fixed issue with UP-TO-DATE check which let tasks fail after SetupTask was executed once (#48)

## [2.3.0]
### Added
- Added additional cli-arguments for proxy-settings (#40)

### Fixed
- Fixed security issue in plugin-output which can expose sensitive data (#43)

### Changed
- JMeterSetupTask now supports up-to-date check (#44)

## [2.2.2]
### Changed
- Aligned required java version with JMeter to Java8 (#36)

## [2.2.0]
### Changed
- Decoupled tool download and setup from jmeter execution tasks (#28)

### Fixed
- JMeter 'tool' can now really be customized (#29)

## [2.1.0]
### Added
- most task properties can now also be defined as CLI arguments (#23)

### Changed
- Removed dependency on java-plugin (#19)

### Fixed
- JMX-File now is really optional for gui tasks (#20)

## [2.0.0]
### Added
- Support for more JMeter cli properties (#12)
- mandatory JMeter configurations are now handled by dependency on respective config-jar (#15)

### Changed
- Restructured available task properties (#12)
- Due to new configuration handling a lot of former configuration properties related to configuration changed. E.g. `jmeter.tool.resourceTemplate` became `customResourceTemplate` in `jmeter`-extension and run- & report-tasks (#15)

### Fixed
- `Run` task now respects report settings (#6)
- `Report` task now respect jmeter property settings (#11)

### Removed
- Most of jmeter.tool properties because using configuration dependency (#15)
- Artifact no longer needs bundled jmeter resources because of #15 so they were removed

## [1.0.0]
### Added
- Initial version
