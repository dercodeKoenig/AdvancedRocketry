import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import org.ajoberstar.grgit.Grgit
import org.gradle.internal.jvm.Jvm
import se.bjurr.gitchangelog.plugin.gradle.GitChangelogTask
import java.text.SimpleDateFormat
import java.util.*

plugins {
    idea
    id("net.minecraftforge.gradle") version "6.+"
    id("wtf.gofancy.fancygradle") version "1.+"
    id("org.ajoberstar.grgit") version "4.1.1"
    id("com.matthewprenger.cursegradle") version "1.4.0"
    id("se.bjurr.gitchangelog.git-changelog-gradle-plugin") version "1.72.0"
    `maven-publish`
}

val mcVersion: String by project
val forgeVersion: String by project
val modVersion: String by project
val archiveBase: String by project

val libVulpesVersion: String by project
val jeiVersion: String by project
val icVersion: String by project
val gcVersion: String by project

val startGitRev: String by project

group = "zmaster587.advancedRocketry"
setProperty("archivesBaseName", archiveBase)

legacy {
    fixClasspath = true
}

val buildNumber: String by lazy { System.getenv("BUILD_NUMBER") ?: getDate() }

fun getDate(): String {
    return "1"
    val format = SimpleDateFormat("HH-mm-dd-MM-yyyy")
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date())
}

version = "$modVersion"

println("$archiveBase v$mcVersion-$version")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks {
    javadoc {
        options.encoding = "UTF-8"
    }
    compileJava {
        options.encoding = "UTF-8"
    }
    compileTestJava {
        options.encoding = "UTF-8"
    }

//    withType(JavaCompile) {
//        options.encoding = "UTF-8"
//    }
}

//configurations.configureEach {
//    exclude(group = "net.minecraftforge", module = "mergetool")
//}

//sourceCompatibility = targetCompatibility = '1.8' // Need this here so eclipse task generates correctly.
tasks.compileJava {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}


minecraft {
    mappings("snapshot", "20171003-1.12")

    accessTransformer(file("src/main/resources/META-INF/accessTransformer.cfg"))

    runs {
        create("client") {
            properties(
                mapOf(
                    "forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP,COREMODLOG",
                    "forge.logging.console.level" to "info"
                )
            )

            workingDirectory = file("run").canonicalPath

            mods {
                create("advancedrocketry") {
                    source(sourceSets["main"])
                }
            }
        }
        create("server") {
            properties(
                mapOf(
                    "forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP,COREMODLOG",
                    "forge.logging.console.level" to "info"//, "fml.coreMods.load" to "com.gramdatis.core.setup.GramdatisPlugin"
                )
            )
            arg("nogui")

            workingDirectory = file("run-server").canonicalPath

            mods {
                create("advancedrocketry") {
                    source(sourceSets["main"])
                }
            }
        }
    }
}

fancyGradle {
    patches {
        resources
        coremods
        codeChickenLib
        asm
    }
}

repositories {
    mavenCentral()
    // Forge Test Framework — resolved via `publishToMavenLocal` from a sibling
    // ForgeTestFramework checkout, OR via composite build (see settings.gradle.kts).
    // Content filter prevents Gradle from poking ~/.m2 for unrelated MC artifacts.
    mavenLocal {
        content {
            includeGroup("com.github.stannismod.forge")
        }
    }
    maven {
        name = "mezz.jei"
        url = uri("https://dvs1.progwml6.com/files/maven/")
    }
    maven {
        url = uri("https://cursemaven.com")
    }
    //ivy {
    //    name = "industrialcraft-2"
    //    artifactPattern("http://jenkins.ic2.player.to/job/IC2_111/39/artifact/build/libs/[module]-[revision].[ext]")
    //}
    maven {
        // location of a maven mirror for JEI files, as a fallback
        name = "ModMaven"
        url = uri("https://modmaven.k-4u.nl")
    }
    //maven {
    //    name = "Galacticraft"
    //    url = uri("https://maven.galacticraft.dev/repository/legacy-releases/")
    //}
//    maven {
//        name = "LibVulpes"
//        url = uri("http://maven.dmodoomsirius.me/")
//        isAllowInsecureProtocol = true
//    }
    flatDir {
        dirs("libs")
    }
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "$mcVersion-$forgeVersion")

