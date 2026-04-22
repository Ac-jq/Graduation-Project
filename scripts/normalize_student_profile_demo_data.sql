-- 学生基础测试数据清洗脚本。
-- 仅用于本地演示/测试数据：统一性别、随机分配年级和学院。

START TRANSACTION;

UPDATE student_profile sp
JOIN sys_user su ON su.id = sp.user_id
SET
    sp.gender = CASE
        WHEN LOWER(TRIM(COALESCE(sp.gender, ''))) IN ('male', 'm', 'man', 'boy', '男') THEN '男'
        WHEN LOWER(TRIM(COALESCE(sp.gender, ''))) IN ('female', 'f', 'woman', 'girl', '女') THEN '女'
        ELSE ELT(FLOOR(1 + RAND() * 2), '男', '女')
    END,
    sp.grade = ELT(FLOOR(1 + RAND() * 4), '2022', '2023', '2025', '2026'),
    sp.college = ELT(
        FLOOR(1 + RAND() * 4),
        '计算机科学与技术学院',
        '人工智能学院',
        '医学院',
        '法学院'
    )
WHERE su.role_code = 'STUDENT';

COMMIT;
