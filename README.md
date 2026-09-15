# LC Private Mod Editor

A private/offline Android launcher + fail-closed structured-data scanner for a separate modifiable copy.

## What it does
- Launches normal LC and a separate private/modifiable LC package.
- Lets the user explicitly select the private data directory through Android's file picker.
- Scans supported text formats for uniquely identifiable game-like fields (for example keys containing `lunacy`, `hp`, `damage`, `currency`, etc.).
- Shows file, field name, type, current value and surrounding context before any edit.
- Refuses ambiguous/unsupported fields.
- Creates a timestamped backup before each write.
- Rechecks the exact old value before writing and verifies the complete file after writing.

## What it deliberately does NOT do
- No blind search-and-replace of arbitrary numbers or bytes.
- No attempt to defeat encryption, anti-cheat, server validation, or access another app's private sandbox without user-provided access.
- No claim that every LC version or binary/serialized format is supported.

The scanner is intentionally conservative. If a real private build stores its data in a binary, encrypted, protobuf, database, or otherwise unsupported format, it reports no editable fields rather than guessing. A format-specific profile/parser can be added later without changing the safety model.

## Package IDs
Normal LC: `com.ProjectMoon.LimbusCompany`
Private/modifiable copy: `com.ribi.limbusprivate`

Change those constants in `MainActivity.java` if your private APK uses another package ID.
