# Audit Finding Tracker

This repository contains a production-ready skeleton for the Audit Finding Tracker internship capstone project.

What's included in this commit:
- Backend Spring Boot 3 (Java 17) skeleton with JWT authentication, security config, and initial entities/repositories.
- AI microservice (Flask) placeholder with streaming endpoint and health check.
- Frontend skeleton (Vite + React) with Tailwind-ready structure.
- Docker Compose file to run backend, postgres, redis, ai-service, and frontend in development.
- Flyway initial migration script.
- .env.example and .gitignore

Next steps (automated but not yet fully implemented in code in this commit):
- Implement AuditFinding CRUD controllers, services, Redis caching annotations, File upload API, Email templates and scheduling, Audit logging service.
- Implement AI service RAG pipeline, Groq integration, ChromaDB persistence, and embeddings.
- Build frontend pages and integrate with backend APIs.
- Add comprehensive tests (JUnit + MockMvc) and frontend tests.
- Security hardening and SECURITY.md (present in repo) to follow.

Run locally:
1. Copy .env.example to .env and customize.
2. docker compose up --build

