dependencies {
    api(project(":forge-core"))
    api(project(":forge-cache"))
    api(project(":forge-ratelimit"))
    api(project(":forge-idempotency"))
    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-aspectj")
}
