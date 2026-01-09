-- Create ADMIN table
CREATE TABLE IF NOT EXISTS ADMIN (
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);

-- STUDENT table remains same
CREATE TABLE IF NOT EXISTS STUDENT (
    student_id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password TEXT
);

-- SKILL table becomes SUBJECT (Containers) - No weight here
CREATE TABLE IF NOT EXISTS SKILL (
    skill_id INTEGER PRIMARY KEY AUTO_INCREMENT,
    skill_name TEXT NOT NULL,
    image_url TEXT,
    status VARCHAR(20) DEFAULT 'active'
);

-- MATERIAL table (The actual content with weights)
CREATE TABLE IF NOT EXISTS MATERIAL (
    id INTEGER PRIMARY KEY,
    skill_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    type TEXT NOT NULL, -- 'FILE', 'LINK', 'TEXT'
    resource_url TEXT,
    weight INTEGER NOT NULL,
    FOREIGN KEY (skill_id) REFERENCES SKILL(skill_id)
);

-- STUDENT_PROGRESS table (Tracks material completion)
CREATE TABLE IF NOT EXISTS STUDENT_PROGRESS (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    student_id INTEGER NOT NULL,
    material_id INTEGER NOT NULL,
    status VARCHAR(50) CHECK(status IN ('Completed', 'Not Started')) DEFAULT 'Not Started',
    FOREIGN KEY (student_id) REFERENCES STUDENT(student_id),
    FOREIGN KEY (material_id) REFERENCES MATERIAL(id)
);

-- ENROLLMENT table (Tracks course enrollments)
CREATE TABLE IF NOT EXISTS ENROLLMENT (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    student_id INTEGER NOT NULL,
    skill_id INTEGER NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_enrollment (student_id, skill_id),
    FOREIGN KEY (student_id) REFERENCES STUDENT(student_id),
    FOREIGN KEY (skill_id) REFERENCES SKILL(skill_id)
);

-- Insert Default Admin
INSERT IGNORE INTO ADMIN (username, password) VALUES ('admin', 'admin123');

-- Insert dummy data

INSERT IGNORE INTO SKILL (skill_id, skill_name) VALUES (1, 'DBMS');

-- DBMS Materials (Sum = 100)
INSERT IGNORE INTO MATERIAL (id, skill_id, title, type, resource_url, weight) VALUES (1, 1, 'Intro to SQL', 'TEXT', 'Read Chapter 1', 40);
INSERT IGNORE INTO MATERIAL (id, skill_id, title, type, resource_url, weight) VALUES (2, 1, 'Normalization PDF', 'FILE', 'normalization.pdf', 60);

-- Progress

