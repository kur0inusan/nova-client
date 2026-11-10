# Handoff: Nova Client ClickGUI

## Overview
In-game ClickGUI for "Nova Client", a Minecraft PvP client. Two alternative layouts share one module/setting model:
- **Layout A · Panels**: draggable floating category panels (classic hacked-client style).
- **Layout B · Window**: single centered window with sidebar + module cards + settings page (modern PvP-client style).

Shared features: search, favorites, theme switching (5 accents), background blur, HUD editor (drag HUD elements), keybinds, toggle notifications.

## About the Design Files
`Nova ClickGUI.dc.html` is a **design reference built in HTML** — an interactive prototype showing intended look and behavior, not production code. Recreate it in the client's real environment (e.g. Fabric/Forge mod rendering via `DrawContext` / NanoVG / Skija, or whatever UI layer the codebase already uses), following its existing patterns. Open the HTML in a browser to try every interaction.

## Fidelity
**High-fidelity.** Colors, type, spacing, radii and interactions are final. Recreate pixel-accurately at GUI scale 1 (values below are CSS px at 1:1 screen pixels).

## Screens / Views

### 0. Background & global layers (bottom → top)
1. Game world (prototype uses a striped placeholder / dropped screenshot).
2. Crosshair (only when GUI closed): 18×2 + 2×18 px, `#F5F5F5` @ 80%.
3. HUD layer (always visible; see HUD).
4. GUI overlay when open: full-screen `#050608`-ish tint `oklch(0.1 0.01 280 / 0.4)` ≈ `rgba(8,8,12,0.4)` + backdrop blur `blurStrength` px (default 8, range 0–20, toggleable). Clicking it closes open dropdowns / cancels key listening.
5. Toasts (bottom-right, 18px inset, 6px gap, newest bottom, max 4, 1.8 s lifetime).
6. Closed hint (bottom-center 28px): "Press RSHIFT to open Nova Client" — pill `rgba(0,0,0,0.55)`, radius 6, padding 8×14, 13px; key name in accent + mono.

### 1. Layout A · Panels
**Top bar** — centered, top 22px. Radius 8, padding 7/8/7/14, gap 12, bg `#0A0B0F` @ 94%, border 1px white @ 7%.
- Logo: 10×10 square rotated 45° in accent + "NOVA" 14px/700, letter-spacing 0.12em.
- Search input 240×30, radius 6, bg `#07070A`, border white @ 8%, 13px, placeholder "Search modules…".
- "Edit HUD" button 30px tall, padding 0 12, radius 6, bg accent @ 15%, text accent 13px/600, nowrap.
- Theme dots: 5 × 14px circles, gap 6; selected has 2px `#F5F5F5` outline, offset 2.

**Panels** — Render, HUD, Misc, Settings, Favorites. Width 220, radius 6, bg `#0A0B0F` @ 95%, border white @ 7%, shadow `0 12px 32px rgba(0,0,0,0.45)`.
- Default position: y = 84, x = 20 + i·step, where `step = clamp((screenW − 256) / 4, 60, 236)` (all 5 always fit on one row; they overlap slightly on narrow screens). User drag is stored as an offset from that default.
- Header 36px: bg `#131318`, 2px bottom border accent, padding 0 8 0 12. Title 13px/700 uppercase, letter-spacing 0.1em; module count JetBrains Mono 11px `#7F8086`; collapse button 22×22 ("−" / "+"). Header drag = move panel (cursor grab).
- Body: max-height `screenH − 170`, scrolls; padding 4 0.
- Module row 28px, padding 0 4 0 12, gap 6, 13px/500:
  - Off: text `#C2C3CB`, transparent bg. On: text accent, bg accent @ 15%.
  - Right side: keybind (mono 10px `#7F8086`, hidden if NONE), star 20×20 (★ accent if fav / ☆ `#626369`), expand caret ▾/▴ 20×20.
  - Left-click = toggle. Right-click or caret = expand settings.
