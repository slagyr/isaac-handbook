# isaac.handbook — Isaac's operating handbook, the handbook itself

You are a crew running inside Isaac, and you are reading this through the
very tool this chapter documents. **isaac.handbook** contributes the two
tools that make the rest of the handbook reachable and editable:
`handbook__read` (read any chapter) and `handbook__configure` (change
config the same way an operator's `isaac config set`/`unset` would). Every
other module's operating knowledge lives in its own chapter, reached
through `handbook__read` — read `isaac.foundation` first if you haven't;
it covers config mechanics (paths, `${VAR}` secrets, hot reload) this
chapter assumes. This chapter's own topic id is `isaac.handbook`; each `##`
heading below is also addressable on its own, e.g.
`isaac.handbook#reading-the-handbook`.

## Reading the handbook

**What it is.** `handbook__read` returns the table of contents when called
with no `topics`: one entry per installed module's chapter (foundation's
chapter always leads; every other module follows sorted by module id),
plus one entry per top-level `##` section inside each chapter. A module
that isn't installed, or whose manifest carries no `:handbook` key, or
whose `:handbook` path doesn't resolve, contributes nothing — that's not
an error, just an empty contribution.

A **topic id** is either a module id (the whole chapter) or
`<module-id>#<slug>` (one `##` section of that chapter, with any nested
subsections — including a `### Troubleshooting` — riding along as part of
its body). The slug is the section's heading, lowercased, punctuation
stripped, spaces turned to hyphens; only top-level `##` headings become
their own addressable topic — a `###` subsection is never independently
addressable, only reachable as part of its parent section.

Call `handbook__read` again with `topics`, an array of topic ids, to read
those parts directly; several can be requested in one call and come back
in the order asked, each naming its source module. An unknown topic never
fails the call — it's listed inline as unknown, with a pointer back to the
table of contents, and every other requested topic still comes back
normally.

A total response size is capped at `config:handbook.max-chars` (default
40000 characters, the built-in fallback when the key is unset). The cap
never cuts a topic in half — it keeps whole chapters/sections up to the
limit and always keeps at least the first one, so the response is never
empty even if that first item alone exceeds the cap. Anything past the cap
is listed by name as omitted, not silently dropped — ask for an omitted
topic by itself, or narrow a whole-chapter request down to just the
section you need, to get past the cap.

**Reference topics — not yet built.** The vocabulary above (`module:<id>`,
`crew:<id>`, `comm:<id>`, `cron:<id>`, `hail-band:<id>`, and
`config:<dotted.path>` with its live effective value) is drafted in this
module's `features/reference.feature` but still tagged `@wip`; only
module-chapter topics (a module id, or `<module-id>#<slug>`) are live
today. Asking for `module:isaac.handbook` or `config:handbook.max-chars`
as a **topic id** on `handbook__read` currently comes back unknown, the
same as any other topic this module hasn't heard of — it is not yet a
generated inventory or config-reference entry. **`[verify]`** the exact
landing order/shape of the reference topics once isaac-niqx ships; this
chapter will need an update at that point (see Shipping a chapter, below,
for the keep-it-current rule).

**How to verify.** Call `handbook__read` with no topics and confirm your
module and its sections are listed; call it with your chapter's own
module id and check the markdown comes back; ask for a topic you expect
to be missing and confirm it's reported unknown rather than erroring.

### Troubleshooting

- **A module you expect isn't in the table of contents.** Either it isn't
  installed (check the running config's module index — `isaac.foundation`,
  Modules and berths), or its manifest carries no `:handbook` key, or the
  path it names doesn't resolve on the classpath. All three are silent
  omissions, never errors.
- **A topic comes back "unknown."** Check the id against the table of
  contents first. A section id is `<module-id>#<slug>` — the module id has
  to match exactly (case-sensitive), and the slug has to match a top-level
  `##` heading in that module's chapter, not a nested `###` one.
- **You expect a reference-style topic (`module:…`, `crew:…`, `config:…`)
  and it comes back unknown.** Expected today — reference topics haven't
  shipped yet (see above). Only module-chapter topics resolve.
