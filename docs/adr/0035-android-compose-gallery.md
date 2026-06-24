# ADR 0035: Android Compose gallery frontend

- Status: Accepted
- Date: 2026-06-22

## Context

ADR 0017 deliberately shipped `tiqian-compose` as Desktop/JVM first: the renderer
used Skia `TextBlob`s and the module had no Android target. The next useful
gallery surface is Android Compose, because Android is one of the real platform
text stacks Tiqian already measures through `AndroidPaintTextShaper`, and the
app can showcase and validate the Compose API in one place.

This must be a real platform frontend, not a demo shortcut:

- Compose API must remain the same public surface (`CjkParagraph`, `CjkText`,
  rich text builders, list blocks).
- Compose must not make CLREQ, fallback, glue, kinsoku, justification, or
  paragraph decisions.
- Android measuring and Android drawing must use the same typeface resolver and
  context-shaped text path.
- AGP 9 must use the official Android-KMP library plugin for KMP library modules;
  the Android application stays in a separate app module.

## Decision

Add Android variants to the layout dependency chain:

```text
core / font / shaping-api / linebreak / clreq / layout / compose
```

These modules use `com.android.kotlin.multiplatform.library` and declare their
Android target inside the Kotlin block. The gallery entry point is a separate
`tiqian-gallery-android` application module, so app packaging does not leak into
shared library modules.

Move the Compose-facing API from `jvmMain` to `commonMain`. The common
`CjkParagraph` node owns measure and draw invalidation; platform code supplies:

- the default `ParagraphMeasurer`;
- the concrete renderer for `LayoutResult`.

Desktop keeps the existing Skia backend. Android uses:

- `AndroidPaintTextShaper` for real advances / glyph ids / halt measurement;
- `AndroidFontMetricsResolver` for Android `TextPaint` raw metrics plus the
  existing explicit CJK ideographic box fallback;
- `Canvas.drawTextRun` with Han context (`中<cluster>中`) for CJK roles, mirroring
  the Android shaper's `HanContextShaping`;
- Android `Paint`/`Path` drawing for emphasis dots, frames, interlinear lines,
  ruby, and Bopomofo placements.

Android default western hyphenation is `AndroidLineBreakerHyphenator`: Android
does expose paragraph-level hyphenation controls through `LineBreaker`, but not a
public word-level dictionary API that returns every deterministic hyphenation
opportunity. The Android actual therefore treats `LineBreaker` as a platform
oracle in two modes:

- `hyphenate(word)` probes realistic word widths and records offsets where
  Android actually chooses an end-hyphen edit;
- `LineWidthHyphenator.hyphenateAtWidth(...)` lets layout pass a concrete
  available width and receive Android's preferred single hyphenation break.

This gives Android gallery real platform hyphenation without pretending the
result is TeX-compatible or exhaustive.

## Consequences

- `tiqian-compose` now produces an Android AAR and exposes the same public
  Compose API on Android and Desktop.
- Android gallery is a first-class Gradle module and can be launched as an app.
- Android rendering uses the same `LayoutResult` contract as Desktop, but the
  glyph backend is Android-native rather than Skia interop.
- CJK Android metrics remain honest about public API limits: raw metrics come
  from `TextPaint`; the ideographic box fallback is explicit until a table-backed
  Android resolver is justified.
- Android western hyphenation is platform-derived and may vary with Android
  version, locale, paint, and installed fonts. Tests that need stable layout
  should continue to inject `NoHyphenator` or another deterministic hyphenator.
- The current paragraph pipeline still pre-seeds break clusters before the
  general line breaker scores lines. `LineWidthHyphenator` is the platform
  capability boundary for remaining-width-aware refinement; it does not mean every
  possible dictionary break is exposed or stable.

## Verification

```shell
./gradlew :tiqian-compose:compileAndroidMain :tiqian-gallery-android:assembleDebug
./gradlew :tiqian-compose:compileKotlinJvm :tiqian-compose:jvmTest
```
