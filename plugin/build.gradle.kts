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
version = "0.1.2"

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
                "load-test",
                "load-testing",
                "end-to-end-test",
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
    val cloud by plugins.creating {
        id = "io.qalipsis.cloud"
        displayName = "QALIPSIS Cloud Gradle Plugin"
        description = "Deploy and execute your QALIPSIS tests worldwide"
        tags.set(
            listOf(
                "qalipsis",
                "qa",
                "test",
                "testing",
                "load",
                "load-test",
                "load-testing",
                "end-to-end-test",
                "end-to-end-testing",
                "performance",
                "performance-testing",
                "continuous-integration",
                "continuous-delivery",
                "ci",
                "cd",
                "cloud",
            )
        )
        implementationClass = "io.qalipsis.gradle.cloud.QalipsisCloudPlugin"
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
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.10.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation("com.squareup.okhttp3:okhttp-sse")
    implementation("com.squareup.okhttp3:okhttp-tls")
    implementation("com.squareup.okhttp3:okhttp-urlconnection")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.15.1"))
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

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

    kaptTest(platform("io.micronaut:micronaut-bom:3.9.3"))
    kaptTest("io.micronaut:micronaut-inject-java")
    testAnnotationProcessor(platform("io.micronaut:micronaut-bom:3.9.3"))
    testAnnotationProcessor("io.micronaut:micronaut-inject-java")

    testImplementation(platform("io.micronaut:micronaut-bom:3.9.3"))
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("io.micronaut:micronaut-context")

    kaptTestFixtures(platform("io.micronaut:micronaut-bom:3.9.3"))
    kaptTestFixtures("io.micronaut.data:micronaut-data-processor")
    kaptTestFixtures("io.micronaut:micronaut-http-validation")
    kaptTestFixtures("io.micronaut:micronaut-inject-java")

    testFixturesImplementation(platform("io.micronaut:micronaut-bom:3.9.3"))
    testFixturesImplementation(platform("com.fasterxml.jackson:jackson-bom:2.15.1"))
    testFixturesImplementation("io.micronaut:micronaut-validation")
    testFixturesImplementation("io.micronaut.beanvalidation:micronaut-hibernate-validator")
    testFixturesImplementation("io.micronaut:micronaut-runtime")
    testFixturesImplementation("io.micronaut:micronaut-http-server-netty")
    testFixturesImplementation("io.micronaut:micronaut-jackson-databind")
    testFixturesImplementation("io.micronaut.kotlin:micronaut-kotlin-extension-functions")
    testFixturesImplementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    testFixturesImplementation("io.micronaut.reactor:micronaut-reactor")
    testFixturesImplementation("org.jetbrains.kotlin:kotlin-reflect")
    testFixturesImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testFixturesImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testFixturesImplementation("io.projectreactor:reactor-core")
    testFixturesImplementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.7")
    testFixturesImplementation("io.github.microutils:kotlin-logging:2.1.23")
    testFixturesImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testFixturesImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testFixturesImplementation("org.apache.commons:commons-lang3:3.13.0")
    testFixturesImplementation("ch.qos.logback:logback-classic")
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