- Expanded settings block: bg `#07070A`, padding 10 12 12, 12px text, 1px top/bottom border white @ 4%, rows gap 11. (Controls: see "Setting controls", compact size.)
- Empty state text 12px `#7F8086`: "Star a module to pin it here" (Favorites) / "No matches".
- Footer bottom-left 20/16: mono 11px `#9D9EA5`: "Nova Client b1.0 · Left-click toggle · Right-click settings · Drag headers".

### 2. Layout B · Window
Centered window `min(940, 94vw) × min(600, 88vh)`, radius 16, bg `#08090C` @ 93%, border white @ 8%, shadow `0 30px 80px rgba(0,0,0,0.5)`. Grid: 220px sidebar | rest.

**Sidebar** — padding 20 14 16, gap 18, bg `#050608` @ 90%, right border white @ 6%.
- Logo: 12px rotated square accent + "Nova" 18/700 + "Client" 18/400 `#8E8F95`.
- Search 34px, radius 8, bg `#111116`, placeholder "Search…".
- Nav (gap 2): Favorites, Render, HUD, Misc, Settings. Item 36px, radius 8, padding 0 10, 14/500, count mono 11 `#7F8086`. Active: bg accent @ 15%, text accent; inactive text `#CCCDD4`. Search active ⇒ no item highlighted.
- Spacer, then "Edit HUD" 38px full-width, radius 8, bg accent, text `#0A0B0F` 14/600.
- Theme dots row + "v1.0" mono 10 `#626369`.

**Main — grid view**
- Header padding 22 26 14: title 22/600 (category or "Search"), sub 13 `#7F8086` ("8 modules" / `N results for “q”`).
- Card grid: `repeat(auto-fill, minmax(min(200px,100%), 1fr))`, gap 10, padding 0 26 26, vertical scroll only.
- Card: padding 14, radius 10, gap 8. Off: bg `#111115` @ 90%, border white @ 6%. On: bg `#14151F` @ 90%, border accent. Transition 150ms on border/bg.
  - Row 1: name 15/600 + star (22×22, 14px).
  - Description 12px, line-height 1.4, `#8E8F95`, min-height 34.
  - Row 3: "Settings" ghost button 26px (radius 6, border white @ 8%, 12px `#C2C3CB`) + switch 34×20.
  - Click card = toggle; Settings = open page; star = favorite (both stop propagation).

**Main — module settings page**
- Header padding 18 26, bottom border white @ 6%: back button "← {Category}" (30px, nowrap), name 20/600 + desc 12 `#7F8086`, star 30×30, master switch 40×22.
- List max-width 560, padding 8 26 26, 14px; each setting row padding 14 0 with 1px bottom border white @ 5% (Controls: large size).

