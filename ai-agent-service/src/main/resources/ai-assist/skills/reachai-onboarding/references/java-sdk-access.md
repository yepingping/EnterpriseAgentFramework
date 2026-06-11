# Java SDK Access

Use this reference after reading the onboarding manifest.

## Module Placement

- Add `reachai-spring-boot2-starter` to the runnable Spring Boot application module.
- Add `reachai-capability-sdk` to every module that declares `@ReachCapability`, `@ReachParam`, or `@ReachOutput`.
- In a single-module service, both dependencies usually go into the root `pom.xml`.
- In a multi-module service, do not add the starter to pure API, DTO, or library modules.

## Maven Dependencies

Use the versions from the manifest. If no manifest version is available, use the platform-provided template.

```xml
<dependency>
  <groupId>com.enterprise.ai</groupId>
  <artifactId>reachai-capability-sdk</artifactId>
  <version>${reachai.sdk.version}</version>
</dependency>
<dependency>
  <groupId>com.enterprise.ai</groupId>
  <artifactId>reachai-spring-boot2-starter</artifactId>
  <version>${reachai.sdk.version}</version>
</dependency>
```

Prefer an existing dependency-management pattern if the business repo already centralizes versions.

## Spring Configuration

Add `reachai.registry`, `reachai.project`, and `reachai.capability` configuration to the active service configuration. Keep the app secret as an environment variable.

The starter scans Spring beans for `@ReachCapability`, registers the project and instance, sends heartbeat data, and syncs capability snapshots on startup when enabled.

## Scan Boundary

ReachAI should sync business APIs, not every controller visible in the Spring application context.

Before writing `reachai.capability` configuration:
- Identify the runnable application's real business package from the `@SpringBootApplication` class, business controller/service packages, and Maven module names.
- If the application class sits in a broad platform root package, do not use that root as the scan package. Choose narrower business packages instead.
- Configure `reachai.capability.scan-packages` with business-owned packages only.
- Configure `reachai.capability.exclude-packages` for framework, platform, starter, generated, and third-party packages.
- If the business package cannot be determined confidently, stop and ask the user to choose from the candidate packages.

Recommended examples:

```yaml
reachai:
  capability:
    scan-beans: true
    scan-packages:
      - com.company.order
      - com.company.customer
    exclude-packages:
      - org.springframework
      - springfox
      - org.springdoc
      - com.enterprise.ai.reach
      - com.company.framework
      - com.company.platform
```

Do not set `scan-packages` to generic roots such as `com`, `com.company`, or a platform framework root unless the user explicitly confirms that all controllers under that root are business APIs.

## Capability Selection

Start with low-risk read-only methods:
- Query by id/code/name.
- List or search methods with simple request DTOs.
- Methods already exposed through a controller and covered by tests.

Avoid first-pass annotations on:
- Payment, deletion, approval, or mutation methods.
- Methods with unclear permissions or tenant boundaries.
- Methods that require interactive captcha, file upload, or long-running transactions.

## Annotation Style

Use stable capability names such as `contract.query` or `team.search`.

Always provide explicit `@ReachParam(name = "...")` for simple parameters, especially in JDK8 projects where compiler parameter names may not be retained.

For request DTOs, annotate important fields with `@ReachParam` so generated parameter metadata is useful for agents.
