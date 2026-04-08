CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(32) NOT NULL UNIQUE COMMENT '角色编码',
    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NULL COMMENT '角色说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    account VARCHAR(64) NOT NULL UNIQUE COMMENT '登录账号',
    password_salt VARCHAR(64) NOT NULL COMMENT '密码盐值',
    password_hash VARCHAR(128) NOT NULL COMMENT '密码摘要',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    display_name VARCHAR(64) NOT NULL COMMENT '显示名称',
    student_no VARCHAR(32) NULL COMMENT '学号',
    counselor_no VARCHAR(32) NULL COMMENT '工号',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_sys_user_role_code FOREIGN KEY (role_code) REFERENCES sys_role (code)
) COMMENT='用户表';

CREATE TABLE IF NOT EXISTS student_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL UNIQUE COMMENT '学生用户ID',
    college VARCHAR(128) NULL COMMENT '学院',
    grade VARCHAR(32) NULL COMMENT '年级',
    gender VARCHAR(16) NULL COMMENT '性别',
    phone VARCHAR(32) NULL COMMENT '联系电话',
    emergency_contact VARCHAR(64) NULL COMMENT '紧急联系人',
    emergency_phone VARCHAR(32) NULL COMMENT '紧急联系人电话',
    counselor_user_id BIGINT NULL COMMENT '负责咨询师用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_student_profile_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) COMMENT='学生档案表';

CREATE TABLE IF NOT EXISTS counselor_student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    counselor_user_id BIGINT NOT NULL COMMENT '咨询师用户ID',
    student_user_id BIGINT NOT NULL COMMENT '学生用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_counselor_student (counselor_user_id, student_user_id),
    CONSTRAINT fk_counselor_student_counselor FOREIGN KEY (counselor_user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_counselor_student_student FOREIGN KEY (student_user_id) REFERENCES sys_user (id)
) COMMENT='咨询师与学生绑定表';

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NULL COMMENT '操作人ID',
    action_code VARCHAR(64) NOT NULL COMMENT '动作编码',
    action_name VARCHAR(128) NOT NULL COMMENT '动作名称',
    detail_text VARCHAR(500) NULL COMMENT '动作详情',
    ip_address VARCHAR(64) NULL COMMENT 'IP地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_action_code (action_code)
) COMMENT='审计日志表';

CREATE TABLE IF NOT EXISTS mental_scale (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '量表编码',
    name VARCHAR(128) NOT NULL COMMENT '量表名称',
    description VARCHAR(255) NULL COMMENT '量表说明',
    introduction TEXT NULL COMMENT '作答须知',
    total_questions INT NOT NULL DEFAULT 0 COMMENT '题目总数',
    page_size INT NOT NULL DEFAULT 5 COMMENT '建议分页大小',
    low_threshold INT NOT NULL DEFAULT 0 COMMENT '低风险阈值',
    medium_threshold INT NOT NULL DEFAULT 10 COMMENT '中风险阈值',
    high_threshold INT NOT NULL DEFAULT 20 COMMENT '高风险阈值',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='心理量表表';

CREATE TABLE IF NOT EXISTS mental_scale_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    scale_id BIGINT NOT NULL COMMENT '量表ID',
    question_no INT NOT NULL COMMENT '题号',
    content VARCHAR(255) NOT NULL COMMENT '题干',
    required_flag TINYINT NOT NULL DEFAULT 1 COMMENT '是否必答',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_scale_question_no (scale_id, question_no),
    CONSTRAINT fk_scale_question_scale_id FOREIGN KEY (scale_id) REFERENCES mental_scale (id)
) COMMENT='量表题目表';

CREATE TABLE IF NOT EXISTS mental_scale_option (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    option_code VARCHAR(32) NOT NULL COMMENT '选项编码',
    content VARCHAR(255) NOT NULL COMMENT '选项内容',
    score INT NOT NULL COMMENT '选项分值',
    sort_no INT NOT NULL DEFAULT 1 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_question_option_code (question_id, option_code),
    CONSTRAINT fk_scale_option_question_id FOREIGN KEY (question_id) REFERENCES mental_scale_question (id)
) COMMENT='量表选项表';

CREATE TABLE IF NOT EXISTS mental_scale_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    scale_id BIGINT NOT NULL COMMENT '量表ID',
    user_id BIGINT NOT NULL COMMENT '作答用户ID',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '作答状态',
    answered_count INT NOT NULL DEFAULT 0 COMMENT '已作答题数',
    total_score INT NULL COMMENT '总分',
    submitted_at DATETIME NULL COMMENT '提交时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_session_user_scale_status (user_id, scale_id, status),
    CONSTRAINT fk_scale_session_scale_id FOREIGN KEY (scale_id) REFERENCES mental_scale (id),
    CONSTRAINT fk_scale_session_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) COMMENT='量表作答会话表';

