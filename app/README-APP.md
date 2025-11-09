Phase D: Java Bootstrap

Goal:
Build a working Java application that connects to the existing MySQL database and performs the two main transactions:

T1: Pick Ticket Creation

T2: Picking Process

This phase proves that our Java frontend (Swing + JDBC) can communicate with the database and execute real operations defined in Phases B and C.

📁 Folder Structure
app/
  ├── lib/
  │    └── mysql-connector-j-8.4.0.jar      ← JDBC driver
  ├── src/
  │    └── main/java/com/ccinfom/
  │         ├── config/                     ← DB config & connection
  │         ├── model/                      ← Table models (POJOs)
  │         ├── dao/                        ← Data Access Objects
  │         ├── service/                    ← Business logic layer
  │         ├── ui/                         ← Swing forms (T1 + T2)
  │         └── util/                       ← Small helpers
  │
  ├── src/main/resources/dbconfig.properties.example
  ├── build.sh / build.bat                  ← Compile scripts
  ├── run.sh / run.bat                      ← Run scripts
  ├── README.md                             ← (this file)
  ├── DECISIONS.md                          ← Design notes
  └── .gitignore

⚙️ Setup Guide
1️⃣ Install Requirements
Tool	Version	Purpose
Java JDK	17 or higher	Run and compile the application
MySQL Server	8.0+	Existing database from Phases B/C
MySQL Connector/J	8.4.0 (JAR)	Enables JDBC connection

📂 Place the driver jar inside app/lib/.

2️⃣ Database Configuration


Copy the example file:
app/src/main/resources/dbconfig.properties.example 
→ rename it to: dbconfig.properties


Fill in your actual database credentials:
db.host=127.0.0.1
db.port=3306
db.schema=ccinfom_dev
db.user=your_username
db.password=your_password


(Optional) Use an external config path at runtime:
-Dccinfom.dbconfig=/absolute/path/to/dbconfig.properties


🔐 Never commit dbconfig.properties — it’s ignored in .gitignore.

3️⃣ Building the App
🪟 Windows
build.bat

🐧 Linux / macOS
./build.sh


This compiles all sources under
app/src/main/java/ into app/out/.

4️⃣ Running the App
🪟 Windows
run.bat

🐧 Linux / macOS
./run.sh


If configured correctly, the program will:

Display “Connected to Database” on startup.

Open the Swing window with menu/buttons for T1 and T2.

🧠 System Overview
Architecture
UI (Swing) → Service → DAO → Database (MySQL)


Layers

Layer	Package	Responsibility
Config	com.ccinfom.config	Load properties and manage DB connections
Model	com.ccinfom.model	Represent DB tables as Java objects
DAO	com.ccinfom.dao	Run SQL queries via PreparedStatements
Service	com.ccinfom.service	Apply business logic and transactions
UI	com.ccinfom.ui	Provide user interface for T1 and T2


Testing Checklist
Test	Expected Result
Run MainApp.java	App launches with “Connected to DB”
Create Pick Ticket (T1)	1 header + lines inserted; ticket status = Open
Start Picking (T2)	Picking header + lines inserted; status flips to Picking/Done
Run QA validation queries	No orphan rows / invariant violations


Definition of Done for Phase D

Phase D is complete when:

Java app connects to the database using external config (no hardcoded creds).

DAO and Service layers execute T1 and T2 correctly (end-to-end).

Swing UI forms for T1 and T2 work and reflect actual DB changes.

Validation queries (in /qa/validation_queries.sql) show no errors.

All source files follow the project folder structure and are fully documented with TODO headers.


Maintenance Tips

Keep SQL logic only in DAO classes.

Always close connections (try with resources or finally).

Test each layer individually:

DbConnection → ping MySQL.

DAO → CRUD check via console.

Service → transaction consistency.

UI → end-to-end flow.

Document successful runs in screenshots for QA.


Phase E additions:
- Launcher now exposes Pack (T3), Dispatch (T4), and Close (T5) buttons alongside T1/T2. Each form uses ComboItem dropdowns, SwingWorker loaders, and status banners so operators see validation errors (e.g., over-pack, unsealed box) immediately.
- Database prep: `mysql -u <user> -p < db/schema.sql` then load `db/seed/tx-T1.sql` through `db/seed/tx-T5.sql` in order. Seeds rely on Phase E tables, so run schema first.
- Demo SQL scripts: run `scripts/demo/demo-T1-to-T4.sql` for pick -> pack -> dispatch (includes exception cases) and `scripts/demo/demo-full-flow.sql` to close tickets (delivered + short). Capture console output for QA evidence.
- Automated service tests: from `app/`, compile with `javac -encoding UTF-8 -cp "lib/*" -d bin @sources.txt` (or equivalent) then run `java -cp "bin;lib/*;src/main/resources" com.ccinfom.test.PhaseEServiceTestRunner`. Expect "Summary: 18 passed, 0 failed" once picking/pack/dispatch/close scenarios pass.
- Manual verification: after running PhaseEServiceTestRunner, open Pack/Dispatch/Close forms in sequence with seeded tickets. Confirm ticket statuses transition Packed -> Dispatched -> Delivered/Short and inventory logs reflect reserve/delivery deltas via `inventory_txn_log`.
- Shared UI helpers live in `com.ccinfom.ui.common` (`StatusPanel`, `UiTaskRunner`) so new forms can show consistent status banners and run SwingWorker tasks without boilerplate.

Full rebuild steps for collaborators (Windows PowerShell):
  1. `cd <repo-root>` (the folder that contains this `app/` directory).
  2. Rebuild database: `mysql -u <user> -p -e "DROP DATABASE IF EXISTS ccinfom_dev; CREATE DATABASE ccinfom_dev;"` then pipe `db/schema.sql`, each file in `db/seed/` (T1?T5), and `insert_products.sql` into `mysql ccinfom_dev`.
  3. Compile UI classes: `cd app`; remove any old `bin` (`Remove-Item bin -Recurse -Force`); `New-Item bin -ItemType Directory`; set `$stage4 = @('src/main/java/com/ccinfom/ui/common/StatusPanel.java','src/main/java/com/ccinfom/ui/common/UiTaskRunner.java','src/main/java/com/ccinfom/ui/t3/PackForm.java','src/main/java/com/ccinfom/ui/t4/DispatchForm.java','src/main/java/com/ccinfom/ui/t5/CloseForm.java','src/main/java/com/ccinfom/ui/MainApp.java')`; run `javac -encoding UTF-8 -cp 'out;lib/*' -d bin $stage4`.
  4. Launch the control center: `java -cp "bin;out;lib/*;src/main/resources" com.ccinfom.ui.MainApp` (expect Phase?E banner plus buttons for T1-T5).
  5. Optional regression: `javac -encoding UTF-8 -cp "lib/*;src/main/resources;bin;out" -d bin app/src/test/java/com/ccinfom/test/PhaseEServiceTestRunner.java` then `java -cp "bin;lib/*;src/main/resources" com.ccinfom.test.PhaseEServiceTestRunner` (should finish with `Summary: 19 passed, 0 failed`).
