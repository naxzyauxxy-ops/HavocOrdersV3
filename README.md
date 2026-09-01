# HavocAuction

A player auction house for **Paper / Purpur 1.21.7+**, built entirely on Minecraft's native
**Dialog API**. Same architecture and conventions as HavocOrders — no chest GUIs anywhere.

Players list the item in their hand for a price, anyone can buy it instantly, and sellers
get paid, notified, and keep a full transaction history.

## Requirements

- **Server:** Paper or Purpur **1.21.7+**
- **Client:** Minecraft **1.21.6+** — dialogs do not render on older clients
- **Vault** plus an economy provider
- JDK 21 to build

## Screens

| Dialog | What it does |
| --- | --- |
| Auction | The board: paged, six sort modes, nine category filters, private search |
| Buy | Confirmation with price, per-item price, and your balance before/after |
| Container Preview | What's inside a shulker box, *before* you buy it |
| Your Items | Live listings, total value, and the way into everything else |
| Manage Listing | Pull a listing off the board |
| Sell | Lists your held item; price field with a per-item helper |
| Confirm Listing | Fee and payout before you commit |
| Collect | Items from cancelled and expired listings |
| History | Sales and purchases with lifetime earned/spent/net |

## Commands

| Command | Description |
| --- | --- |
| `/ah` | Open the auction board |
| `/ah sell <price>` | List the held item without opening anything |
| `/ah reload` | Reload config and dialogs |
| `/ah import [file]` | Import a legacy DonutAuction database |

Permissions: `havocauction.use` (default true), `havocauction.admin` (default op).

## Economy

- **No escrow.** The buyer pays at the moment of purchase and the seller is paid then.
- `LISTING-FEE-PERCENT` / `LISTING-FEE-FLAT` charge the seller up front, win or lose.
- `TAX-PERCENT` takes a cut of the sale before the seller is paid.
- `BROADCAST-PRICE-THRESHOLD` announces expensive listings server-wide.

A purchase claims the listing *before* any money or items move, and rolls the claim back if
the withdrawal fails. Two players clicking the same listing cannot both win it.

## Number shorthand

Prices accept `1k`, `2.5k`, `1m`, `3b`, `$1,250`. Display abbreviates to `1.23m`; turn that
off with `AUCTION.ABBREVIATE-NUMBERS: false` and input shorthand still works.

## Search privacy

Search uses the dialog's own text field, so it goes client-to-server with the button click
and never appears in chat or in chat-logging plugins. Same for the history search and the
sell price field.

## Renamed items and search

Search matches the **real item type**, never the custom display name. Otherwise anyone can
rename a block of dirt to "Elytra", list it for millions, and have it answer every elytra
search — the display name is attacker-controlled text, so it is not something to key a
search on.

Renamed listings are also flagged wherever they appear: the button label gets the real type
appended, and the tooltip carries a red warning line plus a `Type:` row.

```yaml
AUCTION:
  SEARCH-CUSTOM-NAMES: false
```

Turning it on makes search match custom names too. Only do that if you accept the above.
The seller's name is always searchable either way.

## Durability

Damaged items show their durability on the board, the buy screen, your listings and the
collect screen: `Durability: 384/432 (89%)`.

The row is a template in `dialogs.yml`, and lines that resolve to nothing are dropped, so
items without durability simply have no durability row rather than an empty gap:

```yaml
LINES:
  DURABILITY: "&7Durability: &f{durability} &8({durability_percent}%)"
  RENAMED: "&c! &7Renamed. Actually a &f{type}"
  RENAMED-TAG: " &8({type})"
```

Placeholders: `{durability}`, `{durability_percent}`, `{type}`, `{custom_name}`,
`{renamed}`, `{durability_line}`, `{renamed_line}`, `{renamed_tag}`.

## Fast mode

Carried over from the legacy `fast_auction` flag: toggling it on Your Items skips both the
purchase and listing confirmation screens. Off by default.

## Selling

Dialogs have no item slot, so the held item is the input — the same model as `/ah sell`.
That removes a whole class of duplication bugs that come with a deposit slot. The item only
leaves your hand once the listing is stored.

The Sell screen also has a **per-item** button: type `500`, hit it, and a stack of 64 is
priced at 32,000.

## Performance

- Every listing lives in memory; the database is only ever written to, in batches.
- Writes go into a dirty set flushed by one async transaction every 30s.
- Listings are indexed by seller and by buyer, so Your Items and History never scan.
- The board caches its filtered, sorted result per player behind a version counter — a page
  turn is a list slice.
- Item names, materials and stack sizes are computed once at load, so filtering and
  searching never decode an item.
- Drops release a few stacks per tick rather than all at once.
- `HISTORY-KEEP-DAYS` (default 30) purges old sold rows. Each carries a serialised item, so
  this is the memory dial — raise it for a longer log, lower it on a busy server.

## External settings menus (PlaceholderAPI)

Registers the `havocauction` expansion when PlaceholderAPI is installed, plus standalone
toggle commands, so a settings menu can drive both preferences directly.

| Placeholder | Value |
| --- | --- |
| `%havocauction_alerts_status%` | Styled ON / OFF |
| `%havocauction_fast_status%` | Styled ON / OFF for fast mode |
| `%havocauction_alerts_raw%` / `%havocauction_fast_raw%` | `true` / `false` |
| `%havocauction_listings%` | Live listings |
| `%havocauction_collectable%` | Listings waiting to collect |
| `%havocauction_listed_value%` | Asking value of your live listings |
| `%havocauction_total_made%` / `%havocauction_total_spent%` / `%havocauction_net%` | Lifetime totals |
| `%havocauction_sales%` / `%havocauction_purchases%` | Lifetime counts |
| `%havocauction_board_size%` | Listings on the board right now (no player needed) |

Commands: `/toggleauctionalerts` (alias `/ahalerts`), `/togglefastauction` (alias
`/fastauction`).

The ON/OFF text is config-driven, since it renders inside whatever menu plugin reads it:

```yaml
PLACEHOLDERS:
  ENABLED-TEXT: "<green>ON"
  DISABLED-TEXT: "<red>OFF"
```

Use `&a` / `&c` instead if your menu expects legacy colour codes, or plain `ON` / `OFF`.

## Importing from DonutAuction

Drop the old `auction.db` into `plugins/HavocAuction/` as `import.db` and start the server,
or run `/ah import`. Legacy ids are preserved, so re-running skips anything already there.

The old schema stores UUIDs as raw 16-byte blobs and items as `BukkitObjectOutputStream`
dumps; both are read directly.

| Legacy status | Becomes |
| --- | --- |
| `ACTIVE` | Back on the board |
| `CANCELLED` | Item waiting in the seller's collect screen |
| `SOLD` | History row — feeds the transaction log and lifetime totals |

`auction_profiles.alerts` and `fast_auction` come across as the two toggles on Your Items.

**The importer moves no money and hands out no items.** Sold rows are history only; the old
plugin already settled that money.

```yaml
IMPORT:
  IMPORT-HISTORY: true    # false for a clean start with no transaction log
  EXPIRY:
    MODE: EXTEND          # or KEEP: import as expired, item waits for collection
    EXTEND-DAYS: 7
```

Remove the old plugin first so the two are not running against one economy.

## Config files

- `config.yml` — database, economy, fees, limits, history retention, import, messages
- `dialogs.yml` — every title, body line, button label and tooltip, with `{placeholders}`,
  hex colours, and per-dialog `COLUMNS`

## Build

```
mvn clean package
```

Jar lands in `target/HavocAuction-1.0.0.jar`. CI is in `.github/workflows/build.yml`.
