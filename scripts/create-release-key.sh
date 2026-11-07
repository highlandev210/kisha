#!/usr/bin/env bash
# Creates Kisha's release signing key (once, ever) and optionally stores it as GitHub Actions secrets.
#
#   scripts/create-release-key.sh            # create the key in ~/.kisha/
#   scripts/create-release-key.sh --github   # ...and upload it as repository secrets with `gh`
#
# Back up ~/.kisha/ somewhere safe (password manager, encrypted drive). If the key is lost, new
# versions of Kisha can't update existing installs; users would have to uninstall and lose their data.
set -euo pipefail

DIR="$HOME/.kisha"
KEYSTORE="$DIR/kisha-release.jks"
SECRETS="$DIR/kisha-release.properties"
ALIAS="kisha"
KEYTOOL="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}/bin/keytool"

mkdir -p "$DIR" && chmod 700 "$DIR"
if [ -e "$KEYSTORE" ]; then
  echo "A key already exists at $KEYSTORE; not overwriting it."
else
  PASSWORD="$(openssl rand -base64 33 | tr -d '/+=\n' | head -c 32)"
  "$KEYTOOL" -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -alias "$ALIAS" \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -storepass "$PASSWORD" -keypass "$PASSWORD" -dname "CN=Kisha, O=highlandev210" >/dev/null
  printf 'KISHA_KEYSTORE_FILE=%s\nKISHA_KEYSTORE_PASSWORD=%s\nKISHA_KEY_ALIAS=%s\nKISHA_KEY_PASSWORD=%s\n' \
    "$KEYSTORE" "$PASSWORD" "$ALIAS" "$PASSWORD" > "$SECRETS"
  chmod 600 "$KEYSTORE" "$SECRETS"
  echo "Created $KEYSTORE (password in $SECRETS)."
fi

# shellcheck disable=SC1090
source <(sed 's/^/export /' "$SECRETS")
echo "Signing certificate fingerprint (publish this so people can verify downloads):"
"$KEYTOOL" -list -v -keystore "$KEYSTORE" -storepass "$KISHA_KEYSTORE_PASSWORD" | grep -E "SHA256:" | head -1

if [ "${1:-}" = "--github" ]; then
  base64 < "$KEYSTORE" | tr -d '\n' | gh secret set KISHA_KEYSTORE_BASE64
  printf %s "$KISHA_KEYSTORE_PASSWORD" | gh secret set KISHA_KEYSTORE_PASSWORD
  printf %s "$KISHA_KEY_ALIAS" | gh secret set KISHA_KEY_ALIAS
  printf %s "$KISHA_KEY_PASSWORD" | gh secret set KISHA_KEY_PASSWORD
  echo "Stored the four KISHA_* secrets in this repository's GitHub Actions settings."
fi

echo "Back up $DIR now."
