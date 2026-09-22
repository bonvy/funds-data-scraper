# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

A Spring Boot 4 web application that scrapes fund data, split into two Gradle modules:

- **`app`** — the Spring Boot application (`it.francesco.bonvecchio.scraper.ScraperApplication`): `spring-boot-starter-web` plus `@EnableScheduling`, with `ScrapeScheduler` firing on the cron in `scraper.schedule.cron`.
- **`core-scraping`** — the scraping domain: `ScraperService` / `ScraperServiceVirtualThread`, the `ParserSiteDefinition` hierarchy, `ParserInput` (Jsoup `Document`), and `AsyncService` / `VirtualThreadsAsyncService` over a virtual-thread-per-task executor.

Both modules live under `it.francesco.bonvecchio`. The scraping logic itself is still a skeleton — `ScraperServiceVirtualThread.scrape` has an empty body and no parser implementations exist yet.

There is no persistence layer (no JPA, no database) — deliberately deferred.

Not a git repository yet (`git init` has not been run).

## Commands

```bash
./gradlew build                      # compile + test all modules
./gradlew :app:bootRun               # run the application (Tomcat on :8080)
./gradlew :app:bootJar               # executable fat jar at app/build/libs/app.jar
./gradlew test                       # all tests
./gradlew :app:test                  # tests for one module
./gradlew :app:test --tests 'it.francesco.bonvecchio.scraper.ScraperApplicationTests'   # single test
```

There is no linter or formatter configured.

## Spring wiring

`core-scraping` is deliberately Spring-aware: its services carry `@Service` directly and depend on `org.springframework:spring-context` (annotations only — it stays a plain `java-library`, never repackaged into a boot jar). The application's `@SpringBootApplication(scanBasePackages = ["it.francesco.bonvecchio"])` scans from the shared root package so those beans are found across the module boundary; narrowing that attribute to the `app` package silently drops them.

Dependencies are constructor-injected (`ScraperServiceVirtualThread(asyncService)`), not built inline.

`spring.threads.virtual.enabled=true` in `app/src/main/resources/application.yml` puts Tomcat and Spring-managed executors on virtual threads. `VirtualThreadsAsyncService` remains a separate, independently owned executor — that property does not touch it.

## Build architecture

The build is layered; changes to compiler settings, dependency versions, or test config usually belong in `buildSrc`, not in a module's `build.gradle.kts`.

- **`buildSrc/src/main/kotlin/buildlogic.*.gradle.kts`** — precompiled convention plugins: each file is compiled into a real `Plugin` class whose **id is its filename** minus `.gradle.kts`, which is what `kotlin-dsl` in `buildSrc/build.gradle.kts` enables. They are composed, not inherited singly, so a module gets a whole chain by applying one leaf:

```
app                                              core-scraping
 └─ buildlogic.spring-boot-application-conventions └─ buildlogic.kotlin-library-conventions
     ├─ org.springframework.boot  (bootJar/bootRun)    ├─ java-library
     └─ buildlogic.kotlin-application-conventions      └─ buildlogic.kotlin-common-conventions
         ├─ application  (mainClass)                       ├─ org.jetbrains.kotlin.jvm
         └─ buildlogic.kotlin-common-conventions           ├─ org.jetbrains.kotlin.plugin.spring
                                                           ├─ toolchain 25 + Spring Boot BOM + JUnit
                                                           └─ mavenCentral()
```

  Each module applies **exactly one** leaf plugin. Coming from Maven: `kotlin-common-conventions` is this build's parent POM, `platform(...)` is an imported BOM, and `org.springframework.boot` is the counterpart of `spring-boot-maven-plugin` — not of a parent POM.
