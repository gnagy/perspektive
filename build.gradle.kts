plugins {
    kotlin("jvm") version "1.9.10"
    `maven-publish`
    signing
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.github.ben-manes.versions") version "0.50.0"
}

group = "hu.webhejj.perspektive"
version = "0.1.0"
description = "Perspektive is a diagramming tool written in Kotlin"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.classgraph:classgraph:4.8.165")
    implementation("net.sourceforge.plantuml:plantuml:1.2023.12")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("ch.qos.logback:logback-core:1.4.11")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                packaging = "jar"
                name.set("perspektive")
                description.set(project.description)
                url.set("https://github.com/gnagy/perspektive")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("gnagy")
                        name.set("Gergely Nagy")
                        email.set("greg@webhejj.hu")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/gnagy/perspektive.git")
                    developerConnection.set("scm:git:ssh://github.com/gnagy/perspektive.git")
                    url.set("https://github.com/gnagy/perspektive")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrhUsername") as String?
                password = project.findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
