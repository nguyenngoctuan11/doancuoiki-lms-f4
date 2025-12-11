
-- Online LMS Schema v2 (SQL Server / T-SQL)
-- Run on Microsoft SQL Server (2019+). All times in UTC (GETUTCDATE()).
use quanlykhoahoctructuyen
SET NOCOUNT ON;

-- Drop helper (idempotent run in dev)
-- NOTE: In production, remove drops or gate with existence checks.

-- =======================
-- Core: Users & RBAC
-- =======================

CREATE TABLE dbo.users (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  email NVARCHAR(255) NOT NULL UNIQUE,
  password_hash NVARCHAR(255) NOT NULL,
  full_name NVARCHAR(255) NOT NULL,
  avatar_url NVARCHAR(512) NULL,
  username NVARCHAR(64) NULL UNIQUE,
  bio NVARCHAR(2000) NULL,
  locale NVARCHAR(16) CONSTRAINT df_users_locale DEFAULT N'vi',
  two_factor_enabled BIT NOT NULL CONSTRAINT df_users_two_factor DEFAULT 0,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_users_status DEFAULT N'active',
  created_at DATETIME2 NOT NULL CONSTRAINT df_users_created_at DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_users_updated_at DEFAULT GETUTCDATE(),
  CONSTRAINT ck_users_status CHECK (status IN (N'active', N'blocked'))
);

CREATE TABLE dbo.roles (
  id INT IDENTITY(1,1) PRIMARY KEY,
  code NVARCHAR(64) NOT NULL UNIQUE,
  name NVARCHAR(128) NOT NULL
);

CREATE TABLE dbo.user_roles (
  user_id BIGINT NOT NULL,
  role_id INT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id)
);

CREATE TABLE dbo.permissions (
  id INT IDENTITY(1,1) PRIMARY KEY,
  code NVARCHAR(64) NOT NULL UNIQUE,
  name NVARCHAR(128) NOT NULL
);

CREATE TABLE dbo.role_permissions (
  role_id INT NOT NULL,
  permission_id INT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id),
  CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES dbo.permissions(id)
);

-- =======================
-- Catalog: Categories/Tags/Courses
-- =======================

CREATE TABLE dbo.categories (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  parent_id BIGINT NULL,
  slug NVARCHAR(128) NOT NULL UNIQUE,
  name NVARCHAR(255) NOT NULL,
  sort_order INT NOT NULL CONSTRAINT df_categories_sort DEFAULT 0,
  CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES dbo.categories(id)
);

CREATE TABLE dbo.tags (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  slug NVARCHAR(128) NOT NULL UNIQUE,
  name NVARCHAR(255) NOT NULL
);

