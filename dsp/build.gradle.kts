plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
tasks.register<JavaExec>("verifyDsp") {
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("br.com.lumen.dsp.VerificationKt")
}
tasks.named("check") { dependsOn("verifyDsp") }
