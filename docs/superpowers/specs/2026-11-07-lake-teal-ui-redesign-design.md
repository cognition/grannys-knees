# Lake teal UI redesign

## Goal

Replace the cream paper / ink / sharp-corner “scrapbook newspaper” personality with a playful, readable system: **lake teal light**, **soft navy dark**, friendly sans, and soft rounded surfaces. Wit comes from colour hierarchy and copy, not from serif scrapbook styling.

## Decisions locked in brainstorming

- Full visual personality reset (not a tint tweak).
- Mood: playful wit first, still readable for older eyes.
- Colour family: **Lake teal** for light.
- Dark: **Soft navy** surfaces with **mint-teal** accents (not teal-on-teal, not loud green panels).
- Type: **Friendly sans** everywhere (no display serif).
- Shape: softer rounded surfaces (~12–20dp), drop 2dp newspaper corners.

## Colour system

### Light

| Role | Hex | Use |
|------|-----|-----|
| Background / surface | `#F4F8F9` | App ground |
| Surface container | `#E7F1F3` | Cards, secondary panels |
| Primary container | `#D9EEF0` | Index hero well |
| Primary | `#0E7C86` | CTAs, selected nav, index emphasis, chart line |
| On primary | `#F4F8F9` | Text on primary buttons |
| Ink / on surface | `#1C2A30` | Body text |
| Muted / on surface variant | `#5E7380` | Meta, asides |
| Secondary (rough / alert) | `#C24E2D` on `#F8E4E1` | Rough chips and rough-day emphasis only |
| Outline / dividers | soft cool grey-teal (e.g. `#C5D5DA`) | Hairlines, unselected chips |

Do **not** use large green surfaces for “mild.” Mild uses the same cool surface family with a teal edge or label.

### Dark (Soft navy)

| Role | Hex | Use |
|------|-----|-----|
| Background | `#152033` | App ground (lifted, not near-black) |
| Surface | `#1B2A42` | Raised phone chrome / scaffolds |
| Surface container | `#1E2C44` | Side cards, body-map well |
| Primary container | `#243552` | Index / outlook wells |
| Primary / CTA | `#7FE0E8` | Buttons, selected nav, chart line, selected chips |
| On primary | `#0F2A38` | Text on mint CTAs |
| Ink / on surface | `#E8F0F8` | Body text |
| Muted | `#8FA3BC` | Meta |
| Secondary (rough) | `#FFC4BC` on `#5A2E2A` | Rough chips and rough-day columns only |

Dark is **blue-led for depth**; teal is the accent bridge so light and dark still feel like one product. Avoid moss/green fill panels.

### Explicit non-goals

- No cream `#F6E7D4` paper palette.
- No near-black `#161310` cave dark.
- No purple brand accents.
- No large green “good weather” surfaces.
- Gauge “good” in charts, if needed, stays a muted teal — never a green card.

## Typography

- All Material 3 type roles use a sans family (system / default sans — no `FontFamily.Serif`).
- Index number: large, heavy weight (≈48sp feel on Today), primary colour in light, white or mint emphasis in dark.
- Body and labels sized for older eyes (body ≈15–16sp feel; avoid tiny meta where it carries meaning).
- Brand name in UI chrome: sans, semibold — not a serif masthead.

## Shape

- Replace `GrannyCorner` 2dp newspaper corners with friendlier radii.
- Target Material shapes roughly: small 8dp, medium 12–14dp, large 16–20dp for hero cards and primary buttons.
- Chips may use pill radius; primary buttons use large rounded rects (not sharp stamps).

## Screen mapping

Applies across Today, Report (journal), Outlook (curve + five-day), About, Privacy, City search, and bottom navigation.

- **Primary actions** (Log / Save / Confirm): filled primary.
- **Selected chips / nav**: primary fill + on-primary text.
- **Unselected chips**: surface container + outline.
- **Granny asides**: muted on-surface-variant under the index or body map.
- **Rough**: secondary coral family only — never as the brand primary.
- **Outlook “now” marker**: secondary/coral dashed line is acceptable; curve stroke stays primary.

## Widget and system chrome

- Home-screen widget colours today use a separate sage set. Align widget background, primary, and badge with the **light lake teal** tokens so phone and widget match.
- XML `colors.xml` paper/night values update to the new light surface and dark background.
- Logo artwork (`ic_logo_light` / `ic_logo_dark`): keep current marks unless contrast fails on the new grounds; if contrast fails, adjust background treatment first before redrawing the mark.

## Implementation touchpoints

- [`app/src/main/java/com/example/grannysknees/ui/theme/Theme.kt`](app/src/main/java/com/example/grannysknees/ui/theme/Theme.kt) — colour schemes, typography, shapes, `GrannyCorner`.
- [`app/src/main/res/values/colors.xml`](app/src/main/res/values/colors.xml) — paper/night/logo companions.
- [`app/src/main/java/com/example/grannysknees/widget/GrannysKneesWidget.kt`](app/src/main/java/com/example/grannysknees/widget/GrannysKneesWidget.kt) — Glance colour providers.
- Screens that hard-code ink/paper assumptions (journal slider/button colours that already use `colorScheme`) should pick up the theme automatically; re-check any remaining hardcoded hex.
- Feature version bump on the phone app when this ships (current `0.3.32.0` → next feature increment).

## Out of scope

- Copy / saying content changes.
- Layout restructuring of Today, Report, or Outlook beyond what the new shapes/colours require.
- New illustration or logo redesign unless contrast forces a minimal fix.
- Wear tile visual redesign (follow-up unless the tile hard-codes the old cream palette).

## Acceptance

- Light and dark both feel complete: soft tinted grounds, clear hierarchy, no stark white/black voids.
- Playful teal personality reads on first open; rough alerts remain distinct and secondary.
- Dark uses soft navy depth with mint-teal accents and no loud green panels.
- Text remains highly readable; corners feel friendly, not newspaper-cut.
- Widget no longer fights the in-app palette.
