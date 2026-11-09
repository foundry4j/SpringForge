dependencies {
    api(project(":forge-core"))
    api("org.springframework.boot:spring-boot-starter-actuator")
    compileOnly(libs.springdoc.openapi.common)
}
