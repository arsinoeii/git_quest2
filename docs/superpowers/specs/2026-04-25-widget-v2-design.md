# Quest Board Mobile Widget — V2 Design Spec

## Overview

V2 adds three capabilities to the existing 2×2 Android home screen widget: marking tasks as done, adding new tasks, and a visual design that stays eye-catching through daily colour rotation and a time-based progress bar. The widget continues to show tasks with Status = Today from the Quest Board's Task Board database.

## 1. Widget Surface

### Layout (2×2)

- **Header row:** Centred "Today" text. ⟳ sync button right-aligned.
- **Progress bar:** Replaces the static divider. 3px tall, rounded. Fills from left to right based on time of day (9am = 0%, midnight = 100%). Colour matches the day's accent. Updates on each widget refresh (~30 min system cycle + manual sync).
- **Task rows:** 3 slots. Each task is a single line of text, truncated with ellipsis if too long. No deadline text displayed — redundant since all tasks are already "Today".
- **Footer row:** + button on the left. Page dots on the right (only visible when >1 page). Tapping the dots area cycles to the next page.

### Pagination

Tasks are displayed 3 per page. When there are more than 3 tasks:
- Page dots appear in the footer (one dot per page, active dot is white, inactive is dim).
- Tapping the dots area advances to the next page, wrapping around to page 1 after the last page.
- Current page index stored in SharedPreferences, reset to 0 on each data refresh.
- No arrows — dots are the only navigation.

When tasks fit on one page, no dots are shown.

### Daily Colour Rotation

The widget background is a dark-to-colour gradient (e.g. `linear-gradient(160deg, #1a1a2e 40%, <accent>)`). The accent colour is determined by hashing the current date (`dayOfYear * 2654435761` or similar) and mapping to a palette of ~20 colours. This produces an unpredictable daily colour that never repeats on a weekly cycle.

The progress bar fill colour matches the day's accent.

