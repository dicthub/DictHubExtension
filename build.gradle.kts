import org.dicthub.*
import org.dicthub.Script

val kotlinVersion = "1.3.61"
val kotlinHtmlVersion = "0.6.12"

plugins {
    id("kotlin2js") version "1.3.61"
}

allprojects {
    repositories {
        jcenter()
    }
}

subprojects {
    apply {
        plugin("kotlin2js")
    }

    dependencies {
        compile(kotlin("stdlib-js"))
        compile("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinHtmlVersion")

        testCompile(kotlin("kotlin-test-js"))
    }
}


dependencies {
    compile(kotlin("stdlib-js"))
    compile("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinHtmlVersion")

    testCompile("org.jetbrains.kotlin:kotlin-test-js:$kotlinVersion")
}


task<Copy>("extractLibs") {
    group = "release"
    description = "Extract javascript lib from dependency jars"
    dependsOn("jar")
    extra["jsLibDir"] = "$buildDir/include"

    from(zipTree("$buildDir/libs/${rootProject.name}.jar"))
    from(configurations.compile.files.map { zipTree(it) })

    include("*.js")
    into("${extra["jsLibDir"]}/js")
}

// TODO: Move into dependency format
val styleSheetsList =  { page: String ->
    val commonStyles = mutableListOf(
            StyleSheet("lib/css/bootstrap.min.css"),
            StyleSheet("css/style.css")
    )
    when(page) {
        "popup", "overlay", "welcome" -> commonStyles
        "options" -> commonStyles.apply {
            add(StyleSheet("https://use.fontawesome.com/releases/v5.1.0/css/solid.css"))
            add(StyleSheet("https://use.fontawesome.com/releases/v5.1.0/css/fontawesome.css"))
        }
        else -> listOf()
    }
}

// TODO: Move into dependency format
val scriptList = { page: String ->
    val commonScripts = mutableListOf(
            Script("lib/js/jquery-3.2.1.slim.min.js"),
            Script("lib/js/popper.min.js"),
            Script("lib/js/bootstrap.min.js"),
            Script("js/kotlin.js"),
            Script("js/kotlinx-html-js.js"),
            Script("js/DictHubExtension.js")
    )
    when(page) {
        "popup", "overlay" -> commonScripts.apply {
            add(Script("js/ga.js"))
        }
        "options" -> commonScripts.apply {
            add(Script("js/ga.js"))
            add(Script("lib/js/jquery.sortable.min.js"))
        }
        "sandbox", "welcome" -> commonScripts
        else -> listOf()
    }
}

val platforms = arrayOf("chrome", "firefox")
platforms.forEach { platform ->

    val unpackedPath = "$buildDir/$platform"

    val copyTask = task<Copy>("${platform}Copy") {
        group = "release"
        description = "Copy static files together to plugin directory"
        dependsOn("extractLibs")

        val jsLibDir: String by tasks["extractLibs"].extra

        from("src/main/static/$platform")
        from("src/main/static/shared")
        from(jsLibDir)

        into(unpackedPath)
    }

    val pages = arrayOf("popup", "overlay", "sandbox", "options", "welcome")
    val generateTasks = pages.map { page ->
        task<HtmlGenerationTask>("$platform${page}GenerateHtml") {
            outputPath = "$unpackedPath/$page.html"
            title = "DictHub $page Page"
            bodyId = "${page}Body"
            styleSheets = styleSheetsList(page)
            scripts = scriptList(page)
        }
    }

    val zipTask = task<Zip>("${platform}Zip") {
        archiveFileName.set(when(platform) {
            "chrome" -> "chrome.zip" // TODO: Sign into crx
            else -> "$platform.zip"
        })
        from(unpackedPath)
        destinationDirectory.set(file(buildDir))

        dependsOn(copyTask, generateTasks)
    }
    
    task(platform).dependsOn(generateTasks, copyTask, zipTask)
}

tasks["build"].dependsOn(*platforms)


defaultTasks = mutableListOf("build")