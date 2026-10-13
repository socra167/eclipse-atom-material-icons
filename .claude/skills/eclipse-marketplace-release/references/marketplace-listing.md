# Marketplace listing fields

Create the listing at <https://marketplace.eclipse.org> → *Add Content* (needs an Eclipse account
from <https://accounts.eclipse.org>). The form changes over time; these are the values to prepare.

| Field | What to enter |
| --- | --- |
| Title | Product name as users would search for it |
| Short description | One sentence: what it does and where in Eclipse |
| Long description | Feature bullets, requirements, credits/licenses of third-party assets |
| Update site URL | The stable Pages URL, ending in `/` |
| Feature ID | `feature.xml` id, without `.feature.group` |
| Version | Latest released version |
| License | The project's license (choose the open source option for OSI licenses) |
| Market / Categories | Usually *Tools*, plus the closest categories (UI, Editor, Languages, …) |
| Supported Eclipse releases | From the oldest release the build targets (target platform) onward |
| Platforms | Windows, macOS, Linux unless there is native code |
| Minimum Java version | `Bundle-RequiredExecutionEnvironment` of the bundles |
| Website / Support URL | Repository page and its issue tracker |
| Logo | Square PNG from the project's own assets |
| Screenshots | Real screenshots of the feature in Eclipse |
| Tags | Search terms users would type |

After approval the listing page offers an "Install" / drag-to-install snippet; add it to the README.

Write these values into `docs/MARKETPLACE.md` (template in `assets/MARKETPLACE.md`) so the user
can paste them, and so the listing can be kept in sync with later releases.
