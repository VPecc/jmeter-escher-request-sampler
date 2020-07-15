plugins {
    java
    /*
     Apply the java-library-distribution plugin to add support for distributing the source .jar and library .jar files
     so they can be used by JMeter. See the plugin docs at https://docs.gradle.org/current/userguide/java_library_distribution_plugin.html
     */
    `java-library-distribution`
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

val jacksonVersion = "2.11.0"
val jmeterVersion = "5.1.1"
val slf4jVersion = "1.7.30"

dependencies {
    /*
    Specify the JMeter dependencies as `compileOnly`. These dependencies *do not* need to be included in the
    distribution because they are already included in JMeter itself. So, they are needed only as compile-time
    dependencies. SLF4J is also included in JMeter.
     */
    compileOnly(group = "org.apache.jmeter", name = "ApacheJMeter_java", version = jmeterVersion)
    compileOnly(platform("org.apache.jmeter:ApacheJMeter_parent:$jmeterVersion"))
    compileOnly(group = "org.slf4j", name = "slf4j-api", version = slf4jVersion)

    implementation("org.springframework:spring-web:5.1.2.RELEASE")
    implementation("com.emarsys:escher:0.3")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
