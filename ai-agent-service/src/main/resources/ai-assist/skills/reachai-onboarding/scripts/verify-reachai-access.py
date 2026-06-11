#!/usr/bin/env python3
import argparse
import json
import sys
import urllib.error
import urllib.request


def fetch_json(url):
    with urllib.request.urlopen(url, timeout=10) as response:
        charset = response.headers.get_content_charset() or "utf-8"
        return json.loads(response.read().decode(charset))


def post_json(url, payload):
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        charset = response.headers.get_content_charset() or "utf-8"
        return json.loads(response.read().decode(charset))


def main():
    parser = argparse.ArgumentParser(description="Verify ReachAI onboarding manifest and optional SDK access check.")
    parser.add_argument("--manifest-url", required=True)
    parser.add_argument("--run-check", action="store_true")
    parser.add_argument("--api-asset-id", type=int)
    parser.add_argument("--args-json", default="{}")
    parser.add_argument("--gateway-base-url")
    parser.add_argument("--embed-token-path")
    args = parser.parse_args()

    try:
        manifest = fetch_json(args.manifest_url)
        print("manifest.schema=" + str(manifest.get("schema")))
        print("project.code=" + str(manifest.get("project", {}).get("projectCode")))
        print("credential.configured=" + str(manifest.get("project", {}).get("registryCredentialConfigured")))
        print("secret.env=" + str(manifest.get("security", {}).get("appSecretEnv")))

        if args.run_check:
            payload = {
                "apiAssetId": args.api_asset_id,
                "args": json.loads(args.args_json),
                "gatewayBaseUrl": args.gateway_base_url,
                "embedTokenPath": args.embed_token_path,
            }
            result = post_json(manifest["endpoints"]["sdkAccessCheckUrl"], payload)
            print("sdkAccessCheck.overallStatus=" + str(result.get("overallStatus")))
            print(json.dumps(result, ensure_ascii=False, indent=2))
    except (urllib.error.URLError, KeyError, json.JSONDecodeError) as exc:
        print("ReachAI verification failed: " + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
