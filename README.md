# Campus Board / Week 07

A Vue + Spring Boot classroom application with real password authentication,
JWT-protected writes, author-only editing, and PostgreSQL deployment.

## Start locally

Install JDK 21, Node 24 LTS, Git, and VS Code. Java 17 also works.
No global Maven, Vue CLI, Firebase CLI, Docker, or PostgreSQL install is needed
for the local path. Maven Wrapper downloads Maven 3.9.11.

Before starting the API, prepare the included wrapper. From campus-board:

```sh
cd backend
ls -a
ls .mvn/wrapper
chmod +x mvnw
./mvnw --version
```

On Windows PowerShell, use this block instead:

```powershell
cd backend
Get-ChildItem -Force
Get-ChildItem .mvn/wrapper
.\mvnw.cmd --version
```

Expected: Apache Maven 3.9.11 and Java 21 (17 also works). First use requires
internet. Keep mvnw, mvnw.cmd, and the hidden .mvn directory with pom.xml.
If files are missing, restore them from a fresh extraction of the Practice ZIP
without overwriting your edited source files. Permission denied: chmod +x mvnw.
JAVA_HOME errors: check java -version and javac -version, then fix the JDK path.
This only-script wrapper needs no maven-wrapper.jar.
Reference: https://maven.apache.org/tools/wrapper/

Terminal A, from campus-board:

```sh
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

On Windows PowerShell, use `.\mvnw.cmd spring-boot:run` instead of the last two lines.

Terminal B, from campus-board:

```sh
cd frontend
npm ci
npm run dev
```

Open http://localhost:15707. The API is http://localhost:18707.
Swagger UI is http://localhost:18707/swagger-ui.html.
Week 07 uses these dedicated local ports. Stop and restart an already-running
Vite or Java process after updating this project. A second copy of this same
project cannot use the same ports simultaneously. Render supplies PORT in
production; local defaults do not change the public HTTPS address.
The H2 database persists in backend/data. It is intentionally excluded from Git.
Do not delete that folder unless you intend to reset your local practice data.
Tokens stay in browser memory. Reload requires another sign-in.
The local signing key changes when the Java process restarts.

## Run the complete application

All authentication and author-permission code is implemented. No source edits
are required for this workshop. Run the provided project, register Alex and Sam,
publish a post, check editing permissions, and deploy the same tested code.
Use docs/Code_Walkthrough_EN.html to understand how the implementation works.

## Verify

From backend: `./mvnw test` (Windows: `.\mvnw.cmd test`).
From frontend: `npm test` and `npm run build`.
From the project root, with the API running:

```sh
node scripts/smoke.mjs http://localhost:18707
```

The smoke test leaves two practice accounts and deletes its test post.
The backend tests use an isolated in-memory database, not your local accounts.

## Deploy

Create an account, or sign in if you already have one:

- GitHub (repository): https://github.com/signup
- Neon (PostgreSQL database): https://console.neon.tech/signup
- Render (web service): https://dashboard.render.com/register

Complete any requested account verification. These cloud accounts are separate
from the Alex and Sam accounts inside Campus Board. Choose Free lab resources.

Follow docs/Lab_Guide_EN.html. The Dockerfile builds Vue, runs Java tests,
and embeds the static assets inside the Spring Boot JAR.
Create a Neon Free PostgreSQL project. Create a Render Free Web Service
with Docker runtime and health check /actuator/health. Enter:

- SPRING_PROFILES_ACTIVE=prod
- DB_URL=jdbc:postgresql://YOUR_DIRECT_NEON_HOST/neondb?sslmode=require
- DB_USERNAME and DB_PASSWORD from Neon Connect
- JWT_SECRET generated with the command below

```sh
node -e "console.log(require('node:crypto').randomBytes(32).toString('base64'))"
```

.env.example is a reference. Spring Boot does not load it automatically.
Never commit credentials. In production, missing configuration stops startup.
Render supplies PORT. Keep the Docker command override blank.
Do not create an additional frontend host for this lab.
Local H2 data is not uploaded to Neon. Register new cloud practice accounts.

```sh
node scripts/smoke.mjs https://YOUR_SERVICE.onrender.com
```

## Limits

This is a teaching deployment. It has no email verification, password recovery,
refresh tokens, server-side logout revocation, or signup/login rate limiting.
A copied access token remains valid until expiry unless the signing key changes.
Public posts contain practice information only. Add those controls and review
operations before using it for a real public service. Free platform limits apply.

## Documentation

- docs/Visual_Walkthrough_EN.html: click-by-click steps with real screenshots and troubleshooting.
- docs/Lab_Guide_EN.html: setup, checkpoints, deployment, and Swagger practice.
- docs/Code_Walkthrough_EN.html: detailed source explanations.
- docs/API_Reference_EN.html: endpoint and error reference.
- docs/Submission_Checklist_EN.html: practice evidence checklist, with no new grading rules.

Third-party dependencies and the Maven Wrapper retain their original licenses.
