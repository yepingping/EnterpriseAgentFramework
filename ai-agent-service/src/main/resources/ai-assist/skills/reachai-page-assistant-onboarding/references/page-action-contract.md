# Page Action Contract

## Bridge

The business page exposes a browser global:

```ts
window.__REACHAI_PAGE_BRIDGE__
```

Minimum API:

```ts
register(pageKey, actionKey, handler, metadata?)
unregisterPage(pageKey)
execute(pageKey, actionKey, args?, options?)
list(pageKey?)
```

Handlers must operate the current page instance. They must not bypass page permissions, login state, button guards, or data scope.

## Result Shape

```json
{
  "status": "SUCCESS",
  "message": "optional",
  "data": {},
  "error": null,
  "metadata": {}
}
```

Allowed status values are `SUCCESS`, `WARN`, and `ERROR`.

## Recommended Actions

- `getPageState`: read filters, pagination, visible table rows, selected rows, and loading state.
- `setFilters`: set existing filter controls without triggering unrelated business logic.
- `search`: reuse the page's existing search flow.
- `reset`: reuse the page's existing reset flow.
- `readTable`: read visible table columns, rows, pagination, and row keys.
- `openRowAction`: optional and high-risk; require user confirmation.

## Safety

- Default to read-only and query actions.
- Creation, editing, deletion, approval, export, batch processing, status changes, and cross-page navigation are high-risk.
- High-risk actions must set `confirmRequired=true` and metadata `riskLevel=HIGH`.

