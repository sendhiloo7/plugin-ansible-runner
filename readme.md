# Ansible Runner Plugin for Kestra

**Class:** `io.kestra.plugin.ansible.runner.AnsibleRunner`

The Ansible Runner plugin provides native, declarative execution of the `ansible-runner` engine within Kestra workflows. By adopting the standard execution environment directory contract (`/runner/env`, `/runner/project`, `/runner/inventory`, `/runner/artifacts`), this plugin enables enterprise-grade automation orchestration with containerized Execution Environments (EEs), structured JSON telemetry, and zero database bloat.

---

## 🚀 Major Advantages & Architecture Highlights

This plugin is designed to orchestrate massive network and infrastructure fleets without the performance bottlenecks typically associated with raw CLI execution.

### 1. Intelligent Log Management (Zero Database Bloat & UI Lag)
Executing playbooks against large infrastructures or hundreds of devices can generate gigabytes of log output that easily freezes orchestrator UIs and bloats execution databases:
*   **Filtered Live UI Streaming (`streamLogs`, `logsMode`):** `AnsibleRunnerLogConsumer` intercepts container standard output and error line-by-line. In `FULL` mode, human-readable Ansible play, task, and host execution lines are streamed live to the Kestra console, automatically stripping low-level runner JSON telemetry events (`uuid`, `counter` blobs). In `SUMMARY` mode, noise is filtered out to only stream high-level task banners, statuses, and play recaps. If `streamLogs: false` is set, live streaming is disabled entirely for maximum UI performance during runs with 100,000+ log lines.
*   **Full Log Capture in Internal Storage (`outputLogFile`):** When enabled, saves 100% of the complete execution log to Kestra Internal Storage, returning a lightweight download URI via `outputs.<taskId>.outputLogFile`.
*   **Consolidated Structured Telemetry (`resultsUri`):** Instead of forcing users to inspect an archive containing thousands of individual event JSON files, the plugin parses all job events into a single, clean `results.json` file uploaded to Kestra Internal Storage (available at `outputs.<taskId>.resultsUri`).
*   **Optional Artifact Zip Archive (`saveArtifacts`, `artifactsUri`):** Packages the complete `/runner/artifacts/<ident>/` directory into a `.zip` archive stored in Kestra Internal Storage (S3, MinIO, Azure Blob, GCS) when deep compliance auditing is required.
*   **Sub-Kilobyte Database Payload (<1 KB):** Only critical operational metrics (final status, return code, failure counters, and the list of failed hosts) are stored in Kestra's execution state payload. Downstream tasks can route or trigger alerts based on `outputs.<taskId>.failedHosts` without straining database performance.

### 2. Standardized Container Execution Environments
Natively supports standardized containerized Execution Environments (`quay.io/ansible/ansible-runner:latest` or custom EEs). No need to manage host Python versions or virtual environments.

### 3. Automatic Galaxy & Python Dependency Installation
Declare collections and Python libraries directly in the flow or via standard requirement files:
*   `galaxyDependencies`: Automated `ansible-galaxy collection install` before execution.
*   `pythonDependencies`: Automated `pip install` before execution.
*   `autoInstallGalaxyRequirements`: Auto-detects and installs `requirements.yml` from `workingDir` or `project/`.
*   `autoInstallPythonRequirements`: Auto-detects and installs `requirements.txt` from `workingDir` or `project/`.

### 4. Native Secrets Isolation
Passing credentials via CLI flags (`-e "ansible_password=..."`) exposes secrets in process tables (`ps aux`). This plugin injects passwords and SSH keys as ephemeral files inside the secure `/runner/env/passwords` directory and `/runner/env/ssh_key` (0600 permissions), masked automatically by the runner engine before being securely purged.

---

## ⚙️ Plugin Properties

