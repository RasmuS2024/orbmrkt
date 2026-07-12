plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.13" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

jacoco {
    toolVersion = "0.8.12"
}

allprojects {
    group = "orbmrkt"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    val coverageExcluded = setOf(":common-dto")

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        onlyIf { project.path !in coverageExcluded }
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(
            sourceSets.main.get().output.classesDirs.asFileTree.matching {
                exclude(
                    "**/dto/**",
                    "**/*Application.class",
                    "**/*Exception.class",
                    "**/config/*Properties.class",
                    "**/model/**",
                    "**/payment/messaging/**"
                )
            }
        )
    }

    tasks.jacocoTestCoverageVerification {
        dependsOn(tasks.jacocoTestReport)
        onlyIf { project.path !in coverageExcluded }
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    minimum = "0.70".toBigDecimal()
                }
                limit {
                    counter = "BRANCH"
                    minimum = "0.60".toBigDecimal()
                }
            }
        }
        classDirectories.setFrom(
            sourceSets.main.get().output.classesDirs.asFileTree.matching {
                exclude(
                    "**/dto/**",
                    "**/*Application.class",
                    "**/*Exception.class",
                    "**/config/*Properties.class",
                    "**/model/**",
                    "**/payment/messaging/**"
                )
            }
        )
    }

    tasks.check {
        dependsOn(tasks.jacocoTestCoverageVerification)
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.named("test") })

    subprojects.forEach { sub ->
        sub.plugins.withId("jacoco") {
            sourceSets(sub.sourceSets["main"])
            executionData(
                fileTree(sub.layout.buildDirectory.dir("jacoco")) {
                    include("*.exec")
                }
            )
        }
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
