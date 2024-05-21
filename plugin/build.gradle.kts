import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    `java-test-fixtures`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm") version "1.8.21"
    kotlin("kapt") version "1.8.21"
    publishing
    signing
    id("com.gradle.plugin-publish") version "1.2.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.qalipsis.gradle"
version = "0.1.1"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

gradlePlugin {
    website.set("https://qalipsis.io")
    vcsUrl.set("https://github.com/qalipsis/qalipsis-gradle-plugin.git")

    val bootstrap by plugins.creating {
        id = "io.qalipsis.bootstrap"
        displayName = "QALIPSIS Bootstrap Gradle Plugin"
        description = "Simplify the creation of new QALIPSIS projects and integration into CI/CD pipelines"
        tags.set(
            listOf(
                "qalipsis",
                "qa",
                "test",
                "testing",
                "load",
                "load-testing",
                "end-to-end-testing",
                "performance",
                "performance-testing",
                "continuous-integration",
                "continuous-delivery",
                "ci",
                "cd",
            )
        )
        implementationClass = "io.qalipsis.gradle.bootstrap.QalipsisBootstrapPlugin"
    }
}

val target = JavaVersion.VERSION_11

java {
    sourceCompatibility = target
    targetCompatibility = target
}

val signingKeyId = "signing.keyId"
if (System.getProperty(signingKeyId) != null || System.getenv(signingKeyId) != null) {
    signing {
        publishing.publications.forEach { sign(it) }
    }
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = target.majorVersion
            javaParameters = true
        }
    }

    val qalipsisVersion: String by project
    val replacedPropertiesInResources = mapOf(
        "project.version" to project.version,
        "qalipsis.version" to qalipsisVersion
    )
    withType<ProcessResources> {
        filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to replacedPropertiesInResources)
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        mergeServiceFiles()
        archiveClassifier.set(null as String?)
    }

    jar.configure {
        archiveClassifier.set("default")
    }

}

kapt {
    useBuildCache = false

}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.+")

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("io.kotest:kotest-assertions-core:5.5.+")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(gradleTestKit())
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["kaptFunctionalTest"].extendsFrom(configurations["kaptTest"])

project.logger.lifecycle(configurations.filter { it.name.lowercase().contains("functional") }.toString())


// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    group = "verification"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

tasks.withType(Test::class.java) {
    useJUnitPlatform()
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

publishing {
    repositories {
        mavenLocal()
    }
}