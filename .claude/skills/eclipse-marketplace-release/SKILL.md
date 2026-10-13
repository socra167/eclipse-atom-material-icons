---
name: eclipse-marketplace-release
description: Release an Eclipse plug-in and publish it on the Eclipse Marketplace — hosting a p2 update site on GitHub Pages from a Tycho build, versioning releases from git tags, optional PGP signing, a GitHub Actions release workflow, and preparing the Marketplace listing. Use this whenever the user wants to publish, distribute, release or "register" an Eclipse plug-in/feature/update site, asks how to get a plug-in onto Eclipse Marketplace (이클립스 마켓플레이스 등록, 배포, 릴리스, 업데이트 사이트), wants a p2 repository online, wants installs to receive updates via "Check for Updates", or wants to sign Tycho artifacts — even if they only ask "how do I share my Eclipse extension".
---

# Eclipse Marketplace release

Eclipse Marketplace is only a catalog: it stores a listing that points to a **public p2 update site**.
Nothing is uploaded there. So publishing always has two halves:

1. Put the Tycho-built p2 repository at a stable public HTTPS URL, and keep that URL forever so
   installed copies find new versions through *Help › Check for Updates*.
2. Create the listing on marketplace.eclipse.org that points to that URL and feature ID.

Claude can automate the first half completely. The second half (account, listing form, enabling
GitHub Pages, adding secrets) needs the user's accounts, so prepare everything and hand over
exact, copy-pasteable steps.

## 0. Inspect the project first

Find, rather than assume:

- Tycho version (`<tycho.version>` in the parent `pom.xml`) and the build command that works today
  (`mvn clean verify`, with `xvfb-run -a` on Linux if UI tests exist).
- The `eclipse-repository` module (has `category.xml`) — its `target/repository` is the update site.
- The feature ID (`feature.xml` `id=`) — the Marketplace needs it, without `.feature.group`.
- Current versions (`1.0.0-SNAPSHOT` in poms, `1.0.0.qualifier` in MANIFEST/feature.xml).
- GitHub owner/repo (`git remote -v`) → Pages URL `https://<owner>.github.io/<repo>/`
  (or `https://<owner>.github.io/` for a `<owner>.github.io` repo).
- Existing workflows in `.github/workflows/`, and the repo's commit conventions.

If the project is not built with Tycho, the same ideas apply but the templates need adapting;
say so rather than forcing them.

## 1. Version releases from git tags

Keep `-SNAPSHOT`/`.qualifier` in the source tree and let the release job stamp the exact version
from the tag. This avoids "bump version" commits and guarantees the published version matches the
tag:

```sh
mvn -B -ntp org.eclipse.tycho:tycho-versions-plugin:<tycho.version>:set-version -DnewVersion=1.2.3
```

It rewrites every pom, MANIFEST.MF and feature.xml (`1.2.3.qualifier` → `1.2.3`). Verify on the
real project once (`git diff --stat`, then `git checkout .`).

Every release must be higher than the previous one, or p2 will not offer it as an update.

## 2. Optional PGP signing

Unsigned content makes Eclipse warn during install. Signing is optional but recommended. Add the
profile from `assets/sign-profile.xml` to the **`eclipse-repository` module's pom** (not the parent —
the goal signs an assembled repository and fails elsewhere with "No repository found at …/target/repository").

It uses the Bouncy Castle signer, so CI needs no gpg install or agent:

```sh
MAVEN_GPG_PASSPHRASE=... mvn clean verify -Psign -Dtycho.pgp.signer.bc.secretKeys=/path/key.asc
```

Read `references/pitfalls.md` before touching signing — the `bc` signer silently falls back to the
gpg executable unless the key is given as a *file*, which cost real debugging time.

Verify locally with a throwaway key (recipe in `references/verification.md`): build with `-Psign`,
check `pgp.signatures` exists in `artifacts.xml`, and `gpg --verify` one artifact.

## 3. Release workflow

Copy `assets/release.yml` to `.github/workflows/release.yml` and fill in the placeholders
(`@SITE_MODULE@` = path of the eclipse-repository module, `@SITE_ARTIFACT@` = its artifactId,
`@ZIP_NAME@` = a short download name, `@DISPLAY_NAME@` = the product name; leave the literal
`@VERSION@` — the workflow substitutes it into the landing page). On a `v*` tag it:

1. derives the version from the tag (or a manual `workflow_dispatch` input) and validates `X.Y.Z`;
2. runs `set-version`;
3. writes the signing key to `$RUNNER_TEMP` only if the `GPG_PRIVATE_KEY` secret exists
   (otherwise warns and builds unsigned), and deletes it afterwards;
4. builds and tests;
5. assembles `_site/` = repository + landing page, uploads it as the Pages artifact;
6. creates a GitHub release with the zipped repository (tag runs only);
7. a second job deploys to Pages.

Add the landing page `assets/index.html` to `<site-module>/pages/index.html` (fill in name,
description, requirements, repo URL). Without it, opening the update site URL in a browser shows a
404, which looks broken to people who click the Marketplace link.

**Test the workflow before handing it over.** Run `scripts/simulate_release.py` from the repo root
(it extracts the `run:` steps of the build job and executes them with a fake tag in a scratch copy of
the repo). Check the output folder contains `content.jar`/`artifacts.jar`, `plugins/` and
`features/` with exact versions, `index.html` with the version substituted, and the zip. The real
Pages deploy only runs on GitHub — say so.

## 4. Documentation and listing material

Write, from the templates in `assets/`:

- `docs/RELEASING.md` — one-time setup (enable Pages, signing secrets) and the tag-to-release flow.
- `docs/MARKETPLACE.md` — every listing form value; see `references/marketplace-listing.md` for the
  fields and what to put in them.
- README installation section: the update site URL with *Help › Install New Software…* steps, and
  the offline zip from GitHub releases.
- A logo (the Marketplace shows one). Make it from the project's own assets; don't reuse another
  project's logo even when its icons are licensed for reuse — a logo identifies a project.

## 5. Hand over the manual steps

End with a short, ordered checklist of what only the user can do:

1. GitHub *Settings › Pages › Source* → **GitHub Actions** (the deploy job fails until this is set;
   the default `GITHUB_TOKEN` cannot enable Pages).
2. Optional: create a signing key and add `GPG_PRIVATE_KEY` / `GPG_PASSPHRASE` secrets.
3. `git tag vX.Y.Z && git push origin vX.Y.Z`, watch the Release run, open the Pages URL, install
   from it in a real Eclipse.
4. Log in at marketplace.eclipse.org (Eclipse account from accounts.eclipse.org) → *Add Content*,
   paste the values from `docs/MARKETPLACE.md`, submit. Listings are reviewed before they appear.
5. Later releases: just push a higher tag; the listing does not need editing unless the
   supported Eclipse versions or description change.

Be explicit about what was verified locally and what has not run yet (Pages deploy, release
creation, the Marketplace review).
