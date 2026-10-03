# Contributing to Kestra Ansible Runner Plugin

Thank you for your interest in contributing to the **Kestra Ansible Runner Plugin**!

## Development Setup

### Prerequisites
- **Java 21** or later
- **Docker** (for running execution environments and local testing)
- **Gradle 9+** (the repository includes the `./gradlew` wrapper)

### Building the Project
To compile, run documentation linter, and run unit tests:
```bash
./gradlew check
```

To produce the executable shadow JAR:
```bash
./gradlew shadowJar
```
The output JAR is generated at `build/libs/plugin-ansible-runner-<version>.jar`.

### Testing with Local Kestra
1. Start a local Kestra instance:
```bash
docker run -d --name kestra_dev_sandbox -p 8080:8080 -v /var/run/docker.sock:/var/run/docker.sock kestra/kestra:latest server standalone
```
2. Copy the built JAR:
```bash
docker cp build/libs/plugin-ansible-runner-1.0.0-SNAPSHOT.jar kestra_dev_sandbox:/app/plugins/
docker restart kestra_dev_sandbox
```
3. Open `http://localhost:8080` to build and run flows.

## Submitting Pull Requests
1. Fork the repository and create your feature branch: `git checkout -b feature/my-new-feature`
2. Ensure `./gradlew check` passes without any lint or test failures.
3. Commit your changes with clear messages.
4. Push to your branch and open a Pull Request.
