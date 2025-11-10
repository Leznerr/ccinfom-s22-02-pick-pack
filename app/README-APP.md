# Phase E: Production Application

This document provides instructions for compiling, running, and testing the final Phase E Pick & Pack Java application.

## 1. UI Workflow

The application follows a strict transactional order from ticket creation to closing. Launch each form from the `MainApp` control center in the following sequence:

1.  **T1: Create Pick Ticket (`TicketForm`)**
    *   **Purpose:** Register a new customer order (pick ticket).
    *   **Action:** Select a customer and branch, then add products with requested quantities.
    *   **Outcome:** A new pick ticket with status `Open`.

2.  **T2: Allocate & Pick (`PickingForm`)**
    *   **Purpose:** Assign a picker and record the items picked from the warehouse.
    *   **Action:** Load an `Open` ticket, record picked quantities, and handle any shortages.
    *   **Outcome:** Ticket status moves to `Picking` and then `Done` upon completion.

3.  **T3: Pack & Box (`PackForm`)**
    *   **Purpose:** Group picked items into sealed boxes for shipment.
    *   **Action:** Load a `Done` ticket, assign items to boxes, and mark boxes as sealed.
    *   **Outcome:** Ticket status moves to `Packed`.

4.  **T4: Schedule & Dispatch (`DispatchForm`)**
    *   **Purpose:** Assign a driver and vehicle to transport the sealed boxes.
    *   **Action:** Load a `Packed` ticket, create a manifest, and add sealed boxes.
    *   **Outcome:** Ticket status moves to `Dispatched`.

5.  **T5: Close Ticket (`CloseForm`)**
    *   **Purpose:** Reconcile the delivery and formally close the ticket.
    *   **Action:** Load a `Dispatched` ticket and record the final delivery status (`Delivered` or `Short-Closed`).
    *   **Outcome:** Inventory is adjusted, and the ticket is finalized with status `Delivered` or `Closed`.

## 2. Core Operations

All commands should be run from the repository root directory.

### Database Reset & Seed

To ensure a clean environment, reset the database and load all required schemas and seed data.

**Command (using MySQL client):**
```bash
# 1. Drop and recreate the database
mysql -u your_user -p -e "DROP DATABASE IF EXISTS ccinfom_dev; CREATE DATABASE ccinfom_dev;"

# 2. Load schema and all seed data
mysql -u your_user -p ccinfom_dev < db/schema.sql
mysql -u your_user -p ccinfom_dev < db/seed/cores.sql
mysql -u your_user -p ccinfom_dev < db/seed/tx-T1.sql
mysql -u your_user -p ccinfom_dev < db/seed/tx-T2.sql
mysql -u your_user -p ccinfom_dev < db/seed/tx-T3.sql
mysql -u your_user -p ccinfom_dev < db/seed/tx-T4.sql
mysql -u your_user -p ccinfom_dev < db/seed/tx-T5.sql
mysql -u your_user -p ccinfom_dev < insert_products.sql
```

### Compile Application

The project uses a `sources.txt` file to manage the list of Java files for compilation.

**Command (from repository root):**
```powershell
# Ensure the output directory exists
if (-not (Test-Path app/out)) { New-Item -ItemType Directory app/out }

# Compile all source files
javac -encoding UTF-8 -d app/out -cp "app/lib/*" @sources.txt
```

### Run Application

Launch the main Swing application. Ensure your `dbconfig.properties` is correctly configured in `app/src/main/resources/`.

**Command (from repository root):**
```powershell
java -cp "app/out;app/lib/*;app/src/main/resources" com.ccinfom.ui.MainApp
```

### Run Service Tests

Execute the automated service-layer tests to verify business logic for all transactions.

**Command (from repository root):**
```powershell
# 1. Compile the test runner
javac -encoding UTF-8 -d app/out -cp "app/out;app/lib/*;app/src/main/resources" app/src/test/java/com/ccinfom/test/PhaseEServiceTestRunner.java

# 2. Run the tests
java -cp "app/out;app/lib/*;app/src/main/resources" com.ccinfom.test.PhaseEServiceTestRunner
```
**Expected Output:** A summary indicating all tests passed (e.g., `Summary: 19 passed, 0 failed`).

### Run QA Validation

The `run-validation.ps1` script executes a series of SQL queries to validate the integrity of the database after running demos or tests.

**Command (from repository root):**
```powershell
./scripts/qa/run-validation.ps1
```
**Note:** You may need to configure the PowerShell script with your database credentials.

## 3. Demo Scripts

The `scripts/demo/` directory contains SQL scripts to demonstrate key application workflows directly in a SQL client like MySQL Workbench.

-   **`demo-T1-to-T4.sql`**
    *   **Purpose:** Shows the lifecycle from ticket creation through dispatch, including common exceptions like attempting to pack more than was picked or dispatch an unsealed box.
    *   **Use Case:** Ideal for verifying the T1-T4 transaction logic in isolation.
    *   **Prereq:** Seed only through `db/seed/tx-T3.sql` (do **not** run T4/T5) so the script can create its own dispatch records.

-   **`demo-full-flow.sql`**
    *   **Purpose:** Demonstrates the complete, end-to-end process for both a fully delivered ticket and a short-closed ticket. It includes final inventory adjustments and logging.
    *   **Use Case:** Perfect for a full system demonstration and for validating the final `Close` (T5) logic.
    *   **Prereq:** Apply `db/seed/tx-T4.sql` before running so dispatch IDs 5 and 6 exist; reseed afterward if you need to restore the canonical seed state.

## 4. Folder Structure

```text
app/
|-- lib/
|   `-- mysql-connector-j-8.4.0.jar        <- JDBC driver
|-- src/
|   `-- main/java/com/ccinfom/
|       |-- config/                        <- DB config & connection helpers
|       |-- model/                         <- Table models (POJOs)
|       |-- dao/                           <- Data access objects
|       |-- service/                       <- Business logic layer
|       |-- report/                        <- Phase F report helpers/DAOs (TODO)
|       |-- ui/
|       |   |-- common/                    <- Shared Swing widgets (ComboItem, StatusPanel, etc.)
|       |   |-- t1 ... t5                  <- Transaction forms
|       |   `-- report/                    <- Report forms (Phase F TODO)
|       |-- infra/                         <- Cross-cutting helpers (InventoryHelper)
|       `-- util/                          <- Shared utilities (JdbcUtil)
|-- src/main/resources/dbconfig.properties.example
|-- build.sh / build.bat                   <- Compile scripts
|-- run.sh / run.bat                       <- Launch scripts
|-- README-APP.md                          <- This guide
|-- DECISIONS.md                           <- Design notes
`-- .gitignore
```

> TODO[Phase F]: Add a new section documenting the four report screens, their filters, and how to run/validate them once implementation is complete.