### Core Execution Controls
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `containerImage` | `String` | No | `quay.io/ansible/ansible-runner:latest` | Execution Environment container image bundling `ansible-runner`. |
| `checkMode` | `Boolean` | No | `false` | Dry-run check mode (`--check`) for compliance auditing without altering targets. |
| `diff` | `Boolean` | No | `false` | Show line-by-line configuration diffs (`--diff`). |
| `limit` | `String` | No | `null` | Limit execution to specific hosts or groups (`--limit`). |
| `tags` | `List<String>` | No | `null` | Tags to execute (`--tags`). |
| `skipTags` | `List<String>` | No | `null` | Tags to bypass (`--skip-tags`). |
| `verbosity` | `Integer` | No | `0` | Logging verbosity level (`0` to `4`), mapping to `-v` through `-vvvv`. |
| `forks` | `Integer` | No | `5` | Number of concurrent forks/threads (`-f`). |

### Project & Inventory Configuration
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `namespaceFiles` | `NamespaceFiles` | No | `null` | Automatically syncs files from the flow namespace into the execution environment. |
| `project` | `Project` or `String` | No | `site.yml` | Project configuration. Can be a simple string (e.g. `site.yml`) or object with `playbook: site.yml`. |
| `project.playbook` | `String` | No | `site.yml` | Entrypoint playbook filename. Automatically resolved from `workingDir`, `playbooks/`, or namespace files. |
| `project.source` | `String` | No | `null` | URI to a zip/tar archive or directory containing playbooks. |
| `project.inline` / `content` | `String` or `Map` | No | `null` | Inline playbook YAML string or map of filename-to-content. |
| `inventory` | `Inventory` or `String` | No | *Auto-detected* | Complete inventory support. Can be a string path (`inventory/hosts.ini`), inline INI text, or structured object. |
| `inventory.file` | `String` | No | `null` | Path or URI to a static or cached inventory file. |
| `inventory.inline` / `content` | `String` | No | `null` | Raw INI or YAML inventory text written directly in the flow. |
| `inventory.sources` | `List<String>` | No | `null` | Multiple inventory files, directories, or URIs merged into `/runner/inventory/`. |
| `inventory.hosts` | `Map<String, Object>` | No | `null` | Structured host map serialized to `/runner/inventory/hosts.json`. |
| `inventory.groups` | `Map<String, Object>` | No | `null` | Structured group hierarchy serialized to `/runner/inventory/hosts.json`. |
| `inventory.script` | `String` | No | `null` | Executable dynamic inventory script content or path. |

### Dependency Management
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `galaxyDependencies` | `List<String>` | No | `null` | List of Ansible Galaxy collections to install (e.g. `[community.general, cisco.ios]`). |
| `pythonDependencies` | `List<String>` | No | `null` | List of Python packages to install via pip (e.g. `[requests, netmiko]`). |
| `autoInstallGalaxyRequirements` | `Boolean` | No | `true` | Automatically detect and install `requirements.yml` if present. |
| `autoInstallPythonRequirements` | `Boolean` | No | `true` | Automatically detect and install `requirements.txt` if present. |
| `beforeCommands` | `List<String>` | No | `null` | Additional shell commands to execute before running `ansible-runner`. |

### Logging, Output & Safety Controls
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `streamLogs` | `Boolean` | No | `true` | Stream runner logs line-by-line to the Kestra execution console in real time. |
| `logsMode` | `LogsMode` | No | `FULL` | Logging mode: `FULL` streams standard lines; `SUMMARY` filters noise and highlights only task headers, statuses, and play recap. |
| `outputLogFile` | `Boolean` | No | `false` | When `true`, saves the complete execution log output to Kestra Internal Storage (`outputs.<taskId>.outputLogFile`). |
| `outputSummary` | `Boolean` | No | `false` | When `true`, includes global `summary` metrics and top-level `changed` host count in outputs. Set to `false` (default) to keep the Outputs tab clean and minimal. |
| `saveResults` | `Boolean` | No | `true` | When `true`, parses telemetry into a single structured `results.json` in Kestra Internal Storage (`outputs.<taskId>.resultsUri`). Set to `false` to omit `resultsUri` from outputs. |
| `saveArtifacts` | `Boolean` | No | `true` | Package and upload `/runner/artifacts/` as a `.zip` archive to Kestra Internal Storage (`outputs.<taskId>.artifactsUri`). Set to `false` to omit `artifactsUri` from outputs. |
| `outputFiles` | `List<String>` | No | `null` | List of glob patterns for files generated inside the container to capture and upload to Kestra Internal Storage. |
| `maxOutputsSize` | `Long` | No | `10485760` (10MB) | Maximum allowed byte size for task output payload to prevent internal queue saturation. |

