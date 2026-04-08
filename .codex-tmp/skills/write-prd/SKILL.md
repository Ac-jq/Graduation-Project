---
name: write-prd
description: Draft, rewrite, or expand product requirements documents (PRD) in the JQPro standard structure. Use when Codex needs to turn feature ideas, project notes, meeting notes, or rough requirements into a structured PRD; align an existing PRD to the JQPro template; or complete sections such as project background and goals, role-permission matrix, module priority, detailed functional breakdown, and non-functional requirements.
---

# Write PRD

Use this skill to produce Chinese-language PRDs that follow the structure in `assets/prd-template.md` and the section contract in `references/prd-structure.md`.

## Workflow

1. Gather the source material.
- Read user notes, existing PRDs, meeting minutes, feature lists, role definitions, and policy constraints.
- Treat any user-provided PRD file as the primary source of truth for terminology and section order.
- Continue with reasonable assumptions when information is missing, but mark them as `待确认` instead of hiding the gap.

2. Choose the scaffold.
- Reuse the exact heading order and table shape from the provided PRD when one exists.
- Otherwise start from `assets/prd-template.md`.
- Keep the five top-level sections in the bundled template unless the user explicitly requests a different format.

3. Fill the document from top to bottom.
- Write section `1. 项目背景与目标` first to anchor scope, audience, and expected value.
- Keep section `2. 角色权限矩阵` as a table, not prose.
- Keep section `3. 模块优先级划分` as a table with explicit `P0/P1/P2` priorities.
- In section `4. 详细功能点拆分`, create one subsection per major module and order them by priority.
- In section `5. 非功能性需求`, define UX, security, privacy, and any project-specific constraints that affect acceptance.

4. Normalize the wording.
- Write in Chinese unless the user asks for another language.
- Keep the tone product-facing. Describe goals, roles, rules, flows, constraints, and outcomes instead of implementation details.
- Use stable naming for roles, modules, and AI capabilities across the entire document.
- Keep deferred items explicit with labels such as `P2（二期）` or `后续阶段`.

5. Check for internal consistency.
- Make sure role permissions do not conflict with privacy or data isolation rules.
- Make sure every `P0` module has a clear value statement, main business points, and business rules.
- Make sure AI-related modules define persona boundaries, data-access boundaries, and human review or confirmation where risk exists.

## Section Rules

- Read `references/prd-structure.md` when you need the exact contract for each section.
- Copy or adapt `assets/prd-template.md` when you need a ready-to-fill markdown scaffold.
- Preserve tables from the template. Do not flatten them into paragraphs unless the user explicitly asks for prose only.

## Writing Rules

- Prefer concrete statements over slogans. Replace generic claims like "improve user experience" with observable expectations, constraints, or outcomes.
- Keep one major module per subsection in section 4.
- Separate confirmed facts from inferred assumptions. Use `待确认` for unresolved details that materially affect scope, permissions, or process.
- Keep business scope and implementation scope distinct. Mention APIs, schemas, or deployment only when the user explicitly wants technical content inside the PRD.
- When rewriting an existing PRD, preserve confirmed facts and only normalize structure, clarity, and missing sections.

## Output Expectations

- Keep this top-level order by default:
1. `项目背景与目标`
2. `角色权限矩阵`
3. `模块优先级划分`
4. `详细功能点拆分`
5. `非功能性需求`
- Include a document title and horizontal rules when matching the bundled template or a user-provided PRD.
- End with assumptions only when they materially affect the PRD. Do not add process commentary or filler sections.
