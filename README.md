# QALIPSIS Gradle Plugin 

![CI](https://github.com/qalipsis/qalipsis-gradle-plugin/actions/workflows/gradle-main.yml/badge.svg)
![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/io.qalipsis.bootstrap?label=Gradle%20Portal%20Version&style=flat&color=0055ff)

The QALIPSIS Gradle Plugin simplifies the creation of new QALIPSIS projects and integration into CI pipelines.  
The bootstrap plugin simplifies the creation of a new QALIPSIS project.  
The cloud plugin allows for the upload, management and execution of scenarios into the QALIPSIS cloud environment. 

## Setup

### Kotlin

```kotlin
plugins {
    id("io.qalipsis.bootstrap") version "0.1.4"
}
```

### Groovy

```groovy
plugins {
    id 'io.qalipsis.bootstrap' version '0.1.4'
}
```

## Version Configuration

The plugin includes a default QALIPSIS version, but you can specify a different one:

### In `build.gradle.kts` (Kotlin)

```kotlin
qalipsis {
    version("0.15.b")
}
```

### In `build.gradle` (Groovy)

```groovy
qalipsis {
    version = '0.15.b'
}
```

### As a Gradle Property

Add to `gradle.properties` or Gradle execution options:

```properties
qalipsis.version=0.15.b
```

## Creating a QALIPSIS head/factory-only project

By default, the bootstrap plugin creates a QALIPSIS project with all dependencies for running as head, factory or standalone.
You can then select at runtime which deployment mode you want to use.

However, if you want to create a QALIPSIS project with only the dependencies for running as a head (respectively as a factory), you can call the statement 
`qalipsis { asHead() }` (respectively `qalipsis { asFactory() }`) in your `build.gradle.kts` or `build.gradle` file.

By doing so, you will have a QALIPSIS project with only the dependencies for running as head (respectively factory) and do not have to select
the deployment mode at runtime.

### In `build.gradle.kts` (Kotlin)

```kotlin
qalipsis {
    asHead() // or asFactory()
}
```

### In `build.gradle` (Groovy)

```groovy
qalipsis {
    asHead() // or asFactory()
}
```

## Adding QALIPSIS Plugins

To integrate QALIPSIS plugins, configure them in `build.gradle.kts` or `build.gradle`:

### Kotlin

```kotlin
qalipsis {
    plugins {
        apacheKafka()
        jackson("1.2.3") // You can override the version of the plugin.
    }
}
```

### Groovy

```groovy
qalipsis {
    plugins {
        apacheKafka()
        jackson("1.2.3") // You can override the version of the plugin.
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


## Applying the cloud plugin
```kotlin
plugins {
    id("io.qalipsis.cloud") version "0.1.4"
}
```

### Groovy

```groovy
plugins {
    id 'io.qalipsis.cloud' version '0.1.4'
}
```
## Configuring the cloud plugin
To ensure proper configuration of the QALIPSIS cloud plugin, the following 
environment variables should be defined: 
```properties
# Root url to use to access to the factory builder defaults to the value below.
qalipsis.cloud.registry.url=https://app.qalipsis.io/api/scenarios"
# Flag to specify if scenarios should be listed or not.
qalipsis.cloud.registry.list=true
# The token generated from the QALIPSIS GUI or from the api.
qalipsis.cloud.registry.token=api-token
# Root url to run campaigns.
qalipsis.cloud.api.url=https://app.qalipsis.io/api
```

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

### Trigger a test campaign in the cloud
The tasks created from the template `qalipsisRunCampaign` creates and executes a new campaign in the cloud.

### Kotlin

```kotlin
tasks {
    create("qalipsisRunCampaign", CloudRunQalipsis::class.java) {
        campaign("Test campaign") {
            speedFactor.set(2.0)
            startOffsetMs.set(2000)
            campaignTimeout.set("PT1H30M")
            hardTimeout.set(false)
            // Configure your scenario(s) as needed.
            scenario("My scenario 1") {
                minionsCount = 710
                zones {
                    "US" to 45
                    "DE" to 55
                }
                // Any implementation of the profile configuration(more, regular, stages, immediate, 
                // percentage, accelerate, timeframe) can be configured in the profile block.
                profile {
                    stages(completionMode = CompletionMode.HARD) {
                        stage(
                            minionsCount = 10,
                            rampUpDurationMs = 10,
                            totalDurationMs = 100000,
                            resolutionMs = 10000
                        )
                        stage(
                            minionsCount = 200,
                            rampUpDurationMs = 100,
                            totalDurationMs = 10000,
                            resolutionMs = 1000
                        )
                        stage(
                            minionsCount = 500,
                            rampUpDurationMs = 2000,
                            totalDurationMs = 1000,
                            resolutionMs = 4000
                        )
                    }
                }
            }
        }
    }
}
```

### Groovy

```groovy
tasks.create('qalipsisRunCampaign', CloudRunQalipsis) {
    campaign("Test campaign") {
        speedFactor = 2.0
        startOffsetMs = 2000
        campaignTimeout = "PT1H30M"
        hardTimeout = false
        // Configure your scenario(s) as needed.
        scenario("My scenario 1") {
            minionsCount = 710
            zones {
                US 45
                DE 55
            }
            // Any implementation of the profile configuration(more, regular, stages, immediate, 
            // percentage, accelerate, timeframe) can be configured in the profile block.
            profile {
                stages(completionMode = CompletionMode.HARD) {
                    stage(
                        minionsCount = 10,
                        rampUpDurationMs = 10,
                        totalDurationMs = 100000,
                        resolutionMs = 10000
                    )
                    stage(
                        minionsCount = 200,
                        rampUpDurationMs = 100,
                        totalDurationMs = 10000,
                        resolutionMs = 1000
                    )
                    stage(
                        minionsCount = 500,
                        rampUpDurationMs = 2000,
                        totalDurationMs = 1000,
                        resolutionMs = 4000
                    )
                }
            }
        }
    }
}
```

## Additional Documentation

Find more information at [docs.qalipsis.io](https://docs.qalipsis.io).

