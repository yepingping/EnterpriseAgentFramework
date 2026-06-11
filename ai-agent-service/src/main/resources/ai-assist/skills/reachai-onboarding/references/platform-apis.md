# ReachAI Platform APIs

The onboarding manifest is the primary contract for AI coding tools.

## Manifest

`GET /api/ai-assist/projects/{projectId}/onboarding-manifest?aiCodingKey={key}`

AI coding tools normally use the `aiCodingKey` URL from the platform prompt. Platform console users may call the same endpoint with their normal login token.

Important fields:
- `project.id`: ReachAI project id.
- `project.projectCode`: stable business project code.
- `project.baseUrl`: business service base URL as known by ReachAI.
- `project.registryAppKey`: app key for signed registration.
- `project.registryCredentialConfigured`: whether ReachAI has a saved credential.
- `aiCodingAccess.enabled`: whether external AI coding access is enabled.
- `aiCodingAccess.accessKey`: the project-level key used only to fetch this manifest; it is not the registry app secret.
- `sdk.dependencies`: Maven coordinates to add.
- `sdk.config.appSecretEnv`: environment variable name for the app secret.
- `endpoints.skillPackageUrl`: zip URL for this skill.
- `endpoints.sdkAccessCheckUrl`: platform self-check endpoint.

The manifest does not include `appSecret`.

## Self-Check

`POST /api/scan-projects/{projectId}/sdk-access-check`

Example body:

```json
{
  "apiAssetId": 123,
  "args": {},
  "gatewayBaseUrl": "http://localhost:8080",
  "embedTokenPath": "/api/reachai/embed-token"
}
```

Run this only after the business service compiles and has a reachable local or test instance.

## Tool Reconcile

`POST /api/scan-projects/{projectId}/tools/reconcile`

Use after SDK startup/sync when the platform has received capability snapshots and the user wants API catalog rows reconciled.

## Future Extension Points

Do not assume these exist unless the manifest exposes them:
- MCP tool endpoints.
- One-time secret token endpoints.
- Agent or page-assistant generation endpoints.
- Deployment or production rollout endpoints.
