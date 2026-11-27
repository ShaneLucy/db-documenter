# Catalog dump format (verbatim mode)

Phase 1.3 (2026-11-27). The text format written by `internal.catalog.CatalogDumpWriter`. It is the golden format
for reader ITs (`/<vendor>/<fixture>/…-expected-catalog.txt` in test-support), and expected dumps are **written by
hand from this spec and the DDL, never generated**. Internal: not a public contract, may change before 1.0.

## Rules

- One fact per line. Every line is self-contained: it names its full path, so a diff hunk reads without context.
- Lines appear in the model's canonical order (below). The writer never re-sorts; the model's compact constructors
  already define a total order.
- No product name or version, so one expected dump can serve several server versions.
- LF line endings, UTF-8, a trailing newline after the last line. No blank lines, no comments, no trailing spaces.
- Tokens are separated by a single space.

### Tokens

| Token | Form |
|---|---|
| *ident* | bare when it matches `[a-z0-9_]+`, otherwise double-quoted (`"Mixed Case"`, `""` for empty) |
| *qname* | *ident*`.`*ident* (`auth.users`, `main."Mixed Case"`) |
| *string* | always double-quoted (`"int4"`, `"enum('a','b')"`, `""`) |
| *cols* | `(`*ident*`, `*ident*…`)`, in key / list order, separated by comma + space |
| *action* | `no_action`, `restrict`, `cascade`, `set_null`, `set_default` |

Inside quotes, `\n` `\r` `\t` `\\` `"` are written as `\n` `\r` `\t` `\\` `\"`. Other non-printable characters (ISO
controls; Unicode format, line/paragraph separator, private-use, unassigned and lone surrogate code points) are written
as `\uXXXX` per UTF-16 unit, uppercase hex. Everything else is written as itself. (Implementation:
`internal.catalog.Identifiers`.)

### Lines

```
schema <ident>
table <qname>
column <qname>.<ident> type=<string> nullable=<true|false>[ user_type=<qname>]
pk <qname><cols>[ name=<ident>]
unique <qname><cols>[ name=<ident>]
fk <qname><cols> -> <qname><cols> on_delete=<action> on_update=<action>[ name=<ident>]
enum <qname> values=(<string>, <string>…)
```

- `column` lines carry no position: their order in the dump **is** the column order.
- An optional attribute is omitted when the model value is empty (no `name=` for an unnamed key, no `user_type=`).
- `enum` with no values is written `values=()`.

### Order

For each schema (by name):
1. `schema`
2. for each table (by name): `table`, its `column`s (list order), `pk`, `unique`s (model order), `fk`s (model order)
3. its `enum`s (by name)

## Example

```
schema auth
table auth.user_mfa
column auth.user_mfa.user_id type="int4" nullable=false
column auth.user_mfa.method type="mfa_method" nullable=false user_type=auth.mfa_method
pk auth.user_mfa(user_id, method) name=user_mfa_pkey
fk auth.user_mfa(user_id) -> auth.users(id) on_delete=cascade on_update=no_action name=user_mfa_user_id_fkey
table auth.users
column auth.users.id type="int4" nullable=false
column auth.users."Display Name" type="varchar(100)" nullable=true
pk auth.users(id) name=users_pkey
unique auth.users("Display Name")
enum auth.mfa_method values=("totp", "sms")
```

## Neutral mode (Phase 4, not implemented)

For cross-vendor comparison: omit `name=`, normalise `type=` per vendor, drop `enum` lines and `user_type=`, fold
identifier case per `Dialect`.
