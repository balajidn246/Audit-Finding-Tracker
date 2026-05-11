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

Clone the repo (if needed)
git clone https://github.com/balajidn246/Audit-Finding-Tracker.git
cd Audit-Finding-Tracker
Prepare environment
Copy example env:
cp .env.example .env
Edit .env and set at minimum:
JWT_SECRET -> long (>=32 bytes) cryptographic secret
DB_USERNAME, DB_PASSWORD (if different)
MAIL_* if you want email
GROQ_API_KEY if you will enable Groq integration
Generate a secure JWT_SECRET (example):
openssl rand -base64 48 | tr -d '\n' && echo
paste result into .env as JWT_SECRET=
(Optional but recommended) Ensure Docker has enough resources (4+ GB RAM) before building Java image.

Start everything with Docker Compose (recommended)

docker compose up --build
This builds backend, frontend, ai-service, and starts postgres + redis.
Monitor output for Flyway migrations and health checks.
Postgres extension (if Flyway complains about gen_random_uuid)
If you see errors about gen_random_uuid() run:
docker compose exec postgres psql -U ${DB_USERNAME:-postgres} -d ${DB_NAME:-aft} -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
then restart backend: docker compose restart backend
Verify services (URLs)
Backend ping:
curl http://localhost:8080/api/auth/ping
Swagger UI:
http://localhost:8080/swagger-ui.html
AI service:
curl http://localhost:5000/health
Frontend (dev/preview):
If dev server: run npm run dev in frontend or visit mapped port in docker-compose
In compose file frontend maps 3000: http://localhost:3000
Run backend locally (alternate to docker)
cd backend
./mvnw clean package -DskipTests
java -jar target/audit-finding-tracker-1.0.0.jar
To run tests:
./mvnw test
Run AI service locally (alternate)
cd ai-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app.py
Note: sentence-transformers will download models (network + disk). Adjust memory.
Run frontend locally (alternate)
cd frontend
npm install
npm run dev
Vite will print the dev URL (e.g., http://localhost:5173) — set VITE_API_BASE_URL to http://localhost:8080/api in .env for local use.
Quick API examples (replace <token> with accessToken)
Register: curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"admin2","email":"a2@example.com","password":"ComplexP@ss123"}'
Login: curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin2","password":"ComplexP@ss123"}'
Create finding: curl -X POST http://localhost:8080/api/findings -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"title":"Example","description":"Desc"}'
AI describe (local AI service): curl -X POST http://localhost:5000/describe -H "Content-Type: application/json" -d '{"text":"Some finding text"}'
Health & logs troubleshooting
Follow logs:
docker compose logs -f backend
docker compose logs -f ai-service
Common issues & fixes:
Flyway fails -> check DB env and extensions (pgcrypto)
Maven build OOM in Docker -> build locally or increase Docker memory
File storage permission errors -> ensure host path (./uploads) writable or change files.upload-dir in .env
AI model/embedding memory heavy -> use smaller sentence-transformer or run AI service with more memory/CPU
Security & production notes (do these before production)
Store JWT_SECRET and sensitive creds in secret manager; never commit to git.
Use TLS (HTTPS) and place services behind a reverse proxy (nginx, LB).
Rotate refresh tokens and ensure proper revocation (not yet implemented).
Add virus scanning (clamav) for file uploads for production.
Run dependency scanning (Dependabot/Snyk).
Do pen tests for file upload, auth flows, AI endpoints and prompt-injection testing.
CI / GitHub Actions
I can add a GitHub Actions workflow that:
builds backend, runs unit tests, builds docker images, and (optionally) runs integration tests in a matrix.
Let me know if you want me to add that to a branch and open a PR.
What’s already committed
Full backend code (entities, services, controllers, JWT, Redis caching, Flyway migrations, file storage, email service, scheduling, audit log).
AI microservice (Flask) with endpoints, caching, Chroma placeholders, sentence-transformers embeddings, SSE streaming, rate limiting.
Frontend skeleton (Vite + React + Tailwind-ready), JWT Axios interceptor, pages.
Dockerfiles and docker-compose.yml, .env.example, README.md, SECURITY.md, some tests.

