plugins {
    kotlin("jvm") version "1.3.61"
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    compile("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.12")
}