### 3. HUD & HUD Editor
HUD items render for enabled HUD modules. Text pills: padding 4×9, radius 4, bg `rgba(0,0,0,0.45)` (if module's Background on), JetBrains Mono 13/600, text-shadow 0 1px 0 rgba(0,0,0,.5).
- FPS: format string, `{fps}` replaced (default "{fps} FPS"), text color setting.
- CPS: "L | R CPS" or combined "N CPS".
- Coordinates: "XYZ 128, 64, -312" + "  N" when facing direction on.
- Ping: "23 ms". Clock: 24h "HH:MM" or 12h "h:MM AM".
- Keystrokes: 3-col grid of 36px keys, gap 3, radius 4; W / A S D; optional LMB/RMB row (30px, shows CPS 10px) and space bar (16px). Unpressed `rgba(0,0,0,0.45)`, pressed = "Pressed color" setting with dark text. Reacts to real key input.
- Armor Status: 4 rows (or a row when Horizontal), each 18px item icon + durability mono 12/600; ≥100 green `oklch(0.85 0.12 150)`, <100 red `#FF7C80`.
- Potion Effects: box with effect name (colored) + time.

Default positions (px from top-left, W/H = screen): FPS 16,16 · CPS 16,50 · Coords 16,84 · Ping 16,118 · Clock W−90,16 · Potions W−170,54 · Armor W−90,max(110,H/2−70) · Keystrokes 16,max(160,H−190).

**HUD Editor** (from "Edit HUD"): GUI hidden, blur off, 40px grid lines white @ 5% over screen. Each HUD item gets 1px dashed accent outline (offset 3) and its module name above (mono 10 accent); drag to move, clamped inside screen. Top-center toolbar (radius 10, bg `#0C0D11` @ 94%, padding 8 8 8 16, gap 14): "HUD Editor" 14/600 · "Drag elements to reposition" 13 `#9D9EA5` · "Reset" ghost button · "Done" accent button. ESC or Done returns to GUI.

## Setting controls (compact A / large B)
- **Toggle**: whole row clickable. Switch 28×16 knob 12 (A) / 34×20 knob 14 (B); off `#323238`, on accent; knob `#F5F5F5`; 150ms transition.
- **Slider**: label + value (mono, accent, with unit, 1 decimal if step < 1). Track 4px (A) / 5px (B) `#323238`/`#28282E`, fill accent, thumb 10 / 14px white circle. Pointer-down anywhere on 14/18px hit area jumps + drags; values snap to step.
- **Mode (dropdown)**: button showing value + ▼/▲ (22 / 30px tall, bg `#111116`, border white @ 8%). Open → wrap of option chips below; selected chip bg accent @ 15% text accent, others bg `#14141A` text `#CCCDD4`. Only one dropdown/color picker open at a time.
- **Color**: swatch button 18 / 28px (radius 4/6, border white @ 20%). Open → swatch row of 9 circles (16 / 22px): see palette; selected has white 2px outline.
- **Keybind**: mono button (min-width 44 / 64). Click → "PRESS…" with accent border/text and accent @ 15% bg; next key assigns; ESC sets NONE. Every module has an implicit last "Keybind" setting.
- **Text**: label above input (26 / 34px, mono 11 / 13).

## Interactions & Behavior
- **Open key** (ClickGUI › Open key, default RSHIFT) toggles the GUI; also exits HUD editor.
- ESC priority: close dropdown → exit HUD editor → close GUI.
- When GUI is closed (or in HUD editor), module keybinds toggle modules.
- Toggling a module shows a toast (if Client › Toggle notifications on): 8px dot (accent if enabled, `#626369` if disabled) + name 13/600 + "Enabled"/"Disabled" `#8E8F95`; card min-width 180, radius 8, padding 10×14.
- Search filters on name + description + category (case-insensitive). A: filters every panel. B: replaces grid with results across categories.
- Theme: ClickGUI › Theme mode and the theme dots are the same value.
- Typing in text inputs must not trigger keybinds.
- Layouts A/B share all state; in the prototype a dev-only switcher sits top-right (not part of the product — pick one or make it a setting).

## State Management
- `enabled[moduleId]`, `values[moduleId.settingId]`, `binds` (stored as `moduleId.__bind`), `favorites` set.
- UI: `guiOpen`, `hudEdit`, `search`, `openDropdown` (single key), `listeningKey`, `collapsed[panel]`, `panelOffset[panel]` (A), `bCategory`, `bPage` (module id) (B), `hudPos[id]` (null = default), `pressedKeys`, `toasts`.
- Persist enabled/values/binds/favorites/panel offsets/HUD positions to the client config.

## Module catalog (defaults)
Bold = enabled by default.
- **Render**: **Fullbright** (Gamma 10, 1–15) · Zoom [bind C] (Factor 4x 2–10 step .5, Smooth camera on, Scroll to adjust on) · Motion Blur (Amount 4, 1–10) · **Hit Color** (Color red/20, Opacity 40% 0–100 step 5) · Block Outline (Color white, Width 2px 1–5 step .5, Fill faces off) · Particles (Type Always/Criticals/Sharpness, Multiplier 2x 1–10) · Time Changer (Time 6000 0–24000 step 500, Weather Clear/Rain/Thunder) · Nametags (Show own off, Text shadow on, Scale 1x 0.5–2 step .1)
- **HUD**: **FPS** (Background on, Text color white, Format "{fps} FPS") · **CPS** (Layout L | R/Combined, Background on) · **Keystrokes** (Mouse buttons on, Space bar on, Pressed color violet) · **Armor Status** (Orientation Vertical/Horizontal, Durability on) · **Coordinates** (Facing direction on, Background on) · Potion Effects (Show icons on) · Ping (Background on) · Clock (Format 24h/12h, Background on)
- **Misc**: Auto GG (Message "gg", Delay 1s 0–5 step .5) · **Toggle Sprint** [bind R] (Show status text off, Toggle sneak off) · Discord RPC (Show server on, Custom status "Playing Bedwars") · Chat Tweaks (Timestamps on, Stack duplicates on, Height 180px 60–400 step 10) · Screenshot Uploader (Copy link on, Host Imgur/Custom)
- **Settings**: **ClickGUI** (Theme Nova/Crimson/Mint/Ice/Amber, Background blur on, Blur strength 8px 0–20, Open key RSHIFT) · **Client** (Display name "Nova Client", Language English/日本語, Toggle notifications on)
- Default favorites: Zoom, Keystrokes, Hit Color.

## Design Tokens
Source values are OKLCH; hex are sRGB conversions.

Accent themes `oklch(0.74 0.16 h)` (soft variant = same @ 15% alpha):
- Nova h290 `#AB97FF` · Crimson h20 `#FF7C80` · Mint h160 `#23C987` · Ice h230 `#00BBFB` · Amber h75 `#E49900`
- Text on filled accent: `#0A0B0F`

Color-setting palette `oklch(0.72 0.17 h)`: 290 `#A58FFF` · 250 `#40A9FF` · 200 `#00C2CE` · 160 `#00C47F` · 120 `#98B305` · 75 `#E19100` · 40 `#FB794A` · 20 `#FD7277` · white `#F5F5F5`

Neutrals (hue 280):
- Surfaces: `#050608` sidebar · `#07070A` inputs/settings bg (A) · `#08090C` window · `#0A0B0F` panels/top bar/toasts · `#0C0D11` editor bar · `#111115` card · `#111116` inputs (B) · `#131318` panel header · `#14151F` card on
- Tracks/switch off: `#28282E`, `#323238`
- Text: `#EEEEF2` primary · `#CCCDD4` / `#C2C3CB` secondary · `#9D9EA5`, `#8E8F95`, `#7F8086` muted · `#626369` disabled
- Borders: white @ 4–8% (`rgba(255,255,255,.04–.08)`)

Typography: **Outfit** (400/500/600/700) for UI; **JetBrains Mono** (400/600) for values, keys, counts, HUD. Sizes: 10, 11, 12, 13, 14, 15, 18, 20, 22.

Radii: 4 (controls, HUD), 6 (panels, A buttons), 8 (top bar, B nav/buttons, toasts), 10 (cards, editor bar), 16 (window).
Shadows: panels `0 12px 32px rgba(0,0,0,.45)`; window `0 30px 80px rgba(0,0,0,.5)`; editor bar `0 10px 30px rgba(0,0,0,.4)`.
Motion: 150ms for switches/cards; everything else instant.

## Assets
No image assets. Logo mark = rotated square in accent. Armor/item icons are placeholders: use real item textures. The game background is a placeholder.

## Files
- `Nova ClickGUI.dc.html`: the full interactive prototype (template + logic class with module catalog `MODS`, `HUES`, `SW`, and `hudDefaults()`). Needs `support.js` (runtime) and `image-slot.js` next to it to open in a browser.
