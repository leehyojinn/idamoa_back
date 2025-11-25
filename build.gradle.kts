plugins {
    java
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.interior"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // gRPC 버전 통일 (google-analytics-data가 사용하는 버전으로 맞춤)
    implementation(platform("io.grpc:grpc-bom:1.70.0"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Spring Security & JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Flyway Database Migration
    implementation("org.flywaydb:flyway-core:10.8.1")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.8.1")

    // AWS S3 for file upload
    implementation("software.amazon.awssdk:s3:2.20.26")

    // Apache Tika for file type detection
    implementation("org.apache.tika:tika-core:2.9.1")

    // Email notification
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // WebSocket for real-time notifications
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Gmail API (Service Account + Domain-wide Delegation)
    implementation("com.google.apis:google-api-services-gmail:v1-rev20250630-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.40.0")

    // Google Analytics Data API
    implementation("com.google.analytics:google-analytics-data:0.56.0")

    // gRPC - 버전은 BOM이 관리
    implementation("io.grpc:grpc-netty-shaded")
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-protobuf")
    implementation("javax.annotation:javax.annotation-api:1.3.2") // JDK 11+ 필요
    // Hypersistence Utils for JSONB and Array types
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Spring Boot JAR 빌드 시 gRPC 관련 설정
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
