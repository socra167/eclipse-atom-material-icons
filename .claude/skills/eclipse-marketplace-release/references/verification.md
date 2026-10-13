# Verifying a signed build locally

```sh
export GNUPGHOME=/tmp/gpgt            # short path: gpg-agent socket length limit
rm -rf "$GNUPGHOME" && mkdir -m 700 "$GNUPGHOME"
gpg -q --batch --passphrase testpass --quick-gen-key "Test Signer <test@example.invalid>" ed25519 sign never
gpg --batch --pinentry-mode loopback --passphrase testpass --armor --export-secret-keys > /tmp/test-key.asc

MAVEN_GPG_PASSPHRASE=testpass mvn -B clean verify -Psign -DskipTests \
  -Dtycho.pgp.signer.bc.secretKeys=/tmp/test-key.asc

# signatures and public key are recorded in the artifact repository
unzip -p <site-module>/target/repository/artifacts.jar artifacts.xml | grep -o 'pgp\.[a-zA-Z]*' | sort | uniq -c
```

Expect one `pgp.signatures` per plug-in/feature and `pgp.publicKeys`. To check a signature, copy
the `pgp.signatures` property value of a bundle into `bundle.asc` (it is XML-escaped; parse the
XML rather than grepping) and run:

```sh
gpg --verify bundle.asc <site-module>/target/repository/plugins/<bundle>_<version>.jar   # "Good signature"
```

Clean up: `gpgconf --kill gpg-agent; rm -rf /tmp/gpgt /tmp/test-key.asc`.