- **A big chapter comes back truncated with an "omitted" note.** That's
  the size cap (`config:handbook.max-chars`), not a bug — ask for the
  specific `<module-id>#<slug>` section you need instead of the whole
  chapter, or raise the cap (Changing config, below) if every caller
  genuinely needs larger responses.

## Changing config

**What it is.** `handbook__configure` writes Isaac config through
foundation's single write path (`isaac.config.mutate/set-many!`) — the
same validate/write/hot-reload machinery `isaac config set`/`unset` use.
It never writes a default value on your behalf and it never bypasses
validation; there is no `--force` from inside a turn.

A call carries `set` (a map of dotted config path to value), `unset` (a
list of dotted config paths), or both — at least one is required. Every
pair across the whole call is applied as **one atomic batch**: it's
staged and validated as a single resulting config, not pair by pair. If
any pair would leave the config invalid, the **entire call** is refused
and **nothing** is written, not even the pairs that were individually
fine — the refusal names every pair that failed, not just the first one
encountered. There is no way to land part of a batch; fix the bad pair
and call again. Set two fields that are only valid together (a new
model's `model` and `provider`, say) in the same call rather than one at
a time, since setting only one would be refused on its own.

A `set` can create a brand-new entity in one call — a new crew, a new
cron job — by writing its whole map at once, including a companion prose
field inline (a cron job's `:prompt`, a crew's `:soul`): it splits out to
its own file automatically when the field is long or the entity kind
always keeps it separate, the same as `isaac config set`'s stdin-map
form. **Placement** follows `config set`'s own precedent, with no
tool-specific override: an **existing** entry is written wherever it
already lives (never converted from a file to inline or back); a **new**
entry becomes its own entity file when the running config has
`:prefer-entity-files true`, otherwise it lands inline in `isaac.edn`.
A successful call reports exactly which file(s) each pair landed in —
read that confirmation back rather than assuming.

`handbook__configure` never reads or writes `.env`, and it never resolves
a `${VAR}` reference to see the secret behind it. If a path's current
value is already a `${VAR}` reference, a call that would overwrite it
with a literal value is refused — a crew can repoint a secret at a
different `${VAR}` name, but can never turn a reference into a literal.
This only catches *overwriting* an already-referenced field; a brand-new
field set to a literal that merely looks secret-shaped isn't caught
(checking that would mean reading `.env`, which this tool never does) —
always write `${VAR}` for anything secret-shaped from the first call.

Every call is logged as `:handbook/configure`, with the calling crew, the
session, every pair requested, and whether it was written or refused. A
path currently holding (or being set to) a `${VAR}` reference is logged
as the reference text, never a resolved value.

**How to verify.** Read the call's own reply — a written call echoes each
pair and the file it landed in; a refused call names every failing pair
and reason. `config:<dotted.path>` reads the schema plus current value
for one field, per `isaac.foundation`, Schemas — use it to confirm a
write actually took, or that a path you're about to set is the one you
think it is.

### Troubleshooting

- **A call is refused with "unknown key."** The path names a segment a
  schema'd map doesn't declare — a typo, or a field belonging to a module
  that isn't installed. Check the path against that module's own chapter.
- **A call is refused for a `${VAR}` path.** The target already holds a
  `${VAR}` reference. Set it to a different `${VAR}` reference instead of
  a literal value; a human has to set the literal itself, outside this
  tool.
- **A multi-pair call is refused and it's not obvious which pair is bad.**
  The refusal names every pair that failed, not just the first — check
  all of them, since the whole batch is refused together.
- **You set a field and it doesn't seem to take effect.** Confirm hot
  reload is on (`isaac.foundation`, Hot reload) before assuming the write
  itself failed — a successful `handbook__configure` reply already means
  the write landed; a config that then fails to *apply* is a hot-reload
  question, not a `handbook__configure` one.
- **A crew can't call `handbook__configure` at all**, even though it can
  call `handbook__read`. It needs `:handbook/configure` in that crew's
  `:tools :allow`, separately from `:handbook/read` — see Granting the
  tools, below.

## Granting the tools

**What it is.** `handbook__read` and `handbook__configure` are two
separate crew tools, granted independently through a crew's
`:tools :allow` list — `:handbook/read` for the first, `:handbook/configure`
for the second. A crew that can read the handbook cannot necessarily
change config, and a crew granted only `:handbook/configure` (unusual, but
not prevented) can write config without being able to read any chapter
through this module. There is no combined "handbook" grant that implies
both.

```
config set crew.cordelia.tools.allow.handbook/read
config set crew.cordelia.tools.allow.handbook/configure
```

This is the ordinary tool-grant mechanism every other tool uses —
`isaac.agent` owns the allow/deny cascade in full (global allow/deny, crew
deny-over-global, crew allow-over-everything); this module contributes
nothing to that mechanism beyond the two tool names.

**How to verify.** Check a crew's effective tool list (its own next turn's
prompt, or `isaac.agent`'s reference on reading back allow/deny) for
`handbook__read` / `handbook__configure` by name — the wire names use a
double underscore; the config grant uses a single slash
(`:handbook/read` grants `handbook__read`).

### Troubleshooting

- **A crew has `handbook__read` but not `handbook__configure`, and that's
  a surprise.** They're independent grants by design — add
  `:handbook/configure` to that crew's `:tools :allow` if it should also
  be able to change config.
- **Neither tool shows up for a crew at all.** Confirm `isaac.handbook`
  itself is installed (`isaac.foundation`, Modules and berths) — a crew
  can't be granted a tool a module never contributed in the first place.

## Shipping a chapter

**What it is.** A module's chapter lives at `resources/isaac/<pkg>/handbook.md`,
and its manifest's `:handbook` key names that classpath path — any module,
including this one, ships its handbook chapter this way by adding a
top-level `:handbook` key to its `isaac-manifest.edn` (this module's own
manifest declares `:handbook "isaac/handbook/handbook.md"`, backed by
`resources/isaac/handbook/handbook.md` — the file you're reading right
now). The chapter is free-form markdown: each top-level `##` heading
becomes one of that module's addressable sections
(`<module-id>#<slug>`); a nested `### Troubleshooting` under a concept is
the house convention every other chapter follows, not a requirement this
module enforces. A module with no `:handbook` key, or whose declared path
doesn't resolve, contributes nothing to the table of contents — a broken
or missing doc path is a warning on `isaac modules show`, never something
that stops the module from loading.

**Keeping it current** is a working rule, not something this module
enforces mechanically: per `isaac/AGENTS.md` ("Keep the handbook
current"), a bean that changes documented behavior — a config key, a
default, a CLI command, a user-visible behavior, a new troubleshooting
case — updates that module's chapter in the same commit as the code. New
config keys carry a schema `:description`, since the handbook's config
reference (once reference topics land — see Reading the handbook, above)
is generated from it; a key with no description is undocumented even
after that lands.

**How to verify.** `isaac modules show <id>` reports the module's
`Handbook:` path and whether it resolved; `handbook__read` with no topics
confirms the chapter actually appears in the table of contents (and at
the position you expect — foundation first, then alphabetical by module
id). Ask for the module's own id as a topic to read back exactly what
ships.

### Troubleshooting

- **A brand-new chapter doesn't show up after adding `:handbook` to the
  manifest.** Check `isaac modules show <id>` for a `Handbook:` warning —
  the path is classpath-relative to the module's own `deps.edn` `:paths`
  (`resources`/`src` by default), not repo-root-relative; a typo there
  resolves to nothing, silently, from `handbook__read`'s point of view.
- **A section you expect isn't independently addressable.** Only
  top-level `##` headings become their own topic id; a `###` subsection
  (including `### Troubleshooting`) is never separately addressable — it
  rides along inside its parent `##` section's body.
- **You changed a config default or added a key and the chapter wasn't
  touched.** That's a process gap, not a tooling failure — this module
  has no lint that catches a stale chapter's *prose* going out of date
  (only that `` `config:<path>` `` references still resolve and `isaac
  <command>` mentions are still real commands). Update the chapter by
  hand, in the same commit, per the keep-it-current rule above.
