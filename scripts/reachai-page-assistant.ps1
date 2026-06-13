param(
  [Parameter(Position = 0)]
  [string] $Command = "help",
  [string] $ManifestUrl = "",
  [string] $Framework = "angular",
  [string] $OutputDir = ".\src\app\shared\reachai",
  [string] $WorkspaceRoot = ".",
  [string] $FrontendUrl = "",
  [string] $Route = "",
  [string] $PageKey = "",
  [switch] $ReportToPlatform,
  [switch] $Help
)

$ErrorActionPreference = "Stop"

function Write-HelpText {
  @"
ReachAI page assistant helper.

Usage:
  .\scripts\reachai-page-assistant.ps1 scaffold -ManifestUrl <url> -Framework angular -OutputDir .\src\app\shared\reachai
  .\scripts\reachai-page-assistant.ps1 verify -ManifestUrl <url> -WorkspaceRoot . -FrontendUrl http://localhost:9200 -Route /team-build/depart-management -PageKey teamArchive.list
  .\scripts\reachai-page-assistant.ps1 verify -ManifestUrl <url> -ReportToPlatform

Notes:
  - Use local PowerShell/curl for localhost ReachAI APIs. Remote WebFetch often cannot reach localhost.
  - The script never reads or prints app secrets.
  - verify reports to ReachAI only when -ReportToPlatform is set.
"@
}

function Read-Manifest([string] $url) {
  if (-not $url) { throw "ManifestUrl is required" }
  if (Test-Path $url) {
    return Get-Content -Raw -Encoding UTF8 -Path $url | ConvertFrom-Json
  }
  Invoke-RestMethod -Method Get -Uri $url
}

function ConvertTo-Hashtable($value) {
  if ($null -eq $value) { return @{} }
  if ($value -is [hashtable]) { return $value }
  $table = @{}
  foreach ($property in $value.PSObject.Properties) {
    $table[$property.Name] = $property.Value
  }
  return $table
}

