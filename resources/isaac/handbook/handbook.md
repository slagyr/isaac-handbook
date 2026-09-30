# isaac.handbook — the handbook module itself

This chapter is about the handbook module you're reading right now
(`isaac.handbook`). It contributes the two tools you use to read and change
Isaac's config: `handbook__read` (this whole system) and
`handbook__configure`. Every other module's operating knowledge lives in its
own chapter — read `isaac.foundation` first for the vocabulary and concepts
every chapter assumes.

## Reading the handbook

`handbook__read` returns the table of contents when called with no `topics`:
one entry per installed module's chapter, plus one per `##` section inside a
chapter. Call it again with a topic id — a module id for a whole chapter, or
`<module-id>#<slug>` for one section — to read that part. Several topics can
be requested in one call; they come back in the order asked. An unknown topic
never fails the call, it's just listed as unknown. A large response is capped
(`handbook.max-chars`, default 40000 characters); anything past the cap is
listed as omitted by name, not silently dropped.

### Troubleshooting

- **A module you expect isn't in the table of contents.** Either it isn't
  installed (not in the running config's module index), or its manifest
  carries no `:handbook` key — modules with no chapter contribute nothing,
  which is not an error.
- **A topic comes back "unknown."** Check the id against the table of
  contents first; a section id is `<module-id>#<slug>`, where `<slug>` is the
  section's `##` heading lowercased with spaces turned to hyphens.

## Changing config

`handbook__configure` writes Isaac config the same way an operator's `isaac
config set` / `isaac config unset` would — same validation, same write path,
same hot-reload. It never writes a default value, and it never bypasses
validation (no `--force`). It is granted to a crew separately from
`handbook__read`, so a crew that can read the handbook cannot necessarily
change config, and vice versa.

A call carries `set` (a map of dotted config path to value), `unset` (a list
of dotted config paths to remove), or both — at least one is required. Every
pair in one call is applied as **one atomic batch**: it's staged and
validated as a single resulting config, not path by path. If any pair would
be invalid, the **entire call** is refused and **nothing** is written, not
even the pairs that were individually fine. There's no way to write part of
a batch — fix the invalid pair and call again.

A `set` can create a brand-new entity in one call (a new crew, a new cron
job) by writing its whole map at once, including a companion prose field (a
cron job's `:prompt`, a crew's `:soul`) inline in that same map — it splits
out to its own file automatically when the field is long or the entity kind
always keeps it separate, exactly as `isaac config set`'s stdin-map form
does. Where the new entity's own file lands (its own `<kind>/<id>.edn`, or
inline in the root config) follows the same precedent `isaac config set`
already uses; there's no separate rule for `handbook__configure`.

`handbook__configure` never reads or writes `.env`, and it never resolves a
`${VAR}` reference to see the secret behind it. If a path's current value is
already a `${VAR}` reference, a call that would overwrite it with a literal
value is refused — a crew can repoint a secret at a different `${VAR}` name,
but it can never turn a reference into a literal. This only catches
*overwriting* an already-referenced field; it can't stop a brand-new field
from being set to a literal that happens to look like a secret, since
checking that would mean reading `.env`. Always use `${VAR}` for anything
secret-shaped, from the first write.

Every call is logged as `:handbook/configure`, with the calling crew, the
session, every pair requested, and whether it was written or refused. A
path currently holding (or being set to) a `${VAR}` reference is logged as
the reference text, never a resolved value.

### Troubleshooting

- **A call is refused with "unknown key."** The path names a segment a
  schema'd map doesn't declare — a typo, or a field that belongs to a module
  that isn't installed. Check the path against that module's own chapter.
- **A call is refused for a `${VAR}` path.** The target already holds a
  `${VAR}` reference. Set it to a different `${VAR}` reference instead of a
  literal value; a human has to set the literal in `.env`.
- **A multi-pair call is refused and it's not obvious which pair is bad.**
  The refusal names every pair that failed, not just the first — the whole
  batch is refused together, so check all of them, not just the one you
  expected.
- **A crew can't call `handbook__configure` at all.** It needs
  `:handbook/configure` in that crew's `:tools :allow`, separately from
  `:handbook/read`.
