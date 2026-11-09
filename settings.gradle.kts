rootProject.name = "SpringForge"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "forge-core",
    "forge-cache",
    "forge-ratelimit",
    "forge-idempotency",
    "forge-lock",
    "forge-web",
    "forge-data",
    "forge-resilience",
    "forge-messaging",
    "forge-outbox",
    "forge-scheduling",
    "forge-app",
)
