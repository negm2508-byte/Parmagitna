---
name: PaperERP Industrial Executive
colors:
  surface: '#10131a'
  surface-dim: '#10131a'
  surface-bright: '#363941'
  surface-container-lowest: '#0b0e15'
  surface-container-low: '#191b23'
  surface-container: '#1d1f27'
  surface-container-high: '#272a32'
  surface-container-highest: '#32353d'
  on-surface: '#e1e2ec'
  on-surface-variant: '#d6c3b2'
  inverse-surface: '#e1e2ec'
  inverse-on-surface: '#2d3038'
  outline: '#9e8e7e'
  outline-variant: '#514537'
  surface-tint: '#feb95f'
  primary: '#feb95f'
  on-primary: '#472a00'
  primary-container: '#c88a35'
  on-primary-container: '#452900'
  inverse-primary: '#855400'
  secondary: '#bec6de'
  on-secondary: '#283043'
  secondary-container: '#41495c'
  on-secondary-container: '#b0b8cf'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#00ac77'
  on-tertiary-container: '#003723'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffddb7'
  primary-fixed-dim: '#feb95f'
  on-primary-fixed: '#2a1700'
  on-primary-fixed-variant: '#653e00'
  secondary-fixed: '#dae2fb'
  secondary-fixed-dim: '#bec6de'
  on-secondary-fixed: '#131b2d'
  on-secondary-fixed-variant: '#3f475a'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#10131a'
  on-background: '#e1e2ec'
  surface-variant: '#32353d'
typography:
  display-hero:
    fontFamily: IBM Plex Sans
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 44px
    letterSpacing: -0.02em
  display-hero-mobile:
    fontFamily: IBM Plex Sans
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: IBM Plex Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-lg-mobile:
    fontFamily: IBM Plex Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 30px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: IBM Plex Sans
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 28px
  headline-sm:
    fontFamily: IBM Plex Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: IBM Plex Sans
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: IBM Plex Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: IBM Plex Sans
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 16px
  label-numeric-lg:
    fontFamily: JetBrains Mono
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.02em
  label-numeric-md:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-numeric-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.03em
  label-caps:
    fontFamily: IBM Plex Sans
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.08em
spacing:
  gutter: 1rem
  gutter-compact: 0.5rem
  gutter-expanded: 1.5rem
  margin: 1.5rem
  margin-mobile: 0.75rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style
The design system fuses Swiss Grid Functionalism with Classic Industrial Luxury tailored for complex pulp, corrugation, and paper manufacturing operations. It balances the austere, disciplined density required by heavy manufacturing telemetry (reel stand tracking, basis weight profiling, slit patterns, GSM calibration) with the tactile prestige of historic papermaking mills.

The interface serves plant directors, production planners, roll quality auditors, and executive supply-chain leads across the MENA industrial corridor. It must instill unwavering control, architectural permanence, and surgical clarity under high-pressure shift turnarounds.

The visual direction rejects disposable web minimalism in favor of structured industrial prestige:
- **Swiss Precision**: Relentless strict-axis alignment, modular data grids, uncompromising hierarchy, and deliberate structural lines.
- **Classic Industrial Luxury**: Deep obsidian slates layered like heavyweight museum press-boards, calibrated with tactile kraft pulp, patinated brass, and gold leaf accents.
- **Machined Restraint**: Zero decorative fluff, zero emojis, and zero hyper-saturated novelty blurs. Every visual cue serves high-speed cognitive scanning, physical stock traceability, and continuous run reliability.

## Colors
The palette is rooted in dark-mode industrial foundry environments—engineered to eliminate screen glare across shop-floor command stations and executive boardrooms while preserving contrast ratios compliant with WCAG AAA standards.

