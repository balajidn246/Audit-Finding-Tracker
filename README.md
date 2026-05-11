# Audit Finding Tracker

## Overview

Audit Finding Tracker is an **industry-level AI-powered audit management platform** designed for enterprises to manage, track, and analyze audit findings with intelligent recommendations and comprehensive reporting.

## Key Features

✅ **AI-Powered Insights**
- Automatic finding descriptions using Groq LLaMA-3.3-70b
- AI-generated recommendations
- Intelligent categorization
- RAG-based document querying

✅ **Comprehensive Tracking**
- Create, update, and manage audit findings
- Track status and severity levels
- Soft delete with audit trail
- Full change history

✅ **Analytics & Reporting**
- Real-time analytics dashboards
- AI-generated comprehensive reports
- CSV export functionality
- Custom filters and search

✅ **Enterprise Security**
- JWT-based authentication
- Role-based access control (RBAC)
- OWASP Top 10 protection
- Audit logging of all changes
- Secure password hashing

✅ **DevOps Ready**
- Docker & Docker Compose
- Kubernetes-ready
- Health checks
- Prometheus metrics
- Scalable architecture

## Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.x**
- **PostgreSQL 15** for data persistence
- **Redis 7** for caching
- **JWT** for authentication
- **Flyway** for database migrations
- **Swagger/OpenAPI** for API documentation

### AI Service
- **Python 3.11** + **Flask 3.x**
- **Groq API** (LLaMA-3.3-70b model)
- **ChromaDB** for vector storage
- **sentence-transformers** for embeddings
- **flask-limiter** for rate limiting

### Frontend
- **React 18** + **Vite**
- **Tailwind CSS** for styling
- **Axios** for API calls
- **Recharts** for analytics

### DevOps
- **Docker** containers
- **Docker Compose** orchestration
- Production-grade configuration

## Project Structure

```
audit-finding-tracker/
├── backend/                          # Spring Boot application
│   ├── src/main/java/com/internship/tool/
│   │   ├── controller/              # REST endpoints
│   │   ├── service/                 # Business logic
│   │   ├── repository/              # Data access
│   │   ├── entity/                  # JPA entities
│   │   ├── dto/                     # Data transfer objects
│   │   ├── config/                  # Spring configuration
│   │   ├── exception/               # Exception handling
│   │   ├── security/                # JWT & security
│   │   ├── scheduler/               # Scheduled tasks
│   │   └── audit/                   # Audit logging
│   ├── src/main/resources/
│   │   ├── db/migration/            # Flyway migrations
│   │   ├── templates/               # Email templates
│   │   └── application.yml          # Configuration
│   ├── src/test/java/               # Unit & integration tests
│   └── pom.xml                      # Maven configuration
│
├── ai-service/                       # Flask AI microservice
│   ├── routes/                      # API endpoints
│   ├── services/                    # AI logic
│   ├── prompts/                     # Prompt templates
│   ├── app.py                       # Flask app
│   ├── requirements.txt             # Python dependencies
│   ├── Dockerfile                   # Container image
│   └── tests/                       # Unit tests
│
├── frontend/                         # React application
│   ├── src/
│   │   ├── components/              # Reusable components
│   │   ├── pages/                   # Page components
│   │   ├── services/                # API services
│   │   ├── hooks/                   # Custom React hooks
│   │   ├── context/                 # Context providers
│   │   ├── utils/                   # Utility functions
│   │   └── App.jsx                  # Main app component
│   ├── package.json                 # NPM configuration
│   ├── vite.config.js               # Vite configuration
│   └── Dockerfile                   # Container image
│
├── docker-compose.yml               # Multi-container setup
├── .env.example                     # Environment template
├── SECURITY.md                      # Security documentation
└── README.md                        # This file
```

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Git
- Node.js 18+ (for local frontend development)
- Java 17+ (for local backend development)
- Python 3.11+ (for local AI service development)

### Using Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/balajidn246/audit-finding-tracker.git
cd audit-finding-tracker

# Copy environment template
cp .env.example .env

# Update .env with your configuration (especially JWT_SECRET_KEY and GROQ_API_KEY)
vim .env

# Start all services
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
docker-compose logs -f

# Access the application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# AI Service Health: http://localhost:5000/health
```

### Local Development

#### Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

#### AI Service
```bash
cd ai-service
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get tokens
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - Logout user

### Audit Findings
- `GET /api/findings` - List all findings (paginated, filtered)
- `POST /api/findings` - Create new finding
- `GET /api/findings/{id}` - Get finding details
- `PUT /api/findings/{id}` - Update finding
- `DELETE /api/findings/{id}` - Soft delete finding
- `GET /api/findings/search?query=...` - Search findings
- `GET /api/findings/stats` - Get statistics

### AI Features
- `POST /api/ai/describe` - Generate description
- `POST /api/ai/recommend` - Get recommendations
- `POST /api/ai/categorize` - Categorize finding
- `POST /api/ai/generate-report` - Generate comprehensive report
- `POST /api/ai/query-documents` - RAG-based document query

