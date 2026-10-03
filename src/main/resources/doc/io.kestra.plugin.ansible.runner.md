# Ansible Runner Plugin for Kestra

The Ansible Runner plugin provides enterprise-grade orchestration by running `ansible-runner` inside standardized containerized Execution Environments (EEs).

## Features
- **Standardized Execution Environments**: Execute playbooks within official or custom Execution Environments (`quay.io/ansible/ansible-runner`).
- **Standard Runner Contract**: Native mapping to the standard `/runner/` directory structure (`project`, `inventory`, `env`, and `artifacts`).
- **Tri-Layer Data Architecture**: Live UI log streaming, consolidated `results.json` telemetry, raw execution log capture, compressed artifact storage in Kestra Internal Storage, and lightweight (<1 KB) database output payloads.
- **Dependency Automation**: Native support for `galaxyDependencies`, `pythonDependencies`, and automatic installation of `requirements.yml` and `requirements.txt`.
- **Intelligent Log Management**: High-speed filtering via `logsMode: FULL | SUMMARY`, live console suppression via `streamLogs: false`, and full disk log archiving via `outputLogFile: true`.
- **Secure Secrets Handling**: Secrets (passwords, vault passwords, SSH private keys) rendered dynamically to `/runner/env/passwords` and `/runner/env/ssh_key` (0600 permissions), never exposed in CLI arguments or process lists.
- **Granular Telemetry**: Automated extraction of host-level statistics (`stats`), failure counts (`failures`), and list of failed hosts (`failedHosts`) for downstream orchestration.

## Intelligent Log Management & Zero DB Bloat
When executing large playbooks against hundreds or thousands of nodes, traditional Ansible execution floods database logs with gigabytes of unindexed text and JSON dumps. The Ansible Runner plugin manages logs with an intelligent tri-layer pipeline:

1. **Filtered Live UI Log Streaming (`streamLogs`, `logsMode`)**: Captures container stdout and stderr line-by-line and streams human-readable Ansible play, task, and host status lines in real time to the Kestra execution console. Internal runner telemetry event dumps (`uuid`, `event`, `counter` blobs) are automatically filtered out. In `SUMMARY` mode, non-essential output is suppressed to highlight task headers and status indicators.
2. **Consolidated Telemetry (`resultsUri`)**: Parses all event trees into a single, clean `results.json` stored in Kestra Internal Storage containing task breakdowns, execution durations, and host status.
3. **Immutable Artifact Compression & Blob Storage (`artifactsUri`, `outputLogFile`)**: packages runner artifacts and raw execution logs into Kestra Internal Storage (S3, MinIO, Google Cloud Storage, or Azure Blob).
4. **Sub-Kilobyte Database Footprint**: Only critical operational metrics (final status, return code, failure counters, and the list of failed hosts) are stored in Kestra's PostgreSQL/H2 database.