### Palette Architecture
- **Obsidian Base (`#0A0D14`)**: Deepest background tone evoking raw carbon plates and dark machine beds.
- **Graphite Panel Slate (`#121722`)**: Neutral container surface evoking heavyweight pressed paperboard, providing soft separation without elevation glow.
- **Structural Razor Border (`#1E2638`)**: Uniform 1px technical demarcation simulating laser-ruled ledger lines.
- **Kraft Gold & Brass Core (`#C88A35` / `#936021`)**: Primary identity tone derived from raw kraft paper fiber, unbleached linerboard, and machined brass measurement calipers. Used for active selection states, focus rings, primary actions, and reel batch identifiers.
- **Industrial Telemetry Status System**:
  - *Released / QA Cleared*: Deep Sage & Muted Emerald (`#10B981` surface accent, `#047857` container tint).
  - *Running / Warning / Oven Heat*: Industrial Amber (`#F59E0B`).
  - *Tear / Web Break / Critical Downtime*: Oxide Red (`#EF4444`).
  - *Calendering / Cold Idle*: Steel Cyan (`#38BDF8`).

Apply status tones at a subdued 15% fill with solid 1px perimeter definition to prevent visual fatigue during 12-hour operator shifts.

## Typography
The system employs an authoritative bilingual typographic foundation, built for RTL-first Arabic localization (via IBM Plex Sans Arabic / Cairo fallback) with matched Latin technical typesetting.

### Typographic Principles
- **Dual-Engine Balance**: Natural, unembellished readability for industrial terminology in Arabic, complemented by monospaced technical figures.
- **Tabular Rigor**: All quantitative industrial measurements (GSM basis weights, deckle trim widths, reel diameters, linear speed m/min, moisture %, caliper microns) must strictly render in `JetBrains Mono` with fixed tabular numerals (`tnum`, `zero`) to prevent character shifting during real-time telemetry refreshes.
- **Hierarchy & Case**: Micro-labels, parameter categories, and machine states use `label-caps` (in Latin contexts) or balanced semi-bold weighted anchors (in Arabic) to establish clear scan lines along ledger grids.
- **Directional Cohesion**: All typographic hierarchy translates symmetrically between RTL and LTR viewports. Numerical formulas and unit suffixes maintain strict standard sequence (`g/m²`, `mm`, `kg`, `kN/m`).

## Layout & Spacing
The layout adheres strictly to Swiss Functionalist grid theory: modular column divisions, persistent horizontal baseline rules, and balanced distribution of industrial data densities.

### Layout Mechanics
- **Grid Architecture**: Standard 12-column or 16-column layout engines. Dense telemetry dashboards utilize the 16-column matrix with `gutter-compact` (8px) for high data packing without visual clipping.
- **RTL Fluidity**: All grid rails reverse logically for Arabic layouts—visual hierarchy flows from top-right to bottom-left, with persistent machine navigation fixed to the right canvas margin.
- **Adaptive Breakpoints**:
  - *Large Operations Walls / Triple Monitors (≥1920px)*: 16 columns, fixed 24px margins, telemetry charts stretch dynamically.
  - *Control Room Desktop (1280px - 1919px)*: 12 columns, 24px margins, standard modular card spans (3, 4, 6, 12).
  - *Rugged Plant Tablet (768px - 1279px)*: 8 columns, 16px gutters, tabular columns prioritize GSM, width, and reel status.
  - *Handheld Inspector Mobile (<768px)*: 4 columns, reflowing dense horizontal grids into vertically stacked ledger inspection cards.

## Elevation & Depth
Depth in this system avoids playful floating clouds or dramatic blurred drop-shadows. Instead, depth is achieved through **structural surface tiers and 1px razor borders**.

### Layering Model
- **Base Bed (`#0A0D14`)**: Global machine canvas.
- **Tier 1 - Structural Ledger Surface (`#121722`)**: Work centers, machine reels, and slit-line scheduling modules. Demarcated by a 1px solid border in `#1E2638`.
- **Tier 2 - Focus & Flyout Drawers (`#182030`)**: Slitting pattern recalculators, reel spec override drawers, and popovers. Framed with a 1px border in `#2B364E` and an ambient shadow: `0 8px 24px rgba(0, 0, 0, 0.6)`.
- **Tactile Brass Accents**: Active selection or alert elevation uses a localized razor glow: `box-shadow: inset 0 0 0 1px #C88A35, 0 0 12px rgba(200, 138, 53, 0.15)`.