//    implementation(fg.deobf("curse.maven:industrial-craft-242638:2746892"))
    //compileOnly("net.industrial-craft:industrialcraft-2:$icVersion:dev")
    //implementation("zmaster587.libVulpes:LibVulpes:$mcVersion-$libVulpesVersion-$libVulpesBuildNum-deobf")

    //compileOnly(fg.deobf("dev.galacticraft:galacticraft-legacy:$gcVersion"))
    compileOnly(fg.deobf("curse.maven:galacticraft-legacy-564236:4671122"))
    compileOnly(fg.deobf("mezz.jei:jei_${mcVersion}:${jeiVersion}:api"))
    implementation(fg.deobf("mezz.jei:jei_${mcVersion}:${jeiVersion}")) // Sorry but it won't start wihout jei...
    //runtimeOnly(fg.deobf("mezz.jei:jei_${mcVersion}:${jeiVersion}")) // I think this crashes the game for me when running from IntelliJ

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"), "exclude" to listOf("test/**", "compileOnly/**"))))
    compileOnly(fileTree(mapOf("dir" to "libs/compileOnly", "include" to listOf("*.jar"))))

//    implementation ("net.minecraftforge:mergetool:0.2.3.3")
    implementation ("net.minecraftforge:mergetool") { version { strictly("0.2.3.3") } }

    // Test framework (Forge 1.12.2 reusable test framework — see src/test/README.md).
    //
    // Resolution chain (first match wins):
    //   1. Composite build — settings.gradle.kts substitutes the module if
    //      `-PuseLocalFramework=true` AND ../ForgeTestFramework exists.
    //   2. mavenLocal()    — `./gradlew publishToMavenLocal` from ForgeTestFramework.
    //
    // The `:dev` classifier is REQUIRED: Forge dev workspace links against
    // MCP-named MC classes, the reobf (no-classifier) jar has SRG names and
    // won't compile against the dev classpath.
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.github.stannismod.forge:forge-test-framework:0.3.0:dev")
}

tasks.test {
    useJUnit()
    testLogging {
        events("failed", "skipped", "passed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
    // Test-only flag gating /artest probe commands and other test-only behavior
    systemProperty("advancedrocketry.tests", "true")
    // `test` runs only the fast pyramid layers: pure unit (SMART §2.1) and
    // lightweight integration with MC bootstrap in the same JVM (SMART §2.2).
    // Server/client-harness suites live under separate tasks because they
    // require the FG6 runServer classpath and a forked dedicated-server JVM.
    filter {
        includeTestsMatching("zmaster587.advancedRocketry.test.unit.*")
        includeTestsMatching("zmaster587.advancedRocketry.test.integration.*")
    }
}

// Tell the reusable test framework (v0.2.0+) which launcher / asset layout to use.
// Defaults in the framework target RFG/FG4 — these overrides flip it to FG6.
val fg6HarnessProps = mapOf(
    "forge.test.launcher.class.server" to "net.minecraftforge.legacydev.MainServer",
    "forge.test.launcher.class.client" to "mcp.client.Start",
    "forge.test.assets.dir" to "${gradle.gradleUserHomeDir}/caches/forge_gradle/assets",
    "forge.test.launcher.legacyArgs" to "false"
)

// SMART §11 — dedicated task that runs ONLY the AR scenario suite (P0 + P1
// scenarios composed by AdvancedRocketryTestRegistry). Useful in CI to gate
// merges on the in-game scenarios separately from the unit tests.
//
// Configurable expected weather mode for §7.5:
//   ./gradlew testAdvancedRocketryScenarios -Pweather=shared
//   ./gradlew testAdvancedRocketryScenarios -Pweather=per_dimension
val weatherMode: String = (project.findProperty("weather") as? String) ?: "shared"

val parallelForks: Int = (project.findProperty("forks") as? String)?.toIntOrNull() ?: 3

tasks.register<Test>("testAdvancedRocketryScenarios") {
    description = "Runs the AR-specific scenario suite (SMART §7 P0+P1+P2) in parallel."
    group = "verification"
    useJUnit()
    testClassesDirs = sourceSets["test"].output.classesDirs
    // Augment the test classpath with FG6's runServer classpath so that
    // RealDedicatedServerHarness has net.minecraftforge.legacydev.MainServer
    // (and the full MC dev classpath) available when it spawns a subprocess via
    // System.getProperty("java.class.path"). MinecraftRunTask is package-private
    // in the FG6 plugin — fetch its classpath reflectively at task-execution time.
    classpath = sourceSets["test"].runtimeClasspath + files(provider {
        val runServer = tasks.named("runServer").get()
        val cpField = runServer.javaClass.methods.firstOrNull { it.name == "getClasspath" && it.parameterCount == 0 }
                ?: error("runServer task does not expose getClasspath() — FG6 internals changed?")
        cpField.invoke(runServer) as FileCollection
    })
    filter {
        // SMART §2.3 server-harness e2e + §2.4 client-harness e2e. Each test
        // class is plain JUnit + an `AbstractHeadlessServerTest`/`AbstractClientE2ETest`
        // base; gradle filter just routes by package.
        includeTestsMatching("zmaster587.advancedRocketry.test.server.*")
        includeTestsMatching("zmaster587.advancedRocketry.test.client.*")
    }
    systemProperty("advancedrocketry.tests", "true")
    systemProperty("advancedrocketry.tests.expectedWeatherMode", weatherMode)
    // Forward FG6 paths to the test-framework harness (v0.2.0+).
    fg6HarnessProps.forEach { (k, v) -> systemProperty(k, v) }
    // Enable the framework's JUnit base classes (AbstractHeadlessServerTest)
    // by default. Override with -Pharness=false to skip server boot — all
    // tests then SKIP via JUnit Assume rather than failing.
    val harnessEnabled = (project.findProperty("harness") as? String) ?: "true"
    systemProperty("forge.test.harness.enabled", harnessEnabled)
    systemProperty("forge.test.client.enabled",
            (project.findProperty("clientHarness") as? String) ?: "false")

    // Parallel execution: one forked JVM per scenario class, up to `parallelForks`
    // running concurrently. Each scenario is independent (own port via
    // ServerSocket(0), own tempDir) so cross-fork interference is impossible.
    //
    // Budget: each fork holds the test runner JVM (~500 MB) + one harness JVM
    // (~1.5 GB). 6 forks ≈ 12 GB peak RAM. Tune via -Pforks=N.
    maxParallelForks = parallelForks
    setForkEvery(1L)
    // Per-fork JVM args — keep tight so we don't blow past RAM with 6 forks.
    minHeapSize = "256m"
    maxHeapSize = "768m"

    // FG6's MinecraftRunTask.exec() resolves env+sysprops via a runtime token map
    // (see RunConfigGenerator.configureTokensLazy). Replicate the same resolution
    // in doFirst so MainServer gets the env vars (mainClass, tweakClass, MCP_TO_SRG,
    // etc.) it needs. Without this the spawned server JVM dies with
    // "Must specify mainClass environment variable".
    doFirst {
        val runServer = tasks.named("runServer").get()
        val runConfig = runServer.javaClass.methods.first { it.name == "getRunConfig" }
                .invoke(runServer)
                .let { it.javaClass.getMethod("get").invoke(it) }

        // Resolve the token map via the same package-private path FG6 uses.
        val rcgClass = Class.forName("net.minecraftforge.gradle.common.util.runs.RunConfigGenerator")
        val mapModClassesMethod = rcgClass.declaredMethods.first { it.name == "mapModClassesToGradle" }
        mapModClassesMethod.isAccessible = true
        val modClassesStream = mapModClassesMethod.invoke(null, project, runConfig)
        val mcArtifacts = runServer.javaClass.methods.first { it.name == "getMinecraftArtifacts" }.invoke(runServer)
        val rtArtifacts = runServer.javaClass.methods.first { it.name == "getRuntimeClasspathArtifacts" }.invoke(runServer)
        val configureTokens = rcgClass.declaredMethods.first { it.name == "configureTokensLazy" }
        configureTokens.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val tokenMap = configureTokens.invoke(null, project, runConfig, modClassesStream, mcArtifacts, rtArtifacts)
                as Map<String, java.util.function.Supplier<String>>

        // RunConfig.replace(Map, String) substitutes ${...} / {...} placeholders.
        val replaceMethod = runConfig.javaClass.getMethod("replace", Map::class.java, String::class.java)

        @Suppress("UNCHECKED_CAST")
        val rcEnv = runConfig.javaClass.getMethod("getEnvironment").invoke(runConfig) as Map<String, String>
        rcEnv.forEach { (k, v) ->
            val resolved = replaceMethod.invoke(runConfig, tokenMap, v) as String
            environment(k, resolved)
        }
        // FG6's RunConfig.environment uses ${MC_VERSION} as a placeholder for the
        // configured MC version; token map doesn't always resolve it (FG6 plugs
        // it in late, after token resolution). Set it explicitly so the harness
        // subprocess has the right MC version to find the deobfuscation_data file.
        environment("MC_VERSION", mcVersion)
        @Suppress("UNCHECKED_CAST")
        val rcProps = runConfig.javaClass.getMethod("getProperties").invoke(runConfig) as Map<String, String>
        val resolvedProps = mutableMapOf<String, String>()
        rcProps.forEach { (k, v) ->
            val resolved = replaceMethod.invoke(runConfig, tokenMap, v) as String
            systemProperty(k, resolved)
            resolvedProps[k] = resolved
        }
        // The test framework's RealDedicatedServerHarness spawns a child JVM with
        // a hard-coded arg list — it inherits env vars but NOT the -D properties
        // FG6 sets on the parent test JVM (MCP_TO_SRG csv dir, srg.notch-srg,
        // mainClass, tweakClass, etc.). Without those, launchwrapper can't locate
        // the deobfuscation_data file and FMLDeobfuscatingRemapper.setup NPEs.
        //
        // Workaround: pack the same -D flags into JAVA_TOOL_OPTIONS env var, which
        // every spawned JVM auto-prepends to its CLI. Paths with spaces get
        // single-quoted (JAVA_TOOL_OPTIONS uses shell-style quoting).
        val toolOptions = resolvedProps.entries.joinToString(" ") { (k, v) ->
            if (v.contains(" ")) "-D$k=\"$v\"" else "-D$k=$v"
        }
        if (toolOptions.isNotEmpty()) {
            environment("JAVA_TOOL_OPTIONS", toolOptions)
        }
        logger.lifecycle("Forwarded ${rcEnv.size} env vars and ${rcProps.size} system properties from runServer config")
    }
    testLogging {
        events("failed", "skipped", "passed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // Building AR's jar is a soft prereq because runServer's classpath includes it
    // (and the in-game mod must be present for any AR-specific assertion).
    dependsOn(tasks.named("jar"))
}

tasks.processResources {
    //includeEmptyDirs = false
    inputs.properties(
        "advRocketryVersion" to project.version,
        "mcVersion" to mcVersion,
        "libVulpesVersion" to libVulpesVersion
    )

    filesMatching("mcmod.info") {
        expand(
            "advRocketryVersion" to project.version,
            "mcVersion" to mcVersion,
            "libVulpesVersion" to libVulpesVersion
        )
    }

    exclude("**/*.sh")
}