### Environment & Secure Secrets (`/runner/env`)
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `env.extraVars` | `Map<String, Object>` | No | `null` | Extra variables serialized as JSON to `/runner/env/extravars`. |
| `env.passwords` | `Map<String, String>` | No | `null` | Ephemeral passwords for interactive prompts (`secret = true`). |
| `env.envVars` | `Map<String, String>` | No | `null` | Linux environment variables written to `/runner/env/envvars`. |
| `env.sshKey` | `String` | No | `null` | Private SSH key for target authentication (`secret = true`, written with 0600 permissions). |

### Engine Performance Controls
| Property | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `timeout` | `Duration` | No | `null` | Hard execution timeout (maps to runner `job_timeout`). |
| `idleTimeout` | `Duration` | No | `null` | Halts execution if no output is produced within this duration (maps to runner `idle_timeout`). |
| `autoCleanArtifacts` | `Boolean` | No | `true` | Automatically deletes the ephemeral `/artifacts` directory after processing. |

---

## 📊 Outputs Emitted

When executing an Ansible playbook, the plugin generates several different files to help you manage logs, extract data, and chain tasks. It is important to understand the difference between them:

### 1. `resultsUri` (The Structured JSON Telemetry)
* **What it is:** A clean, structured JSON file generated by parsing Ansible's internal telemetry. It contains a high-level summary of the execution: host statistics (ok/changed/failed counters) and a list of all executed tasks with their durations and statuses. 
* **What it is NOT:** It does **not** contain every single loop iteration (e.g. `runner_item_on_ok`). If you loop over 10,000 users, this JSON will only show 1 overarching task entry. This is intentional to prevent the JSON file from becoming hundreds of megabytes and crashing your memory.
* **When to use it:** Use this when you want to pass operational statistics to another Kestra task (e.g., sending a Slack alert if `failures > 0` or storing task durations in a database).

### 2. `outputLogFile` (The Raw Terminal Text)
* **What it is:** The exact, raw text output of the `ansible-playbook` command line. Even if Kestra's live UI streaming is capped at 10,000 lines (`maxLogLines`), this file will **always** contain the complete 100% execution log.
* **When to use it:** Use this when you need to read the raw terminal output line-by-line, search for specific string matches, or investigate a failure exactly as it appeared in the terminal.

### 3. `outputFiles` (The Custom Files YOUR Playbook Creates)
* **What it is:** Files explicitly extracted from the container. 
* **When to use it:** If your Ansible playbook generates specific business data (for example, a list of processed users, a generated CSV report, or a downloaded backup archive), you should have your playbook write it to a file (e.g., `ansible.builtin.copy` to `report.json`), and then configure Kestra's `outputFiles: ["report.json"]` property. Kestra will capture it and make it available for download or downstream tasks.

### 4. `artifactsUri` (The Internal Ansible Zip Archive)
* **What it is:** A ZIP file containing the internal, deeply-technical files that `ansible-runner` generates behind the scenes (the raw event JSON blobs). 
* **When to use it:** You generally **never** need to use this unless you are performing deep debugging of Ansible Runner itself, or you absolutely need to parse the raw, uncompressed JSON data for every single loop iteration (`runner_item_on_ok`).

