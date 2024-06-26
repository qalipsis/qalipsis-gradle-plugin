# QALIPSIS Gradle Plugin 

![CI](https://github.com/qalipsis/qalipsis-gradle-plugin/actions/workflows/gradle-main.yml/badge.svg)

The QALIPSIS Gradle plugin simplifies the creation of new QALIPSIS projects and the integration of QALIPSIS projects into CI pipelines.

## Setup

The Gradle plugin is integrated into the Bootstrap project. From the cloned/downloaded bootstrap project on your computer, open the `build.gradle.kts` file in your project directory.

### Kotlin

```kotlin
plugins {
    id("io.qalipsis.bootstrap") version "0.1.0"
}
```

### Groovy

```groovy
plugins {
    id 'io.qalipsis.bootstrap' version '0.1.0'
}
```

## Version Configuration

The plugin includes a default QALIPSIS version. You can specify a different version in the appropriate `build` file or as a Gradle property.

### Kotlin

In `build.gradle.kts`: 
```kotlin
qalipsis {
    version("0.10.a")
}
```

### Groovy

In `build.gradle`:
```groovy
qalipsis {
    version = '0.10.a'
}
```

### As a Gradle Property

Add to `gradle.properties` or Gradle execution options:

```properties
qalipsis.version=0.10.a
```

## Adding QALIPSIS Plugins

You can integrate QALIPSIS plugins by configuring them in the appropriate `build` file.

###  Kotlin

In `build.gradle.kts`"
```kotlin
qalipsis {
    plugins {
        apacheKafka()
        jackson()
    }
}
```

### Groovy
In `build.gradle`:
```groovy
qalipsis {
    plugins {
        apacheKafka()
        jackson()
    }
}
```

Available plugins:
- `apacheCassandra`
- `apacheKafka`
- `elasticsearch`
- `graphite`
- `influxDb`
- `jackson`
- `jms`
- `jakartaMessaging`
- `mail`
- `mongoDb`
- `netty`
- `r2dbcJasync`
- `rabbitMq`
- `redisLettuce`
- `slack`
- `timescaleDb`

## Executing QALIPSIS

### Default Execution

The `qalipsisRunAllScenarios` task runs all scenarios in your project's classpath.

### Kotlin

```kotlin
tasks {
    named("qalipsisRunAllScenarios") {
        configuration(
            "report.export.junit.enabled" to "true",
            "report.export.junit.folder" to project.layout.buildDirectory.dir("test-results/my-new-scenario").get().asFile.path
        )
    }
}
```

### Groovy

```groovy
tasks.named('qalipsisRunAllScenarios') {
    configuration 'report.export.junit.enabled': 'true',
                  'report.export.junit.folder': layout.buildDirectory.dir('test-results/my-new-scenario').get().asFile.path
}
```

### Custom Execution

Create custom tasks of type `RunQalipsis` to run specific scenarios with custom configurations:

### Kotlin

```kotlin
tasks {
    create("executeRestApiSpikeTest", RunQalipsis::class.java) {
        scenarios("spike-test-of-rest-api", "constant-monitoring")
        configuration(
            "report.export.junit.enabled" to "true",
            "report.export.junit.folder" to project.layout.buildDirectory.dir("test-results/my-new-scenario").get().asFile.path
        )
    }
}
```

### Groovy

```groovy
tasks.create('executeRestApiSpikeTest', RunQalipsis) {
    scenarios 'spike-test-of-rest-api', 'constant-monitoring'
    configuration 'report.export.junit.enabled': 'true',
                  'report.export.junit.folder': layout.buildDirectory.dir('test-results/my-new-scenario').get().asFile.path
}
```

## Additional Documentation

Find more information at [docs.qalipsis.io](https://docs.qalipsis.io).

