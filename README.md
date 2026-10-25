# Finify

Finify is a finance and invoicing web app built with Next.js. It lets users create an account, sign in, and manage invoices from a dashboard with revenue overview, recent sales, and quick search.

> **Status:** early-stage project. Authentication and data are currently simulated on the client; no backend or database is wired up yet.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Framework | [Next.js 15](https://nextjs.org) (App Router, Turbopack) |
| UI | React 19 (RC), [Tailwind CSS](https://tailwindcss.com), [lucide-react](https://lucide.dev) |
| Theming | [next-themes](https://github.com/pacocoursey/next-themes) (light / dark / system) |
| Language | TypeScript |
| Package manager | pnpm (a lockfile is included) |

## Getting Started

Install dependencies and start the development server:

```bash
pnpm install
pnpm dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Available Scripts

| Script | Description |
| --- | --- |
| `pnpm dev` | Start the dev server with Turbopack |
| `pnpm build` | Create a production build |
| `pnpm start` | Serve the production build |
| `pnpm lint` | Run ESLint via `next lint` |

## Project Structure

```text
fina/
├── public/                          # Static assets
├── src/
│   ├── app/
│   │   ├── layout.tsx               # Root layout + theme provider
│   │   ├── page.tsx                 # Landing page
│   │   ├── auth/
│   │   │   ├── signin/page.tsx      # Sign-in form
│   │   │   └── signup/page.tsx      # Sign-up form
│   │   ├── Home/dashboard/
│   │   │   ├── page.tsx             # Dashboard shell
│   │   │   └── components/          # Dashboard widgets
│   │   ├── components/              # Shared components (button, theme toggle)
│   │   └── ui/                      # UI helpers and styles
│   └── lib/
│       ├── definitions.ts           # Shared TypeScript types (Invoice)
│       └── utils.ts                 # Utility helpers
├── next.config.ts
├── tailwind.config.ts
└── package.json
```

## UML Diagrams

The diagrams below describe the intended system. They are written in [Mermaid](https://mermaid.js.org) and render directly on GitHub.

### Use Case Diagram

```mermaid
flowchart LR
    Visitor((Visitor))
    User((Registered User))

    subgraph Finify["Finify"]
        UC1([View landing page])
        UC2([Create account])
        UC3([Sign in])
        UC4([Browse dashboard])
        UC5([Review revenue overview])
        UC6([View recent sales])
        UC7([Search])
        UC8([Manage invoices])
        UC9([Toggle theme])
    end

    Visitor --> UC1
    Visitor --> UC2
    Visitor --> UC3
    Visitor --> UC9

    UC2 -.->|"<<include>>"| UC3
    User --> UC3
    User --> UC4
    User --> UC9

    UC4 -.->|"<<include>>"| UC5
    UC4 -.->|"<<include>>"| UC6
    UC4 -.->|"<<include>>"| UC7
    UC4 -.->|"<<include>>"| UC8
```

### Component / Architecture Diagram

```mermaid
flowchart TD
    subgraph Client["Browser"]
        Landing["Landing Page<br/>src/app/page.tsx"]
        SignIn["Sign In Page<br/>auth/signin"]
        SignUp["Sign Up Page<br/>auth/signup"]
        Dashboard["Dashboard Page<br/>Home/dashboard"]
        DashboardCmp["Dashboard Components<br/>overview · recent-sales · search"]
        Theme["ThemeProvider<br/>next-themes"]
    end

    subgraph NextApp["Next.js App Router"]
        Layout["Root Layout<br/>src/app/layout.tsx"]
        Router["Client-side Router<br/>next/navigation"]
        Lib["Shared Library<br/>lib/definitions.ts · lib/utils.ts"]
    end

    subgraph Styling["Styling"]
        Tailwind["Tailwind CSS"]
        Icons["lucide-react"]
    end

    Layout --> Theme
    Layout --> Landing
    Layout --> SignIn
    Layout --> SignUp
    Layout --> Dashboard
    Dashboard --> DashboardCmp
    SignIn --> Router
    SignUp --> Router
    Router --> Dashboard
    Landing --> Router
    DashboardCmp --> Lib
    Landing --> Tailwind
    SignIn --> Tailwind
    SignUp --> Tailwind
    Dashboard --> Tailwind
    DashboardCmp --> Icons
```

### Class Diagram

```mermaid
classDiagram
    class Invoice {
        +string id
        +string customer_name
        +string customer_email
        +string invoice_date
        +string invoice_due_date
        +number invoice_amount
        +InvoiceStatus invoice_status
        +SentStatus sent
        +string customer_id
    }

    class InvoiceStatus {
        <<enumeration>>
        pending
        paid
    }

    class SentStatus {
        <<enumeration>>
        yes
        no
    }

    class Dashboard {
        +renderOverview()
        +renderRecentSales()
        +renderSearch()
    }

    class AuthForm {
        +string email
        +string password
        +validate()
        +submit()
    }

    class SignInForm {
        +handleSubmit()
    }

    class SignUpForm {
        +string name
        +string confirmPassword
        +handleSubmit()
    }

    Invoice --> InvoiceStatus : status
    Invoice --> SentStatus : sent
    Dashboard ..> Invoice : displays
    AuthForm <|-- SignInForm
    AuthForm <|-- SignUpForm
```

### Sequence Diagram — Sign In

```mermaid
sequenceDiagram
    actor User
    participant Page as SignIn Page
    participant Router as Next.js Router
    participant Dashboard as Dashboard

    User->>Page: Enter email + password
    Page->>Page: Validate required fields
    alt Missing fields
        Page-->>User: Show "Email and password are required"
    else Valid input
        Page->>Page: Simulate API call (setTimeout)
        Page->>Router: router.push("/dashboard")
        Router->>Dashboard: Render dashboard
        Dashboard-->>User: Show overview and recent sales
    end
```

### Sequence Diagram — Sign Up

```mermaid
sequenceDiagram
    actor User
    participant Page as SignUp Page
    participant Router as Next.js Router
    participant Dashboard as Dashboard

    User->>Page: Enter name, email, password, confirm password
    Page->>Page: Validate all fields present
    Page->>Page: Check passwords match and length >= 8
    alt Validation fails
        Page-->>User: Show validation error
    else Validation passes
        Page->>Page: Simulate API call (setTimeout)
        Page->>Router: router.push("/dashboard")
        Router->>Dashboard: Render dashboard
        Dashboard-->>User: Show overview and recent sales
    end
```

## Roadmap

- [ ] Connect authentication to a real backend / auth provider
- [ ] Persist invoices in a database
- [ ] Wire up invoice creation, editing, and export
- [ ] Enable the Analytics, Reports, and Notifications dashboard tabs
- [ ] Fix auth redirect paths (`/signing` → `/signin`, dashboard route)

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md) before opening an issue or pull request.

## License

Released under the [MIT License](LICENSE). © 2026 Caleb Ngeno.
