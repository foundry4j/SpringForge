dependencies {
    api(project(":forge-core"))
    api("org.springframework.boot:spring-boot-starter-aspectj")
    api(libs.resilience4j.circuitbreaker)
    api(libs.resilience4j.retry)
}
