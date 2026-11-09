dependencies {
    api(project(":forge-data"))
    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-websocket")

    testRuntimeOnly("com.h2database:h2")
}
