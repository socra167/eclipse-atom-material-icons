# Releasing

Releases are built by the [Release workflow](../.github/workflows/release.yml). It publishes the
update site to GitHub Pages at

```
@SITE_URL@
```

and attaches the zipped update site to a GitHub release. The address stays the same for every
release, so installed copies find new versions with *Help › Check for Updates*.

## One-time setup

1. **Enable GitHub Pages**: *Settings › Pages › Build and deployment › Source* → **GitHub Actions**.
2. **Optional: PGP signing.** Without it Eclipse warns about unsigned content during installation.
   1. Create a key that is only used for signing releases:
      ```sh
      gpg --quick-gen-key "@DISPLAY_NAME@ <you@example.com>" ed25519 sign 3y
      gpg --armor --export-secret-keys <KEY-ID> > signing-key.asc
      ```
   2. In *Settings › Secrets and variables › Actions*, add the repository secrets
      - `GPG_PRIVATE_KEY`: the content of `signing-key.asc`
      - `GPG_PASSPHRASE`: the passphrase of the key
   3. Delete `signing-key.asc` and keep a backup of the key in a safe place.
   4. Optionally publish the public key (`gpg --send-keys <KEY-ID>` to keys.openpgp.org) so
      users can check who signed the plug-in.

## Making a release

1. Make sure the build is green on the default branch.
2. Tag the commit with the new version and push the tag:
   ```sh
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. The Release workflow sets all versions to the tag (`1.0.0`, without qualifier), runs the tests,
   deploys the update site and creates the GitHub release.

Each release must have a higher version than the previous one, otherwise Eclipse does not offer it
as an update. Use semantic versioning: `1.0.1` for fixes, `1.1.0` for new features.

To republish the update site without a new tag, run the Release workflow by hand
(*Actions › Release › Run workflow*) and enter the version.

The versions in the source tree (`-SNAPSHOT` / `.qualifier`) are only used for local and CI
builds; they do not need to be changed for a release.
