# QALIPSIS Gradle Plugin 

![CI](https://github.com/qalipsis/qalipsis-gradle-plugin/actions/workflows/gradle-main.yml/badge.svg)

The QALIPSIS Gradle Plugin simplifies the creation of new QALIPSIS projects and integration into CI pipelines.

## Setup

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

The plugin includes a default QALIPSIS version, but you can specify a different one:

### In `build.gradle.kts` (Kotlin)

```kotlin
qalipsis {
    version("0.10.a")
}
```

### In `build.gradle` (Groovy)

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

To integrate QALIPSIS plugins, configure them in `build.gradle.kts` or `build.gradle`:

### Kotlin

```kotlin
qalipsis {
    plugins {
        apacheKafka()
        jackson()
    }
}
```

### Groovy

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

