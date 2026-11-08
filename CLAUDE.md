# Phovo: notes for Claude

Kotlin Multiplatform photo/video backup app (Android, iOS, desktop server, web). The README's
"Branching and Releases" and "Downloads" sections are the full reference for everything below; keep
them in sync when these conventions change.

## Branches

| Branch | Role |
|---|---|
| `develop` | Default branch. All `feature/*`, `fix/*`, `docs/*`, `chore/*` PRs target it. Alphas (and betas before 1.0) are released from it. |
| `main` | Stable only. Changes only by PR from `develop`, `release/*` or `hotfix/*` (enforced by the `main-source-guard` check). |
| `release/x.y` | A beta line, cut from `develop` once x.y stops taking features. Only exists after 1.0. Deleted after it ships. |
| `hotfix/x.y.z` | Patch for the current stable version, cut from `main`. |

- Never push to `main`, `develop` or `release/*` directly. Rulesets require a PR and block force pushes.
  Commit on a prefixed branch and open a PR into `develop` unless the work is a hotfix or a beta fix.
- Fixes go on the **oldest** line that has the bug and are **merged forward**: `hotfix/*` → `main` →
  `release/x.y` → `develop`. Don't fix on `develop` and cherry-pick backwards, and never merge `develop`
  into `release/*` or `main` outside a planned promotion: it drags unfinished work into the beta or
  stable line.
- `v*` tags are immutable (ruleset). Never move or delete one.

## Labels

- Branch prefixes are labeled automatically: `feature/` → `enhancement`, `fix/`, `hotfix/` → `bug`,
  `docs/` → `documentation`, `chore/` → `chore`. PRs from `develop`, `main` or `release/*` get
  `skip-changelog`. The labels pick the release notes section (`.github/release.yml`).
- **`release` publishes a release when the PR merges.** Only put it on a PR whose purpose is to bump
  `version.properties` for a release, and only when the user asks.

## Versions

- The only version source is `versionName` in `version.properties`, in SemVer:
  `1.0.0-alpha.1`, `1.0.0-beta.2`, `1.0.0-rc.1`, `1.0.0`. Keep the dot before the pre-release number.
- Never hardcode a version anywhere else. Android's `versionCode` and the desktop package version are
  calculated by `build-logic/.../PhovoVersion.kt`
  (`major·1,000,000 + minor·10,000 + patch·100 + stage`, with alpha.N = 10+N, beta.N = 50+N,
  rc.N = 80+N, stable = 99), and app code reads the generated `PhovoBuildInfo` in `core:common`.
- `./gradlew -q printVersion` shows the computed values. `./gradlew -q checkReleaseVersion
  -Pphovo.releaseBranch=<branch>` says whether the current version can be released from that branch.

## Flavors and releases

- `gradle/flavor.properties` stays `FLAVOR=dev` (Dev Menu on). Release builds pass `-Pphovo.flavor=prod`.
  Don't change the file for a release.
- `.github/workflows/release.yml` builds every file and creates a **draft** release: on merge of a PR
  labeled `release`, or by hand (`gh workflow run release.yml --ref <branch> -f mode=test-only|draft`).
  `test-only` creates nothing and is the safe way to check packaging changes. The maintainer publishes
  drafts by hand.
- Desktop files come from `:applicationContainers:desktopApp:packageReleaseDistributions` on each OS's
  own runner. jpackage can't cross-build, so Linux packaging can only be verified in a workflow run.
- Every draft's notes start with `.github/release-notes-header.md` (install steps, including the
  workarounds for unsigned macOS and Windows builds until #214 lands).

## Signing and secrets

- The Android release key is read from `phovo.signing.*` Gradle properties or `PHOVO_SIGNING_*`
  environment variables. CI maps the `ANDROID_*` secrets onto them and checks the APK certificate
  against the `ANDROID_SIGNING_CERT_SHA256` variable. Never commit keystores (`*.jks`, `*.keystore` are
  ignored) or write signing passwords into files.
- Don't ask the user to run commands that print secrets through the chat's shell input: the output
  lands in the session transcript. Have them use their own terminal.

## Checks

- `./gradlew -p build-logic test` runs the build-logic unit tests (version scheme, release checks, DEB
  dependency rewriting). The root `./gradlew build` doesn't run them; CI runs both.
- Kotlin style: don't use `!!`. Use smart casts, `?:` with a clear fallback, or
  `requireNotNull(x) { "why" }`.
