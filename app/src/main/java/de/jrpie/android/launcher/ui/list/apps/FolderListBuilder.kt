package de.jrpie.android.launcher.ui.list.apps

import de.jrpie.android.launcher.apps.AbstractAppInfo
import de.jrpie.android.launcher.apps.AbstractDetailedAppInfo
import de.jrpie.android.launcher.apps.AppFilter
import de.jrpie.android.launcher.apps.Folder
import de.jrpie.android.launcher.preferences.LauncherPreferences
import de.jrpie.android.launcher.ui.list.AbstractListActivity
import java.util.Locale

/**
 * Builds the rows of the app list, grouping apps that belong to a [Folder] behind
 * an expandable header. Moved out of [AppsRecyclerAdapter] so that this fork's
 * code sits in files upstream does not have, which keeps merge conflicts down.
 */

/**
 * Folders are shown in the plain "all apps" list, and - grouping only apps that
 * are favorited - in the favorites list. Searching flattens the list, and the
 * hidden / private space lists are already filtered views where a second
 * grouping would only get in the way.
 */
internal fun AppsRecyclerAdapter.foldersActive(): Boolean {
    return intention == AbstractListActivity.Companion.Intention.VIEW
            && appFilter.query.isEmpty()
            && appFilter.favoritesVisibility != AppFilter.Companion.AppSetVisibility.HIDDEN
            && appFilter.hiddenVisibility == AppFilter.Companion.AppSetVisibility.HIDDEN
            && appFilter.privateSpaceVisibility == AppFilter.Companion.AppSetVisibility.VISIBLE
}

/**
 * The folders that should render a header, given whether the list is currently
 * restricted to favorites.
 *
 * Outside the favorites view every folder is kept, including empty ones, so it
 * stays reachable for renaming and deleting. Inside the favorites view a folder
 * is only kept when it has at least one favorited member - an empty folder
 * header there would have nothing to expand and no favorites-specific way to
 * reach it, since folder management (rename / delete) already happens from the
 * main list.
 *
 * Pulled out of [rebuildEntries] as a pure function of [Folder.apps] and
 * [favorites] so the favorites/folders interaction can be unit tested without
 * an [android.app.Activity] to build an [AppsRecyclerAdapter] with.
 */
internal fun foldersToRender(
    folders: List<Folder>,
    onlyFavorites: Boolean,
    favorites: Set<AbstractAppInfo>
): List<Folder> {
    if (!onlyFavorites) {
        return folders
    }
    return folders.filter { folder -> folder.apps.any { it in favorites } }
}

/**
 * Rebuilds [entries] from the current app list, folders and expansion state.
 * Does not notify - callers decide which notification is appropriate.
 */
internal fun AppsRecyclerAdapter.rebuildEntries() {
    val filtered = apps.value?.let { appFilter(it) } ?: emptyList()

    entries.clear()

    if (!foldersActive()) {
        filtered.mapTo(entries) { AppListEntry.AppEntry(it) }
        return
    }

    val onlyFavorites =
        appFilter.favoritesVisibility == AppFilter.Companion.AppSetVisibility.EXCLUSIVE
    val favorites = LauncherPreferences.apps().favorites() ?: setOf()
    val folders = foldersToRender(Folder.all(), onlyFavorites, favorites)
    if (folders.isEmpty()) {
        filtered.mapTo(entries) { AppListEntry.AppEntry(it) }
        return
    }

    // An app is listed under the first folder that claims it. The stored format
    // allows an app in several folders; the UI currently keeps it to one.
    val folderByApp = HashMap<AbstractAppInfo, Folder>()
    folders.forEach { folder ->
        folder.apps.forEach { app ->
            if (!folderByApp.containsKey(app)) {
                folderByApp[app] = folder
            }
        }
    }

    // `filtered` is already sorted by label, so members stay sorted too.
    val members = HashMap<Int, MutableList<AbstractDetailedAppInfo>>()
    val topLevel = mutableListOf<Pair<String, AppListEntry>>()

    filtered.forEach { app ->
        val folder = folderByApp[app.getRawInfo()]
        if (folder == null) {
            topLevel.add(
                Pair(
                    app.getCustomLabel(activity).lowercase(Locale.ROOT),
                    AppListEntry.AppEntry(app)
                )
            )
        } else {
            members.getOrPut(folder.id) { mutableListOf() }.add(app)
        }
    }

    // Folder headers are sorted in alongside ungrouped apps. Outside the
    // favorites view, empty folders are kept so they remain reachable for
    // renaming and deleting; `folders` was already narrowed to those with a
    // favorited member above when onlyFavorites is set, so none of them are
    // empty here.
    folders.forEach { folder ->
        topLevel.add(
            Pair(folder.label.lowercase(Locale.ROOT), AppListEntry.FolderEntry(folder))
        )
    }

    topLevel.sortBy { it.first }

    topLevel.forEachIndexed { index, (_, entry) ->
        entries.add(entry)
        if (entry is AppListEntry.FolderEntry && expandedFolders.contains(entry.folder.id)) {
            val folderMembers = members[entry.folder.id]
            folderMembers?.mapTo(entries) {
                AppListEntry.AppEntry(it, inFolder = true)
            }
            // Only where the grid would otherwise merge the folder's last row
            // with what follows. An empty folder has no row to close, and a
            // folder at the very end of the list has nothing to be confused
            // with, so neither gets a dangling rule.
            if (markFolderEnd
                && !folderMembers.isNullOrEmpty()
                && index < topLevel.size - 1
            ) {
                entries.add(AppListEntry.FolderEndEntry(entry.folder.id))
            }
        }
    }
}
