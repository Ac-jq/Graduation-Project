-- Normalize demo accounts in sys_user.
-- Protected accounts: admin, teacher01. This script never updates them.

START TRANSACTION;

-- Move target rows to temporary unique accounts first to avoid account UNIQUE conflicts.
UPDATE sys_user
SET account = CONCAT('__cleanup_', id)
WHERE account NOT IN ('admin', 'teacher01')
  AND account IN (
    '20230001',
    '123123',
    '20230002',
    '20209998',
    'teacher164720',
    'c_t009',
    'c_t0413195503',
    'counselor20260407193027',
    'counselor20260407193113',
    'counselor20260407205242',
    'counselor20260407212002',
    'counselor20260407212232'
  );

-- Precise required replacement: account 20230001 -> 20220353.
UPDATE sys_user
SET
  account = '20220353',
  student_no = '20220353',
  counselor_no = NULL,
  real_name = '贾琼',
  display_name = '向日葵同学'
WHERE account = CONCAT('__cleanup_', id)
  AND role_code = 'STUDENT'
  AND id = (
    SELECT target_id FROM (
      SELECT id AS target_id
      FROM sys_user
      WHERE account = CONCAT('__cleanup_', id)
        AND student_no = '20230001'
      LIMIT 1
    ) AS target
  );

UPDATE sys_user
SET account = 'student01', student_no = '20220001', counselor_no = NULL, real_name = '李静', display_name = '知心学姐'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'STUDENT' AND student_no = '20220353';

UPDATE sys_user
SET account = 'student02', student_no = '20220002', counselor_no = NULL, real_name = '王雨晴', display_name = '薄荷同学'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'STUDENT' AND student_no = '20230002';

UPDATE sys_user
SET account = 'student03', student_no = '20200001', counselor_no = NULL, real_name = '赵晨', display_name = '晨光同学'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'STUDENT' AND student_no = '20209998';

UPDATE sys_user
SET account = 'counselor01', student_no = NULL, counselor_no = 'T002', real_name = '陈老师', display_name = '陈老师'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'COUNSELOR' AND counselor_no = 'TC164720';

UPDATE sys_user
SET account = 'counselor02', student_no = NULL, counselor_no = 'T003', real_name = '王建国', display_name = '王老师'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'COUNSELOR' AND counselor_no = 'T009';

UPDATE sys_user
SET account = 'counselor03', student_no = NULL, counselor_no = 'T004', real_name = '刘敏', display_name = '刘老师'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'COUNSELOR' AND counselor_no = 'T0413195503';

UPDATE sys_user
SET account = 'counselor04', student_no = NULL, counselor_no = 'T005', real_name = '周宁', display_name = '周老师'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'COUNSELOR' AND counselor_no = 'C20260407193027';

UPDATE sys_user
SET account = 'counselor05', student_no = NULL, counselor_no = 'T006', real_name = '孙悦', display_name = '孙老师'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'COUNSELOR' AND counselor_no = 'C20260407193113';

UPDATE sys_user
SET account = 'counselor06', student_no = NULL, counselor_no = 'T007', real_name = '何安', display_name = '何老师'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'COUNSELOR' AND counselor_no = 'C20260407205242';

UPDATE sys_user
SET account = 'counselor07', student_no = NULL, counselor_no = 'T008', real_name = '林夏', display_name = '林老师'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'COUNSELOR' AND counselor_no = 'C20260407212002';

UPDATE sys_user
SET account = 'counselor08', student_no = NULL, counselor_no = 'T009', real_name = '许文', display_name = '许老师'
WHERE account = CONCAT('__cleanup_', id) AND role_code = 'COUNSELOR' AND counselor_no = 'C20260407212232';

-- Fallback for any remaining temporary rows. Keep the values readable and deterministic.
UPDATE sys_user
SET
  account = CONCAT('student', LPAD(id, 2, '0')),
  student_no = CONCAT('2022', LPAD(id, 4, '0')),
  counselor_no = NULL,
  real_name = COALESCE(NULLIF(real_name, ''), '学生用户'),
  display_name = COALESCE(NULLIF(display_name, ''), '学生用户')
WHERE account = CONCAT('__cleanup_', id)
  AND role_code = 'STUDENT';

UPDATE sys_user
SET
  account = CONCAT('counselor', LPAD(id, 2, '0')),
  student_no = NULL,
  counselor_no = CONCAT('T', LPAD(id, 3, '0')),
  real_name = COALESCE(NULLIF(real_name, ''), '咨询老师'),
  display_name = COALESCE(NULLIF(display_name, ''), '咨询老师')
WHERE account = CONCAT('__cleanup_', id)
  AND role_code = 'COUNSELOR';

COMMIT;
