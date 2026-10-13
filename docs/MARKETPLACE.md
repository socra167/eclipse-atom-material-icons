# Eclipse Marketplace listing

Values for the listing form at <https://marketplace.eclipse.org> (*Add Content*). Publish a release
first (see [RELEASING.md](RELEASING.md)), since the listing points to the update site.

| Field | Value |
| --- | --- |
| Title | Atom Material Icons |
| Short description | Material Design file and folder icons for the Project Explorer, ported from the Atom Material Icons plug-in for IntelliJ IDEA. |
| Update site URL | `https://socra167.github.io/eclipse-atom-material-icons/` |
| Feature ID | `io.github.socra167.atommaterialicons.feature` |
| Version | the latest released version, for example `1.0.1` |
| Status | **Beta** until there is feedback from real installs, then **Production/Stable** |
| License | MIT (Open Source) |
| Market / Categories | Tools; UI, Editor |
| Supported Eclipse releases | 2024-03 (4.31) and newer |
| Platforms | Windows, macOS, Linux |
| Minimum Java version | 17 |
| Website | `https://github.com/socra167/eclipse-atom-material-icons` |
| Support URL | `https://github.com/socra167/eclipse-atom-material-icons/issues` |
| Logo | `docs/images/logo.png` |
| Screenshots | `docs/images/project-explorer.png`, `docs/images/java-types.png`, `docs/images/preferences.png` |
| Tags | icons, material, theme, project explorer, file icons, intellij |

## Solution description

Paste into the description field (it accepts basic HTML):

```html
<p><strong>Atom Material Icons</strong> brings the Material Design file and folder icons of the
popular IntelliJ IDEA plug-in to the Eclipse <strong>Project Explorer</strong>.</p>

<ul>
  <li><strong>1,700+ file icons and 350+ folder icons</strong>, chosen by file name, extension and
      folder name with the same rules as Atom Material Icons (<code>pom.xml</code>,
      <code>package.json</code>, <code>Dockerfile</code>, <code>src</code>, <code>test</code>, ...).</li>
  <li><strong>Java type icons</strong>: <code>.java</code> files show the type they declare &mdash;
      class, abstract class, interface, enum, record, annotation, exception or test class.</li>
  <li><strong>Clear error and warning overlays</strong>, each of which can be turned off separately.
      Git and other decorations keep working.</li>
  <li><strong>Crisp on HiDPI</strong> screens.</li>
  <li>Preferences under <em>Window &rsaquo; Preferences &rsaquo; General &rsaquo; Appearance &rsaquo;
      Atom Material Icons</em>, including optional icon packs (Angular, NestJS, Redux, Rails, ...).</li>
</ul>

<p>Requires Eclipse 2024-03 or newer and Java 17. Java type icons require the Eclipse Java
development tools (JDT).</p>

<p>Tip: JDT's <em>Java Type Indicator</em> label decoration adds small badges that duplicate the
type icons; turn it off under <em>General &rsaquo; Appearance &rsaquo; Label Decorations</em>.</p>

<p>Open source under the MIT License. Icons and icon associations:
Copyright &copy; 2015-2025 Elior "Mallowigi" Boukhobza, MIT License.</p>
```

## After the listing is approved

The listing page shows an *Install* button snippet ("Drag to Install"). Add it to the README
under *Installation*.
