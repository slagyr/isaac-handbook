# 🍏 Isaac Handbook 📖

Isaac's operating handbook (the POH — Pilot Operating Handbook) for
[Isaac](https://github.com/slagyr/isaac): lets a self-aware crew read how the
running instance it's inside actually operates, drawn from every installed
module's own handbook chapter.

Depends on [isaac-foundation](https://github.com/slagyr/isaac-foundation) and
[isaac-agent](https://github.com/slagyr/isaac-agent). Contributes two crew
tools: `handbook__read` (read the handbook) and `handbook__configure`
(change config the same way `isaac config set`/`unset` would).

[![Handbook](https://github.com/slagyr/isaac-handbook/actions/workflows/ci-tests.yml/badge.svg)](https://github.com/slagyr/isaac-handbook/actions/workflows/ci-tests.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Clojure](https://img.shields.io/badge/Clojure-1.11%2B-blue?logo=clojure)](https://clojure.org)
[![Babashka](https://img.shields.io/badge/Babashka-1.3%2B-red?logo=clojure)](https://babashka.org)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk)](https://openjdk.org/)

## What's here

- Module `isaac.handbook` (`isaac.handbook.module/create-module`).
- Crew tool `handbook__read` — granted like any other tool, via a crew's
  `:tools :allow` list (`handbook/read`).
- Called with no topics, it returns a table of contents: one entry per
  installed module's handbook chapter, plus one per concept section inside a
  chapter. Foundation's chapter always leads the table of contents; every
  other module follows sorted by module id.
- Called with topics, it returns those items — in the order asked — as
  markdown, each naming its source module. A topic id is either a module id
  (the whole chapter) or `<module-id>#<slug>` (one `##` section of it, with
  any nested subsections such as `### Troubleshooting` riding along).
- An unknown topic never fails the call; it's listed as unknown alongside
  where to find what's available. A total response size cap
  (`handbook.max-chars`, default 40000 characters) returns what fits and
  lists the rest as omitted by name.
- Reference topics — one per crew, module, comm, cron job, and hail band, plus
  one per config path with its current effective value — are drafted
  (`features/reference.feature`, `@wip`) but not yet built (isaac-niqx).
- Crew tool `handbook__configure` — granted separately from `handbook__read`,
  via `:tools :allow [:handbook/configure]`. Writes config through
  foundation's single write path (`isaac.config.mutate/set-many!`), the same
  validate/write/hot-reload machinery `isaac config set`/`unset` use — never
  a second writer, never `--force`. A call carries `set` (a map of dotted
  config path -> value) and/or `unset` (a list of dotted config paths); every
  pair in one call is applied as ONE atomic batch, so an invalid pair refuses
  the whole call and nothing is written, even the individually-valid pairs.
  Companion prose fields (a cron job's `:prompt`, a crew's `:soul`) and
  brand-new entities (a new crew, a new cron job) ride the same call — where
  a new entity lands follows `config set`'s own placement precedent, with no
  tool-specific override. The tool never reads or writes `.env` and refuses a
  literal value over a path whose current value is a `${VAR}` reference (a
  crew can repoint a secret at a different `${VAR}`, never overwrite it with
  a literal). Every call is logged as `:handbook/configure`.

## How a module ships a chapter

A module adds a top-level `:handbook` key to its `isaac-manifest.edn`, naming
a classpath-relative markdown file (for example `:handbook
"isaac/foundation/handbook.md"`, alongside `resources/isaac/foundation/handbook.md`).
The chapter is free-form markdown: `##` headings become the module's
addressable sections (`<module-id>#<slug>`); a nested `### Troubleshooting`
under a concept is the house convention, not a requirement enforced by this
module. A module with no `:handbook` key just contributes nothing to the
table of contents — that's not an error.

Per `isaac/AGENTS.md` ("Keep the handbook current"): a bean that changes
documented behavior (a config key, a default, a CLI command, a user-visible
behavior, a new troubleshooting case) updates the module's chapter in the
same commit. New config keys carry a schema `:description`, since the
handbook's config reference is generated from it.

## Installation

Declare the module in your Isaac config's `:modules` map, or install it by
its registry id:

```bash
isaac modules install isaac.handbook
```

```clojure
{:modules {:isaac.handbook {:git/url "https://github.com/slagyr/isaac-handbook.git"
                            :git/sha "<sha>"}}}
```

Then grant `handbook__read` to a crew:

```clojure
{:crew {:cordelia {:tools {:allow [:handbook/read]}}}}
```

## Development

Sibling checkouts expected:

```
plan/
  isaac-foundation/
  isaac-agent/
  isaac-handbook/   # this repo
```

```sh
bb spec            # unit specs
bb features        # acceptance features
bb lint-cli-host   # process-global operations stay inside the CLI host boundary
bb lint            # clj-kondo (fast syntax/paren check; pass a file/dir to narrow)
bb ci              # lint-cli-host + specs + features
```

From the JVM:

```sh
clj -M:spec
clj -M:features
```

Use the `:dev-local` alias to point `deps.edn` at sibling checkouts instead
of the pinned git SHAs:

```sh
clj -M:dev-local:spec
```

## Consumer coordinate

```clojure
io.github.slagyr/isaac-handbook {:local/root "../isaac-handbook"}
;; or {:git/url "https://github.com/slagyr/isaac-handbook.git" :git/sha "..."}
```
