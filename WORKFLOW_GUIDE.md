# 🔄 FinFlow Business Workflows

This guide outlines the standard end-to-end journey for both **Applicants** and **Administrators** within the FinFlow Loan Management System.

---

## 🙋‍♂️ Applicant Workflow

The applicant's journey focuses on a seamless, document-driven loan request process.

```mermaid
sequenceDiagram
    participant A as Applicant
    participant Auth as Auth Service
    participant App as Application Service
    participant Doc as Document Service
    participant Notif as Notification Service

    A->>Auth: Register & Login
    Auth-->>A: JWT Issued
    A->>App: Create Draft Application
    App-->>A: AppID: 123 (Status: DRAFT)
    A->>Doc: Upload ID & Income Proof
    Doc-->>A: Documents Linked
    A->>App: Submit Application (Finalize)
    App->>Notif: Trigger Submission Event
    Notif-->>A: Email: "Application Received"
    A->>App: Monitor Status (Polling/Gateway)
```

### Key Applicant Actions:
- **`POST /api/auth/register`**: Onboard into the system.
- **`POST /api/applications`**: Start a new loan (creates a `DRAFT`).
- **`POST /api/documents/upload`**: Securely upload required KYC/Financial files.
- **`PATCH /api/applications/{id}/status?status=SUBMITTED`**: Transition the loan to the review phase.
- **`GET /api/applications/me`**: Track progress in real-time.

---

## 👔 Administrator Workflow

The administrator act as the "Reviewer," managing the risk and approval lifecycle.

```mermaid
sequenceDiagram
    participant Admin as Administrator
    participant AS as Admin Service
    participant App as Application Service
    participant Doc as Document Service
    participant Notif as Notification Service

    Admin->>AS: Fetch Pending Applications
    AS-->>Admin: List of SUBMITTED loans
    Admin->>AS: Review Applicant Details
    AS->>Doc: Peek Document Metadata
    Admin->>AS: decision: APPROVE / REJECT
    AS->>App: Update Global Status
    App->>Notif: Trigger Decision Event
    Notif-->>Admin: Audit Log Created
    Notif-->>AS: Broadcast Alert to all Admins
```

### Key Administrator Actions:
- **`GET /api/admin/applications/pending`**: View the queue of applications awaiting review.
- **`GET /api/admin/documents/{appId}`**: Audit the validity of uploaded files.
- **`POST /api/admin/decide`**: The final action to either move a loan to `APPROVED` or `REJECTED`. 
- **`GET /api/admin/audit`**: View a system-wide log of all administrative decisions for compliance across the microservice mesh.

---

## 🚦 Application Status Lifecycle

1.  **`DRAFT`**: Initial creation (Applicant).
2.  **`PENDING_DOCS`**: Awaiting file uploads (Auto-transitioned or manually flagged).
3.  **`SUBMITTED`**: Ready for administrative review (Applicant final action).
4.  **`UNDER_REVIEW`**: Currently being audited by an Admin (Admin starts work).
5.  **`APPROVED` / `REJECTED`**: The terminal business states (Admin final action).
6.  **`CLOSED`**: Lifecycle complete after disbursement or withdrawal.
