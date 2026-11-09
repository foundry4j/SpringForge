plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":forge-web"))
    implementation(project(":forge-ratelimit"))
    implementation(project(":forge-idempotency"))
    implementation(project(":forge-data"))
    implementation(project(":forge-cache"))
    implementation(project(":forge-lock"))
    implementation(project(":forge-resilience"))
    implementation(project(":forge-messaging"))
    implementation(project(":forge-outbox"))
    implementation(project(":forge-notification"))
    implementation(project(":forge-scheduling"))
    implementation(project(":forge-validation"))
    implementation(project(":forge-crypto"))
    implementation(project(":forge-security"))
    implementation(project(":forge-audit"))
    implementation(project(":forge-excel"))
    implementation(project(":forge-rpc"))
    implementation(project(":forge-observability"))
    implementation(project(":forge-migration"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":forge-test"))
}

springBoot {
    mainClass = "io.github.foundry4j.forge.app.SpringForgeApplication"
}
