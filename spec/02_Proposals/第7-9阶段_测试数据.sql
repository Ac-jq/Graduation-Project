-- Phase 7-9 acceptance seed data
USE jqpro;

INSERT INTO sys_user (account, password_salt, password_hash, role_code, real_name, display_name, student_no, status)
SELECT '20230002', src.password_salt, src.password_hash, 'STUDENT', '阶段79隔离学生', 'Stage79-Unbound', '20230002', 'ACTIVE'
FROM sys_user src
WHERE src.account = '20230001'
  AND NOT EXISTS (SELECT 1 FROM sys_user WHERE account = '20230002');

INSERT INTO student_profile (user_id, college, grade, gender, phone, emergency_contact, emergency_phone)
SELECT u.id, 'Software College', '2023', 'Female', '13800138002', 'Parent Li', '13900139002'
FROM sys_user u
WHERE u.account = '20230002'
  AND NOT EXISTS (SELECT 1 FROM student_profile sp WHERE sp.user_id = u.id);

DELETE cs FROM counselor_student cs
JOIN sys_user su ON su.id = cs.student_user_id
WHERE su.account = '20230002';

INSERT INTO resource_category (name, description, sort_no, status)
SELECT 'Stage79 Acceptance', 'Acceptance data for phase 7-9', 79, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM resource_category WHERE name = 'Stage79 Acceptance');

INSERT INTO resource_tag (name, description)
SELECT 'stage79', 'Acceptance tag for phase 7-9'
WHERE NOT EXISTS (SELECT 1 FROM resource_tag WHERE name = 'stage79');

INSERT INTO mental_resource (category_id, title, summary_text, resource_type, content_url, status, published_at)
SELECT rc.id,
       'Stage79 Acceptance Resource Seed',
       'Seeded published resource used by phase 7-9 recommendation and security acceptance.',
       'ARTICLE',
       'https://example.com/stage79-seed',
       'PUBLISHED',
       NOW()
FROM resource_category rc
WHERE rc.name = 'Stage79 Acceptance'
  AND NOT EXISTS (SELECT 1 FROM mental_resource WHERE title = 'Stage79 Acceptance Resource Seed');

INSERT INTO resource_tag_relation (resource_id, tag_id)
SELECT mr.id, rt.id
FROM mental_resource mr
JOIN resource_tag rt ON rt.name = 'stage79'
WHERE mr.title = 'Stage79 Acceptance Resource Seed'
  AND NOT EXISTS (
      SELECT 1 FROM resource_tag_relation rtr
      WHERE rtr.resource_id = mr.id AND rtr.tag_id = rt.id
  );
