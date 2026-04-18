# Page Archetypes

## 1. Form, List, or Single Interaction

Use this archetype for scale answering, slot selection, focused forms, or any page where one action dominates.

Reference files:

- `frontend/src/views/student/StudentScaleIntroRoute.vue`
- `frontend/src/views/student/StudentAssessmentSessionRoute.vue`
- `frontend/src/views/student/StudentAppointmentSlotRoute.vue`

Apply these cues:

- Center one floating glass card on the page.
- Keep the main container roughly `max-width: 680px` to `820px`.
- Use large serif titles and generous vertical spacing.
- Turn options into large border-light blocks instead of native radio or checkbox controls.
- Show selection through deep ink background inversion.
- Add subtle horizontal or vertical motion on hover and selection.

Frozen references in this group:

- `frontend/src/views/student/StudentScaleIntroRoute.vue`
- `frontend/src/views/student/StudentAssessmentSessionRoute.vue`

## 2. Gallery or Resource Library

Use this archetype for favorites, curated resources, media collections, and image-first browsing surfaces.

Reference files:

- `frontend/src/views/student/StudentFavoriteRoute.vue`
- `frontend/src/views/student/StudentResourceListRoute.vue`

Apply these cues:

- Use an open grid instead of rigid cards with heavy frames.
- Favor 16:9 covers with hover scale on the media itself.
- Keep metadata hierarchy obvious: category, title, summary, then quiet stats.
- Downplay destructive or secondary actions.
- Let the cover image and title carry the page, not decorative borders.

Frozen references in this group:

- `frontend/src/views/student/StudentFavoriteRoute.vue`
- `frontend/src/views/student/StudentResourceListRoute.vue`

## 3. Editorial Detail or Long Reading

Use this archetype for article pages, report details, resource details, and other long-form content surfaces.

Reference file:

- `frontend/src/views/student/StudentResourceDetailRoute.vue`

Apply these cues:

- Prefer unboxed magazine layout over stacked cards.
- Use large title treatment with clean spacing above and below.
- Keep body copy on warm paper-like backgrounds.
- Use sparse metadata and thin separators only where necessary.
- Let typography, whitespace, and image placement define sections.

Frozen reference in this group:

- `frontend/src/views/student/StudentResourceDetailRoute.vue`

## 4. Workspace Shell and Home

Use this archetype for cross-role layout shells, dashboards, and role home pages.

Reference files:

- `frontend/src/layouts/RoleWorkspaceLayout.vue`
- `frontend/src/views/student/StudentHomeRoute.vue`

Apply these cues:

- Keep a calm left navigation rail with soft cards and generous spacing.
- Use a central stage instead of dense dashboard columns.
- Let the home hero feel editorial and atmospheric, with one memorable primary action.
- Share one product language across student, counselor, and admin, then shift tone by palette and content density.
- Never fall back to stiff admin-dashboard widgets, boxed tables, or cramped KPI walls.

Frozen reference in this group:

- `frontend/src/views/student/StudentHomeRoute.vue`