plugins {
    id("org.springframework.boot")
}

dependencyManagement {
    dependencies {
        dependency("org.bouncycastle:bcprov-jdk18on:1.84")
    }
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.3"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.17")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}
