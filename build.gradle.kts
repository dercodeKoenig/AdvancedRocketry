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

// ─── Mixin annotation-processor configuration ────────────────────────────────
//
// Mixins are written against deobfuscated (MCP) names so the dev workspace
// compiles and the IDE understands them. At runtime in a reobf production jar
// the targets are SRG-named. The Mixin annotation processor solves this by
// emitting a "refmap" — a JSON file (`mixins.advancedrocketry.refmap.json`)
// mapping every MCP selector in our `@Mixin`/`@At`/`@Inject` annotations to
// its SRG counterpart. The mixin runtime reads the refmap at apply time.
//
// FG6 emits MCP↔SRG mappings in two forms:
//   - build/createMcpToSrg/output.tsrg — MCP→SRG, but in TSRG v2 format
//     (Mixin AP 0.8.5's TSRG reader only understands v1, no header).
//   - build/createSrgToMcp/output.srg — SRG→MCP, but in classic FG3 SRG format
//     (CL:/FD:/MD: lines), which Mixin AP DOES understand — just in the WRONG
//     direction (it'd think the SRG name is the "source" name).
//
// Cheapest reliable path: take createSrgToMcp's .srg file and swap the last
// two whitespace-separated columns on every CL:/FD:/MD: line so it becomes a
// MCP→SRG .srg file. Mixin AP reads it via -AreobfSrgFile and emits a
// populated refmap (otherwise it silently produces an empty mappings table
// and any @At/@Accessor that names MCP fields fails to bind in reobf jars).
val mixinRefmapFile = layout.buildDirectory.file("refmaps/mixins.advancedrocketry.refmap.json")
val mixinSrgFile = layout.buildDirectory.file("mixinMappings/mcp_to_srg.srg")

val mixinReverseSrg by tasks.registering {
    dependsOn("createSrgToMcp")
    val srcFile = layout.buildDirectory.file("createSrgToMcp/output.srg")
    val dstFile = mixinSrgFile
    inputs.file(srcFile)
    outputs.file(dstFile)
    doLast {
        val src = srcFile.get().asFile
        val dst = dstFile.get().asFile
        dst.parentFile.mkdirs()
        val out = StringBuilder()
        for (raw in src.readLines()) {
            val parts = raw.split(' ')
            // Line shapes:
            //   PK: srcPkg dstPkg                    (2 cols after tag)
            //   CL: srcCls dstCls                    (2 cols)
            //   FD: srcCls/srcName dstCls/dstName    (2 cols)
            //   MD: srcCls/srcName srcDesc dstCls/dstName dstDesc   (4 cols)
            // Swap pairs to invert direction.
            when {
                parts.size == 3 && parts[0].endsWith(":") -> {
                    // PK / CL / FD: <src> <dst>
                    out.append(parts[0]).append(' ').append(parts[2]).append(' ').append(parts[1])
                }
                parts.size == 5 && parts[0] == "MD:" -> {
                    // MD: srcCls/srcName srcDesc dstCls/dstName dstDesc
                    out.append("MD: ").append(parts[3]).append(' ').append(parts[4]).append(' ')
                            .append(parts[1]).append(' ').append(parts[2])
                }
                else -> {
                    // Comment / blank / unknown — preserve verbatim.
                    out.append(raw)
                }
            }
            out.append('\n')
        }
        dst.writeText(out.toString())
    }
}

