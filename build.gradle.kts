plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.16" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

jacoco {
    toolVersion = "0.8.12"
}

val coverageExcludePatterns = arrayOf(
    "**/dto/**",
    "**/*Application.class",
    "**/*Exception.class",
    "**/config/*Properties.class",
    "**/model/**",
    "**/payment/messaging/**",
    "**/order/messaging/**"
)

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
    apply(plugin = "checkstyle")

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        dependencies {
            dependency("org.apache.commons:commons-lang3:3.18.0")
            dependency("ch.qos.logback:logback-core:1.5.35")
            dependency("com.fasterxml.jackson.core:jackson-databind:2.21.5")
        }
    }

    configure<org.gradle.api.plugins.quality.CheckstyleExtension> {
        toolVersion = "10.23.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        maxErrors = 0
        maxWarnings = 0
    }

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
                exclude(*coverageExcludePatterns)
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
                    minimum = "0.60".toBigDecimal()
                }
                limit {
                    counter = "BRANCH"
                    minimum = "0.50".toBigDecimal()
                }
            }
        }
        classDirectories.setFrom(
            sourceSets.main.get().output.classesDirs.asFileTree.matching {
                exclude(*coverageExcludePatterns)
            }
        )
    }

    tasks.check {
        dependsOn(tasks.jacocoTestCoverageVerification)
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    val jacocoSubs = subprojects.filter { it.name != "common-dto" }

    dependsOn(jacocoSubs.map { it.tasks.named("test") })

    jacocoSubs.forEach { sub ->
        sub.plugins.withId("jacoco") {
            sourceSets(sub.sourceSets["main"])
            executionData(
                fileTree(sub.layout.buildDirectory.dir("jacoco")) {
                    include("*.exec")
                }
            )
        }
    }

    classDirectories.setFrom(
        files(jacocoSubs.map { sub ->
            sub.sourceSets["main"].output.classesDirs.asFileTree.matching {
                exclude(*coverageExcludePatterns)
            }
        })
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
