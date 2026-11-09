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
    "forge-validation",
    "forge-audit",
    "forge-crypto",
    "forge-security",
    "forge-resilience",
    "forge-messaging",
    "forge-outbox",
    "forge-notification",
    "forge-scheduling",
    "forge-excel",
    "forge-rpc",
    "forge-observability",
    "forge-migration",
    "forge-test",
    "forge-bom",
    "forge-app",
)
