# Contributing to Finify

Thanks for your interest in improving Finify. This guide covers how to set up the project, submit changes, and what to expect during review.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please report unacceptable behavior to **82827770+ogc16@users.noreply.github.com**.

## Getting Started

### Prerequisites

- [Node.js](https://nodejs.org) 18.18 or newer
- [pnpm](https://pnpm.io) (recommended; `npm` also works)

### Setup

```bash
git clone https://github.com/ogc16/fina.git
cd fina
pnpm install
pnpm dev
```

The app runs at [http://localhost:3000](http://localhost:3000).

## How to Contribute

1. **Check existing issues** before starting work to avoid duplication. If you plan a large change, open an issue first so we can agree on the approach.
2. **Fork the repository** and create a branch from `main`:

   ```bash
   git checkout -b feat/short-description
   ```

3. **Make your changes** following the style guidelines below.
4. **Verify your work**:

   ```bash
   pnpm lint
   pnpm build
   ```

5. **Commit** using a clear message (see [Commit Messages](#commit-messages)).
6. **Push** your branch and open a pull request against `main`.

## Style Guidelines

- **TypeScript** for all new code; prefer explicit types over `any`.
- **Functional React components** with hooks, consistent with the existing App Router structure.
- **Tailwind CSS** for styling; reuse the shared components in `src/app/components` where possible.
- **Formatting**: match the surrounding code (2-space indentation, single quotes in most files, no semicolon drift).
- **No commented-out code or debug logging** in committed changes.
- Keep components small and focused; extract reusable widgets into `src/app/components` or the relevant `components/` folder.

## Commit Messages

Use the [Conventional Commits](https://www.conventionalcommits.org) format:

```text
<type>(<scope>): <short summary>
```

Common types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.

Examples:

```text
feat(auth): validate password length on sign up
fix(dashboard): correct overview card alignment
docs: add UML diagrams to README
```

## Pull Request Checklist

- [ ] The change is focused and scoped to a single concern
- [ ] `pnpm lint` passes
- [ ] `pnpm build` succeeds
- [ ] New or changed behavior is described in the PR description
- [ ] Documentation (README, diagrams) is updated when relevant
- [ ] No secrets, credentials, or `.env` files are included

## Reporting Bugs

When filing an issue, include:

- A short, descriptive title
- Steps to reproduce
- Expected vs. actual behavior
- Browser and OS
- Screenshots or console output if applicable

## Suggesting Features

Open an issue describing the problem you want to solve, the proposed solution, and any alternatives you considered. Mention how it fits the project's finance/invoicing scope.

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
