# Visual Rules

## Design Attitude

Build for "minimalist, editorial, calming". The interface should feel like a quiet magazine spread, not a back-office console.

Before writing CSS, mentally check:

- Remove extra lines.
- Increase whitespace.
- Increase title scale.
- Optimize for immersive reading and calm interaction.

## Do Not Use

- Edge-to-edge main content without `max-width` and centering.
- Pure black text such as `#000000`.
- Hard borders such as `1px solid #ccc`.
- Native `<table>` layouts for page-level data.
- Dense dashboard chrome, cramped metric tiles, or visually noisy control bars.

## Must Use

- Large page padding such as `padding: 4rem` and generous gaps such as `gap: 2rem`.
- Serif display typography:
  - `font-family: 'Noto Serif SC', serif;`
  - `font-weight: 600;`
- Sans-serif body and numeric typography:
  - `font-family: 'Manrope', sans-serif;`
- Responsive display scale:
  - `font-size: clamp(2rem, 4vw, 3.5rem);`

## Preferred Palette

- Primary ink: `#1e2821`
- Deep ink: `#2a362e`
- Secondary copy: `#5c6b60`
- Muted copy: `#7b8c80`
- Warm paper: `#fcfbf9`
- Quiet background: `#f4f6f4`

## Glass Material

Use this family when a page needs an enclosed card or floating module:

```css
background: linear-gradient(145deg, rgba(255, 255, 255, 0.75), rgba(248, 246, 242, 0.85));
backdrop-filter: blur(24px);
box-shadow: 0 40px 80px rgba(54, 66, 58, 0.06);
border: 1px solid rgba(255, 255, 255, 0.8);
```

## Interaction Rules

- Use `transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);` on all buttons, cards, and primary click targets.
- Hover states should lift with `transform: translateY(-4px);` or a small lateral motion when the reference archetype calls for it.
- Strengthen hover states with deeper shadow; do not rely on color-only feedback.
- Replace native radio and checkbox visuals with custom selectable blocks when the page archetype is form or assessment driven.

## Layout Rules

- Student-facing forms should usually sit in a centered floating card around `max-width: 680px` to `820px`.
- Gallery and library pages should use open grid layouts with large 16:9 covers and softened secondary actions.
- Long-form detail pages should avoid wrapping prose in boxes; use typography and spacing to create hierarchy.
- Shared workspace shells should preserve large side breathing room, soft navigation cards, and a calm central stage.
