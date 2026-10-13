# Pitfalls found while doing this for real

## Maven 4 on CI runners

- GitHub-hosted Ubuntu images moved their default `mvn` to Maven 4. Tycho 4.0.x then fails at
  startup with `No implementation for MavenTargetLocationFactory was bound` (plus warnings
  `Error injecting: org.eclipse.tycho.p2resolver.DefaultTargetPlatformFactory`). The log line
  `Loaded … auto-discovered prefixes for remote repository central` identifies Maven 4.
  It is not a Java version problem: the same build passes on Java 17 and 21 with Maven 3.9.
- Fix: commit a Maven Wrapper pinned to 3.9.x and use `./mvnw` everywhere. `setup-java` does not
  choose the Maven version.
- To reproduce locally, download the Maven 4 binary distribution from Maven Central and run the
  build with it.

## Signing (tycho-gpg-plugin `sign-p2-artifacts`)

- **Put the profile in the `eclipse-repository` module**, not the parent pom. In the parent it runs
  for `pom` packaging and fails with `No repository found at file:<root>/target/repository`.
- **`<signer>bc</signer>` alone still launches `gpg`.** The Bouncy Castle signer is only used when
  it gets the secret key as a *file* via `-Dtycho.pgp.signer.bc.secretKeys=<path>` (or the
  `secretKeys` parameter). With only `MAVEN_GPG_KEY` set it falls back to the gpg executable and
  fails with `gpg: no default secret key` / `Exit code: 2`. The passphrase is read from
  `MAVEN_GPG_PASSPHRASE`.
- `addPublicKeyToRepo` puts the public key into `artifacts.xml` (`pgp.publicKeys`), so users can
  see who signed without a key server.
- In CI, write the key to `$RUNNER_TEMP`, never to the workspace, and delete it in an
  `if: always()` step. Keep the key passphrase in its own secret.
- Local tests with a throwaway key: `gpg-agent` cannot start when `GNUPGHOME` is a long path (Unix
  socket path limit ~108 chars). Use a short directory such as `/tmp/gpgt`, and delete it afterwards.

## Versions

- `tycho-versions-plugin:set-version -DnewVersion=1.2.3` turns `1.2.3-SNAPSHOT`/`1.2.3.qualifier`
  into exact `1.2.3`. Run it with the fully qualified plugin coordinate and the project's Tycho
  version, read from the pom so it never drifts.
- p2 compares versions: a release that is not higher than the installed one is never offered.
  Qualifier builds (`1.0.0.2026...`) sort *above* `1.0.0`, so don't publish SNAPSHOT builds to the
  same site as releases.

## GitHub Pages

- The deploy job needs *Settings › Pages › Source = GitHub Actions*. `GITHUB_TOKEN` cannot turn
  Pages on, so this is a manual step for the user.
- The `github-pages` environment that Pages creates only allows deployments from the default
  branch. A release workflow triggered by a tag gets a `deploy` job that fails within a second,
  with no step logs (the logs API returns 404). Fix: *Settings › Environments › github-pages ›
  Deployment branches and tags* → add a tag rule `v*`, then re-run the failed job. The build job and
  the GitHub release are unaffected.
- Workflow permissions needed: `pages: write`, `id-token: write`, and `contents: write` for the
  release.
- The site root holds the p2 files directly (`content.jar`, `artifacts.jar`, `p2.index`,
  `plugins/`, `features/`). Keep that layout and the URL stable across releases.
- Only the latest version is published; p2 updates still work because the newer version replaces
  the old one. If older versions must stay installable, build a composite repository instead.

## Marketplace

- The Marketplace never hosts binaries; the listing's update site URL and feature ID are what
  the Marketplace client installs from. Enter the feature ID *without* `.feature.group`.
- A GitHub release zip is not an update site URL (p2 cannot browse it remotely); it is only for
  offline installs via *Add… › Archive…*.
- Listings are moderated; expect a delay before they show up.

## Local verification of the workflow

- The release job cannot be run locally as-is, but its `run:` steps are plain bash. Executing them
  in a scratch copy of the repo (see `scripts/simulate_release.py`) caught path and naming mistakes
  before the first real tag.
