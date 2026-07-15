import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    `java-library`
    `maven-publish`
    jacoco
}

group = "com.voicetel"
version = "2.2.10"
description = "Official Kotlin SDK for the VoiceTel REST API (v2.2.10)."

repositories {
    mavenCentral()
}

val ktorVersion = "3.5.1"
val kxsVersion = "1.11.0"
val coroutinesVersion = "1.11.0"
val junitVersion = "6.1.1"

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kxsVersion")
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    testLogging {
        events("passed", "skipped", "failed")
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    // Data classes in the models package are wire DTOs whose generated
    // equals/hashCode/toString/componentN/copy methods skew coverage even
    // though their fields are exercised end-to-end by every resource test.
    // Exclude the package from the coverage report.
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("com/voicetel/sdk/models/**")
            }
        })
    )
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "voicetel-sdk-kotlin"
            pom {
                name.set("VoiceTel Kotlin SDK")
                description.set(project.description)
                url.set("https://github.com/voicetel/kotlin-sdk")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("VoiceTel")
                        email.set("support@voicetel.com")
                        organization.set("VoiceTel Communications")
                        organizationUrl.set("https://voicetel.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/voicetel/kotlin-sdk.git")
                    developerConnection.set("scm:git:git@github.com:voicetel/kotlin-sdk.git")
                    url.set("https://github.com/voicetel/kotlin-sdk")
                }
            }
        }
    }
}
