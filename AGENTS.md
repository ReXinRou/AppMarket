# AppMarket — repository notes

Kotlin Multiplatform Android app aggregating several Chinese app stores. Modules:
`:domain` (models + repository interfaces), `:data` (source APIs/repositories),
`:app:shared` (Compose UI + view models), `:app:android`, `:app:desktop`.

## Build & test

```bash
export ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk \
       JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew :app:shared:desktopTest :data:desktopTest :domain:desktopTest
./gradlew :app:shared:compileKotlinDesktop :data:compileKotlinDesktop
```

`StoreMigrationTest.updatePrefsWaitsForTheFirstPersistedSnapshot` fails on a clean
tree (pre-existing, unrelated to feature work). Live source probes are gated behind
env vars and skipped by default.

## App vs game classification

- `domain/.../market/AppKind.kt`: `AppKind { GAME, APP, UNKNOWN }` +
  `classifyKindFromCategory()`. Deliberately NOT complementary — `UNKNOWN` stays
  unclassified so the display layer decides.
- Prefer structured source signals over name heuristics:
  Honor `appType` (1=game, 0=app); Huawei `kindName`; Xiaomi `appCategoryType` /
  `level1CategoryName` (`Games`); vivo `category` (1/2=game, 0=app) and `atype`;
  OPPO/Samsung/Wandoujia category names; TapTap is games-only.
- `MarketAppInfo.kind` carries the signal from parsers to the UI.
- `ui/model/AppCategory.kt`: `AppCategory.GAMES/APPS` each hold the target `AppKind`;
  `MarketAppInfo.matches(category)` is a strict `kind == category.kind`.
- Section search is category-aware in `SearchViewModel`: `SearchUiState.category`
  filters results and drives `loadMore` progress detection. Do not re-filter by
  category in the UI layer — it desyncs the lazy list from pagination.

## Pagination

- `SearchViewModel.loadMore()` loops up to `MAX_PAGES_PER_LOAD`, skipping pages whose
  items are all duplicates or (in a section) all foreign-kind. Progress is measured on
  the visible/filtered result count, so a page of only other-kind items keeps loading.
- Wandoujia has no reliable last-page marker: `hasMore = content.isNotEmpty() && page < 99`.
