Student Course Management and Readiness Tracker

👉ABOUT THE PROJECT
Student Course Management and Readiness Tracker is a Java-based web application designed to manage students and courses, provide structured learning materials, and analyze students’ learning readiness and progress. Unlike traditional learning platforms that only show how much of a course is completed, this system focuses on how prepared a student actually is by calculating readiness based on skill weightage and performance. This helps students understand not just what they have completed, but how well they are prepared.

👉TECH STACK
- Backend: Java (Core Java, JDBC)
- Database: MySQL
- Frontend: HTML, CSS, JavaScript
- Architecture: Layered Architecture (MVC-style)
- Server: Custom Java HTTP Server

👉FEATURES

1.Student Module
  - Register and login
  - View available and enrolled courses
  - Access learning materials for each skill
  - View readiness score based on skill weightage
  - Track learning progress
  - Identify weak and strong areas

2.Admin Module
  - Create, update, and delete courses
  - Define skills for each course and assign weightage
  - Upload and manage learning materials
  - Enroll students into courses
  - Monitor student readiness and performance

👉UNIQUENESS OF THE PROJECT
  - Most platforms show only progress percentage based on completed lessons.
  - This does not reflect lesson importance or learning quality.
  - Each skill is assigned a weightage based on importance.
  - Student performance is tracked per skill.
  - Readiness is calculated as a weighted score instead of simple completion percentage.
  - Critical skills contribute more to readiness than minor ones.

👉READINESS CALCULATION

    🧩 Readiness Score = (Sum of Skill Weightage × Skill Performance) / Total Weightage
This ensures readiness reflects learning quality, not just learning quantity.

👉SYSTEM ARCHITECTURE

The system follows the MVC (Model–View–Controller) architecture.
  -Model: Manages data and rules of the system.
  -View: Displays information to the user.
  -Controller: Handles user input and updates the model and view.
This structure separates responsibilities and makes the system easy to maintain and extend.

👉FUTURE ENHANCEMENTS
  - AI-based readiness prediction
  - Online quiz and test integration
  - Personalized learning recommendations
  - Mobile application support
  - Real-time notifications and reminders
