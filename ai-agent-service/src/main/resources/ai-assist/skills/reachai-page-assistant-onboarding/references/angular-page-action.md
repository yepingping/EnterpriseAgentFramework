# Angular Page Action Onboarding

## Files

Copy the Angular templates into the business frontend, usually under:

```text
src/app/shared/reachai/
```

Then import `ReachAiPageActionService` in the target page component and register handlers during component initialization. Call `unregisterPage(pageKey)` during destroy.

## Page Component Pattern

```ts
constructor(private readonly reachAiPageActions: ReachAiPageActionService) {}

ngOnInit(): void {
  registerReachAiPageActions(this.reachAiPageActions, {
    getPageState: () => this.getReachAiPageState(),
    setFilters: (filters) => this.setReachAiFilters(filters),
    search: () => this.search(),
    reset: () => this.reset(),
    readTable: () => this.readReachAiTable(),
  });
}

ngOnDestroy(): void {
  this.reachAiPageActions.unregisterPage(reachAiPageKey);
}
```

Prefer existing component state and services. Do not duplicate query logic.

## Verification

Run the business frontend's minimum build or type check. If a logged-in browser is available, verify:

```js
await window.__REACHAI_PAGE_BRIDGE__.execute('teamArchive.list', 'getPageState')
await window.__REACHAI_PAGE_BRIDGE__.execute('teamArchive.list', 'setFilters', { teamName: 'test' })
await window.__REACHAI_PAGE_BRIDGE__.execute('teamArchive.list', 'search')
await window.__REACHAI_PAGE_BRIDGE__.execute('teamArchive.list', 'readTable')
```

