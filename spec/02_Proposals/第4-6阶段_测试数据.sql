-- Stage 4-6 acceptance seed data
-- Safe to execute multiple times.

SET NAMES utf8mb4;
USE jqpro;

SET @student_id := (SELECT id FROM sys_user WHERE account = '20230001' LIMIT 1);
SET @counselor_id := (SELECT id FROM sys_user WHERE account = 'teacher01' LIMIT 1);
SET @admin_id := (SELECT id FROM sys_user WHERE account = 'admin' LIMIT 1);

INSERT IGNORE INTO counselor_student (counselor_user_id, student_user_id)
VALUES (@counselor_id, @student_id);

INSERT INTO resource_category (name, description, sort_no, status)
SELECT 'Acceptance Resource', 'Stage 4-6 acceptance resources', 990, 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM resource_category WHERE name = 'Acceptance Resource'
);
SET @acceptance_category_id := (SELECT id FROM resource_category WHERE name = 'Acceptance Resource' LIMIT 1);

INSERT INTO resource_tag (name, description)
SELECT 'stage46-acceptance', 'Stage 4-6 acceptance tag'
WHERE NOT EXISTS (
  SELECT 1 FROM resource_tag WHERE name = 'stage46-acceptance'
);
SET @acceptance_tag_id := (SELECT id FROM resource_tag WHERE name = 'stage46-acceptance' LIMIT 1);

INSERT INTO mental_resource (category_id, title, summary_text, resource_type, content_url, cover_url, status, published_at)
SELECT @acceptance_category_id, 'Acceptance Breathing Guide', 'Breathing practice resource used for stage 4-6 acceptance.', 'ARTICLE', 'https://example.com/acceptance-breathing', NULL, 'PUBLISHED', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM mental_resource WHERE title = 'Acceptance Breathing Guide'
);
SET @resource_1_id := (SELECT id FROM mental_resource WHERE title = 'Acceptance Breathing Guide' LIMIT 1);
INSERT IGNORE INTO resource_tag_relation (resource_id, tag_id) VALUES (@resource_1_id, @acceptance_tag_id);

INSERT INTO mental_resource (category_id, title, summary_text, resource_type, content_url, cover_url, status, published_at)
SELECT @acceptance_category_id, 'Acceptance Sleep Reset', 'Sleep checklist resource used for stage 4-6 acceptance.', 'ARTICLE', 'https://example.com/acceptance-sleep-reset', NULL, 'PUBLISHED', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM mental_resource WHERE title = 'Acceptance Sleep Reset'
);
SET @resource_2_id := (SELECT id FROM mental_resource WHERE title = 'Acceptance Sleep Reset' LIMIT 1);
INSERT IGNORE INTO resource_tag_relation (resource_id, tag_id) VALUES (@resource_2_id, @acceptance_tag_id);

INSERT INTO mental_resource (category_id, title, summary_text, resource_type, content_url, cover_url, status, published_at)
SELECT @acceptance_category_id, 'Acceptance Focus Toolkit', 'Study-focus resource used for stage 4-6 acceptance.', 'ARTICLE', 'https://example.com/acceptance-focus-toolkit', NULL, 'PUBLISHED', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM mental_resource WHERE title = 'Acceptance Focus Toolkit'
);
SET @resource_3_id := (SELECT id FROM mental_resource WHERE title = 'Acceptance Focus Toolkit' LIMIT 1);
INSERT IGNORE INTO resource_tag_relation (resource_id, tag_id) VALUES (@resource_3_id, @acceptance_tag_id);

INSERT INTO mental_scale (code, name, description, introduction, total_questions, page_size, low_threshold, medium_threshold, high_threshold, status)
SELECT 'ACC_STAGE456', 'Acceptance Stage 4-6 Scale', 'Acceptance scale for stage 4-6 verification', 'Please answer based on your recent three days.', 2, 2, 1, 3, 5, 'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1 FROM mental_scale WHERE code = 'ACC_STAGE456'
);
SET @scale_id := (SELECT id FROM mental_scale WHERE code = 'ACC_STAGE456' LIMIT 1);

INSERT IGNORE INTO mental_scale_question (scale_id, question_no, content, required_flag)
VALUES
(@scale_id, 1, 'During the last three days, did you often feel emotionally tense?', 1),
(@scale_id, 2, 'During the last three days, did this feeling affect your study or daily rhythm?', 1);

SET @question_1_id := (SELECT id FROM mental_scale_question WHERE scale_id = @scale_id AND question_no = 1 LIMIT 1);
SET @question_2_id := (SELECT id FROM mental_scale_question WHERE scale_id = @scale_id AND question_no = 2 LIMIT 1);

INSERT IGNORE INTO mental_scale_option (question_id, option_code, content, score, sort_no)
VALUES
(@question_1_id, 'A', 'Almost never', 0, 1),
(@question_1_id, 'B', 'Occasionally', 1, 2),
(@question_1_id, 'C', 'Frequently', 2, 3),
(@question_1_id, 'D', 'Persistently obvious', 3, 4),
(@question_2_id, 'A', 'No impact', 0, 1),
(@question_2_id, 'B', 'Slight impact', 1, 2),
(@question_2_id, 'C', 'Moderate impact', 2, 3),
(@question_2_id, 'D', 'Severe impact', 3, 4);

INSERT INTO consult_appointment_slot (counselor_user_id, start_time, end_time, status)
SELECT @counselor_id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:50:00'),
       'BOOKED'
WHERE NOT EXISTS (
  SELECT 1 FROM consult_appointment_slot
  WHERE counselor_user_id = @counselor_id
    AND start_time = TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00')
);
SET @accepted_slot_id := (
  SELECT id FROM consult_appointment_slot
  WHERE counselor_user_id = @counselor_id
    AND start_time = TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00')
  LIMIT 1
);

INSERT INTO consult_appointment (slot_id, student_user_id, counselor_user_id, anonymous_name, issue_summary, status, result_message)
SELECT @accepted_slot_id, @student_id, @counselor_id, 'Stage46-Student', 'Seeded accepted appointment for stage 4-6 acceptance.', 'ACCEPTED', 'Seeded for counselor acceptance pages.'
WHERE NOT EXISTS (
  SELECT 1 FROM consult_appointment
  WHERE slot_id = @accepted_slot_id AND student_user_id = @student_id
);

INSERT INTO consult_appointment_slot (counselor_user_id, start_time, end_time, status)
SELECT @counselor_id,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00'),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:50:00'),
       'OPEN'
WHERE NOT EXISTS (
  SELECT 1 FROM consult_appointment_slot
  WHERE counselor_user_id = @counselor_id
    AND start_time = TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00')
);

SELECT 'seed_complete' AS marker,
       @student_id AS student_user_id,
       @counselor_id AS counselor_user_id,
       @scale_id AS acceptance_scale_id,
       @resource_1_id AS resource_1_id,
       @resource_2_id AS resource_2_id,
       @resource_3_id AS resource_3_id,
       @accepted_slot_id AS accepted_slot_id;