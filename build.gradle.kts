plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.1"
    id("com.google.protobuf") version "0.9.6"
}

group = "com.bureauveritas"
version = "2.8.6"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("software.amazon.awssdk:codegen:2.41.13") {
        exclude(group = "software.amazon.awssdk", module = "ruleset-testing-core")
    }
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
    implementation("org.openapitools:openapi-generator:7.19.0") {
        // Exclude heavy template dependencies and unnecessary modules
        exclude(group = "org.openapitools", module = "openapi-generator-cli")
        exclude(group = "org.openapitools", module = "openapi-generator-online")
        exclude(group = "org.openapitools", module = "openapi-generator-maven-plugin")
        exclude(group = "org.openapitools", module = "openapi-generator-gradle-plugin")
    }
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

        // Exclude unnecessary resources to reduce size
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
        exclude("META-INF/README*")
        exclude("META-INF/DEPENDENCIES")
        exclude("about.html")
        exclude("about_files/**")
        exclude("changelog.txt")
        exclude("AUTHORS")
        exclude("*.md")
        exclude("*.txt")
        exclude("LICENSE")
        exclude("NOTICE")

        // Exclude OpenAPI Generator templates (huge space saver - ~15MB+)
        exclude("**/openapitools/codegen/**/*.mustache")
        exclude("**/openapitools/codegen/**/templates/**")
        exclude("handlebars/**")
        exclude("Java/**")
        exclude("JavaJaxRS/**")
        exclude("JavaSpring/**")
        exclude("kotlin*/**")
        exclude("python*/**")
        exclude("typescript*/**")
        exclude("go/**")
        exclude("rust/**")
        exclude("ruby/**")
        exclude("php/**")
        exclude("scala/**")
        exclude("swift/**")
        exclude("csharp/**")
        exclude("apex/**")
        exclude("aspnet*/**")
        exclude("ada/**")
        exclude("android/**")
        exclude("bash/**")
        exclude("c/**")
        exclude("cpp*/**")
        exclude("clojure/**")
        exclude("dart*/**")
        exclude("elixir/**")
        exclude("elm/**")
        exclude("eiffel/**")
        exclude("erlang*/**")
        exclude("groovy/**")
        exclude("haskell*/**")
        exclude("jmeter/**")
        exclude("julia*/**")
        exclude("lua/**")
        exclude("markdown/**")
        exclude("mysql-schema/**")
        exclude("n4js/**")
        exclude("objc/**")
        exclude("ocaml/**")
        exclude("perl/**")
        exclude("powershell/**")
        exclude("protobuf-schema/**")
        exclude("r/**")
        exclude("xojo-client/**")
        exclude("asciidoc-documentation/**")
        exclude("avro-schema/**")

        // Additional template exclusions (these are at root level in the jar)
        exclude("*-Apollo/**")
        exclude("*-Closure/**")
        exclude("*-Flowtyped/**")
        exclude("Ada/**")
        exclude("Android/**")
        exclude("Apex/**")
        exclude("C/**")
        exclude("Clojure/**")
        exclude("Cpp*/**")
        exclude("Crystal/**")
        exclude("CSharp/**")
        exclude("Dart*/**")
        exclude("Eiffel/**")
        exclude("Elixir/**")
        exclude("Elm/**")
        exclude("Erlang*/**")
        exclude("F*/**")
        exclude("Go*/**")
        exclude("Groovy/**")
        exclude("Haskell*/**")
        exclude("Java*/**")
        exclude("Javascript*/**")
        exclude("Jmeter/**")
        exclude("Julia*/**")
        exclude("Kotlin*/**")
        exclude("Lua/**")
        exclude("N4js/**")
        exclude("Objc/**")
        exclude("OCaml/**")
        exclude("Perl/**")
        exclude("Php/**")
        exclude("Plantuml/**")
        exclude("Powershell/**")
        exclude("Python*/**")
        exclude("R/**")
        exclude("Ruby/**")
        exclude("Rust*/**")
        exclude("Scala*/**")
        exclude("Swift*/**")
        exclude("Typescript*/**")
        exclude("Xojo/**")
        exclude("graphql-*/**")
        exclude("java-*/**")
        exclude("javascript-*/**")
        exclude("mysql-*/**")
        exclude("openapi*/**")
        exclude("spring/**")
        exclude("wsdl*/**")
        exclude("csharp-*/**")

        // Exclude mustache templates
        exclude("**/*.mustache")

        // Exclude Unicode character tables if not needed
        exclude("**/*.aut")
        exclude("**/*.profile")

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