function Get-TemplateContent([string] $name) {
  switch ($name) {
    "reachai-page-action.types.ts" {
@'
export type ReachAiPageActionStatus = 'SUCCESS' | 'WARN' | 'ERROR';

export interface ReachAiPageActionResult<T = unknown> {
  status: ReachAiPageActionStatus;
  message?: string;
  data?: T;
  error?: { code?: string; message: string };
  metadata?: Record<string, unknown>;
}

export interface ReachAiPageActionContext {
  pageKey: string;
  actionKey: string;
  confirmed?: boolean;
  requestId?: string;
}

export type ReachAiPageActionHandler<TArgs = unknown, TResult = unknown> = (
  args: TArgs,
  context: ReachAiPageActionContext,
) => ReachAiPageActionResult<TResult> | Promise<ReachAiPageActionResult<TResult>>;

export interface ReachAiPageActionRegistration {
  pageKey: string;
  actionKey: string;
  metadata?: Record<string, unknown>;
}

export interface ReachAiPageBridge {
  register<TArgs = unknown, TResult = unknown>(
    pageKey: string,
    actionKey: string,
    handler: ReachAiPageActionHandler<TArgs, TResult>,
    metadata?: Record<string, unknown>,
  ): void;
  unregisterPage(pageKey: string): void;
  execute<TArgs = unknown, TResult = unknown>(
    pageKey: string,
    actionKey: string,
    args?: TArgs,
    options?: { confirmed?: boolean; requestId?: string },
  ): Promise<ReachAiPageActionResult<TResult>>;
  list(pageKey?: string): ReachAiPageActionRegistration[];
}

declare global {
  interface Window {
    __REACHAI_PAGE_BRIDGE__?: ReachAiPageBridge;
  }
}
'@
    }
    "reachai-page-action.service.ts" {
@'
import { Injectable, OnDestroy } from '@angular/core';
import {
  ReachAiPageActionHandler,
  ReachAiPageActionRegistration,
  ReachAiPageActionResult,
  ReachAiPageBridge,
} from './reachai-page-action.types';

@Injectable({ providedIn: 'root' })
export class ReachAiPageActionService implements ReachAiPageBridge, OnDestroy {
  private readonly handlers = new Map<string, ReachAiPageActionHandler>();
  private readonly metadata = new Map<string, Record<string, unknown>>();

  constructor() {
    window.__REACHAI_PAGE_BRIDGE__ = this;
    window.addEventListener('message', this.onMessage);
  }

  register<TArgs = unknown, TResult = unknown>(
    pageKey: string,
    actionKey: string,
    handler: ReachAiPageActionHandler<TArgs, TResult>,
    metadata: Record<string, unknown> = {},
  ): void {
    const key = this.keyOf(pageKey, actionKey);
    this.handlers.set(key, handler as ReachAiPageActionHandler);
    this.metadata.set(key, { ...metadata, pageKey, actionKey });
  }

  unregisterPage(pageKey: string): void {
    for (const key of Array.from(this.handlers.keys())) {
      if (key.startsWith(`${pageKey}::`)) {
        this.handlers.delete(key);
        this.metadata.delete(key);
      }
    }
  }

  async execute<TArgs = unknown, TResult = unknown>(
    pageKey: string,
    actionKey: string,
    args?: TArgs,
    options: { confirmed?: boolean; requestId?: string } = {},
  ): Promise<ReachAiPageActionResult<TResult>> {
    const handler = this.handlers.get(this.keyOf(pageKey, actionKey));
    if (!handler) {
      return {
        status: 'ERROR',
        message: `Page action handler not found: ${pageKey}/${actionKey}`,
        error: { code: 'HANDLER_NOT_FOUND', message: 'Page action handler not found' },
      };
    }
    try {
      return await handler(args, {
        pageKey,
        actionKey,
        confirmed: options.confirmed,
        requestId: options.requestId,
      }) as ReachAiPageActionResult<TResult>;
    } catch (error) {
      return {
        status: 'ERROR',
        message: error instanceof Error ? error.message : 'Page action failed',
        error: {
          code: 'HANDLER_ERROR',
          message: error instanceof Error ? error.message : String(error),
        },
      };
    }
  }

  list(pageKey?: string): ReachAiPageActionRegistration[] {
    return Array.from(this.metadata.values())
      .filter((item) => !pageKey || item.pageKey === pageKey)
      .map((item) => ({
        pageKey: String(item.pageKey),
        actionKey: String(item.actionKey),
        metadata: item,
      }));
  }

  ngOnDestroy(): void {
    window.removeEventListener('message', this.onMessage);
    if (window.__REACHAI_PAGE_BRIDGE__ === this) {
      delete window.__REACHAI_PAGE_BRIDGE__;
    }
  }

  private readonly onMessage = async (event: MessageEvent): Promise<void> => {
    const payload = event.data || {};
    if (payload.type !== 'reachai.pageAction.execute') return;
    const result = await this.execute(payload.pageKey, payload.actionKey, payload.args, {
      confirmed: payload.confirmed,
      requestId: payload.requestId,
    });
    event.source?.postMessage({
      type: 'reachai.pageAction.result',
      requestId: payload.requestId,
      pageKey: payload.pageKey,
      actionKey: payload.actionKey,
      result,
    }, event.origin || '*');
  };

  private keyOf(pageKey: string, actionKey: string): string {
    return `${pageKey}::${actionKey}`;
  }
}
'@
    }
    "page-registry.example.ts" {
@'
import { ReachAiPageActionService } from './reachai-page-action.service';

export const reachAiPageKey = 'replace.with.pageKey';

export function registerReachAiPageActions(
  bridge: ReachAiPageActionService,
  page: {
    getPageState: () => unknown;
    setFilters: (filters: Record<string, unknown>) => void;
    search: () => Promise<unknown> | unknown;
    reset: () => Promise<unknown> | unknown;
    readTable: () => unknown;
    openRowAction?: (args: Record<string, unknown>) => Promise<unknown> | unknown;
  },
): void {
  bridge.register(reachAiPageKey, 'getPageState', async () => ({
    status: 'SUCCESS',
    data: page.getPageState(),
    metadata: { mode: 'readonly', riskLevel: 'LOW' },
  }));

  bridge.register(reachAiPageKey, 'setFilters', async (args) => {
    page.setFilters((args || {}) as Record<string, unknown>);
    return { status: 'SUCCESS', data: page.getPageState(), metadata: { riskLevel: 'LOW' } };
  });

  bridge.register(reachAiPageKey, 'search', async () => {
    const data = await page.search();
    return { status: 'SUCCESS', data, metadata: { riskLevel: 'LOW' } };
  });

  bridge.register(reachAiPageKey, 'reset', async () => {
    const data = await page.reset();
    return { status: 'SUCCESS', data, metadata: { riskLevel: 'LOW' } };
  });

  bridge.register(reachAiPageKey, 'readTable', async () => ({
    status: 'SUCCESS',
    data: page.readTable(),
    metadata: { mode: 'readonly', riskLevel: 'LOW' },
  }));

  if (page.openRowAction) {
    bridge.register(reachAiPageKey, 'openRowAction', async (args, context) => {
      if (!context.confirmed) {
        return {
          status: 'WARN',
          message: 'openRowAction requires user confirmation',
          metadata: { riskLevel: 'HIGH', confirmRequired: true },
        };
      }
      const data = await page.openRowAction?.((args || {}) as Record<string, unknown>);
      return { status: 'SUCCESS', data, metadata: { riskLevel: 'HIGH', confirmRequired: true } };
    }, { riskLevel: 'HIGH', confirmRequired: true });
  }
}
'@
    }
    default { throw "Unknown template: $name" }
  }
}

