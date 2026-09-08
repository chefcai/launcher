package de.jrpie.android.launcher.ui.list.apps

import de.jrpie.android.launcher.apps.AbstractDetailedAppInfo
import de.jrpie.android.launcher.apps.Folder

sealed interface AppListEntry {
    data class FolderEntry(val folder: Folder) : AppListEntry
    data class AppEntry(
        val app: AbstractDetailedAppInfo,
        /** true when this row is shown because its folder is expanded */
        val inFolder: Boolean = false
    ) : AppListEntry

    /** Closes an expanded folder in the grid layouts, see [AppsRecyclerAdapter]. */
    data class FolderEndEntry(val folderId: Int) : AppListEntry
}
