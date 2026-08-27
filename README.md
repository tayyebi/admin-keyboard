# Admin Keyboard for Android

An Android keyboard for administration work, with TKL and T9 layouts and English/Persian language support.

## Features

- **TKL Layout**: No numeric keypad, includes F1-F12, navigation keys, and arrow cluster
- **T9 Layout**: Compact multi-tap phone layout, selectable from Settings
- **Modifier Keys**: Ctrl, Alt, Fn, Shift with toggle states and shifted symbol input (!@#$%^&*...)
- **Language Switcher**: Globe key toggles between English and Persian
- **Dark Theme**: Modern dark UI similar to VS Code
- **Full Navigation**: Home, End, Page Up, Page Down, Insert, Delete
- **Special Keys**: Escape, Tab, Caps Lock, Print Screen, Scroll Lock, Pause

## Installation

1. Open in Android Studio
2. Build and run on device/emulator
3. Enable keyboard in Settings → System → Languages & Input → Virtual Keyboard
4. Switch to Admin Keyboard
5. Open Admin Keyboard Settings and choose TKL or T9

## Package

- **Package Name**: `com.admin.keyboard`
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 33

## CI/CD

This project uses GitHub Actions to build signed release APKs.

### Workflows

| Workflow | Trigger | Description |
|----------|---------|-------------|
| `build-apk.yml` | Push to `v*` tags or manual | Builds release APK with signing |

### Artifact Naming

Release APKs are named: `tlk-keyboard-<version>.apk`

### Signing

Release builds are signed using a keystore configured via GitHub Actions secrets.

| Secret | Purpose |
|--------|---------|
| `STORE_FILE_B64` | Base64-encoded `.keystore` file |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key entry password |
| `KEY_ALIAS` | Key alias name within the keystore |

**Fallback**: If no signing secrets are configured, a throwaway key is generated automatically. The build will succeed but the APK will not be suitable for distribution.

### Creating a Release

1. Update `versionName` and `versionCode` in `app/build.gradle`
2. Commit changes
3. Tag the commit: `git tag v1.0.0`
4. Push the tag: `git push origin v1.0.0`
5. GitHub Actions will build the signed APK and create a GitHub Release

## Persistent Signatures

When building release APKs, the signing key becomes part of the APK's identity. This has important implications:

### Why Persistent Signatures Matter

- **Update Chain**: Android requires all updates to an app to be signed with the same key. If you lose your signing key, you cannot publish updates to existing users.
- **App Integrity**: The signature verifies that the APK hasn't been tampered with since it was signed.
- **Play Store**: Google Play requires consistent signing for app updates.

### Key Management Best Practices

1. **Never commit keystores to version control** - Use GitHub Actions secrets or a secure key management system.
2. **Backup your keystore** - Store a secure backup in multiple locations (e.g., encrypted cloud storage, hardware security module).
3. **Document your key details** - Keep a record of:
   - Keystore filename
   - Key alias
   - Key password (stored securely, not in code)
   - Creation date
4. **Use strong passwords** - Use complex, unique passwords for keystore and key.
5. **Consider key rotation** - For long-lived apps, plan for key rotation using Android's key rotation support.

### Generating a Release Keystore

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias tkl-keyboard \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Your Name, OU=Dev, O=Your Org, L=City, ST=State, C=US"
```

### Encoding for GitHub Secrets

```bash
base64 -i release.keystore | tr -d '\n'
```

Copy the output to the `STORE_FILE_B64` secret.

### Generating each GitHub secret

The release workflow reads four secrets from **Settings → Secrets and variables → Actions**.
Generate them as follows (keep `STORE_PASSWORD`, `KEY_PASSWORD`, and `KEY_ALIAS`
consistent with the values passed to `keytool` above):

| Secret | What it is | How to generate |
|--------|------------|-----------------|
| `STORE_FILE_B64` | Base64 of the keystore file | `base64 -i release.keystore \| tr -d '\n'` — paste the output |
| `STORE_PASSWORD` | Keystore password (`-storepass`) | Choose a strong password and pass it as `-storepass` to `keytool`. Example random 24-char password: `openssl rand -base64 18 \| tr -d '/+' \| head -c 24` |
| `KEY_PASSWORD` | Key-entry password (`-keypass`) | Same approach as `STORE_PASSWORD`; it may be identical to or different from the keystore password. Pass as `-keypass` to `keytool` |
| `KEY_ALIAS` | Alias of the key inside the keystore (`-alias`) | Whatever you used for `-alias` (e.g. `tkl-keyboard`) |

Typical end-to-end sequence:

```bash
# 1. Pick passwords (record them securely — they cannot be recovered)
STORE_PASSWORD=$(openssl rand -base64 18 | tr -d '/+' | head -c 24)
KEY_PASSWORD=$(openssl rand -base64 18 | tr -d '/+' | head -c 24)
KEY_ALIAS=tkl-keyboard

# 2. Create the keystore using those values
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias "$KEY_ALIAS" \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=Your Name, OU=Dev, O=Your Org, L=City, ST=State, C=US"

# 3. Produce STORE_FILE_B64
base64 -i release.keystore | tr -d '\n'

# 4. Add the four values as GitHub Actions secrets:
#    STORE_FILE_B64 = output of step 3
#    STORE_PASSWORD = $STORE_PASSWORD
#    KEY_PASSWORD   = $KEY_PASSWORD
#    KEY_ALIAS      = $KEY_ALIAS
```

> If any of these four do not match the keystore (wrong password or alias, or a
> corrupted `STORE_FILE_B64`), the release build fails at `packageRelease` with
> `keystore password was incorrect`. After fixing the secrets, re-run the
> `v2.3.0` workflow (Actions → the run → "Re-run all jobs").

## Layout

```
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│ Esc │  F1 │  F2 │  F3 │  F4 │  F5 │  F6 │  F7 │  F8 │  F9 │ F10 │ F11 │ F12 │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬────────┐
│  `  │  1  │  2  │  3  │  4  │  5  │  6  │  7  │  8  │  9  │  0  │  -  │  =  │  Bksp  │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴────────┘
┌──────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬──────┐
│ Tab  │  q  │  w  │  e  │  r  │  t  │  y  │  u  │  i  │  o  │  p  │  [  │  ]  │  \   │
└──────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴──────┘
┌────────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────────┐
│  Caps  │  a  │  s  │  d  │  f  │  g  │  h  │  j  │  k  │  l  │  ;  │  '  │  Enter  │
└────────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────────┘
┌─────────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────────┐
│  Shift  │  z  │  x  │  c  │  v  │  b  │  n  │  m  │  ,  │  .  │  /  │  Shift  │
└─────────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────────┘
┌──────┬─────┬─────┬────┬────┬────────────────────┬─────┬─────┬──────┐
│ Ctrl │ Alt │  Fn │ 🌐 │                    │  <  │ Alt │ Ctrl │
└──────┴─────┴─────┴────┴────┴────────────────────┴─────┴─────┴──────┘
┌─────┬─────┬─────┐                                    ┌─────┐
│ Ins │ Home│ PgUp│                                    │  ^  │
└─────┴─────┴─────┘                                    └─────┘
┌─────┬─────┬─────┐                               ┌─────┬─────┬─────┐
│ ScLk│Pause│ PgDn│                               │  <  │  v  │  >  │
└─────┴─────┴─────┘                               └─────┴─────┴─────┘
```

## License

Private