CREATE TABLE IF NOT EXISTS mental_scale_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '作答会话ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    option_id BIGINT NOT NULL COMMENT '选项ID',
    score INT NOT NULL COMMENT '得分',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_session_question (session_id, question_id),
    CONSTRAINT fk_scale_answer_session_id FOREIGN KEY (session_id) REFERENCES mental_scale_session (id),
    CONSTRAINT fk_scale_answer_question_id FOREIGN KEY (question_id) REFERENCES mental_scale_question (id),
    CONSTRAINT fk_scale_answer_option_id FOREIGN KEY (option_id) REFERENCES mental_scale_option (id)
) COMMENT='量表答案表';

CREATE TABLE IF NOT EXISTS mental_scale_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT NOT NULL UNIQUE COMMENT '作答会话ID',
    scale_id BIGINT NOT NULL COMMENT '量表ID',
    user_id BIGINT NOT NULL COMMENT '学生用户ID',
    level_code VARCHAR(32) NOT NULL COMMENT '风险等级',
    total_score INT NOT NULL COMMENT '总分',
    summary_text VARCHAR(255) NOT NULL COMMENT '评分总结',
    ai_interpretation TEXT NOT NULL COMMENT 'AI解读文本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_report_user_id (user_id),
    CONSTRAINT fk_scale_report_session_id FOREIGN KEY (session_id) REFERENCES mental_scale_session (id),
    CONSTRAINT fk_scale_report_scale_id FOREIGN KEY (scale_id) REFERENCES mental_scale (id),
    CONSTRAINT fk_scale_report_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) COMMENT='量表报告表';

CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    student_user_id BIGINT NOT NULL COMMENT '学生用户ID',
    title VARCHAR(128) NOT NULL COMMENT '会话标题',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
    summary_text VARCHAR(255) NULL COMMENT '会话摘要',
    risk_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否命中风险',
    risk_level VARCHAR(16) NULL COMMENT '风险等级',
    last_active_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_ai_chat_session_student (student_user_id),
    CONSTRAINT fk_ai_chat_session_student FOREIGN KEY (student_user_id) REFERENCES sys_user (id)
) COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    sender_type VARCHAR(16) NOT NULL COMMENT '发送方类型',
    content_text TEXT NOT NULL COMMENT '消息内容',
    risk_level VARCHAR(16) NULL COMMENT '风险等级',
    hit_keywords VARCHAR(255) NULL COMMENT '命中关键词',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ai_chat_message_session (session_id),
    CONSTRAINT fk_ai_chat_message_session FOREIGN KEY (session_id) REFERENCES ai_chat_session (id)
) COMMENT='AI会话消息表';

CREATE TABLE IF NOT EXISTS consult_appointment_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    counselor_user_id BIGINT NOT NULL COMMENT '咨询师用户ID',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT '时段状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_appointment_slot_counselor (counselor_user_id),
    CONSTRAINT fk_appointment_slot_counselor FOREIGN KEY (counselor_user_id) REFERENCES sys_user (id)
) COMMENT='预约时段表';

CREATE TABLE IF NOT EXISTS consult_appointment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    slot_id BIGINT NOT NULL COMMENT '预约时段ID',
    student_user_id BIGINT NOT NULL COMMENT '学生用户ID',
    counselor_user_id BIGINT NOT NULL COMMENT '咨询师用户ID',
    anonymous_name VARCHAR(64) NOT NULL COMMENT '匿名代号',
    issue_summary VARCHAR(500) NOT NULL COMMENT '问题简介',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '预约状态',
    result_message VARCHAR(255) NULL COMMENT '处理结果',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_appointment_student (student_user_id),
    INDEX idx_appointment_counselor (counselor_user_id),
    CONSTRAINT fk_appointment_slot FOREIGN KEY (slot_id) REFERENCES consult_appointment_slot (id),
    CONSTRAINT fk_appointment_student FOREIGN KEY (student_user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_appointment_counselor FOREIGN KEY (counselor_user_id) REFERENCES sys_user (id)
) COMMENT='咨询预约表';

CREATE TABLE IF NOT EXISTS site_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    receiver_user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content_text VARCHAR(500) NOT NULL COMMENT '通知内容',
    read_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    read_at DATETIME NULL COMMENT '已读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_notification_receiver (receiver_user_id),
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_user_id) REFERENCES sys_user (id)
) COMMENT='站内通知表';

CREATE TABLE IF NOT EXISTS consult_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    appointment_id BIGINT NOT NULL UNIQUE COMMENT '预约单ID',
    student_user_id BIGINT NOT NULL COMMENT '学生用户ID',
    counselor_user_id BIGINT NOT NULL COMMENT '咨询师用户ID',
    open_time DATETIME NOT NULL COMMENT '开放时间',
    close_time DATETIME NOT NULL COMMENT '关闭时间',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '会话状态',
    sealed_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否已封存',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_chat_session_student (student_user_id),
    INDEX idx_chat_session_counselor (counselor_user_id),
    CONSTRAINT fk_chat_session_appointment FOREIGN KEY (appointment_id) REFERENCES consult_appointment (id)
) COMMENT='私密聊天室会话表';

