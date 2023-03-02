import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    application
}

group = "me.eagely"
version = ""

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.aallam.openai:openai-client:3.0.0")
    implementation("io.ktor:ktor-client-java:2.2.4")
    implementation("org.slf4j:slf4j-simple:1.7.9")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("ClipboardGPT")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "com.github.csolem.gradle.shadow.kotlin.example.App"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
}

application {
    mainClass.set("MainKt")
}
