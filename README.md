# EduPlanner

EduPlanner is a JavaFX desktop application that helps students plan assessments, track study progress, and keep their resources organised. The app combines task management, rubric tracking, flashcards, and note-taking so students can stay on top of their workload from a single workspace.

## Project Resources
- `src/main/java/com/cab302/eduplanner/App.java` � JavaFX entry point and application bootstrap.
- `src/main/java/com/cab302/eduplanner/controller` � View controllers that connect FXML layouts to business logic.
- `src/main/java/com/cab302/eduplanner/service` � Core services (authentication, rubric operations, schedulers).
- `src/main/java/com/cab302/eduplanner/repository` � SQLite-backed repositories handling persistence.
- `src/main/resources/com/cab302/eduplanner` � FXML layouts and CSS that define the UI.
- `src/test/java/com/cab302/eduplanner` � Unit tests for models, repositories, controllers, and services.
- `eduplanner_database.db` � Local SQLite database used for development data.
- `.github/workflows/build.yml` � GitHub Actions workflow that runs the automated test suite.

## Feature Checklist

### Core Experience
- [x] Email/password authentication with hashed credentials (`AuthService`, `UserRepository`).
- [x] Dashboard overview for tasks and progress (`DashboardController`, `dashboard.fxml`).
- [x] Flashcard creation and study flows (`FlashcardController`, `flashcard.fxml`, `add-flashcard.fxml`).
- [x] Rubric management and scoring interface (`RubricController`, `rubric.fxml`).
- [x] Notes module with per-task annotations (`NoteController`, `note.fxml`).
- [ ] Automated weekly workload recommendations.
- [ ] Planner timetable and drag-and-drop scheduling.

### Integrations & Automation
- [ ] Google Calendar export.
- [ ] Google Drive / OneDrive resource sync.
- [ ] OCR ingestion for rubric files.
- [ ] Smart notifications and reminders.

### Quality & Delivery
- [x] SQLite persistence layer with repositories and services.
- [x] JUnit 5 unit tests for auth, flashcards, folders, dashboard, and rubrics.
- [x] GitHub Actions CI running the Maven wrapper on pull requests to `main`.
- [ ] UI / integration test suite.
- [ ] Automated packaging & release pipeline.

## Project Management & CI
- Work is tracked on the GitHub Projects board (backlog -> in progress -> review -> done) to keep priorities and ownership clear.
- Issues and pull requests are linked to the project board so progress updates automatically as cards move through the workflow.
- Continuous Integration is handled by GitHub Actions (`.github/workflows/build.yml`). Every pull request to `main` runs `./mvnw test` on any OS with Coretto JDK 21. Builds must be green before merging.

## Running the Application
- **With the Maven wrapper:** `./mvnw clean javafx:run` (macOS / Linux) or `mvnw.cmd clean javafx:run` (Windows PowerShell or Command Prompt).
- **Inside IntelliJ IDEA:** use the JavaFX run configuration pointing to `com.cab302.eduplanner.AppLauncher`.

JavaFX 21 libraries are pulled automatically by Maven; no manual SDK setup is required beyond installing JDK 21.

## Running Unit Tests

1. Ensure JDK 21 is installed and `JAVA_HOME` points to it.
2. From the project root, run the Maven wrapper:
   - Windows PowerShell / CMD: `mvnw.cmd test`
   - macOS / Linux / Git Bash: `./mvnw test`
3. Maven downloads dependencies on the first run and executes all tests under `src/test/java` via the Surefire plugin.
4. Review the summary in the terminal or open `target/surefire-reports/*.txt` for detailed results.

### Running a single test class
```
./mvnw -Dtest=AuthServiceTest test
```
Replace `AuthServiceTest` with the class you want to execute.

## Database & Login Setup

The application uses an embedded **SQLite database** (`eduplanner_database.db`) included in the project root. The schema is automatically created on first run via `DatabaseConnection.initSchema()`.

Two test accounts are pre-seeded with separate tasks for demonstration:

- **Username:** `test`  
  **Password:** `test`

- **Username:** `Rahul`  
  **Password:** `123`

These accounts can be used to log in directly after launching the app. Tasks created while logged in are persisted to the database and associated with the current user.


### Troubleshooting
- If Maven reports that it cannot find Java, verify `JAVA_HOME` and your `PATH` include JDK 21.
- Delete the `target` folder (`mvnw.cmd clean` or `./mvnw clean`) if stale compiled classes cause inconsistent results.

## Contributing
- Create a feature branch from `main` for each change set.
- Keep pull requests small, reference the related GitHub Project card, and include screenshots or screen recordings for UI updates when possible.
- Update or add tests alongside code changes so the GitHub Actions workflow remains green.