- **The Boot plugin goes only on a module that has a `main` and gets deployed.** `bootJar` emits an application package — classes under `BOOT-INF/classes/`, dependencies nested in `BOOT-INF/lib/`, `Main-Class` set to Spring's `JarLauncher` — which is *not* consumable as a dependency, since no compiler looks under `BOOT-INF/`. `core-scraping` must stay a plain `java-library` producing a flat jar; that jar is then nested inside `app.jar`. Applying the Boot plugin to a library forces the `tasks.bootJar { enabled = false }` workaround, which is the signal it should not have been applied.
- **The `kotlin-spring` plugin is not optional.** Kotlin classes are `final` by default, which breaks the CGLIB proxies Spring creates for `@Configuration`, `@Scheduled` and `@Transactional`. It is applied in `kotlin-common-conventions` so every module gets it.
- **The Spring Boot BOM is applied as a Gradle `platform(...)`, never via `io.spring.dependency-management`.** This is load-bearing: the BOM pins `kotlin 2.3.21` while this build compiles with **2.4.0**. A platform contributes *constraints*, which raise a version but never lower it, so conflict resolution keeps `kotlin-stdlib` at 2.4.0 (`./gradlew :app:dependencies --configuration runtimeClasspath` shows `2.3.21 -> 2.4.0 (c)`). `io.spring.dependency-management` uses Maven semantics and would *force* the stdlib back down to 2.3.21 under a newer compiler. Because the Boot plugin does not apply dependency management on its own, it contributes only `bootJar` / `bootRun`.
- Spring-managed dependencies are therefore declared **without a version** (`implementation("org.springframework.boot:spring-boot-starter-web")`); the BOM supplies it. Non-Spring shared dependencies go in `gradle/libs.versions.toml` and are wired in `kotlin-common-conventions` (Jsoup is wired this way).
- **`gradle/libs.versions.toml`** — version catalog. `buildSrc` is a *separate* Gradle build, compiled before the main one and contributing its jar to every project's build-script classpath (which is why convention plugins and `org.springframework.boot` are applied **without a version**); it only sees the catalog because its own `settings.gradle.kts` imports it explicitly. It holds the Kotlin Gradle plugin, `kotlin-allopen` (which provides the `org.jetbrains.kotlin.plugin.spring` id), the Spring Boot Gradle plugin, the Spring Boot BOM, and Jsoup.
- **Type-safe `libs.` accessors do not exist inside `buildSrc` precompiled script plugins.** They work in module build scripts and in `buildSrc/build.gradle.kts`, but a convention plugin must look the catalog up explicitly: `val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")`, then `libs.findLibrary("org-jsoup").get()` (alias spelled with the hyphen, as in the TOML).

### Where a dependency goes

`buildSrc/build.gradle.kts` and a convention plugin's `dependencies { }` look alike but target different classpaths, and this is the easiest mistake to make here:

| Declared in | Lands on | Use for |
|---|---|---|
| `buildSrc/build.gradle.kts` | Gradle's own build classpath | plugins the convention scripts need to *apply* (`kotlin-allopen`, `spring-boot-gradle-plugin`) |
| `buildlogic.*.gradle.kts` | every module applying that convention | dependencies genuinely wanted by **all** modules of that category |
| `<module>/build.gradle.kts` | that module only | everything else |

Putting `spring-context` in `buildSrc/build.gradle.kts` would hand it to the build scripts, not to `core-scraping`, and `@Service` would not compile.

A dependency earns its way up into a convention plugin when *every* module of that category wants it — not when two do. A second Spring-aware library should therefore get a new `buildlogic.spring-library-conventions` layer (applying `kotlin-library-conventions` + `spring-context`) rather than loading the generic library conventions, which are named for "Kotlin library", not "Spring library".

Known wart: Jsoup sits in `kotlin-common-conventions`, so it reaches `app`, which does not use it. Inherited from the generated build and kept for consistency; the tidier home is `core-scraping/build.gradle.kts` next to `spring-context`.

After editing a convention plugin, a stale `buildSrc` incremental-compile cache can produce nonsense errors such as `Unresolved reference 'implementation'` on code that is in fact correct. `rm -rf buildSrc/build buildSrc/.gradle` and rebuild before debugging the script itself.

Module graph: `app` → `core-scraping`. Nothing is exposed with `api`, so no transitive types leak across the boundary — a module needing Jsoup or Spring types on its own API must declare them itself.

The **Gradle configuration cache is enabled** (`org.gradle.configuration-cache=true` in `gradle.properties`) and the Spring Boot plugin is compatible with it. Build logic must stay configuration-cache compatible — no reading `project` at execution time, no unserializable task state. Failures here surface as configuration-cache problem reports rather than ordinary build errors.

The Java toolchain is 25 and the foojay resolver plugin (in `settings.gradle.kts`) auto-downloads a matching JDK, so the local `JAVA_HOME` version does not need to match.

Gradle 9.7.1 via the wrapper; Kotlin 2.4.0; Spring Boot 4.1.1 (Spring Framework 7.0.9); JUnit Jupiter 6.0.3 (from the BOM).

## Adding a module

1. Create `<name>/build.gradle.kts` applying `buildlogic.kotlin-library-conventions`.
2. Add `<name>` to the `include(...)` list in the root `settings.gradle.kts` — it is a flat list, not auto-discovered.
3. Keep new packages under `it.francesco.bonvecchio` so the application's component scan reaches any beans they declare.
