dependencies {
    api(project(":forge-core"))
    api("org.springframework.boot:spring-boot-starter-aspectj")
    compileOnly("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
}