val currentJvm: String = Jvm.current().toString()
println("Current Java version: $currentJvm")

val gitHash: String by lazy {
    // .git can be a file (git worktree) — Grgit only works on a real .git directory.
    // Fall back to "unknown" so that builds in worktrees / shallow checkouts don't fail
    // configuration phase for tasks unrelated to changelog generation.
    val gitMarker = File(projectDir, ".git")
    val hash: String = if (gitMarker.isDirectory) {
        try {
            val repo = Grgit.open(mapOf("currentDir" to project.rootDir))
            repo.log().first().abbreviatedId
        } catch (e: Exception) {
            println("Grgit failed to resolve HEAD: ${e.message}")
            "unknown"
        }
    } else {
        "unknown"
    }
    println("GitHash: $hash")
    return@lazy hash
}

// Name pattern: [archiveBaseName]-[archiveAppendix]-[archiveVersion]-[archiveClassifier].[archiveExtension]
tasks.withType(Jar::class) {
    archiveAppendix.set(mcVersion)
    manifest {
        attributes(
                "Built-By" to System.getProperty("user.name"),
                "Created-By" to currentJvm,
                "Implementation-Title" to archiveBase,
                "Implementation-Version" to project.version,
                "Git-Hash" to gitHash,
                "FMLAT" to "accessTransformer.cfg",
                "FMLCorePlugin" to "zmaster587.advancedRocketry.asm.AdvancedRocketryPlugin",
                "FMLCorePluginContainsFMLMod" to "true"
        )
    }
}

