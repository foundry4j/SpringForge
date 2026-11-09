dependencies {
    api(project(":forge-core"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-cache")

    testRuntimeOnly("com.h2database:h2")
}
