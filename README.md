## Phase F Reporting Helpers

- `db/views/helpers.sql` now defines a reusable `dim_date` calendar (2023-01-01 → 2030-12-31) plus `dim_iso_week`. These views power ISO-week grouping for R2/R4 and are sourced automatically by `db/schema.sql` after the core tables and dispatch/close modules compile.
- Java shared components under `app/src/main/java/com/ccinfom/report/` provide:
  - `ReportFilterPanel`: year dropdown + Month/ISO-week toggle plus slots for extra filters.
  - `ReportDaoBase`: JDBC helper for executing queries with consistent date-range binding.
  - `ReportTableModel`: non-editable table model with CSV export utility.

Use `dim_date` to join on `calendar_date` (or `iso_year/iso_week`) inside report SQL so every report uses the same definition for ISO weeks, weekends, and month boundaries.

## Core Record Management – Products

- New `ProductForm` (Swing) lets admins search, add, edit, and activate/deactivate products. Launch it from MainApp via **Products (Core Data)**.
- Validation is enforced via `ProductService` + `CoreValidationUtil` (SKU uniqueness, non-negative quantities/prices, reserved ≤ on-hand).
- Automated smoke test: `java -cp "app/out;app/lib/*;app/src/main/resources" com.ccinfom.daoTest.ProductCrudRunner` creates a temp product, updates it, toggles the status, and cleans up.
- Evidence/screenshot: capture the form with data and save as `docs/evidence/stageF/product-form.png`.

## Core Record Management â€“ Customers

- `CustomerForm` (Swing) allows searching, adding, editing, and activating/deactivating customer records. Launch via **Customers (Core Data)** in MainApp.
- Validation leverages `CustomerService` + `CoreValidationUtil` (required name/address, PH phone format, email format + uniqueness).
- Smoke test: `java -cp "app/out;app/lib/*;app/src/main/resources" com.ccinfom.daoTest.CustomerCrudRunner`.
- Save evidence screenshot as `docs/evidence/stageF/customer-form.png`.

## Core Record Management â€“ Branches

- `BranchForm` provides CRUD for branches (address/city/contact fields, activate/deactivate with in-progress ticket guard). Launch via **Branches (Core Data)**.
- `BranchService` uses `BranchDao` + `CoreValidationUtil` to enforce required name/address/city/phone and prevent deactivation when pick tickets are still Open→Dispatched.
- Smoke test: `java -cp "app/out;app/lib/*;app/src/main/resources" com.ccinfom.daoTest.BranchCrudRunner`.
- Screenshot target: `docs/evidence/stageF/branch-form.png`.
## Core Record Management – Employees

- EmployeeForm covers employee CRUD (first/last name, role, phone/email, activate/deactivate). Launch via **Employees (Core Data)**.
- Validation via EmployeeService + CoreValidationUtil enforces PH phone/email format + uniqueness, and blocks deactivation if pickers have open picking sessions or drivers have open dispatch manifests.
- Smoke test: java -cp "app/out;app/lib/*;app/src/main/resources" com.ccinfom.daoTest.EmployeeCrudRunner.
- Screenshot target: docs/evidence/stageF/employee-form.png.

## Core Record Management - Vehicles

- `VehicleForm` provides vehicle CRUD (plate, type, capacity, SLA hours) with activate/deactivate and guards against deactivating vehicles that are still used in active dispatch manifests. Launch via **Vehicles (Core Data)** in MainApp.
- Validation via `VehicleService` + `CoreValidationUtil` enforces plate uniqueness; allowed types (van, truck, motorcycle); non-negative capacity; non-negative SLA hours.
- Smoke test: `java -cp "app/out;app/lib/*;app/src/main/resources" com.ccinfom.daoTest.VehicleCrudRunner`.
- Screenshot target: `docs/evidence/stageF/vehicle-form.png`.

## Reports (R1/R2/R4) – Chart Views

- Each report now includes a Chart tab alongside the table and lets you save the chart as PNG.
- R1: daily outcomes grouped bars (Delivered, Short-Closed); R2: weekly picker productivity bars; R4: on-time vs late percentages.
- Tables remain the source of truth; chart data is derived directly from current rows. Use CSV/PDF export for tabular data; use “Save Chart PNG” for visuals.