Colour palette (indicative, not exhaustive):
- Coral (#e94560), Teal (#06b6d4), Amber (#f59e0b), Purple (#8b5cf6), Blue (#3b82f6), Pink (#ec4899), Green (#10b981), Red (#ef4444), Lavender (#a78bfa), Emerald (#34d399), Rose (#f472b6), Cyan (#22d3ee), Orange (#f97316), Indigo (#6366f1), Lime (#84cc16), Fuchsia (#d946ef), Sky (#38bdf8), Yellow (#eab308), Slate Blue (#6482ff), Mint (#2dd4bf)

### Interactions

| Tap target | Action |
|---|---|
| Task row | Opens TaskActionActivity overlay |
| ⟳ | Sends ACTION_REFRESH broadcast, fetches from Notion API |
| + | Opens AddTaskActivity |
| Page dots | Advances to next page |

## 2. Task Action Overlay (TaskActionActivity)

A translucent Activity that overlays the home screen when a task is tapped.

### Layout

- Solid dark background (#1a1a2e), no gradient. Rounded card (14dp radius).
- Task title — single line, ellipsis if long.
- Two pill buttons side by side:
  - **"Open ↗"** — muted pill (#ffffff12 background). Opens the task's Notion page URL via ACTION_VIEW intent.
  - **"Done ✓"** — solid green (#10b981 background, white bold text). Calls Notion API to set Status = Done, then triggers widget refresh and closes the Activity.
- Tapping outside the card dismisses the Activity (cancel).

### Notion API Call (Mark Done)

`PATCH https://api.notion.com/v1/pages/{page_id}` with body:
```json
{
  "properties": {
    "Status": { "status": { "name": "Done" } }
  }
}
```

The API call runs on a background thread. On success: trigger widget refresh, close Activity. On failure: show a Toast with the error, keep Activity open.

## 3. Add Task Form (AddTaskActivity)

A translucent Activity that overlays the home screen when + is tapped.

### Layout

Solid dark background (#1a1a2e), no gradient. Rounded card (20dp radius).

**Fields (top to bottom):**

| Field | Type | Default | Required |
|---|---|---|---|
| Title | Text input | Empty | Yes |
| Phase | Chip selector | 🏁 This Week | No (has default) |
| Status | Chip selector | Today | No (has default) |
| Life Area | Chip selector | None | No |
| Deadline | Date picker | None | No |
| Goal | Picker from Goals database | None | No |

**Phase chips:** 🏁 This Week, This Month, 🧠 Dump, This Quarter, This Year
**Status chips:** Today, To Do, Not Started
**Life Area chips:** 🧾 Admin, 💼 Career, 💬🫶 Relationships, 🧘‍♀️ Health, 🎨 Joy, 💸 Finances, 🪷 Home, 🎱 Misc

**Goal picker:** On tap, fetches goals from the Goals database (`33f95c67-143a-81f7-89d6-000b3ab547fc`) via Notion API and presents them in a scrollable list/dialog. User selects one or leaves as "None".

**Buttons:** "Cancel" (muted, closes Activity) and "Create" (solid blue #60a5fa, submits).

### Notion API Call (Create Task)

`POST https://api.notion.com/v1/pages` with body:
```json
{
  "parent": { "database_id": "<TASK_BOARD_DB_ID>" },
  "properties": {
    "Task": { "title": [{ "text": { "content": "<title>" } }] },
    "Phase": { "select": { "name": "<selected phase>" } },
    "Status": { "status": { "name": "<selected status>" } },
    "Life Area": { "select": { "name": "<selected life area>" } },
    "Deadline": { "date": { "start": "<YYYY-MM-DD>" } },
    "Goal": { "relation": [{ "id": "<goal_page_id>" }] }
  }
}
```

Omit optional fields that are left as "None". On success: trigger widget refresh, close Activity. On failure: show Toast with error, keep form open.

## 4. Technical Architecture

### Components

- **QuestWidgetProvider** (existing, modified) — handles widget lifecycle, refresh, pagination, daily colour computation, progress bar calculation.
- **TaskActionActivity** (new) — overlay for Open/Done actions on a task.
- **AddTaskActivity** (new) — overlay form for creating a new task.
- **NotionApi** (existing, extended) — add `markTaskDone(pageId)` and `createTask(...)` methods. Add `fetchGoals()` for the Goal picker.
- **TaskCache** (existing, extended) — store page IDs alongside titles so TaskActionActivity can reference them. Store current page index.
- **ColourUtil** (new) — date-to-colour hash function and palette.

### Widget Layout Changes

The widget layout (widget_layout.xml) changes from the current MVP:
- Replace divider TextView with a ProgressBar (supported in RemoteViews).
- Add + button (TextView) in the footer.
- Add page dot TextViews (3 dots max, show/hide based on page count — use "●" and "○" characters since RemoteViews can't style individual dots dynamically via drawables easily).
- Background gradient: generated as a GradientDrawable programmatically in QuestWidgetProvider and applied via RemoteViews.setInt on the root layout's background.

### Gradient Implementation

RemoteViews doesn't support setting a GradientDrawable directly. Options:
1. **Pre-generate 20 gradient drawables** as XML resources (one per palette colour) and select by index. Simple, reliable.
2. **Use a Bitmap** — generate the gradient as a Bitmap in code and set it via `setImageViewBitmap` on a background ImageView. More flexible but uses more memory.

Recommend option 1 — 20 XML drawable files is manageable and avoids runtime bitmap allocation.

### Progress Bar

Use Android's `ProgressBar` (horizontal, deterministic) in the widget layout. Set max=100, progress based on:
```
minutesSince9am = (currentHour * 60 + currentMinute) - 540
progress = (minutesSince9am * 100) / 900
// 540 = 9:00am in minutes, 900 = minutes from 9am to midnight
// Clamp to 0–100
```

Style the ProgressBar's progress colour to match the day's accent. Use `RemoteViews.setProgressBar(id, max, progress, false)`.

### Data Flow

1. Widget refresh → fetch tasks from Notion → cache tasks (with page IDs and Notion URLs) → update widget.
2. Tap task → launch TaskActionActivity with extras: title, pageId, notionUrl.
3. Tap Done → API call to mark done → broadcast ACTION_REFRESH → Activity finishes.
4. Tap + → launch AddTaskActivity.
5. Tap Create → API call to create task → broadcast ACTION_REFRESH → Activity finishes.
6. Tap dots → broadcast ACTION_PAGE_NEXT → increment page in SharedPreferences → re-render widget.

### Secrets

Same as MVP: Notion token in `local.properties` (gitignored), injected via BuildConfig. No change.

## 5. Out of Scope

- Swipe gestures (not possible on Android widgets)
- Drag-to-reorder
- Multiple widget instances bound to different columns
- Settings screen / theming UI
- Deadline display on the widget surface
- Animation (RemoteViews limitation — progress bar is the closest we get)
