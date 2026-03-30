plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

group = "com.preagonal"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.slf4j:slf4j-nop:2.0.0")
    implementation("org.json:json:20210307")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.0")
    implementation("net.dv8tion:JDA:5.3.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

// Configure source sets to include files directly in src directory
sourceSets {
    main {
        kotlin {
            srcDirs("src")
        }
        resources {
            srcDirs("src/main/resources")
        }
    }
}

// Set duplicates strategy for processResources task
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

application {
    mainClass.set("MainKt")
}

tasks.register<Jar>("shadowJar") {
    archiveBaseName.set("SetShape2Gen")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(sourceSets.main.get().output)
}