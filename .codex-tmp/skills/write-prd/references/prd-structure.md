# PRD Structure Contract

Use this file when you need the exact section-by-section structure that matches the current JQPro PRD style.

## 1. 项目背景与目标

Write this section first. It frames why the product exists and what value it should create.

### 1.1 项目背景

- Explain the real-world problem, current friction, and why the current way of working is insufficient.
- Keep the description grounded in the target environment, not generic market slogans.

### 1.2 产品目标

- State what the product changes for the core stakeholders.
- Prefer separate bullets for each stakeholder group when the product serves multiple parties.
- Typical pattern:
  - End users
  - Operators or professionals
  - Organization or management side

## 2. 角色权限矩阵

Keep this section as a Markdown table.

Required columns:

| 角色名称 | 使用终端 | 核心目标 | 权限边界 |
| :--- | :--- | :--- | :--- |

Rules:

- Use one row per role.
- State both what the role needs to do and what the role is explicitly forbidden from doing.
- When privacy is important, make the access boundary explicit in the table instead of leaving it to later sections.

## 3. 模块优先级划分

Keep this section as a Markdown table.

Required columns:

| 模块名称 | 优先级 | 核心说明 |
| :--- | :--- | :--- |

Priority convention:

- `P0`: core path or launch-blocking capability
- `P1`: important but not required for first delivery
- `P2`: deferred, enhanced, or hidden support capability

Rules:

- Make every module name stable enough to reuse in section 4.
- The priority reason must be understandable from the description, not implied.

## 4. 详细功能点拆分

Create one subsection per major module. Keep the module order aligned with section 3.

Subsection title pattern:

```markdown
### 4.x 模块名称 (Px)
```

Recommended content contract:

- `功能说明：` explain the module's role in the overall product.
- `详细业务点：` list the core user flows, features, or capability slices.
- `业务规则：` state permissions, review gates, status transitions, thresholds, or hard constraints.
- `前/后置条件：` add only when the workflow depends on login state, prior submission, generated artifacts, or downstream archiving.

Use this section to describe product behavior, not implementation tasks.

### AI-related module rules

When a module includes AI behavior, explicitly define:

- persona or interaction style
- data it can read
- data it cannot read
- whether human review or second confirmation is required before side effects happen

## 5. 非功能性需求

Include this section even when the user only talks about features. It prevents the PRD from being one-dimensional.

Default subsections:

- `5.1 界面与交互体验 (UX/UI)`
- `5.2 信息安全与权限管控`
- `5.3 隐私保护机制`

Add more when the project requires them, for example:

- performance and response time
- compatibility and terminal adaptation
- compliance or auditability
- observability or availability

Rules:

- Write requirements as constraints that can be reviewed, checked, or argued about.
- Do not hide critical access-control rules only in section 5 if they should already appear in the permission matrix.

## Gap Handling

- If the user gives incomplete information, make the best reasonable assumption and mark it as `待确认`.
- If the user provides a real project PRD, mirror its terminology and heading style before using the generic template.
- If a requirement belongs to a future phase, mark the phase explicitly instead of mixing it into the current scope.