CREATE TABLE dbo.courses (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  title NVARCHAR(255) NOT NULL,
  slug NVARCHAR(160) NOT NULL UNIQUE,
  short_desc NVARCHAR(1000) NULL,
  long_desc NVARCHAR(MAX) NULL,
  language NVARCHAR(8) NOT NULL CONSTRAINT df_courses_lang DEFAULT N'vi',
  level NVARCHAR(16) NOT NULL CONSTRAINT df_courses_level DEFAULT N'beginner',
  target_roles NVARCHAR(255) NULL,
  thumbnail_url NVARCHAR(512) NULL,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_courses_status DEFAULT N'draft',
  price DECIMAL(12,2) NULL,
  is_free BIT NOT NULL CONSTRAINT df_courses_isfree DEFAULT 0,
  publish_at DATETIME2 NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_courses_created DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_courses_updated DEFAULT GETUTCDATE(),
  CONSTRAINT ck_courses_level CHECK (
    level IN (
      N'beginner',
      N'intermediate',
      N'advanced',
      N'350+',
      N'450+',
      N'550+',
      N'650+',
      N'750+',
      N'850+',
      N'950+'
    )
  ),
  CONSTRAINT ck_courses_status CHECK (status IN (N'draft', N'published', N'archived')),
  CONSTRAINT fk_courses_creator FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.course_categories (
  course_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  PRIMARY KEY (course_id, category_id),
  CONSTRAINT fk_course_categories_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_categories_category FOREIGN KEY (category_id) REFERENCES dbo.categories(id) ON DELETE CASCADE
);

CREATE TABLE dbo.course_tags (
  course_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (course_id, tag_id),
  CONSTRAINT fk_course_tags_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_tags_tag FOREIGN KEY (tag_id) REFERENCES dbo.tags(id) ON DELETE CASCADE
);

CREATE TABLE dbo.course_instructors (
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (course_id, user_id),
  CONSTRAINT fk_course_instructors_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_instructors_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);

-- =======================
-- Content Structure: Modules/Lessons/Assets
-- =======================

CREATE TABLE dbo.modules (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title NVARCHAR(255) NOT NULL,
  sort_order INT NOT NULL CONSTRAINT df_modules_sort DEFAULT 0,
  created_at DATETIME2 NOT NULL CONSTRAINT df_modules_created DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_modules_updated DEFAULT GETUTCDATE(),
  CONSTRAINT fk_modules_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE
);

CREATE TABLE dbo.lessons (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  module_id BIGINT NOT NULL,
  title NVARCHAR(255) NOT NULL,
  type NVARCHAR(16) NOT NULL,
  duration_seconds INT NULL,
  is_preview BIT NOT NULL CONSTRAINT df_lessons_preview DEFAULT 0,
  sort_order INT NOT NULL CONSTRAINT df_lessons_sort DEFAULT 0,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_lessons_status DEFAULT N'draft',
  created_at DATETIME2 NOT NULL CONSTRAINT df_lessons_created DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_lessons_updated DEFAULT GETUTCDATE(),
  CONSTRAINT ck_lessons_type CHECK (type IN (N'video',N'article',N'quiz',N'assignment',N'scorm',N'link')),
  CONSTRAINT ck_lessons_status CHECK (status IN (N'draft', N'published')),
  CONSTRAINT fk_lessons_module FOREIGN KEY (module_id) REFERENCES dbo.modules(id) ON DELETE CASCADE
);

CREATE TABLE dbo.lesson_assets (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  lesson_id BIGINT NOT NULL,
  kind NVARCHAR(16) NOT NULL,
  url NVARCHAR(1024) NOT NULL,  -- S3/Cloud URL
  bytes BIGINT NULL,
  mime_type NVARCHAR(128) NULL,
  drm_enabled BIT NOT NULL CONSTRAINT df_assets_drm DEFAULT 0,
  extra_json NVARCHAR(MAX) NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_assets_created DEFAULT GETUTCDATE(),
  CONSTRAINT ck_assets_kind CHECK (kind IN (N'video',N'file',N'link')),
  CONSTRAINT fk_lesson_assets_lesson FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(id) ON DELETE CASCADE
);

-- =======================
-- Enrollment & Progress
-- =======================

CREATE TABLE dbo.enrollments (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  source NVARCHAR(16) NOT NULL,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_enroll_status DEFAULT N'active',
  start_at DATETIME2 NOT NULL CONSTRAINT df_enroll_start DEFAULT GETUTCDATE(),
  end_at DATETIME2 NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_enroll_created DEFAULT GETUTCDATE(),
  CONSTRAINT ck_enroll_source CHECK (source IN (N'purchase',N'free',N'admin',N'coupon',N'subscription')),
  CONSTRAINT ck_enroll_status CHECK (status IN (N'active',N'expired',N'refunded',N'revoked')),
  CONSTRAINT uq_enroll_user_course UNIQUE (user_id, course_id),
  CONSTRAINT fk_enroll_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
  CONSTRAINT fk_enroll_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE
);

CREATE TABLE dbo.lesson_progress (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lesson_id BIGINT NOT NULL,
  progress_percent TINYINT NOT NULL CONSTRAINT df_progress_percent DEFAULT 0, -- 0..100
  last_position_seconds INT NULL,
  completed_at DATETIME2 NULL,
  updated_at DATETIME2 NOT NULL CONSTRAINT df_progress_updated DEFAULT GETUTCDATE(),
  CONSTRAINT uq_progress_user_lesson UNIQUE (user_id, lesson_id),
  CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
  CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(id) ON DELETE CASCADE
);

-- =======================
-- Quiz (manual grading)
-- =======================

CREATE TABLE dbo.quizzes (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  course_id BIGINT NULL,
  lesson_id BIGINT NULL,
  title NVARCHAR(255) NOT NULL,
  time_limit_sec INT NULL,
  shuffle BIT NOT NULL CONSTRAINT df_quizzes_shuffle DEFAULT 0,
  grading_policy NVARCHAR(16) NOT NULL CONSTRAINT df_quizzes_policy DEFAULT N'manual',
  created_at DATETIME2 NOT NULL CONSTRAINT df_quizzes_created DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_quizzes_updated DEFAULT GETUTCDATE(),
  CONSTRAINT ck_quizzes_policy CHECK (grading_policy IN (N'manual')),
  CONSTRAINT fk_quizzes_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
  CONSTRAINT fk_quizzes_lesson FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(id) ON DELETE CASCADE
);

CREATE TABLE dbo.question_banks (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  name NVARCHAR(255) NOT NULL,
  description NVARCHAR(MAX) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_qbanks_created DEFAULT GETUTCDATE(),
  CONSTRAINT fk_qbanks_creator FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.questions (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  quiz_id BIGINT NULL,
  bank_id BIGINT NULL,
  type NVARCHAR(8) NOT NULL,
  text NVARCHAR(MAX) NOT NULL,
  points DECIMAL(6,2) NOT NULL CONSTRAINT df_questions_points DEFAULT 1.00,
  sort_order INT NOT NULL CONSTRAINT df_questions_sort DEFAULT 0,
  CONSTRAINT ck_questions_type CHECK (type IN (N'sc',N'mc',N'fill',N'text')),
  CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id) REFERENCES dbo.quizzes(id) ON DELETE CASCADE,
  CONSTRAINT fk_questions_bank FOREIGN KEY (bank_id) REFERENCES dbo.question_banks(id)
);

CREATE TABLE dbo.question_options (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  question_id BIGINT NOT NULL,
  text NVARCHAR(MAX) NOT NULL,
  is_correct BIT NOT NULL CONSTRAINT df_qoptions_correct DEFAULT 0,
  sort_order INT NOT NULL CONSTRAINT df_qoptions_sort DEFAULT 0,
  CONSTRAINT fk_qoptions_question FOREIGN KEY (question_id) REFERENCES dbo.questions(id) ON DELETE CASCADE
);

CREATE TABLE dbo.quiz_attempts (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  quiz_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  started_at DATETIME2 NOT NULL CONSTRAINT df_qattempts_started DEFAULT GETUTCDATE(),
  finished_at DATETIME2 NULL,
  score DECIMAL(6,2) NULL,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_qattempts_status DEFAULT N'in_progress',
  CONSTRAINT ck_qattempts_status CHECK (status IN (N'in_progress',N'submitted',N'graded')),
  CONSTRAINT fk_qattempts_quiz FOREIGN KEY (quiz_id) REFERENCES dbo.quizzes(id) ON DELETE CASCADE,
  CONSTRAINT fk_qattempts_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);

CREATE TABLE dbo.quiz_answers (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  selected_option_id BIGINT NULL,
  text_answer NVARCHAR(MAX) NULL,
  is_correct BIT NULL,
  points_awarded DECIMAL(6,2) NULL,
  CONSTRAINT fk_qanswers_attempt FOREIGN KEY (attempt_id) REFERENCES dbo.quiz_attempts(id) ON DELETE CASCADE,
  CONSTRAINT fk_qanswers_question FOREIGN KEY (question_id) REFERENCES dbo.questions(id) ON DELETE CASCADE,
  CONSTRAINT fk_qanswers_selected_option FOREIGN KEY (selected_option_id) REFERENCES dbo.question_options(id)
);

-- =======================
-- Assignments & Submissions
-- =======================

CREATE TABLE dbo.assignments (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  lesson_id BIGINT NOT NULL,
  title NVARCHAR(255) NOT NULL,
  description NVARCHAR(MAX) NULL,
  due_at DATETIME2 NULL,
  max_points DECIMAL(6,2) NOT NULL CONSTRAINT df_assignments_points DEFAULT 10.00,
  created_at DATETIME2 NOT NULL CONSTRAINT df_assignments_created DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_assignments_updated DEFAULT GETUTCDATE(),
  CONSTRAINT fk_assignments_lesson FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(id) ON DELETE CASCADE
);

CREATE TABLE dbo.submissions (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  submitted_at DATETIME2 NOT NULL CONSTRAINT df_submissions_submitted DEFAULT GETUTCDATE(),
  text_answer NVARCHAR(MAX) NULL,
  file_url NVARCHAR(1024) NULL,
  grade DECIMAL(6,2) NULL,
  graded_at DATETIME2 NULL,
  grader_id BIGINT NULL,
  feedback NVARCHAR(MAX) NULL,
  CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES dbo.assignments(id) ON DELETE CASCADE,
  CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
  CONSTRAINT fk_submissions_grader FOREIGN KEY (grader_id) REFERENCES dbo.users(id)
);

-- =======================
-- Commerce: Orders/Payments/Coupons/Subscriptions
-- =======================

CREATE TABLE dbo.orders (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_orders_status DEFAULT N'pending',
  currency NCHAR(3) NOT NULL CONSTRAINT df_orders_currency DEFAULT N'VND',
  subtotal DECIMAL(12,2) NOT NULL CONSTRAINT df_orders_subtotal DEFAULT 0.00,
  discount DECIMAL(12,2) NOT NULL CONSTRAINT df_orders_discount DEFAULT 0.00,
  tax DECIMAL(12,2) NOT NULL CONSTRAINT df_orders_tax DEFAULT 0.00,
  total DECIMAL(12,2) NOT NULL CONSTRAINT df_orders_total DEFAULT 0.00,
  created_at DATETIME2 NOT NULL CONSTRAINT df_orders_created DEFAULT GETUTCDATE(),
  paid_at DATETIME2 NULL,
  CONSTRAINT ck_orders_status CHECK (status IN (N'pending',N'paid',N'cancelled',N'refunded')),
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
CREATE INDEX idx_orders_user_status_created ON dbo.orders(user_id, status, created_at);

CREATE TABLE dbo.order_items (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_id BIGINT NOT NULL,
  item_type NVARCHAR(16) NOT NULL,
  item_id BIGINT NOT NULL,
  title_snapshot NVARCHAR(255) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  qty INT NOT NULL CONSTRAINT df_order_items_qty DEFAULT 1,
  CONSTRAINT ck_order_items_type CHECK (item_type IN (N'course',N'subscription',N'bundle')),
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE
);

CREATE TABLE dbo.payments (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_id BIGINT NOT NULL,
  provider NVARCHAR(16) NOT NULL CONSTRAINT df_payments_provider DEFAULT N'zalopay',
  provider_txn_id NVARCHAR(128) NULL,
  amount DECIMAL(12,2) NOT NULL,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_payments_status DEFAULT N'pending',
  paid_at DATETIME2 NULL,
  raw_payload NVARCHAR(MAX) NULL,
  zp_trans_id NVARCHAR(64) NULL,
  zp_app_trans_id NVARCHAR(64) NULL,
  zp_order_id NVARCHAR(64) NULL,
  CONSTRAINT ck_payments_status CHECK (status IN (N'pending',N'succeeded',N'failed',N'refunded')),
  CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE
);
CREATE INDEX idx_payments_provider_txn ON dbo.payments(provider, provider_txn_id);
CREATE INDEX idx_payments_zp ON dbo.payments(zp_trans_id, zp_app_trans_id, zp_order_id);

CREATE TABLE dbo.coupons (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  code NVARCHAR(64) NOT NULL UNIQUE,
  type NVARCHAR(16) NOT NULL,
  value DECIMAL(12,2) NOT NULL,
  max_uses INT NULL,
  used INT NOT NULL CONSTRAINT df_coupons_used DEFAULT 0,
  starts_at DATETIME2 NULL,
  ends_at DATETIME2 NULL,
  min_total DECIMAL(12,2) NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_coupons_created DEFAULT GETUTCDATE(),
  CONSTRAINT ck_coupons_type CHECK (type IN (N'percent',N'fixed'))
);

CREATE TABLE dbo.order_coupons (
  order_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  PRIMARY KEY (order_id, coupon_id),
  CONSTRAINT fk_order_coupons_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_coupons_coupon FOREIGN KEY (coupon_id) REFERENCES dbo.coupons(id) ON DELETE CASCADE
);

CREATE TABLE dbo.plans (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  name NVARCHAR(255) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  plan_interval NVARCHAR(8) NOT NULL,
  features NVARCHAR(MAX) NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_plans_created DEFAULT GETUTCDATE(),
  CONSTRAINT ck_plans_interval CHECK (plan_interval IN (N'month',N'year'))
);

CREATE TABLE dbo.subscriptions (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_subs_status DEFAULT N'active',
  current_period_start DATETIME2 NOT NULL,
  current_period_end DATETIME2 NOT NULL,
  cancel_at DATETIME2 NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_subs_created DEFAULT GETUTCDATE(),
  CONSTRAINT ck_subs_status CHECK (status IN (N'active',N'past_due',N'cancelled')),
  CONSTRAINT fk_subs_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
  CONSTRAINT fk_subs_plan FOREIGN KEY (plan_id) REFERENCES dbo.plans(id)
);

-- =======================
-- Certificates
-- =======================

CREATE TABLE dbo.certificates (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  serial NVARCHAR(64) NOT NULL UNIQUE,
  issued_at DATETIME2 NOT NULL CONSTRAINT df_cert_issued DEFAULT GETUTCDATE(),
  pdf_url NVARCHAR(1024) NULL,
  verify_code NVARCHAR(64) NOT NULL UNIQUE,
  CONSTRAINT fk_cert_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
  CONSTRAINT fk_cert_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE
);

-- =======================
-- Reviews & Discussions
-- =======================

CREATE TABLE dbo.reviews (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  title NVARCHAR(255) NULL,
  content NVARCHAR(MAX) NULL,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_reviews_status DEFAULT N'pending',
  created_at DATETIME2 NOT NULL CONSTRAINT df_reviews_created DEFAULT GETUTCDATE(),
  CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT ck_reviews_status CHECK (status IN (N'pending',N'approved',N'rejected')),
  CONSTRAINT uq_review_course_user UNIQUE (course_id, user_id),
  CONSTRAINT fk_reviews_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
  CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);

CREATE TABLE dbo.discussion_threads (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  course_id BIGINT NULL,
  lesson_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  title NVARCHAR(255) NOT NULL,
  pinned BIT NOT NULL CONSTRAINT df_threads_pinned DEFAULT 0,
  created_at DATETIME2 NOT NULL CONSTRAINT df_threads_created DEFAULT GETUTCDATE(),
  CONSTRAINT fk_threads_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
  CONSTRAINT fk_threads_lesson FOREIGN KEY (lesson_id) REFERENCES dbo.lessons(id) ON DELETE CASCADE,
  CONSTRAINT fk_threads_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);

CREATE TABLE dbo.discussion_posts (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  thread_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content NVARCHAR(MAX) NOT NULL,
  parent_id BIGINT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_posts_created DEFAULT GETUTCDATE(),
  edited_at DATETIME2 NULL,
  CONSTRAINT fk_posts_thread FOREIGN KEY (thread_id) REFERENCES dbo.discussion_threads(id) ON DELETE CASCADE,
  CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
  CONSTRAINT fk_posts_parent FOREIGN KEY (parent_id) REFERENCES dbo.discussion_posts(id)
);

-- =======================
-- Live Sessions (Zoom) & Attendance
-- =======================

CREATE TABLE dbo.live_sessions (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title NVARCHAR(255) NOT NULL,
  start_at DATETIME2 NOT NULL,
  end_at DATETIME2 NOT NULL,
  provider NVARCHAR(16) NOT NULL CONSTRAINT df_live_provider DEFAULT N'zoom',
  join_url NVARCHAR(1024) NOT NULL,
  record_url NVARCHAR(1024) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_live_created DEFAULT GETUTCDATE(),
  CONSTRAINT ck_live_provider CHECK (provider IN (N'zoom')),
  CONSTRAINT fk_live_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
  CONSTRAINT fk_live_creator FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.live_attendance (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  session_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  joined_at DATETIME2 NOT NULL,
  left_at DATETIME2 NULL,
  CONSTRAINT fk_attend_session FOREIGN KEY (session_id) REFERENCES dbo.live_sessions(id) ON DELETE CASCADE,
  CONSTRAINT fk_attend_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);

-- =======================
-- Notifications, Webhooks, Audit
-- =======================

CREATE TABLE dbo.notifications (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type NVARCHAR(64) NOT NULL,
  title NVARCHAR(255) NOT NULL,
  body NVARCHAR(MAX) NULL,
  data NVARCHAR(MAX) NULL,
  is_read BIT NOT NULL CONSTRAINT df_notifications_read DEFAULT 0,
  created_at DATETIME2 NOT NULL CONSTRAINT df_notifications_created DEFAULT GETUTCDATE(),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);

CREATE TABLE dbo.webhooks (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  event NVARCHAR(128) NOT NULL,
  target_url NVARCHAR(1024) NOT NULL,
  secret NVARCHAR(255) NOT NULL,
  is_active BIT NOT NULL CONSTRAINT df_webhooks_active DEFAULT 1,
  created_at DATETIME2 NOT NULL CONSTRAINT df_webhooks_created DEFAULT GETUTCDATE()
);

CREATE TABLE dbo.webhook_logs (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  webhook_id BIGINT NOT NULL,
  event_id NVARCHAR(128) NOT NULL,
  status_code INT NULL,
  request_json NVARCHAR(MAX) NULL,
  response_json NVARCHAR(MAX) NULL,
  sent_at DATETIME2 NOT NULL CONSTRAINT df_whlogs_sent DEFAULT GETUTCDATE(),
  CONSTRAINT fk_whlogs_webhook FOREIGN KEY (webhook_id) REFERENCES dbo.webhooks(id) ON DELETE CASCADE
);

CREATE TABLE dbo.audit_logs (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NULL,
  actor_ip NVARCHAR(64) NULL,
  action NVARCHAR(128) NOT NULL,
  entity_type NVARCHAR(64) NULL,
  entity_id BIGINT NULL,
  meta NVARCHAR(MAX) NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_audit_created DEFAULT GETUTCDATE(),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

-- =======================
-- Learning Paths & Prerequisites
-- =======================

CREATE TABLE dbo.learning_paths (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  name NVARCHAR(255) NOT NULL,
  description NVARCHAR(MAX) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_lpaths_created DEFAULT GETUTCDATE(),
  CONSTRAINT fk_lpaths_creator FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

CREATE TABLE dbo.learning_path_items (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  path_id BIGINT NOT NULL,
  item_type NVARCHAR(16) NOT NULL,
  item_id BIGINT NOT NULL,
  sort_order INT NOT NULL CONSTRAINT df_lpi_sort DEFAULT 0,
  prerequisite_item_id BIGINT NULL,
  CONSTRAINT ck_lpi_type CHECK (item_type IN (N'course')),
  CONSTRAINT fk_lpi_path FOREIGN KEY (path_id) REFERENCES dbo.learning_paths(id) ON DELETE CASCADE
);

CREATE TABLE dbo.prerequisites (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  course_id BIGINT NOT NULL,
  required_course_id BIGINT NOT NULL,
  CONSTRAINT uq_prereq UNIQUE (course_id, required_course_id),
  CONSTRAINT fk_prereq_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE,
  CONSTRAINT fk_prereq_required FOREIGN KEY (required_course_id) REFERENCES dbo.courses(id) ON DELETE CASCADE
);

-- =======================
-- Orders & Payments
-- =======================
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID('dbo.orders', 'U') IS NOT NULL DROP TABLE dbo.orders;

CREATE TABLE dbo.orders (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  external_code NVARCHAR(64) NULL,
  amount DECIMAL(18,2) NOT NULL CONSTRAINT df_orders_amount DEFAULT 0,
  currency NVARCHAR(8) NOT NULL CONSTRAINT df_orders_currency DEFAULT N'VND',
  method NVARCHAR(16) NOT NULL CONSTRAINT df_orders_method DEFAULT N'BANK',
  status NVARCHAR(16) NOT NULL CONSTRAINT df_orders_status DEFAULT N'pending',
  provider NVARCHAR(32) NULL,
  provider_txn NVARCHAR(128) NULL,
  return_url NVARCHAR(256) NULL,
  cancel_url NVARCHAR(256) NULL,
  note NVARCHAR(512) NULL,
  metadata NVARCHAR(MAX) NULL,
  paid_at DATETIME2 NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_orders_created DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_orders_updated DEFAULT GETUTCDATE(),
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
  CONSTRAINT fk_orders_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id)
);
CREATE INDEX idx_orders_user ON dbo.orders(user_id);
CREATE INDEX idx_orders_course ON dbo.orders(course_id);
CREATE UNIQUE INDEX uq_orders_external_code ON dbo.orders(external_code) WHERE external_code IS NOT NULL;

CREATE TABLE dbo.payments (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_id BIGINT NOT NULL,
  provider NVARCHAR(16) NOT NULL CONSTRAINT df_payments_provider DEFAULT N'unknown',
  provider_txn_id NVARCHAR(128) NULL,
  channel NVARCHAR(32) NULL,
  amount DECIMAL(18,2) NOT NULL CONSTRAINT df_payments_amount DEFAULT 0,
  status NVARCHAR(16) NOT NULL CONSTRAINT df_payments_status DEFAULT N'pending',
  payload NVARCHAR(MAX) NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_payments_created DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_payments_updated DEFAULT GETUTCDATE(),
  CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE
);
CREATE INDEX idx_payments_order ON dbo.payments(order_id);
CREATE INDEX idx_payments_provider ON dbo.payments(provider, provider_txn_id);

-- =======================
-- Support Chat & Ratings
-- =======================

CREATE TABLE dbo.support_threads (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  student_id BIGINT NOT NULL,
  manager_id BIGINT NULL,
  course_id BIGINT NULL,
  topic NVARCHAR(64) NOT NULL,
  subject NVARCHAR(255) NULL,
  origin NVARCHAR(64) NULL,
  status NVARCHAR(32) NOT NULL CONSTRAINT df_support_threads_status DEFAULT N'new',
  priority NVARCHAR(16) NULL,
  channel NVARCHAR(32) NULL,
  metadata NVARCHAR(MAX) NULL,
  last_message_preview NVARCHAR(512) NULL,
  last_message_at DATETIME2 NULL,
  last_sender NVARCHAR(16) NULL,
  last_student_activity_at DATETIME2 NULL,
  last_manager_activity_at DATETIME2 NULL,
  has_unread_for_student BIT NOT NULL CONSTRAINT df_support_threads_unread_student DEFAULT 0,
  has_unread_for_manager BIT NOT NULL CONSTRAINT df_support_threads_unread_manager DEFAULT 1,
  created_at DATETIME2 NOT NULL CONSTRAINT df_support_threads_created DEFAULT GETUTCDATE(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_support_threads_updated DEFAULT GETUTCDATE(),
  closed_at DATETIME2 NULL,
  CONSTRAINT fk_support_threads_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
  CONSTRAINT fk_support_threads_manager FOREIGN KEY (manager_id) REFERENCES dbo.users(id),
  CONSTRAINT fk_support_threads_course FOREIGN KEY (course_id) REFERENCES dbo.courses(id),
  CONSTRAINT ck_support_threads_status CHECK (status IN (N'new', N'in_progress', N'waiting_student', N'closed'))
);

CREATE INDEX ix_support_threads_status_created ON dbo.support_threads(status, created_at);
CREATE INDEX ix_support_threads_manager_status ON dbo.support_threads(manager_id, status) WHERE manager_id IS NOT NULL;
CREATE INDEX ix_support_threads_student_status ON dbo.support_threads(student_id, status);

CREATE TABLE dbo.support_messages (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  thread_id BIGINT NOT NULL,
  sender_id BIGINT NULL,
  sender_type NVARCHAR(16) NOT NULL,
  content NVARCHAR(MAX) NOT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_support_messages_created DEFAULT GETUTCDATE(),
  CONSTRAINT fk_support_messages_thread FOREIGN KEY (thread_id) REFERENCES dbo.support_threads(id) ON DELETE CASCADE,
  CONSTRAINT fk_support_messages_sender FOREIGN KEY (sender_id) REFERENCES dbo.users(id),
  CONSTRAINT ck_support_messages_sender_type CHECK (sender_type IN (N'student', N'manager', N'system', N'bot'))
);

CREATE TABLE dbo.support_message_attachments (
  message_id BIGINT NOT NULL,
  attachment_url NVARCHAR(1024) NOT NULL,
  PRIMARY KEY (message_id, attachment_url),
  CONSTRAINT fk_support_msg_attachments_message FOREIGN KEY (message_id) REFERENCES dbo.support_messages(id) ON DELETE CASCADE
);

CREATE INDEX ix_support_messages_thread ON dbo.support_messages(thread_id, id);

CREATE TABLE dbo.support_ratings (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  thread_id BIGINT NOT NULL UNIQUE,
  student_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  comment NVARCHAR(1000) NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_support_ratings_created DEFAULT GETUTCDATE(),
  CONSTRAINT fk_support_ratings_thread FOREIGN KEY (thread_id) REFERENCES dbo.support_threads(id) ON DELETE CASCADE,
  CONSTRAINT fk_support_ratings_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
  CONSTRAINT ck_support_ratings_value CHECK (rating BETWEEN 1 AND 5)
);

-- Helpful indexes
CREATE INDEX idx_courses_status_publish ON dbo.courses(status, publish_at);
CREATE INDEX idx_lessons_module_sort ON dbo.lessons(module_id, sort_order);
CREATE INDEX idx_modules_course_sort ON dbo.modules(course_id, sort_order);

-- Note: For automatic 'updated_at' maintenance, add triggers if desired.
-- Example (users):
-- CREATE TRIGGER trg_users_touch ON dbo.users AFTER UPDATE AS
-- BEGIN
--   SET NOCOUNT ON;
--   UPDATE dbo.users SET updated_at = GETUTCDATE() WHERE id IN (SELECT DISTINCT id FROM inserted);
-- END;
