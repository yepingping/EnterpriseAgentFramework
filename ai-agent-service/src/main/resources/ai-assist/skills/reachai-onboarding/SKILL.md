---
name: reachai-onboarding
description: Integrate Java business systems with ReachAI SDK registration and capability sync. Use when asked to connect a Spring Boot service to ReachAI, add reachai-capability-sdk or reachai-spring-boot2-starter, configure reachai.registry/reachai.project/reachai.capability, expose @ReachCapability methods, or verify SDK onboarding from a ReachAI manifest.
---

# ReachAI Onboarding

## Operating Rules

Treat the current business repository as the source of truth. Inspect its Maven modules, Java version, Spring Boot version, configuration files, existing controller/service boundaries, and test commands before editing.

Never paste, print, or commit the registry app secret. Use the environment variable named by the manifest, normally `REACHAI_REGISTRY_APP_SECRET`.

Prefer minimal, reviewable changes:
- Add ReachAI dependencies only to the modules that need them.
- Put `reachai-spring-boot2-starter` in the runnable Spring Boot application module.
- Put `reachai-capability-sdk` in modules that declare `@ReachCapability` methods or DTO field metadata.
- Avoid changing unrelated business logic, package structure, formatting, or dependency versions.
- For automatic Spring MVC scanning, restrict ReachAI to business-owned packages. Do not sync framework, platform, third-party, starter, or shared infrastructure controllers as business APIs.

## Workflow

1. Read the ReachAI onboarding manifest URL from the user prompt.
2. Download this skill package if it is not already installed, then read the reference files only as needed.
3. Detect the project layout:
   - Maven root and child modules.
   - Java source level.
   - Spring Boot version.
   - Runnable application module.
   - Business-owned Java base packages from application classes, controllers, services, and module names.
   - Framework/platform packages that should be excluded from ReachAI scanning.
   - Existing `application.yml`, `bootstrap.yml`, profile-specific config, or config-center conventions.
4. Add dependencies using `references/java-sdk-access.md` and `templates/pom-dependencies.xml`.
5. Add configuration using `templates/application-reachai.yml`, replacing the package placeholders with narrow business package allow-lists and framework/package deny-lists.
6. Select one or two low-risk query-style business methods and annotate them with `@ReachCapability` / `@ReachParam`. Use `templates/reach-capability-example.java` only as a style example.
7. Run the smallest meaningful verification command in the business repo, normally Maven compile or a targeted test.
8. Call the manifest's `sdkAccessCheckUrl` only after local compile/config succeeds, or explain why a live check cannot run.
9. Report changed files, commands run, results, scan package choices, and manual secrets still required.

## References

- For dependency and Java/Spring guidance, read `references/java-sdk-access.md`.
- For platform API contracts, read `references/platform-apis.md`.
- For credential handling and prompt safety, read `references/security.md`.
- For ready-to-copy snippets, use files under `templates/`.
- For an optional local verification helper, run `scripts/verify-reachai-access.py`.

## Output Contract

End with:
- Files changed.
- Dependency/configuration summary.
- Capability annotations added.
- Verification commands and results.
- Whether `REACHAI_REGISTRY_APP_SECRET` still needs to be configured outside the repository.