CREATE TABLE IF NOT EXISTS consult_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    chat_session_id BIGINT NOT NULL COMMENT '聊天室会话ID',
    sender_user_id BIGINT NOT NULL COMMENT '发送用户ID',
    sender_type VARCHAR(16) NOT NULL COMMENT '发送方类型',
    content_cipher_text TEXT NOT NULL COMMENT '加密消息内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_chat_message_session (chat_session_id),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (chat_session_id) REFERENCES consult_chat_session (id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_user_id) REFERENCES sys_user (id)
) COMMENT='私密聊天室消息表';

CREATE TABLE IF NOT EXISTS resource_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL UNIQUE COMMENT '分类名称',
    description VARCHAR(255) NULL COMMENT '分类说明',
    sort_no INT NOT NULL DEFAULT 99 COMMENT '排序号',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '分类状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='资源分类表';

CREATE TABLE IF NOT EXISTS resource_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL UNIQUE COMMENT '标签名称',
    description VARCHAR(255) NULL COMMENT '标签说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='资源标签表';

CREATE TABLE IF NOT EXISTS mental_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    title VARCHAR(128) NOT NULL COMMENT '资源标题',
    summary_text VARCHAR(500) NOT NULL COMMENT '资源简介',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型',
    content_url VARCHAR(500) NOT NULL COMMENT '资源地址',
    cover_url VARCHAR(500) NULL COMMENT '封面地址',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '资源状态',
    published_at DATETIME NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_resource_category (category_id),
    INDEX idx_resource_status (status),
    CONSTRAINT fk_resource_category FOREIGN KEY (category_id) REFERENCES resource_category (id)
) COMMENT='心理资源表';

CREATE TABLE IF NOT EXISTS resource_tag_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    UNIQUE KEY uk_resource_tag_relation (resource_id, tag_id),
    CONSTRAINT fk_resource_tag_relation_resource FOREIGN KEY (resource_id) REFERENCES mental_resource (id),
    CONSTRAINT fk_resource_tag_relation_tag FOREIGN KEY (tag_id) REFERENCES resource_tag (id)
) COMMENT='资源标签关联表';

CREATE TABLE IF NOT EXISTS resource_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    student_user_id BIGINT NOT NULL COMMENT '学生用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_resource_favorite (resource_id, student_user_id),
    INDEX idx_resource_favorite_student (student_user_id),
    CONSTRAINT fk_resource_favorite_resource FOREIGN KEY (resource_id) REFERENCES mental_resource (id),
    CONSTRAINT fk_resource_favorite_student FOREIGN KEY (student_user_id) REFERENCES sys_user (id)
) COMMENT='资源收藏表';

CREATE TABLE IF NOT EXISTS resource_view_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    student_user_id BIGINT NOT NULL COMMENT '学生用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_resource_view_resource (resource_id),
    INDEX idx_resource_view_student (student_user_id),
    CONSTRAINT fk_resource_view_resource FOREIGN KEY (resource_id) REFERENCES mental_resource (id),
    CONSTRAINT fk_resource_view_student FOREIGN KEY (student_user_id) REFERENCES sys_user (id)
) COMMENT='资源浏览日志表';

CREATE TABLE IF NOT EXISTS admin_ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    admin_user_id BIGINT NOT NULL COMMENT '管理员用户ID',
    instruction_text VARCHAR(500) NOT NULL COMMENT '原始指令',
    task_type VARCHAR(32) NULL COMMENT '任务类型',
    parse_status VARCHAR(32) NOT NULL DEFAULT 'NEED_MORE_INFO' COMMENT '解析状态',
    confirm_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '确认状态',
    execute_status VARCHAR(32) NOT NULL DEFAULT 'WAITING' COMMENT '执行状态',
    summary_text VARCHAR(500) NULL COMMENT '任务摘要',
    failure_reason VARCHAR(500) NULL COMMENT '失败原因',
    confirmed_at DATETIME NULL COMMENT '确认时间',
    executed_at DATETIME NULL COMMENT '执行时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_admin_ai_task_admin (admin_user_id),
    CONSTRAINT fk_admin_ai_task_admin FOREIGN KEY (admin_user_id) REFERENCES sys_user (id)
) COMMENT='管理员AI任务表';

CREATE TABLE IF NOT EXISTS admin_ai_task_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型',
    target_id BIGINT NULL COMMENT '目标ID',
    target_label VARCHAR(128) NOT NULL COMMENT '目标标识',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型',
    field_name VARCHAR(64) NOT NULL COMMENT '字段名',
    old_value VARCHAR(255) NULL COMMENT '旧值',
    new_value VARCHAR(255) NULL COMMENT '新值',
    sort_no INT NOT NULL DEFAULT 1 COMMENT '执行顺序',
    execute_status VARCHAR(32) NOT NULL DEFAULT 'WAITING' COMMENT '执行状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_admin_ai_task_item_task (task_id),
    CONSTRAINT fk_admin_ai_task_item_task FOREIGN KEY (task_id) REFERENCES admin_ai_task (id)
) COMMENT='管理员AI任务明细表';
