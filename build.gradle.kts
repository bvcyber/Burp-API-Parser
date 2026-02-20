plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.1"
    id("com.google.protobuf") version "0.9.6"
}

group = "com.bureauveritas"
version = "2.8.3"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("software.amazon.awssdk:codegen:2.41.13")
    implementation("net.portswigger.burp.extensions:montoya-api:2025.12")
    implementation("com.intellij:forms_rt:7.0.3")
    implementation("org.json:json:20251224")
    implementation("commons-io:commons-io:2.21.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("io.burt:jmespath-gson:0.6.0")
    // reference
    // https://github.com/google/protobuf-gradle-plugin/blob/master/examples/exampleKotlinDslProject/build.gradle.kts
    implementation("com.google.protobuf:protobuf-java:4.33.3")
    implementation("io.grpc:grpc-netty-shaded:1.78.0")
    implementation("io.grpc:grpc-protobuf:1.78.0")
    implementation("io.grpc:grpc-stub:1.78.0")
    compileOnly("jakarta.annotation:jakarta.annotation-api:3.0.0")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    implementation("io.swagger.core.v3:swagger-core:2.2.42")
    implementation("io.swagger.parser.v3:swagger-parser:2.1.37")
    implementation("io.swagger:swagger-inflector:2.0.14")
    implementation("org.openapitools:openapi-generator:7.19.0")
    implementation("tools.jackson.dataformat:jackson-dataformat-xml:3.0.3")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.0.3")

    implementation(platform("io.modelcontextprotocol.sdk:mcp-bom:0.18.0"))
    implementation("io.modelcontextprotocol.sdk:mcp")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.32.1"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.78.0"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without
                // options. Note the braces cannot be omitted, otherwise the
                // plugin will not be added. This is because of the implicit way
                // NamedDomainObjectContainer binds the methods.
                create("grpc") { }
            }
        }
    }
}

tasks {
    shadowJar {
        // This fixes dependency conflicts in the fat jar created by shadowJar
        // See: https://github.com/grpc/grpc-java/issues/10853#issuecomment-1917363853
        mergeServiceFiles()

        // Set to INCLUDE so that necessary files aren't dropped
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        // Relocate protobuf to fix conflicts with Burp's classloader
        relocate("com.google.protobuf", "shadow.com.google.protobuf")

        minimize {
            exclude(dependency("io.grpc:.*:.*"))
            exclude(dependency("io.swagger.core.v3:.*:.*"))
            exclude(dependency("io.swagger.parser.v3:.*:.*"))
            exclude(dependency("org.openapitools:openapi-generator:.*:.*"))
            exclude(dependency("io.modelcontextprotocol.sdk:.*:.*"))
            exclude(dependency("io.modelcontextprotocol.sdk:mcp"))
        }
    }
    processResources {
        expand("version" to version)
    }
    test {
        useJUnitPlatform()
    }
}
