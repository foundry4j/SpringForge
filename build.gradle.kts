import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot) apply false
}

allprojects {
    group = "io.github.foundry4j"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(27)
        }
        withSourcesJar()
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        "api"(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))
        "annotationProcessor"(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))
        "annotationProcessor"("org.springframework.boot:spring-boot-configuration-processor")
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))
        "testImplementation"(platform("org.junit:junit-bom:${property("junitVersion")}"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        "testImplementation"("org.assertj:assertj-core")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-serial,-processing"))
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required = true
            html.required = true
        }
    }
}