function Invoke-Scaffold {
  $manifest = Read-Manifest $ManifestUrl
  if ($Framework -ne "angular") { throw "Only angular scaffold is supported in V1" }
  $templates = @("reachai-page-action.types.ts", "reachai-page-action.service.ts", "page-registry.example.ts")
  New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
  $created = @()
  $skipped = @()
  foreach ($template in $templates) {
    $target = Join-Path $OutputDir $template
    if (Test-Path $target) {
      $skipped += $target
      continue
    }
    Set-Content -Path $target -Value (Get-TemplateContent $template) -Encoding UTF8
    $created += $target
  }
  [pscustomobject]@{
    ok = $true
    command = "scaffold"
    schema = $manifest.schema
    framework = "angular"
    outputDir = (Resolve-Path $OutputDir).Path
    created = $created
    skipped = $skipped
  } | ConvertTo-Json -Depth 20
}

function Get-RelativePath([string] $root, [string] $path) {
  $rootPath = (Resolve-Path $root).Path.TrimEnd("\", "/")
  $fullPath = (Resolve-Path $path).Path
  if ($fullPath.StartsWith($rootPath)) {
    return $fullPath.Substring($rootPath.Length).TrimStart("\", "/")
  }
  return $fullPath
}

function Get-FileHashSafe([string] $path) {
  try { return (Get-FileHash -Algorithm SHA256 -Path $path).Hash.ToLowerInvariant() } catch { return "" }
}

function Find-ReachAiFiles([string] $root, [string] $bridgeGlobal, [string] $pageKey) {
  $files = Get-ChildItem -Path $root -Recurse -File -Include *.ts,*.tsx,*.js,*.jsx,*.vue -ErrorAction SilentlyContinue |
    Where-Object {
      $_.FullName -notmatch "\\node_modules\\" -and
      $_.FullName -notmatch "\\dist\\" -and
      $_.FullName -notmatch "\\target\\"
    }
  $matches = @()
  foreach ($file in $files) {
    $content = Get-Content -Raw -Encoding UTF8 -Path $file.FullName -ErrorAction SilentlyContinue
    if ($content -and ($content.Contains($bridgeGlobal) -or ($pageKey -and $content.Contains($pageKey)))) {
      $role = if ($content.Contains($bridgeGlobal)) { "bridge-or-handler" } else { "page-registry" }
      $matches += [pscustomobject]@{
        path = Get-RelativePath $root $file.FullName
        role = $role
        exists = $true
        sha256 = Get-FileHashSafe $file.FullName
      }
    }
  }
  return $matches
}

function New-ActionDefinition([string] $actionKey) {
  $readonly = $actionKey -eq "getPageState" -or $actionKey -eq "readTable"
  $highRisk = $actionKey -eq "openRowAction"
  [pscustomobject]@{
    actionKey = $actionKey
    title = $actionKey
    description = if ($readonly) { "Read current page state from the visible page." } else { "Execute page action through the current page instance." }
    confirmRequired = $highRisk
    inputSchema = @{ type = "object"; properties = @{} }
    outputSchema = @{ type = "object" }
    sampleArgs = @{}
    allowedAgentIds = @()
    metadata = @{
      riskLevel = if ($highRisk) { "HIGH" } else { "LOW" }
      mode = if ($readonly) { "readonly" } else { "page-action" }
    }
  }
}

function Invoke-Verify {
  $manifest = Read-Manifest $ManifestUrl
  $contract = ConvertTo-Hashtable $manifest.pageActionContract
  $target = ConvertTo-Hashtable $manifest.target
  $endpoints = ConvertTo-Hashtable $manifest.endpoints
  $bridgeGlobal = if ($contract.bridgeGlobal) { [string]$contract.bridgeGlobal } else { "__REACHAI_PAGE_BRIDGE__" }
  $resolvedPageKey = if ($PageKey) { $PageKey } elseif ($target.pageKey) { [string]$target.pageKey } else { "" }
  $resolvedRoute = if ($Route) { $Route } elseif ($target.routePattern) { [string]$target.routePattern } else { "" }
  $actions = @()
  if ($target.actionKeys) { $actions = @($target.actionKeys) }
  if ($actions.Count -eq 0 -and $contract.recommendedActions) { $actions = @($contract.recommendedActions) }
  if ($actions.Count -eq 0) { $actions = @("getPageState", "setFilters", "search", "reset", "readTable") }

  $root = (Resolve-Path $WorkspaceRoot).Path
  $files = @(Find-ReachAiFiles $root $bridgeGlobal $resolvedPageKey)
  $hasBridge = $files.Count -gt 0
  $staticStatus = if ($hasBridge -and $resolvedPageKey) { "PASS" } else { "WARN" }
  $browserStatus = "SKIPPED"
  $browserMessage = "FrontendUrl was not provided."
  if ($FrontendUrl) {
    $targetUrl = $FrontendUrl.TrimEnd("/") + $resolvedRoute
    try {
      $response = Invoke-WebRequest -Method Get -Uri $targetUrl -TimeoutSec 8 -UseBasicParsing
      $browserStatus = if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) { "WARN" } else { "FAIL" }
      $browserMessage = "HTTP route probe returned $($response.StatusCode). Runtime bridge probe requires an authenticated browser session."
    } catch {
      $browserStatus = "WARN"
      $browserMessage = "Frontend route probe failed or requires login: $($_.Exception.Message)"
    }
  }

  $verification = [ordered]@{
    build = @{ status = "WARN"; message = "Build command was not executed by this helper. Run the business frontend minimal build separately." }
    staticHandler = @{
      status = $staticStatus
      bridgeGlobal = $bridgeGlobal
      handlerCount = $files.Count
      message = if ($hasBridge) { "Bridge or handler files were found." } else { "No bridge or handler files were found." }
    }
    browser = @{ status = $browserStatus; message = $browserMessage }
  }
  $result = [ordered]@{
    ok = $true
    command = "verify"
    schema = $manifest.schema
    pageKey = $resolvedPageKey
    routePattern = $resolvedRoute
    bridgeGlobal = $bridgeGlobal
    files = $files
    actions = $actions
    verification = $verification
  }

  if ($ReportToPlatform) {
    if (-not $endpoints.registerPageUrl) { throw "registerPageUrl is missing from manifest" }
    $body = @{
      sessionId = $manifest.session.sessionId
      toolName = "PowerShell verify"
      pageKey = $resolvedPageKey
      pageName = $resolvedPageKey
      routePattern = $resolvedRoute
      framework = "angular"
      frameworkVersion = ""
      bridgeGlobal = $bridgeGlobal
      replaceActions = $true
      files = $files
      actions = @($actions | ForEach-Object { New-ActionDefinition ([string]$_) })
      verification = $verification
      handoffSummary = "reachai-page-assistant verify reported static evidence"
    }
    $report = Invoke-RestMethod -Method Post -Uri $endpoints.registerPageUrl -ContentType "application/json; charset=utf-8" -Body ($body | ConvertTo-Json -Depth 50 -Compress)
    $result.reportedToPlatform = $true
    $result.platformResponse = $report
  }
  $result | ConvertTo-Json -Depth 50
}

if ($Help -or $Command -eq "help" -or $Command -eq "-Help") {
  Write-HelpText
  exit 0
}

switch ($Command) {
  "scaffold" { Invoke-Scaffold }
  "verify" { Invoke-Verify }
  default {
    Write-HelpText
    throw "Unsupported command: $Command"
  }
}