| Output Variable | Type | Description |
| :--- | :--- | :--- |
| `resultsUri` | `URI` | **Consolidated Telemetry URL:** Link to the structured `results.json` (populated when `saveResults: true`). |
| `artifactsUri` | `URI` | **Full Artifacts Archive URL:** Link to the zipped `/runner/artifacts/` directory (populated when `saveArtifacts: true`). |
| `outputLogFile` | `URI` | **Execution Log URL:** Link to the raw execution log (populated when `outputLogFile: true`). |
| `outputFiles` | `Map<String, URI>` | **Extracted Output Files:** Map of captured file names to URIs (populated when `outputFiles` property is configured). |
| `status` | `String` | Final execution status (`successful`, `failed`, `timeout`). |
| `rc` | `Integer` | Process exit return code. |
| `failures` | `Integer` | Total count of failed hosts. |
| `changed` | `Integer` | Total count of changed hosts (populated when `outputSummary: true`). |
| `failedHosts` | `List<String>` | Array of hostnames that experienced task failures. |
| `stats` | `Map<String, Object>` | Host-by-host execution statistics breakdown (`ok`, `changed`, `failures`, `unreachable`, `skipped`). |
| `summary` | `Map<String, Integer>` | Global execution summary totals (populated when `outputSummary: true`). |

---

## 💡 Example 1: Minimal Namespace Files Execution

Running playbooks and inventories stored in Kestra Namespace Files with zero configuration boilerplate:

```yaml
id: run_namespace_playbook
namespace: dev

tasks:
  - id: ansible
    type: io.kestra.plugin.ansible.runner.AnsibleRunner

    # Syncs namespace files (playbooks/site.yml, inventory/hosts.ini)
    namespaceFiles:
      enabled: true

    # Simplified syntax: entrypoint playbook and inventory file path
    project:
      playbook: site.yml
    inventory: inventory/hosts.ini

    verbosity: 1
    logsMode: SUMMARY
    outputLogFile: true

  - id: check_results
    type: io.kestra.plugin.core.log.Log
    message: |
      Execution Status: {{ outputs.ansible.status }} (rc={{ outputs.ansible.rc }})
      Results JSON: {{ outputs.ansible.resultsUri }}
      Raw Log File: {{ outputs.ansible.outputLogFile }}
```

---

## 💡 Example 2: Enterprise Compliance Audit with Dependencies & Secrets

```yaml
id: network_compliance_audit
namespace: network.automation

tasks:
  - id: execute_runner
    type: io.kestra.plugin.ansible.runner.AnsibleRunner
    containerImage: "quay.io/ansible/ansible-runner:latest"

    # Automated Galaxy & Python Dependencies
    galaxyDependencies:
      - cisco.ios
      - community.general
    pythonDependencies:
      - netmiko
      - requests

    # Execution Controls
    checkMode: true            
    diff: true                 
    limit: "core_routers" 
    tags: 
      - "acls"
    forks: 20
    verbosity: 2
    logsMode: SUMMARY
    outputLogFile: true

    # Targeting
    project:
      source: "{{ namespace.files.get('/projects/compliance') }}"
      playbook: audit.yml
    inventory:
      file: "{{ namespace.files.get('/inventories/cached_netbox.json') }}"

    # Secure Parameter Injection
    env:
      extraVars:
        strict_enforcement: false
      envVars:
        ANSIBLE_HOST_KEY_CHECKING: "False"
      passwords:
        enable_pass: "{{ secret('cisco_enable_secret') }}"
        vault_pass: "{{ secret('ansible_vault_password') }}"

    timeout: PT30M

  - id: raise_incident
    type: io.kestra.plugin.servicenow.incident.Create
    runIf: "{{ outputs.execute_runner.failures > 0 }}"
    callerId: "Kestra Orchestrator"
    shortDescription: "Compliance Audit Failed on {{ outputs.execute_runner.failures }} Devices"
    comments: "Audit results: {{ outputs.execute_runner.resultsUri }}. Failed hosts: {{ outputs.execute_runner.failedHosts }}"
```