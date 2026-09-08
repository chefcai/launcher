package de.jrpie.android.launcher.ui.list.apps

import de.jrpie.android.launcher.apps.AbstractAppInfo
import de.jrpie.android.launcher.apps.AbstractDetailedAppInfo
import de.jrpie.android.launcher.apps.AppFilter
import de.jrpie.android.launcher.apps.Folder
import de.jrpie.android.launcher.ui.list.AbstractListActivity
import java.util.Locale

/**
 * Builds the rows of the app list, grouping apps that belong to a [Folder] behind
 * an expandable header. Moved out of [AppsRecyclerAdapter] so that this fork's
 * code sits in files upstream does not have, which keeps merge conflicts down.
 */

/**
 * Folders are only shown in the plain "all apps" list. Searching flattens the
 * list, and the favorites / hidden / private space lists are already filtered
 * views where a second grouping would only get in the way.
 */
internal fun AppsRecyclerAdapter.foldersActive(): Boolean {
    return intention == AbstractListActivity.Companion.Intention.VIEW
            && appFilter.query.isEmpty()
            && appFilter.favoritesVisibility == AppFilter.Companion.AppSetVisibility.VISIBLE
            && appFilter.hiddenVisibility == AppFilter.Companion.AppSetVisibility.HIDDEN
            && appFilter.privateSpaceVisibility == AppFilter.Companion.AppSetVisibility.VISIBLE
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

    val folders = Folder.all()
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

    // Folder headers are sorted in alongside ungrouped apps. Empty folders are
    // kept so they remain reachable for renaming and deleting.
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
