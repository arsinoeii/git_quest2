# Mobile Task Widget — Brief

A working brief from a brainstorming session. Pick this up in a fresh session
on the user's actual machine and start building once they hand over the four
inputs listed under **What the user needs to bring**.

## The user need

The user runs an "execution Kanban" in Notion (the quest board project in this
repo). They keep forgetting what they committed to doing today. They want a
home-screen widget on their phone (Android — Samsung, personal device) that
acts as a glanceable reminder of today's tasks.

The official Notion Android widget is unusable for this: it only lets you pin
the Tasks page, shows a title, and forces you to tap through into Notion and
manually navigate to the right tab. Cluttered, slow, not glanceable.

## What we decided to build

A small **custom Android widget app**, sideloaded as an APK. Free
(no Play Store fee, no subscriptions). Talks to the Notion API directly from
the phone using the user's existing integration token.

Why not the alternatives:
- **Stock Notion widget**: already tried, rejected as cluttered/unusable.
- **KWGT** (~$5 widget builder): great for static display, weak for the
  interactive features the user wants later (add task, swipe between columns).
- **Third-party Notion widget apps**: mostly freemium/subscription, ruled out.

## The hard MVP — do not exceed without explicit ask

> The widget shows the cards from a single Notion column (the `Today` column
> of the execution Kanban). Clean card layout: title, plus one small line for
> due date if present. Tap a card → opens it in Notion. That's it.
>
> No add-task button. No swipe between columns. No drag-reorder. No in-app
> settings screen — token and database ID are hardcoded into the build since
> it's a personal app for one user.

The user is explicitly anxious about scope creep ("I do this to make my life
easier and then it becomes a two-week project"). Honor that. Ship the MVP,
let them live with it for a week, then revisit V2.

## Explicitly out of scope for MVP (V2 candidates only)

- "+" button in the widget to create a task via Notion API
- Swipe between columns inside one widget (Today / This Week / In Progress) —
  Android `StackView` widget pattern
- Drag-to-reorder (would require opening a small companion activity, since
  Android widgets cannot truly drag-reorder on the widget surface itself)
- Multiple widget instances bound to different columns
- Settings screen / theming

Do not build any of these in the first pass. Mention them only if the user
asks "what's next?".

## What the user needs to bring tomorrow

1. **Notion integration token** — they confirmed one already exists for this
   quest board project. They should paste it at the start of the session.
   Plan to rotate it in Notion once the build is done so this transcript
   can't be replayed against their workspace.
2. **Database ID** of the execution Kanban / Tasks database. Easiest: have
   them paste the full Notion URL of the database opened as a page; extract
   the ID from there.
3. **Property name and values** for the Kanban column. Almost certainly a
   `Status` select or status property. Need to know: exact property name,
   exact value to filter on (e.g. is it `Today`? Does that value already
   exist or does the user need to add it to the Notion property first?).
4. **One screenshot of a card** as it currently looks in Notion, so the
   widget shows the fields they actually care about (title only? title +
   due date? title + tag?).

## Build outline (for the next session)

- **Language/stack**: Kotlin, single-module Android app, min SDK ~26 is fine
  for a personal Samsung device (confirm their Android version on day one).
- **Components**:
  - `AppWidgetProvider` subclass — handles widget lifecycle, refresh.
  - `RemoteViewsService` + `RemoteViewsFactory` — backs a `ListView` inside
    the widget with the Notion task cards.
  - A small background fetcher (WorkManager periodic, ~15 min, plus a manual
    refresh tap target on the widget) that calls Notion's
    `POST /v1/databases/{id}/query` with a filter on the Status property,
    caches results to local storage (Room or just a JSON file — JSON is
    fine for MVP), and triggers a widget update.
  - Tap target on each row → `Intent.ACTION_VIEW` with the Notion page URL.
- **Auth**: the integration token goes into `local.properties` (gitignored)
  and is injected via `BuildConfig` at build time. Do not commit the token.
- **Output**: a release-signed APK (debug-signed is fine for personal use,
  actually — skip the signing rigmarole for MVP). Hand it to the user via
  whatever transfer is convenient (download link, attach to chat).

## Sideload flow the user will follow (Samsung, personal device)

1. Download the APK to the phone (email, Drive, direct download — anything).
2. Tap the file. Android will say "<app you downloaded with> isn't allowed
   to install apps — allow?". Tap allow once.
3. Install. The widget will appear in the long-press → Widgets picker under
   the new app's name. Drag it to a home-screen.
4. Updates: rebuild → re-download → re-install. ~2 minutes each time.

Knox is not a concern on personal-mode Samsung devices.

## Effort calibration (so we recalibrate fast tomorrow)

- **User's hands-on time across the whole MVP**: ~15-30 min, mostly spent
  pasting the token / database info, sideloading once, and giving one round
  of "tweak the spacing" feedback.
- **Build time on Claude's side**: one focused session for the MVP as
  scoped. Not days. The risk is iterations on Notion API quirks specific
  to the user's database schema, or Samsung-specific install hiccups.
- **Hard budget**: if MVP isn't installable on the phone within one working
  session, stop and re-scope with the user before continuing. Do not silently
  expand into V2.

## Notes for the next Claude session

- The user explicitly asked to be engaged with as a collaborator, not
  pitched at. They notice (and dislike) when Claude over-corrects after
  pushback or pivots the plan when none was requested. If they raise a
  concern, address the concern — don't redesign the project.
- They are not going to write code or touch Android tooling. They want
  an APK in their hand. Don't ask them to install Android Studio.
- Token handling: paste-in-chat is acceptable for this personal use case
  *because* they will rotate after. Do not invent a more elaborate auth
  flow for MVP.
- The branch for this work is `claude/mobile-task-widget-We1B7`. Stay on it.
