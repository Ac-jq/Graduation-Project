---
name: jqpro-editorial-ui
description: Apply JQPro's project-specific minimalist editorial UI language to Vue pages, layouts, components, and CSS. Use when building or restyling any frontend surface in this repo, especially files under frontend/src/views, frontend/src/layouts, frontend/src/components, frontend/src/styles, or shared theme tokens. Trigger this skill for student, counselor, admin, and public screens that should feel minimal, magazine-like, calming, and clearly unlike a conventional admin dashboard.
---

# JQPro Editorial UI

Use this skill as the only frontend UI rule set for JQPro tasks. Do not treat the global `frontend-design` skill as an additional project requirement.

## Workflow

1. Identify the page archetype before coding.
- Read `references/page-archetypes.md` and choose the closest pattern.

2. Respect frozen reference pages.
- Treat the frozen student pages listed below as approved visual baselines.
- Do not modify them unless the user explicitly asks to change those exact files.

3. Keep product boundaries stable.
- Prefer editing `frontend/src/views/*`, `frontend/src/layouts/*`, `frontend/src/components/*`, `frontend/src/styles/*`, and theme files.
- Avoid changing route paths, API semantics, store contracts, auth flow, or websocket conventions unless the user explicitly asks.

4. Establish shared visual tokens before broad refactors.
- When touching multiple screens, unify spacing, typography, color, radius, shadow, and motion instead of styling each page in isolation.

5. Compose with restraint.
- Remove extra lines.
- Increase whitespace.
- Increase title scale.
- Favor reading rhythm and emotional calm over dense dashboards.

## Frozen Reference Pages

- `frontend/src/views/student/StudentHomeRoute.vue`
- `frontend/src/views/student/StudentFavoriteRoute.vue`
- `frontend/src/views/student/StudentResourceListRoute.vue`
- `frontend/src/views/student/StudentResourceDetailRoute.vue`
- `frontend/src/views/student/StudentScaleIntroRoute.vue`
- `frontend/src/views/student/StudentAssessmentSessionRoute.vue`

## Hard Rules

- Constrain content with `max-width` plus centering; never let primary content hug the viewport edge.
- Avoid pure black text and hard visible borders.
- Do not use native data tables for page-level data presentation; use grid, flex, and card composition.
- Use `'Noto Serif SC', serif` for titles and `'Manrope', sans-serif` for body, metadata, and numeric content.
- Use `clamp()` for major headings.
- Use soft translucent gradients, blur, and low-contrast shadows when a card enclosure is needed.
- Give interactive elements `transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);`.
- Add lift on hover through translation and shadow, not color-only changes.
- Preserve the same breathing room and hierarchy on mobile.

## Reference Files

- Read `references/visual-rules.md` for mandatory CSS rules, palette guidance, and banned patterns.
- Read `references/page-archetypes.md` for the exact page types and code references to mirror.

## Role Tone

- Keep student pages calming, soft, and companion-like.
- Keep counselor pages ordered and professional without becoming stiff.
- Keep admin pages structured and governance-oriented without falling back to generic dashboard chrome.