### Export
- `GET /api/export/csv` - Export findings as CSV
- `GET /api/export/report` - Download generated report

### Analytics
- `GET /api/analytics/dashboard` - Dashboard metrics
- `GET /api/analytics/trends` - Trend analysis
- `GET /api/analytics/by-severity` - Severity breakdown

### Users (Admin only)
- `GET /api/users` - List all users
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `PUT /api/users/{id}/role` - Change user role

### Audit Logs
- `GET /api/audit-logs` - List audit logs
- `GET /api/audit-logs/{id}` - Get specific log

## Authentication & Authorization

### Roles
- **ADMIN** - Full access, user management, settings
- **MANAGER** - Create, edit, delete findings, view reports
- **VIEWER** - View-only access to findings and reports

### JWT Tokens
- Access Token: Valid for 15 minutes
- Refresh Token: Valid for 7 days
- Tokens stored in HTTP-only cookies

## Security Features

✅ **OWASP Top 10 Protection**
- SQL Injection prevention (PreparedStatements)
- XSS protection (Content Security Policy headers)
- CSRF protection (CSRF tokens on state-changing requests)
- Broken Authentication prevention (JWT + secure password hashing)
- Sensitive Data Exposure prevention (HTTPS, encryption)
- XML External Entities (XXE) prevention
- Broken Access Control (RBAC implementation)
- Security Misconfiguration prevention
- Using Components with Known Vulnerabilities (dependency scanning)
- Insufficient Logging & Monitoring (audit logs)

✅ **Additional Security**
- JWT with HS512 algorithm
- BCrypt password hashing
- Input validation and sanitization
- Rate limiting (flask-limiter, Spring Security)
- Security headers (HSTS, X-Content-Type-Options, etc.)
- Secure cookie configuration
- Environment-based secrets management
- File upload restrictions
- Prompt injection protection

See [SECURITY.md](./SECURITY.md) for detailed security documentation.

## Database Schema

### Key Tables
- `users` - User accounts
- `roles` - Role definitions
- `user_roles` - User-role mapping
- `findings` - Audit findings
- `finding_attachments` - File uploads
- `audit_logs` - Change tracking
- `email_notifications` - Email queue
- `ai_cache` - AI response cache

All tables are created and migrated using Flyway.

## Caching Strategy

- **GET requests**: Cached in Redis (TTL: 10 minutes)
- **AI responses**: Cached separately (TTL: 1 hour)
- **Cache keys**: Scoped by user to maintain data isolation
- **Cache invalidation**: Automatic on CREATE/UPDATE/DELETE

## Email Notifications

- Finding created notification
- Overdue finding alerts (daily at 9 AM)
- Weekly summary report (Monday at 8 AM)
- Customizable email templates

## Deployment

### Docker Compose
```bash
docker-compose -f docker-compose.yml up -d
```

### Kubernetes
Helmcharts available in `k8s/` directory (coming soon)

### Cloud Platforms
- AWS ECS/Fargate
- Google Cloud Run
- Azure Container Instances
- DigitalOcean App Platform

## Monitoring & Logging

- **Logs**: Centralized logging with Spring Cloud Config
- **Metrics**: Micrometer + Prometheus integration
- **Distributed Tracing**: Spring Cloud Sleuth (optional)
- **Health Checks**: `/actuator/health` endpoint

## Testing

### Backend
```bash
cd backend
mvn test                              # Unit tests
mvn verify                            # Integration tests
mvn clean test -Dtest=*ControllerTest  # Specific test class
```

### Frontend
```bash
cd frontend
npm test                              # Jest tests
npm run test:coverage                 # Coverage report
```

### AI Service
```bash
cd ai-service
pytest tests/                         # Run all tests
pytest tests/ -v --cov                # With coverage
```

## Performance Optimization

- Database query optimization with indexes
- Redis caching for frequently accessed data
- Connection pooling (HikariCP)
- Pagination for large datasets
- Lazy loading of related entities
- AI response caching
- Frontend code splitting with Vite
- Image optimization

## Troubleshooting

### Services won't start
```bash
# Check logs
docker-compose logs -f

# Check service status
docker-compose ps

# Restart services
docker-compose restart
```

### Database connection issues
```bash
# Verify database is running
docker-compose logs postgres

# Check database credentials in .env
cat .env | grep DB_
```

### AI service errors
```bash
# Check AI service logs
docker-compose logs ai-service

# Verify Groq API key
echo $GROQ_API_KEY

# Test AI service health
curl http://localhost:5000/health
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

MIT License - see LICENSE file for details

## Support

For issues, questions, or suggestions:
- Open an GitHub Issue
- Contact: internship@audittracker.com

## Acknowledgments

- Spring Boot team
- Groq AI team
- React community
- PostgreSQL team