## Shapes
The shape language is strictly **Sharp (0px roundedness)** to honor classic industrial drafting blueprints, heavy guillotine paper cutters, and modular machine enclosures.

- Every button, input field, data grid cell, modal surface, and indicator badge uses razor-sharp 90-degree corners.
- Visual interest and touch safety are reinforced through precise 1px micro-borders, high-contrast interior padding, and deliberate linear separators rather than rounded corner radii.
- Badge counters and status pills are executed as crisp, rectangular ledger stamps with structured monospaced numbers.

## Components

### Buttons
- **Primary Industrial Button**: Background `#C88A35`, foreground `#0A0D14`, crisp 0px corners, bold weight. Hover state: `#DB9E48`. Active state: `#936021`.
- **Secondary Machine Button**: Background `#121722`, border 1px solid `#1E2638`, foreground `#E2E8F0`. Hover state: border color moves to `#C88A35` with subtle brass text tint.
- **Critical Abort Button**: Background `#EF4444` at 15% opacity, border 1px solid `#EF4444`, foreground `#FCA5A5`. Active state: solid `#EF4444` background with `#0A0D14` text.
- **RTL Alignment**: Icons with directional meaning (arrows, ejectors, step markers) mirror automatically across RTL contexts.

### Data Grids & Ledgers (Core ERP Component)
- **Cell Structure**: 36px compact row heights for data density; 44px rows for touch inspection consoles.
- **Borders**: Continuous 1px horizontal and vertical rules in `#1E2638`.
- **Headers**: Background `#0E131C`, uppercase `label-caps` font, muted slate text `#64748B`.
- **Numeric Alignment**: Numbers strictly right-aligned in LTR, left-aligned in RTL, always styled in `label-numeric-md` with `JetBrains Mono`.

### Input Fields & Caliper Selectors
- **Container**: Background `#0E131C`, 1px border `#1E2638`, height 36px, zero corner radius.
- **Focus State**: 1px solid `#C88A35` border with zero blur spread.
- **Unit Affix**: Suffix badges (`GSM`, `μm`, `kN/m`, `t/h`) locked into fixed-width dark graphite segments with monospaced labels.

### Status Indicators & Badges
- **Form**: Sharp rectangular stamps, padding `2px 6px`.
- **Released Batch Badge**: Surface `#047857` (20% opacity), border 1px solid `#10B981`, text `#34D399` in `label-numeric-sm`.
- **Hold / Rewind Alert**: Surface `#936021` (20% opacity), border 1px solid `#F59E0B`, text `#FCD34D`.
- **Zero-Emoji Mandate**: Status indicators strictly rely on geometric vector strokes (1.5px continuous path) or two-letter state codes (`RL` for Released, `HL` for Hold, `RJ` for Rejected).

### Cards & Telemetry Pods
- **Surface**: `#121722` solid background with sharp 1px border `#1E2638`.
- **Header Structure**: Border-bottom 1px solid `#1E2638`, containing title in `headline-sm` alongside real-time monospaced reel timestamps.
- **Telemetry Readout**: Prominent monospaced value (`label-numeric-lg`) over miniature baseline sparklines rendered in `#C88A35` with no area-fill gradients.

### Vector Iconography Specification
- Stroke width: Strictly 1.5px uniform.
- Endcaps and joins: Mitered or squared corners matching the 0px shape system.
- Scale: Standard bounding box 16x16px or 20x20px.
- Subject matter: Technical drafting motifs (calipers, rolls, slitters, pressure gauges, shear blades, pallet grids) rather than generic consumer abstractions.