# Kestra Ansible Plugins: CLI vs. Runner

Kestra currently has two primary ways to execute Ansible automation: the core `io.kestra.plugin.ansible.cli.AnsibleCLI` and the community-driven `io.github.sendhiloo7.kestra.plugin.ansible.runner.AnsibleRunner`. 

This document provides a high-level overview and a deep-dive 1-to-1 comparison to help you choose the right plugin for your automation needs.

---

## High-Level Differences

### 1. The Core Engine
* **AnsibleCLI** directly executes the `ansible-playbook` command inside a script runner container. It is a lightweight, literal translation of running Ansible from your terminal. 
* **AnsibleRunner** is built on top of [Red Hat's Ansible Runner](https://ansible-runner.readthedocs.io/), which is the official enterprise execution framework used by AWX and Ansible Automation Platform. It enforces a strict directory contract (`/runner/project`, `/runner/inventory`, `/runner/env`) and interacts with Ansible programmatically.

### 2. Telemetry and Logging
* **AnsibleCLI** relies entirely on standard output (stdout) scraping. If you want structured data out of it, you generally have to parse the text output or use basic JSON callbacks.
* **AnsibleRunner** emits rich, structured telemetry. For every task and host, it generates deterministic `job_events` in JSON format. It natively isolates logs from system events, preventing Kestra UI freezes during massive scale executions.

### 3. Purpose & Philosophy
* **AnsibleCLI** is perfect for quick scripts, ad-hoc commands, and simple playbooks where you just need to get something running fast.
* **AnsibleRunner** is designed for **scale and auditability**. It is not overengineered—it simply uses the official Red Hat standard for containerized execution. If you have hundreds of hosts, need strict secrets management, or require detailed JSON audit trails of exactly what changed on every device, the Runner is the right choice.

---

## Code Comparison

Notice how `AnsibleCLI` relies on raw bash commands and environment variables, while `AnsibleRunner` uses strongly typed Kestra schema properties.

### The `AnsibleCLI` Approach
```yaml
id: ansible_cli_example
type: io.kestra.plugin.ansible.cli.AnsibleCLI
env:
  ANSIBLE_FORKS: "50"
commands:
  - ansible-playbook -i inventory.ini site.yml -vv
```

### The `AnsibleRunner` Approach
```yaml
id: ansible_runner_example
type: io.github.sendhiloo7.kestra.plugin.ansible.runner.AnsibleRunner
projectSource: kestra://workspace/network-playbooks
playbook: site.yml
inventoryInline: |
  [switches]
  192.168.1.10
forks: 50
verbosity: 2
```

---

## Solving the Enterprise Scale Problem

When automating hundreds or thousands of devices (like network switches), standard CLI plugins often cause the Kestra UI to freeze because they attempt to stream hundreds of thousands of log lines into the browser in real-time.

**AnsibleRunner solves this gracefully without overengineering:**

1. **Smart Log Batching & Capping:** The Runner streams logs to the UI in optimized batches. If a playbook exceeds a safety threshold (e.g., 100,000 lines), it stops streaming to the UI to keep Kestra responsive.
2. **`outputLogFile` Fallback:** Even if the UI stream is capped, every single log line is preserved and spooled directly to the Kestra internal storage in a downloadable `.log` file.
3. **Deterministic `results.json`:** Instead of forcing you to grep through massive text logs to find out which device failed, the Runner parses the background telemetry and returns a small, structured JSON output object. You get an exact `failedHosts` array and `stats` mapping immediately, perfectly sized for Kestra downstream tasks.

---

## 1-to-1 Feature Comparison

| Feature | `AnsibleCLI` (Core) | `AnsibleRunner` (Community) |
| :--- | :--- | :--- |
| **Execution Method** | Raw `ansible-playbook` CLI command | Programmatic via `ansible-runner run` |
| **Forks & Threads** | Passed manually via `env` variables | Native schema property (`forks: 50`) |
| **UI Responsiveness** | Can freeze Kestra UI on 10,000+ line outputs | Smart log batching/capping keeps UI responsive |
| **Output Data** | Raw text stdout | Structured JSON event stream (`results.json`) |
| **Host-Level Stats** | Requires manual text parsing | Native JSON maps (Ok, Changed, Failed per host) |
| **Audit Trail** | Only what is printed to the screen | Zipped JSON artifact of every single task event |
| **Secrets Management** | Passed in CLI or environment | Ephemeral `/runner/env/passwords` (more secure) |
| **Configuration** | Open-ended script properties | Strongly typed Java schema parameters |
| **Best For...** | Simple tasks, fast prototyping | Enterprise scale, strict audits, massive inventories |

---

## When to use which?

### Choose `AnsibleCLI` if:
* You are migrating a simple bash script that calls `ansible-playbook`.
* You just need to ping a few servers or run a quick configuration task.
* You are developing locally and want to see raw, unfiltered Ansible text logs immediately.

### Choose `AnsibleRunner` if:
* You are managing network devices (routers, switches) across hundreds of sites.
* You need to programmatically parse exactly which devices failed and which succeeded to trigger downstream Kestra tasks (e.g., Slack alerts for specific failed hosts).
* You are executing long-running playbooks (1hr+) that generate massive amounts of log output and need UI safety.
* You want to align with the Red Hat / AWX standard for execution environments (EEs).
