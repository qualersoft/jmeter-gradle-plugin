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
