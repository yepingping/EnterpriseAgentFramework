# ReachAI Page Assistant Onboarding

Use this skill when a business frontend repository needs to connect one concrete page to ReachAI Page Assistant.

## Boundary

- This is page assistant onboarding, not project SDK onboarding.
- Do not add backend API Tools for simple page queries when the current page can expose Page Actions.
- Do not implement gateway, SDK, or embed token broker work unless the user explicitly switches to project AI quick access.
- Never read, print, or commit app secrets. Only use the configured environment variable name.

## Workflow

1. Read the ReachAI page assistant manifest.
2. Ask the user to choose the target page when multiple routes or page components match.
3. Scaffold the Angular bridge if the business frontend does not already have one.
4. Register query-first actions: `getPageState`, `setFilters`, `search`, `reset`, `readTable`.
5. Mark high-risk row actions with `confirmRequired=true` and metadata `riskLevel=HIGH`.
6. Run the business frontend build or type check.
7. Register the page through `endpoints.registerPageUrl`.
8. Report files, action keys, build/static/browser verification, and remaining blockers.

## References

- `references/page-action-contract.md`
- `references/angular-page-action.md`

## Templates

- `templates/angular/reachai-page-action.types.ts`
- `templates/angular/reachai-page-action.service.ts`
- `templates/angular/page-registry.example.ts`

## Helper Script

- `scripts/reachai-page-assistant.ps1`