tasks.compileJava {
    dependsOn(mixinReverseSrg)
    val srgFile = mixinSrgFile.get().asFile
    val refmapOut = mixinRefmapFile.get().asFile
    options.compilerArgs.addAll(listOf(
        "-AreobfSrgFile=${srgFile.absolutePath}",
        "-AoutRefMapFile=${refmapOut.absolutePath}",
        "-AdefaultObfuscationEnv=searge"
    ))
    doFirst { refmapOut.parentFile.mkdirs() }
    outputs.file(mixinRefmapFile)
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
    // MixinBooter — modpack-standard Mixin loader for 1.12.2 (CleanroomMC). Used
    // at runtime to bootstrap our `mixins.advancedrocketry.json` config via the
    // jar's `MixinConfigs` manifest entry; we depend on it `compileOnly` (the
    // mod expects MixinBooter to be present in any modern 1.12.2 modpack
    // environment, incl. "Towards Rocket Science").
    maven {
        name = "CleanroomMC"
        url = uri("https://maven.cleanroommc.com")
    }
    // SpongePowered Mixin (compile-time API + annotation processor for refmap
    // generation). Runtime Mixin is provided by MixinBooter.
    maven {
        name = "SpongePowered"
        url = uri("https://repo.spongepowered.org/maven")
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

    // ── Mixin (Sponge 0.8.x via MixinBooter) ──────────────────────────────
    //
    // Compile-time API (annotations + interfaces) + annotation processor
    // (generates the refmap mapping MCP names in mixin source to the SRG
    // names present in production reobf jars).
    //
    // MixinBooter is the 1.12.2 modpack-standard Mixin loader. It IS itself a
    // coremod — its FMLCorePlugin manifest entry installs the MixinTweaker,
    // and during FML coremod scan it reads each loaded coremod's
    // `MixinConfigs` manifest attribute and applies the referenced configs.
    //
    // Pinned to 8.x: MixinBooter 9.x bundles a Mixin that validates class
    // features against ASM 7+ (`ConstantDynamic` etc.) during config init —
    // Forge 1.12.2's launchwrapper ships ASM 5, so 9.x crashes at boot with
    // NoClassDefFoundError before our config is even applied. 8.9 is what
    // mainstream 1.12.2 modpacks (GTNH and friends) actually ship, and it
    // works on stock ASM 5.
    //
    // Declared `implementation(fg.deobf(...))` so it lands on the dev RUNTIME
    // classpath (runClient/runServer/test) — same pattern JEI uses. Without
    // this our mixins would compile fine but silently fail to apply at run
    // time. Production jar is NOT shaded; the dependency is recorded in the
    // .pom so modpack tooling knows to ship MixinBooter alongside AR.
    implementation(fg.deobf("zone.rong:mixinbooter:7.0"))
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT:processor")
    compileOnly("org.spongepowered:mixin:0.8.5-SNAPSHOT")

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
    testImplementation("com.github.stannismod.forge:forge-test-framework:0.4.2:dev")
}

// The client harness (testClient) launches the real Minecraft client through
// FG6's `legacydev` launcher. legacydev 0.2.4.1 bundles GradleForgeHacks /
// CoremodTweaker — the machinery that scans the run classpath for coremods and
// FMLAT access transformers and remaps their SRG names to MCP for the dev
// environment. FG6's own runClient uses 0.2.4.1; but the Forge POM only
// requests `legacydev:0.2.3.+`, and 0.2.3.1 predates that machinery entirely.
// Without this force the harness launches with 0.2.3.1 and dependency-mod
// access transformers are silently skipped — JEI's jei_at.cfg never widens
// TextureMap.initMissingImage(), so mod init dies with an IllegalAccessError.
configurations.named("testRuntimeClasspath") {
    resolutionStrategy {
        force("net.minecraftforge:legacydev:0.2.4.1")
    }
}

// ─── Test task topology ──────────────────────────────────────────────────────
//
// Test TYPE is selected by DIRECTORY, never by command-line flags. Each of the
// four SMART §2 pyramid layers lives in its own package under src/test/java and
// has its own Gradle task:
//
//   ./gradlew testUnit         → §2.1 pure unit            (fast, no harness)
//   ./gradlew testIntegration  → §2.2 MC-bootstrap integ.  (fast, no harness)
//   ./gradlew testServer       → §2.3 dedicated-server e2e (harness, forked JVMs)
//   ./gradlew testClient       → §2.4 real-client + server (harness + GL client)
//   ./gradlew test             → ALL of the above (umbrella — runs the whole
//                                src/test tree by delegating to the four tasks)
//
// No -P flags are REQUIRED for any of these. Two OPTIONAL perf/mode overrides
// still exist (they have sane defaults — you never have to pass them):
//   -Pforks=N      parallel harness JVMs               (default 3)
//   -Pweather=...  expected weather mode for §7.5       (default shared)
//
// In the IDE, "Run all tests in directory" on any of the four packages works
// natively (IntelliJ drives JUnit directly, bypassing the Gradle filter).

// Default flipped from "shared" to "per_dimension" once the B1 weather wrapper
// (PlanetWeatherManager + Mixin on WorldServerMulti) landed — AR planets now
// have independent vanilla weather state by default. Pass -Pweather=shared to
// override (e.g. for bisecting whether the wrap silently regressed).
val weatherMode: String = (project.findProperty("weather") as? String) ?: "per_dimension"
val parallelForks: Int = (project.findProperty("forks") as? String)?.toIntOrNull() ?: 3
// The client harness layer (testClient) launches a real, GL-rendering Minecraft
// client per scenario. Running several of those concurrently makes the
// right-click → openGui → displayGuiScreen round-trip unreliable (the GUI
// silently fails to open under GL/CPU contention), so the client layer
// serialises by default. Override with -PclientForks=N if your host can take it.
val clientForks: Int = (project.findProperty("clientForks") as? String)?.toIntOrNull() ?: 1

// Tell the reusable test framework (v0.2.0+) which launcher / asset layout to use.
// Defaults in the framework target RFG/FG4 — these overrides flip it to FG6.
//
// FG6's `legacydev` module is the GradleStart analog: net.minecraftforge.legacydev
// .MainServer / .MainClient. Both are env-var driven (mainClass, tweakClass,
// MCP_TO_SRG, MC_VERSION, assetIndex, assetDirectory, nativesDirectory). The
// server harness gets those from the test JVM's environment (forwarded from
// runServer below); the client harness gets them via the framework's
// forge.test.client.env.* channel (forwarded from runClient below) because
// server and client need DIFFERENT mainClass/tweakClass and can't share one
// inherited environment.
val fg6HarnessProps = mapOf(
    "forge.test.launcher.class.server" to "net.minecraftforge.legacydev.MainServer",
    "forge.test.launcher.class.client" to "net.minecraftforge.legacydev.MainClient",
    "forge.test.assets.dir" to "${gradle.gradleUserHomeDir}/caches/forge_gradle/assets",
    "forge.test.launcher.legacyArgs" to "false"
)

// Reflects a FG6 MinecraftRunTask's RunConfig and resolves its environment +
// properties through the exact token map FG6 uses at runtime
// (RunConfigGenerator.configureTokensLazy). Returns (resolvedEnv, resolvedProps).
// MinecraftRunTask + RunConfigGenerator are package-private in the FG6 plugin,
// hence the reflection.
fun resolveFg6RunConfig(runTaskName: String): Pair<Map<String, String>, Map<String, String>> {
    val runTask = tasks.named(runTaskName).get()
    val runConfig = runTask.javaClass.methods.first { it.name == "getRunConfig" }
            .invoke(runTask)
            .let { it.javaClass.getMethod("get").invoke(it) }

    val rcgClass = Class.forName("net.minecraftforge.gradle.common.util.runs.RunConfigGenerator")
    val mapModClassesMethod = rcgClass.declaredMethods.first { it.name == "mapModClassesToGradle" }
    mapModClassesMethod.isAccessible = true
    val modClassesStream = mapModClassesMethod.invoke(null, project, runConfig)
    val mcArtifacts = runTask.javaClass.methods.first { it.name == "getMinecraftArtifacts" }.invoke(runTask)
    val rtArtifacts = runTask.javaClass.methods.first { it.name == "getRuntimeClasspathArtifacts" }.invoke(runTask)
    val configureTokens = rcgClass.declaredMethods.first { it.name == "configureTokensLazy" }
    configureTokens.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val tokenMap = configureTokens.invoke(null, project, runConfig, modClassesStream, mcArtifacts, rtArtifacts)
            as Map<String, java.util.function.Supplier<String>>

    val replaceMethod = runConfig.javaClass.getMethod("replace", Map::class.java, String::class.java)
    @Suppress("UNCHECKED_CAST")
    val rcEnv = runConfig.javaClass.getMethod("getEnvironment").invoke(runConfig) as Map<String, String>
    @Suppress("UNCHECKED_CAST")
    val rcProps = runConfig.javaClass.getMethod("getProperties").invoke(runConfig) as Map<String, String>

    val resolvedEnv = rcEnv.mapValues { (_, v) -> replaceMethod.invoke(runConfig, tokenMap, v) as String }
    val resolvedProps = rcProps.mapValues { (_, v) -> replaceMethod.invoke(runConfig, tokenMap, v) as String }
    return resolvedEnv to resolvedProps
}

// Packs a -D property map into a JAVA_TOOL_OPTIONS-style string (every JVM
// auto-prepends JAVA_TOOL_OPTIONS to its CLI). Paths with spaces get quoted.
fun packToolOptions(props: Map<String, String>): String =
    props.entries.joinToString(" ") { (k, v) ->
        if (v.contains(" ")) "-D$k=\"$v\"" else "-D$k=$v"
    }

// Standard logging/JUnit config shared by every test task.
fun Test.applyCommonTestConfig() {
    useJUnit()
    // Test-only flag gating /artest probe commands and other test-only behaviour.
    systemProperty("advancedrocketry.tests", "true")
    testLogging {
        events("failed", "skipped", "passed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Shared config for the harness-backed layers (server + client). Bakes in the
// FG6 runServer classpath augmentation, harness system properties, parallel-fork
// budget, and the env/sysprop forwarding doFirst block. `enableClient` additionally
// turns on the real-client harness — auto-skipped (JUnit Assume) on headless
// machines, no flag required.
fun Test.configureHarnessLayer(enableClient: Boolean) {
    group = "verification"
    applyCommonTestConfig()
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
    systemProperty("advancedrocketry.tests.expectedWeatherMode", weatherMode)
    // Forward FG6 paths to the test-framework harness (v0.2.0+).
    fg6HarnessProps.forEach { (k, v) -> systemProperty(k, v) }
    // The dedicated-server harness is ALWAYS on for these tasks — that's the
    // entire point of running them. There is no -Pharness flag: to skip the
    // harness you simply don't run the server/client task.
    systemProperty("forge.test.harness.enabled", "true")
    if (enableClient) {
        // The real Minecraft client needs an OpenGL-capable display. Auto-detect:
        // enabled on desktops, auto-skipped via JUnit Assume on headless CI.
        // No -PclientHarness flag. (GraphicsEnvironment is resolved reflectively
        // because the Kotlin build-script classpath doesn't expose java.awt.*.)
        val headless = runCatching {
            val ge = Class.forName("java.awt.GraphicsEnvironment")
            ge.getMethod("isHeadless").invoke(null) as Boolean
        }.getOrDefault(true)
        systemProperty("forge.test.client.enabled", (!headless).toString())
        // FG6 extracts the LWJGL natives into <project>/build/natives — not the
        // RFG/FG4 cache layout the framework's RealClientHarness defaults to.
        // forge-test-framework 0.4.0+ honours this override.
        systemProperty("forge.test.client.nativesDir",
                layout.buildDirectory.dir("natives").get().asFile.absolutePath)
    }

    // Parallel execution: one forked JVM per scenario class, up to the layer's
    // fork budget running concurrently. Each scenario is independent (own port
    // via ServerSocket(0), own tempDir) so cross-fork interference is impossible.
    //
    // The dedicated-server layer scales out (`parallelForks`, default 3). The
    // client layer launches a real GL-rendering Minecraft per scenario; those
    // contend badly when run concurrently, so it serialises by default
    // (`clientForks`, default 1).
    //
    // Budget: each fork holds the test runner JVM (~500 MB) + one harness JVM
    // (~1.5 GB). Tune via -Pforks=N (server) / -PclientForks=N (client).
    maxParallelForks = if (enableClient) clientForks else parallelForks
    setForkEvery(1L)
    // Per-fork JVM args — keep tight so we don't blow past RAM with 6 forks.
    minHeapSize = "256m"
    maxHeapSize = "768m"

    // FG6's MinecraftRunTask.exec() resolves env+sysprops via a runtime token map
    // (see RunConfigGenerator.configureTokensLazy). Replicate the same resolution
    // in doFirst so the legacydev launchers (MainServer / MainClient) get the env
    // vars (mainClass, tweakClass, MCP_TO_SRG, …) they need. Without this the
    // spawned JVM dies with "Must specify mainClass environment variable".
    doFirst {
        // --- Server harness environment -------------------------------------
        // AbstractClientE2ETest boots a dedicated server too, so EVERY harness
        // task forwards runServer's config. RealDedicatedServerHarness's child
        // JVM inherits the test JVM's env vars; the -D properties additionally
        // ride along via JAVA_TOOL_OPTIONS (every JVM auto-prepends it).
        val (serverEnv, serverProps) = resolveFg6RunConfig("runServer")
        serverEnv.forEach { (k, v) -> environment(k, v) }
        // FG6's RunConfig.environment uses ${MC_VERSION} as a placeholder the
        // token map doesn't always resolve — set it explicitly.
        environment("MC_VERSION", mcVersion)
        serverProps.forEach { (k, v) -> systemProperty(k, v) }
        val serverToolOptions = packToolOptions(serverProps)
        if (serverToolOptions.isNotEmpty()) {
            environment("JAVA_TOOL_OPTIONS", serverToolOptions)
        }
        logger.lifecycle("Forwarded ${serverEnv.size} env + ${serverProps.size} props from runServer")

        // --- Client harness environment -------------------------------------
        // The client subprocess can't inherit the test JVM's env: that carries
        // runServer's mainClass/tweakClass. The client needs runClient's. The
        // framework (forge-test-framework 0.4.0+) applies any
        // forge.test.client.env.<NAME> system property as env var <NAME> on the
        // client process ONLY, overriding the inherited (server) value.
        if (enableClient) {
            val (clientEnv, clientProps) = resolveFg6RunConfig("runClient")
            clientEnv.forEach { (k, v) -> systemProperty("forge.test.client.env.$k", v) }
            systemProperty("forge.test.client.env.MC_VERSION", mcVersion)
            val clientToolOptions = packToolOptions(clientProps)
            if (clientToolOptions.isNotEmpty()) {
                systemProperty("forge.test.client.env.JAVA_TOOL_OPTIONS", clientToolOptions)
            }
            logger.lifecycle("Forwarded ${clientEnv.size} env + ${clientProps.size} props "
                    + "from runClient (as forge.test.client.env.*)")

            // Merge main resources into the main classes dir for the client.
            //
            // Gradle splits a source set's output into build/classes/java/main
            // (compiled classes) and build/resources/main (assets, mcmod.info).
            // FML's ModDiscoverer.findClasspathMods() makes a SEPARATE mod
            // candidate per classpath directory: the @Mod class is found in the
            // classes dir → AR's mod + its IResourceManager resource pack are
            // rooted there, with no assets/ or mcmod.info. The result on the
            // client: mcmod.info missing ("missing required element 'name'")
            // and every TileEntitySpecialRenderer that loads an .obj model via
            // Minecraft.getResourceManager() throws → ClientProxy.registerRenderers
            // NPEs the whole mod load.
            //
            // The dedicated server doesn't render, so it never trips this — only
            // the client needs assets co-located with classes, exactly like a
            // packaged mod jar has them. Sync them together for the client run.
            val classesDir = sourceSets["main"].output.classesDirs.files
                    .firstOrNull { it.name == "main" && it.parentFile.name == "java" }
                    ?: sourceSets["main"].output.classesDirs.files.first()
            val resourcesDir = sourceSets["main"].output.resourcesDir
            if (resourcesDir != null && resourcesDir.isDirectory) {
                copy {
                    from(resourcesDir)
                    into(classesDir)
                }
                logger.lifecycle("Merged main resources into $classesDir for the client harness")
            }
        }
    }
    // Building AR's jar is a soft prereq because runServer's classpath includes it
    // (and the in-game mod must be present for any AR-specific assertion).
    dependsOn(tasks.named("jar"))
    // FG6 generates the SRG/MCP mapping files lazily — runServer/runClient pull
    // them in, but they are NOT inputs to compileJava. On a fresh checkout where
    // those run tasks have never been invoked, build/extractMappings,
    // build/createSrgToMcp, and build/createLegacyObf2Srg don't exist; the -D
    // props we forward (csvDir, srg.notch-srg) point at missing files, and the
    // forked dedicated server NPEs in FMLDeobfuscatingRemapper.setup before
    // printing its ready marker. Force the mapping outputs to materialise.
    // Use string-based dependsOn — FG6 registers these tasks lazily during its
    // own plugin apply, so `tasks.named("extractMappings")` here would throw
    // UnknownTaskException at script-evaluation time.
    dependsOn("extractMappings", "createSrgToMcp", "createLegacyObf2Srg")
    if (enableClient) {
        // The real client also needs assets resolved (sounds.json, lang files,
        // textures) — FG6's downloadAssets populates the assetIndex referenced
        // by the launcher.
        dependsOn("downloadAssets")
    }
}

// ── §2.1 — pure unit tests (no MC runtime) ──
tasks.register<Test>("testUnit") {
    description = "SMART §2.1 — pure unit tests (src/test/.../unit). Fast, no harness."
    group = "verification"
    applyCommonTestConfig()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("zmaster587.advancedRocketry.test.unit.*") }
}

// ── §2.2 — lightweight integration tests (MC bootstrap in-JVM) ──
tasks.register<Test>("testIntegration") {
    description = "SMART §2.2 — integration tests (src/test/.../integration). Fast, no harness."
    group = "verification"
    applyCommonTestConfig()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("zmaster587.advancedRocketry.test.integration.*") }
    mustRunAfter("testUnit")
}

// ── §2.3 — dedicated-server harness e2e ──
tasks.register<Test>("testServer") {
    description = "SMART §2.3 — dedicated-server scenario e2e (src/test/.../server)."
    configureHarnessLayer(enableClient = false)
    filter { includeTestsMatching("zmaster587.advancedRocketry.test.server.*") }
    mustRunAfter("testIntegration")
}

// ── §2.4 — real-client + dedicated-server e2e ──
tasks.register<Test>("testClient") {
    description = "SMART §2.4 — real-client + server e2e (src/test/.../client). " +
            "Auto-skips on headless machines."
    configureHarnessLayer(enableClient = true)
    filter { includeTestsMatching("zmaster587.advancedRocketry.test.client.*") }
    mustRunAfter("testServer")
    // The real client JVM loads LWJGL natives from build/natives — make sure
    // FG6 has extracted them before the harness tries to launch the client.
    dependsOn("extractNatives")
}

// `test` is the umbrella: running it runs the ENTIRE src/test tree by delegating
// to the four per-layer tasks. It runs no tests in its own JVM — that keeps each
// layer's fork strategy intact (the fast layers must NOT inherit the harness
// forkEvery(1), which would spawn a JVM per unit test class).
tasks.test {
    useJUnit()
    filter {
        isFailOnNoMatchingTests = false
        // Umbrella task — the real work is in the four dependency tasks below.
        includeTestsMatching("__advancedrocketry_umbrella_runs_nothing__")
    }
    dependsOn("testUnit", "testIntegration", "testServer", "testClient")
}

// Back-compat alias — SMART §11 and src/test/README.md still reference this name.
// It runs the two harness layers (server + client).
tasks.register("testAdvancedRocketryScenarios") {
    description = "Alias — runs the harness layers (testServer + testClient)."
    group = "verification"
    dependsOn("testServer", "testClient")
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
    // The testClient harness layer merges build/resources/main into
    // build/classes/java/main (so the client sees AR's assets co-located with
    // its @Mod class, like a packaged jar). That leaves the resource files
    // present in BOTH source-set output dirs, so a subsequent `jar` run sees
    // every asset twice. The duplicates are byte-identical, so first-wins is
    // correct.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
                "Built-By" to System.getProperty("user.name"),
                "Created-By" to currentJvm,
                "Implementation-Title" to archiveBase,
                "Implementation-Version" to project.version,
                "Git-Hash" to gitHash,
                "FMLAT" to "accessTransformer.cfg",
                "FMLCorePlugin" to "zmaster587.advancedRocketry.asm.AdvancedRocketryPlugin",
                "FMLCorePluginContainsFMLMod" to "true",
                // MixinBooter scans coremod manifests for this attribute at FML
                // coremod-load time and applies each comma-separated mixin config.
                // We ship a single config covering the per-dimension weather wrapper.
                "MixinConfigs" to "mixins.advancedrocketry.json"
        )
    }
    // Package the Mixin AP's generated refmap into the jar root, where the
    // mixin runtime expects to find the file referenced by mixins.advancedrocketry.json
    // ("refmap": "mixins.advancedrocketry.refmap.json").
    from(mixinRefmapFile)
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
