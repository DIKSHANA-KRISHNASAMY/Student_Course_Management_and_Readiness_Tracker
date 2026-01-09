Student Course Management and Readiness Tracker

===>About the Project<===

Student Course Management and Readiness Tracker is a Java-based web application designed to manage students and courses, provide structured learning materials, and analyze students’ learning readiness and progress. Unlike traditional learning platforms that only show how much of a course is completed, this system focuses on how prepared a student actually is by calculating readiness based on skill weightage and performance. This helps students understand not just what they have completed, but how well they are prepared.

===>Tech Stack<===

  Backend: Java (Core Java, JDBC)
  Database: MySQL
  Frontend: HTML, CSS, JavaScript
  Architecture: Layered Architecture (MVC-style)
  Server: Custom Java HTTP Server

===>Features<===
1.Student Module
  Register and login to the system
  View available and enrolled courses
  Access learning materials for each course and skill
  View readiness score calculated based on skill weightage
  Track learning progress and identify weak areas
  Understand which skills contribute more to overall readiness

Admin Module

Create and manage courses

Define skills for each course and assign weightage

Upload and manage learning materials

Enroll students into courses

Monitor student readiness and performance

Uniqueness of the Project

Most learning platforms display only progress percentage, which is based on how many lessons are completed. This does not reflect how important each lesson is or how well the student has learned.

This system introduces a readiness-based evaluation model:

Each skill or module is assigned a weightage based on its importance.

Student performance is tracked per skill.

Readiness is calculated as a weighted score instead of simple completion percentage.

This ensures that critical skills contribute more to readiness than minor ones, giving a more accurate picture of student preparedness.

Readiness Calculation (Concept)

Readiness Score = Sum of (Skill Weightage × Skill Performance) / Total Weightage

This model ensures readiness reflects learning quality, not just learning quantity.

System Architecture

Presentation Layer: HTML, CSS, JavaScript

Business Logic Layer: Java handlers and services

Data Access Layer: DAO classes using JDBC

Database Layer: MySQL

The layered architecture ensures separation of concerns, maintainability, and scalability.

Future Enhancements
