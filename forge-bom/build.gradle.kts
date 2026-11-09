plugins {
    `java-platform`
}

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it.name.startsWith("forge-") && it.name != project.name }
            .sortedBy { it.name }
            .forEach { api(project(it.path)) }
    }
}
