<!-- Shields from shields.io -->
[![Android CI](https://github.com/chefcai/launcher/actions/workflows/android.yml/badge.svg?branch=feature/app-folders)](https://github.com/chefcai/launcher/actions/workflows/android.yml)
[![][shield-license]][license]

# μLauncher (folders fork)

> **This is a personal fork of [jrpie/Launcher][upstream-repo], not the official project.**
>
> It adds one thing: **folders in the app list**. Upstream has indicated they do
> not want folders as a concept in µLauncher, so this fork exists as a permanent
> divergence rather than as work heading for a pull request.
>
> **Please do not report problems with this fork to upstream.** Their issue
> tracker and chat rooms are for the official app, and its maintainers did not
> write and cannot support this code. Anything wrong here is this fork's fault.

µLauncher is an Android home screen that lets you launch apps using swipe gestures
and button presses. It is *minimal, efficient and free of distraction*.

## Download

There is no store listing for this fork. Builds are produced by CI and published
as a prerelease:

**[⬇ Download the latest debug build][fork-apk]** — or see the [release page][fork-release].

A few things worth knowing before installing:

- **This is a debug build**, signed with a debug certificate. It is not a signed
  release and is not intended for everyday use.
- The package id is `de.jrpie.android.launcher.debug`, so it **installs alongside**
  a normal µLauncher rather than replacing it. You can keep both.
- Builds are signed with a stable debug key, so a new build **installs over the top
  of an older one** and keeps your folders and settings. No need to uninstall.
- The version shown under *Settings → About* identifies the build, e.g.
  `0.2.12-folders.5-debug`.

If you want the real µLauncher, get it from [upstream][upstream-repo] — via F-Droid,
Accrescent, Obtainium or their GitHub releases. Do not use this fork for that.

## Folders

Folders group apps under a named header in the app list. They are text headers in
a text list, not an icon grid — the aim was for them to feel native to the way
µLauncher already presents apps.

**Using them**

- **Long press an app → _Add to folder_.** Pick an existing folder, or create one.
- **Tap a folder header** to expand it in place. The apps inside appear below it
  and the rest of the list moves down. Tap again to collapse.
- **Long press a folder header** to rename or delete it. Deleting a folder keeps
  its apps — they simply return to the main list.

**Behaviour**

- Folder headers are sorted alphabetically **among** the ungrouped apps, not in a
  separate section. Renaming a folder therefore moves it in the list.
- Several folders can be open at once. Expanding one never moves anything above it.
- **Search ignores folders entirely.** Typing gives a flat list of matching apps,
  exactly as it does without this feature.
- Folders do not appear in the favourites, hidden or private space lists, which
  behave exactly as upstream.

**Current limitations**

- An app can be in **one folder at a time**. The stored format already allows more,
  so this is a user interface restriction rather than a structural one.
- **Expansion is not remembered** between visits — the list opens with every folder
  closed. It does survive rotation.
- **An empty folder expands to nothing.** Its header still appears so it can be
  renamed or deleted, but tapping it has no visible effect.
- Folder names are **English only**. They are deliberately kept out of upstream's
  translation project, which does not know about this fork.

Nothing else is changed: no upstream menu, setting, action or default gesture
binding is altered by this fork.

## Features

Everything below is upstream's work and is unchanged here.

µLauncher only displays the date, time and a wallpaper.
Pressing back or swiping up (this can be configured) opens a list
of all installed apps, which can be searched efficiently.

The following gestures are available:
 - volume up / down,
 - swipe up / down / left / right,
 - swipe with two fingers,
 - swipe on the left / right resp. top / bottom edge,
 - tap, then swipe up / down / left / right,
 - draw < / > / V / Λ
 - click on date / time,
 - double click,
 - long click,
 - back button.

To every gesture you can bind one of the following actions:
 - launch an app,
 - open a list of all / favorite / private apps,
 - open µLauncher settings,
 - toggle private space lock,
 - lock the screen,
 - toggle the torch,
 - volume up / down,
 - go to previous / next audio track.

µLauncher is compatible with [work profile](https://www.android.com/enterprise/work-profile/),
so apps like [Shelter](https://gitea.angry.im/PeterCxy/Shelter) can be used.

By default the font is set to [Hack][hack-font], but other fonts can be selected.

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg"
     alt="screenshot"
     height="400">
     <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg"
     alt="screenshot"
     height="400">
     <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg"
     alt="screenshot"
     height="400">
     <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg"
     alt="screenshot"
     height="400">

*Screenshots are upstream's and do not show folders.*

## Repository layout

| Branch | What it is |
| --- | --- |
| `master` | A clean mirror of [upstream][upstream-repo]. Never modified, so syncing it is always a fast-forward. |
| `feature/app-folders` | The fork. Upstream plus folders. **This is the default branch and the one that is built and released.** |

Keeping `master` untouched means `git diff master..feature/app-folders` is always
exactly this fork's changes.

To work on it:

```bash
git clone https://github.com/chefcai/launcher
cd launcher
git remote add upstream https://github.com/jrpie/Launcher
```

Syncing with upstream is a fast-forward of `master`, then a merge into the folders
branch. Merge rather than rebase: it records each conflict resolution once instead
of replaying it on every sync.

## Building

See [build.md](docs/build.md) for instructions.

CI builds every push to `master` and `feature/**` and uploads a debug APK. The
debug signing key is restored in CI from an `ANDROID_DEBUG_KEYSTORE` repository
secret so that every build shares a signature and updates install cleanly over
each other. A local build without that secret gets whatever debug key Gradle
generates for you, which will **not** install over a CI build.

## Credits and licence

This is a fork of [µLauncher by Josia Pietsch][upstream-repo], which is itself a
fork of [Launcher by finnmglas][original-repo]. All of the application other than
the folders feature is their work.

Licensed under the MIT licence — see [LICENSE][license], which retains the original
copyright of Finn Glas and the modification copyright of Josia Pietsch. The folders
feature is contributed under the same licence.

Upstream's documentation, community and translation project are linked from
[their repository][upstream-repo]. Please direct anything that is not specific to
this fork there, and anything that *is* specific to this fork to [this
repository's issues][fork-issues].

---
  [hack-font]: https://sourcefoundry.org/hack/
  [original-repo]: https://github.com/finnmglas/Launcher
  [upstream-repo]: https://github.com/jrpie/Launcher

<!-- This fork -->

  [fork-apk]: https://github.com/chefcai/launcher/releases/download/folders-debug/ulauncher-folders-debug.apk
  [fork-release]: https://github.com/chefcai/launcher/releases/tag/folders-debug
  [fork-issues]: https://github.com/chefcai/launcher/issues

<!-- Shields and Badges -->

  [shield-license]: https://img.shields.io/badge/license-MIT-007ec6?style=flat
  [license]: https://github.com/chefcai/launcher/blob/feature/app-folders/LICENSE
