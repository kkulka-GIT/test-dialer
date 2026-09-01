# Multi-agent development workflow

This repository uses a controlled, feature-oriented workflow for agent-assisted development.

## Roles

### Project owner

The project owner makes high-level product decisions, approves scope changes, and decides which completed features may be integrated.

### Coordinator

The coordinator owns cross-project architecture and delivery control. The coordinator:

- translates product goals into bounded features;
- assigns one agent to one feature branch;
- reviews plans, commits, diffs, tests, and CI results;
- checks interactions with the rest of the application;
- authorizes continuation at checkpoints;
- escalates only high-level product decisions or material risk;
- is the only agent allowed to merge changes into `main`.

### Feature agent

A feature agent works only within the assigned scope and branch. The agent:

- inspects current code and repository guidance before editing;
- proposes a short implementation plan to the coordinator;
- makes small, logical commits;
- pushes checkpoints to the feature branch;
- maintains a report under `brain/reports/`;
- opens a Draft pull request;
- reports CI results and blockers;
- never merges to `main`.

## Branch and pull request rules

- One feature uses one uniquely named branch.
- No feature work is committed directly to `main`.
- Force push and history rewriting are prohibited.
- Every feature is presented as a Draft pull request before review.
- GitHub Actions is the standard APK build and verification path.
- Passing CI does not authorize a merge.
- Only the coordinator may merge after review and project-owner approval where required.

## Checkpoints

A feature agent pauses for coordinator review after:

1. repository and dependency analysis;
2. the proposed implementation plan;
3. any architectural discovery that changes the expected scope;
4. the first coherent implementation;
5. tests and GitHub Actions;
6. final documentation and handoff.

The coordinator may approve continuation, request changes, narrow the task, or stop it.

## Stop conditions

Work stops without destructive or speculative changes when:

- required behavior is ambiguous and materially affects the product;
- implementation would cross feature boundaries;
- a migration could lose or invalidate user data;
- new Android permissions, paid actions, secrets, or external credentials are required;
- a test could initiate an uncontrolled call, SMS, or data transfer;
- an unexpected architectural rewrite is needed;
- existing behavior, tests, build, or CI is broken;
- the branch conflicts with its base or cannot be updated safely;
- repository permissions or tooling prevent a required operation.

The agent reports the evidence and waits for the coordinator. The coordinator resolves technical questions in the context of the whole project and consults the project owner only for high-level decisions.

## Safety and traceability

- Application code changes are excluded from process-only branches.
- Secrets, subscriber identifiers, internal endpoints, and credentials must not be committed.
- Existing user data and repository history must be preserved.
- Each report records branch, scope, commits, verification, CI, known limitations, and decision status.
