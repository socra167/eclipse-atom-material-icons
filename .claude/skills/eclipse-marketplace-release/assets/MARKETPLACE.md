# Eclipse Marketplace listing

Values for the listing form at <https://marketplace.eclipse.org> (*Add Content*). Publish a release
first (see [RELEASING.md](RELEASING.md)), since the listing points to the update site.

| Field | Value |
| --- | --- |
| Title | @DISPLAY_NAME@ |
| Short description | @SHORT_DESCRIPTION@ |
| Update site URL | `@SITE_URL@` |
| Feature ID | `@FEATURE_ID@` |
| Version | the latest released version, for example `1.0.0` |
| License | @LICENSE@ |
| Market / Categories | @CATEGORIES@ |
| Supported Eclipse releases | @ECLIPSE_RELEASES@ |
| Platforms | Windows, macOS, Linux |
| Minimum Java version | @JAVA_VERSION@ |
| Website | `@REPO_URL@` |
| Support URL | `@REPO_URL@/issues` |
| Logo | `docs/images/logo.png` |
| Screenshots | @SCREENSHOTS@ |
| Tags | @TAGS@ |

## Long description

> @LONG_DESCRIPTION@

## After the listing is approved

The listing page shows an *Install* button snippet ("Drag to Install"). Add it to the README
under *Installation*.
