plugins {
    `java-library`
    id("java-test-fixtures")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.16")
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.kafka:spring-kafka")
    testFixturesImplementation("org.projectlombok:lombok")
    testFixturesAnnotationProcessor("org.projectlombok:lombok")
}