val deobfJar by tasks.registering(Jar::class) {
    from(sourceSets["main"].output)
    archiveClassifier.set("deobf")
}

tasks.build {
    dependsOn(deobfJar)
}

val makeChangelog by tasks.creating(GitChangelogTask::class.java) {
    file = file("changelog.html")
    untaggedName = "Current release ${mcVersion}-${project.version}"

    //Get the last commit from the cache or config if no cache exists
    val lastHashFile = file("lasthash.txt")

    fromCommit = if (!lastHashFile.exists())
        startGitRev
    else
        lastHashFile.readText()

    // Defer writing lasthash.txt to task execution so the changelog metadata is only
    // touched when this task actually runs (release pipeline), not on every configure.
    doFirst {
        lastHashFile.writeText(gitHash)
    }

    toRef = "HEAD"
    gitHubIssuePattern = "nonada123";
    templateContent = """
        {{#tags}}
          <h3>{{name}}</h3>
          <ul>
            {{#commits}}
            <li> <a href="https://github.com/zmaster587/AdvancedRocketry/commit/{{hash}}" target=_blank> {{{message}}}</a>
        </li>
            {{/commits}}
          </ul>
        {{/tags}}
    """.trimIndent()
}

curseforge {
    apiKey = (project.findProperty("thecursedkey") as String?).orEmpty()

    project(closureOf<CurseProject> {
        id = "236542"
        relations(closureOf<CurseRelation> {
            requiredDependency("libvulpes")
        })
        changelog = file("changelog.html")
        changelogType = "html"
        // Why is it hardcoded to beta tho?..
        releaseType = "release"
        addGameVersion(mcVersion)
        mainArtifact(tasks.jar.get(), closureOf<CurseArtifact> {
            displayName = "AdvancedRocketry ${ project.version } build $buildNumber for $mcVersion"
            })
        addArtifact(deobfJar.get(), closureOf<CurseArtifact> {
            displayName = "AdvancedRocketry ${ project.version }-deobf build $buildNumber for $mcVersion"
        })
    })
}

tasks.curseforge {
    dependsOn(makeChangelog)
}

publishing {
    repositories {
        maven {
            url = if (project.findProperty("local") == "true")
                uri("$buildDir/build/maven")
            else
                uri("file:///usr/share/nginx/maven/")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            //from(components["java"])

            artifact(tasks.jar.get())
            artifact(deobfJar.get())
            artifact(makeChangelog.file)
        }
    }
}

tasks.curseforge {
  dependsOn("reobfJar")
}

tasks.publish {
    dependsOn(makeChangelog)
}

idea {
    module {
        inheritOutputDirs = true
    }
}
