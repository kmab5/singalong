# p0 visual baseline

Status: implemented in Compose source; preview rendering and screenshots deferred at the user's request.

The subject is reading lyrics while another app plays music. Original text is the primary content. The audience is the owner using a phone; the primary action is opening a small lyric panel without replacing the player.

## tokens and type

Source: canonical `brand-kit/02_COLOR_SYSTEM/design-tokens.json` in the KMAB repository. Root-level legacy Space Grotesk guidance is not used.

| Role | Token |
| --- | --- |
| Background | `#0E0E11` |
| Panel | `#17171B` |
| Raised control | `#1E1E24` |
| Main text | `#EDEBE4` |
| Secondary text | `#9A9AA6` |
| Primary action/cue | `#B24BFF` |
| Small purple text | `#D49AFF` |
| Connected status | `#3DE08A` |

Inter variable uses optical size 14 for body/UI and 32 for display; weights 400/500/600/700 are explicitly selected. PT Mono regular is reserved for short status/timing labels. Native script fallback remains available. UI controls use lowercase; incoming metadata is not modified.

## layout

Now uses a quiet header, a typographic introduction, an actual preview panel, one primary action and a media-connection section. A permission/settings screen explains each grant. Library/history intentionally show honest empty states because persistence is not part of P0. No fake listening statistics, downloaded songs or album covers appear.

The panel pairs a narrow purple cue with the current original lyric. When bilingual mode is shown in the design gallery, the English meaning sits underneath in a smaller readable style. All sample copy is original for this project and labelled as a layout preview. The distinction is visible both in-app and in the floating window.

```text
now                              floating panel
λ singalong              settings ┌──────────────────────────────┐
                                 │ drag  layout preview   close │
connection state                 │                              │
intro                            │ ▏ original line              │
                                 │ ▏ continued                  │
compact panel preview            │   English meaning            │
[ open floating preview ]        │                              │
P0 availability note             │ original sample               │
                                 └──────────────────────────────┘
player connection
live metadata / access guidance
[ start / stop tracking ]

now       library       history
```

## signature and self-review

The purple cue is the one expressive device. It identifies the active lyric pair rather than functioning as decoration. The KMAB lambda is used only as the brand mark. Album-driven colors, animated backgrounds and decorative audio visualizers were excluded because they compete with reading.

The ui-ux-pro-max entertainment query suggested a generic video-led site and alternate typography. Those outputs were rejected for this native utility; native touch, accessibility and layout guidance were retained. frontend-design informed the typography-led hierarchy and single signature motif.

## source-level accessibility provisions

- Buttons target at least 48 dp and use native interaction feedback.
- Full-screen content scrolls and respects scaffold/system-bar insets.
- Essential text uses main/muted colors, not the dim brand token.
- Font sizes use scalable sp and text can wrap.
- Icons have action descriptions where needed; decorative icons have none.
- Overlay handle includes accessible up/down actions in addition to dragging.
- No continuous/decorative animation or automatic spoken lyric announcements.

Outstanding rendered checks: small-phone and landscape layouts, very large font scaling, TalkBack traversal, mixed/RTL scripts, contrast on actual composed windows and cutout/rotation handling. P3 adds full overlay sizing/geometry persistence and the minimize state machine. Do not report these checks as passed before rendering on a device.
