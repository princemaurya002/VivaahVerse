# VivaahaVerse – Wedding Expense Tracker

VivaahaVerse is a full-stack wedding expense tracker built with:

- **Backend:** Node.js, Express, Mongoose, MongoDB Atlas
- **Auth:** JWT-based authentication
- **Android app:** Kotlin, Jetpack Compose, Retrofit, DataStore

The goal is to let a couple (or family) log in, add expenses, and see a clean dashboard of total and per-category spending.

---

## Features

- **User authentication**
  - Email/password signup & login
  - JWT tokens returned by the backend and used by the app

- **Expense management**
  - Create, update, delete expenses
  - Each expense has: amount, description, date, and category (Food, Travel, Shopping, Bills, Other)

- **Dashboard & filters (Android)**
  - Top card shows **Total spent** for the logged-in user and **Spending by category**
  - All-expenses card:
    - List of all expenses in compact cards
    - Filters by category and date range
    - "Apply filter" button to fetch filtered data
    - "Reset filter" to clear filters and reload all expenses

- **Add/Edit expense dialog (Android)**
  - Calendar date picker plus manual input (`YYYY-MM-DD`)
  - Searchable category dropdown (type to filter, tap to select)
  - Validation for amount, date, and category

---

## Project Structure

```text
VivaahaVerse/
├─ backend/
│  ├─ src/
│  │  ├─ index.js              # Express app entrypoint
│  │  ├─ config/db.js          # MongoDB connection (local or Atlas)
│  │  ├─ models/User.js        # User model
│  │  ├─ models/Expense.js     # Expense model
│  │  ├─ controllers/
│  │  │  ├─ authController.js      # Signup/login
│  │  │  └─ expenseController.js   # CRUD + summary API
│  │  ├─ routes/
│  │  │  ├─ authRoutes.js
│  │  │  └─ expenseRoutes.js
│  │  └─ middleware/authMiddleware.js # JWT auth
│  ├─ package.json
│  └─ .env.example
│
├─ app/                        # Android app module
│  ├─ src/main/java/com/princemaurya/vivaahaverse/
│  │  ├─ MainActivity.kt
│  │  ├─ data/
│  │  │  ├─ model/             # Auth + Expense models
│  │  │  ├─ remote/            # Retrofit services & client
│  │  │  ├─ repository/        # AuthRepository, ExpenseRepository
│  │  │  └─ local/             # DataStore preferences
│  │  ├─ ui/
│  │  │  ├─ VivaahaVerseApp.kt # Root composable: auth vs expenses
│  │  │  ├─ auth/              # AuthViewModel, AuthScreen
│  │  │  └─ expenses/          # ExpenseViewModel, ExpenseScreen
│  │  └─ ui/theme/             # Material 3 theme
│  └─ build.gradle.kts
└─ README.md
```

---

## Backend Setup

### 1. Install dependencies

From the `backend` folder:

```bash
npm install
```

### 2. Configure MongoDB

You can run against:

- **Local MongoDB** (`mongodb://localhost:27017/expense_db`), or
- **MongoDB Atlas** (recommended)

Create a `.env` file in the `backend` folder based on `.env.example`:

```env
PORT=3000
MONGODB_URI=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/expense_db?retryWrites=true&w=majority&appName=Cluster0
JWT_SECRET=some-long-secret
JWT_EXPIRES_IN=7d
```

Make sure:

- You created a **Database User** in MongoDB Atlas.
- Your IP or `0.0.0.0/0` is allowed in Atlas Network Access (for testing).
- The URI is copied exactly from Atlas (user, password, cluster name).

### 3. Run the backend locally

From `backend/`:

```bash
npm run dev
```

This starts Express on `http://localhost:3000` (or `PORT` from `.env`).

### 4. Key API endpoints

- `POST /auth/signup` – register new user
- `POST /auth/login` – login, returns `{ token, user }`
- `GET /expenses` – list + optional filters
  - Query params: `category`, `startDate`, `endDate`, `includeSummary=true`
- `POST /expenses` – create expense
- `PUT /expenses/:id` – update expense
- `DELETE /expenses/:id` – delete expense

All expenses routes require `Authorization: Bearer <token>` header.

### 5. Summary behavior

`GET /expenses?includeSummary=true` returns:

```json
{
  "expenses": [ /* filtered list for this user */ ],
  "summary": {
    "perCategory": {
      "Food": 557,
      "Shopping": 200
    },
    "total": 757
  }
}
```

- The list respects `category`, `startDate`, and `endDate` for the **current user**.
- The `summary` always represents **all-time totals** for that user across all expenses.

---

## Deploying Backend to Render

1. Push the backend code to GitHub.
2. In the Render dashboard, create a **New Web Service**.
3. Connect it to your GitHub repo and select the backend directory (or root with the correct `package.json`).
4. Set:
   - **Build Command:** `npm install`
   - **Start Command:** `npm start` or `npm run dev` (use whatever is in `package.json`).
5. Configure environment variables in Render:
   - `PORT` (Render usually sets this automatically)
   - `MONGODB_URI` (your Atlas URI)
   - `JWT_SECRET`
   - `JWT_EXPIRES_IN`
6. Deploy, then note the public URL, e.g.:

```text
https://vivaahverse-backend.onrender.com
```

Use this URL in the Android app as `BASE_URL`.

---

## Android App Setup

### 1. Open in Android Studio

- Open the `VivaahaVerse` project in Android Studio.
- Let Gradle sync and download dependencies.

### 2. Configure API base URL

In `app/src/main/java/com/princemaurya/vivaahaverse/data/remote/ExpenseApiService.kt`:

```kotlin
object ExpenseApiClient {
    // 10.0.2.2 is the host loopback address for Android emulators
    //private const val BASE_URL = "http://10.0.2.2:3000/"
    private const val BASE_URL = "https://vivaahverse-backend.onrender.com/"
    // ...
}
```

- For **local backend from emulator**, uncomment the `10.0.2.2` line and comment out the Render URL.
- For **deployed backend**, keep the Render URL.

### 3. Run the app

- Select an Android emulator or physical device.
- Click **Run** in Android Studio.
- Sign up or log in, then start adding expenses.

---

## Android UI Overview

- **Auth screen**
  - Handles login/signup with validation.
  - Stores auth token + user info via DataStore.

- **Expenses screen** (`ExpenseScreen`)
  - **Top app bar:** title + greeting + logout.
  - **Dashboard card:**
    - Shows total spent for the logged-in user.
    - Shows per-category totals with colored labels and progress bars.
  - **All expenses card:**
    - Header row: title, filter status, Apply/Hide filters, and Reset filter.
    - Collapsible filter panel: category chips + date range inputs.
    - List of expenses in compact cards, each showing:
      - Description (single line, truncated)
      - Date (`YYYY-MM-DD`) and colored category label
      - Amount + inline edit/delete icons
  - **Dialogs:**
    - Add/Edit Expense: calendar picker, searchable category dropdown, validation.
    - Confirm Delete.

---

## Technologies Used

- **Backend**
  - Node.js, Express
  - MongoDB + Mongoose
  - JWT for authentication

- **Android**
  - Kotlin
  - Jetpack Compose (Material 3)
  - Retrofit + OkHttp
  - DataStore Preferences

---

## Future Improvements

- Multi-user or shared events (e.g., share expenses between multiple people).
- Export expenses to CSV/PDF.
- More flexible categories and custom color selection.
- Analytics: monthly charts, budget targets, overspend alerts.

---

## License

This project is currently not licensed for public distribution. Add a LICENSE file here if you plan to open source it.
