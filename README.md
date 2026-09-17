# Audit Finding Tracker

Audit Finding Tracker is a capstone project for managing audit findings with a Spring Boot backend, an AI service, and a React frontend.

The project provides a production-style starting point that you can run locally with Docker Compose and extend with your own business features.

## What is included

- **Backend:** Spring Boot 3, Java 17, JWT authentication, security configuration, entities, repositories, and services
- **AI service:** Flask service with health checks, streaming support, caching, and embedding placeholders
- **Frontend:** React + Vite application with a Tailwind-ready structure
- **Database:** PostgreSQL with Flyway migrations
- **Caching:** Redis
- **Development environment:** Docker Compose configuration for all services
- **Security:** `SECURITY.md` with security guidance

## Project structure

```text
backend/            Spring Boot API
ai-service/         Flask AI microservice
frontend/           React + Vite frontend
uploads/            File upload directory
docker-compose.yml  Local development environment
.env.example        Example environment configuration
SECURITY.md         Security guidance
```

## Planned features

The following features are planned or are still being completed:

- Audit finding CRUD APIs
- Redis caching annotations
- File upload API
- Email templates and scheduled notifications
- Audit logging service
- AI RAG pipeline, Groq integration, and ChromaDB persistence
- Frontend pages and backend API integration
- Comprehensive backend and frontend tests
- Additional production security hardening

## Quick start with Docker

### Prerequisites

Make sure you have the following installed:

- Git
- Docker Desktop or Docker Engine with Docker Compose
- At least 4 GB of memory available to Docker

### 1. Clone the repository

```bash
git clone https://github.com/balajidn246/Audit-Finding-Tracker.git
cd Audit-Finding-Tracker
```

### 2. Create your environment file

```bash
cp .env.example .env
```

Open `.env` and configure the values you need:

- `JWT_SECRET` — a strong secret with at least 32 bytes
- `DB_USERNAME` and `DB_PASSWORD` — database credentials, if different from the defaults
- `MAIL_*` — required only if you want email functionality
- `GROQ_API_KEY` — required only if you enable Groq integration

Generate a secure JWT secret with:

```bash
openssl rand -base64 48 | tr -d '\n' && echo
```

Copy the generated value into `.env`:

```env
JWT_SECRET=your-generated-secret
```

### 3. Start all services

```bash
docker compose up --build
```

This starts the backend, frontend, AI service, PostgreSQL, and Redis.

## Service URLs

| Service | URL |
| --- | --- |
| Backend API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| AI service | `http://localhost:5000` |
| Frontend | `http://localhost:3000` |

Check the services:

```bash
curl http://localhost:8080/api/auth/ping
curl http://localhost:5000/health
```

## Run services without Docker

### Backend

```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/audit-finding-tracker-1.0.0.jar
```

Run backend tests:

```bash
./mvnw test
```

### AI service

```bash
cd ai-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app.py
```

> `sentence-transformers` may download models and require additional disk space and memory.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite will display the local development URL, usually `http://localhost:5173`.

For local API calls, set this value in the frontend environment file:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## API examples

The examples below assume that the backend is running. Replace `<token>` with the `accessToken` returned by the login request.

### Register a user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin2","email":"a2@example.com","password":"ComplexP@ss123"}'
```

### Log in

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin2","password":"ComplexP@ss123"}'
```

### Create an audit finding

```bash
curl -X POST http://localhost:8080/api/findings \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Example","description":"Desc"}'
```

### Generate an AI description

```bash
curl -X POST http://localhost:5000/describe \
  -H "Content-Type: application/json" \
  -d '{"text":"Some finding text"}'
```

## Troubleshooting

### PostgreSQL reports a `gen_random_uuid()` error

Enable the PostgreSQL `pgcrypto` extension:

```bash
docker compose exec postgres psql \
  -U ${DB_USERNAME:-postgres} \
  -d ${DB_NAME:-aft} \
  -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
```

Restart the backend afterward:

```bash
docker compose restart backend
```

### View service logs

```bash
docker compose logs -f backend
docker compose logs -f ai-service
```

### Common problems

- **Flyway fails:** Check database variables and enable the `pgcrypto` extension.
- **Docker runs out of memory:** Increase Docker memory or build the backend locally.
- **File uploads fail:** Make sure the `./uploads` directory is writable.
- **AI service uses too much memory:** Use a smaller sentence-transformer model or assign more resources to Docker.

## Security and production checklist

Before deploying to production:

- Store `JWT_SECRET` and other credentials in a secret manager.
- Never commit secrets to Git.
- Use HTTPS behind a reverse proxy or load balancer.
- Implement refresh-token rotation and revocation.
- Add malware scanning for uploaded files.
- Run dependency scanning with tools such as Dependabot or Snyk.
- Perform security testing for file uploads, authentication, AI endpoints, and prompt injection.

Read `SECURITY.md` for more information.

## CI / GitHub Actions

A GitHub Actions workflow can be added to:

- Build the backend
- Run unit tests
- Build Docker images
- Run optional integration tests

## Contributing

1. Create a feature branch.
2. Make your changes.
3. Run the relevant tests.
4. Update the documentation when behavior changes.
5. Open a pull request with a clear description.

## License

No license has been specified yet. Add a license file before distributing or reusing this project publicly.
