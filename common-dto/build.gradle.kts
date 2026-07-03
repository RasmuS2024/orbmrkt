plugins {
    `java-library`
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.13")
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
