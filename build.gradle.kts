plugins {
    kotlin("multiplatform") version "1.9.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.github.ben-manes.versions") version "0.49.0"
}

group = "hu.webhejj.perspektive"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
//    js {
//        nodejs {}
//    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                implementation("io.github.classgraph:classgraph:4.8.115")
                implementation("net.sourceforge.plantuml:plantuml:1.2021.10")
            }
        }
        val jvmTest by getting
//        val jsMain by getting
//        val jsTest by getting
    }
}
