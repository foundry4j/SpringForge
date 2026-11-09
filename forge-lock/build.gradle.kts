dependencies {
    api(project(":forge-core"))
    api(project(":forge-cache"))
    api("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
