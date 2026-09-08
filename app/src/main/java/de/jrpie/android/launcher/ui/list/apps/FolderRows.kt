package de.jrpie.android.launcher.ui.list.apps

import android.os.Build
import android.widget.PopupMenu
import de.jrpie.android.launcher.R
import de.jrpie.android.launcher.apps.Folder
import de.jrpie.android.launcher.preferences.LauncherPreferences
import de.jrpie.android.launcher.preferences.list.ListLayout

/**
 * Rendering and interaction for folder header rows, and the layout each list
 * layout uses for them. Moved out of [AppsRecyclerAdapter] for the same reason as
 * [rebuildEntries]: to keep this fork's code out of files upstream edits.
 */

/**
 * Layout used for folder headers. The grid layouts reuse the linear header, since
 * a header is always rendered across the full width of the list.
 */
internal val ListLayout.folderLayoutResource: Int
    get() = when (this) {
        ListLayout.TEXT -> R.layout.list_apps_folder_row_variant_text
        else -> R.layout.list_apps_folder_row
    }

/**
 * Whether the row at [position] is a heading rather than a cell, and so must
 * take the whole width in the grid layouts.
 */
internal fun AppsRecyclerAdapter.isFullSpanRow(position: Int): Boolean {
    return when (entries.getOrNull(position)) {
        is AppListEntry.FolderEntry, is AppListEntry.FolderEndEntry -> true
        else -> false
    }
}

internal fun AppsRecyclerAdapter.bindFolder(
    viewHolder: AppsRecyclerAdapter.FolderViewHolder,
    entry: AppListEntry.FolderEntry
) {
    val expanded = expandedFolders.contains(entry.folder.id)

    viewHolder.textView.text = nameFormat.format(entry.folder.label)
    viewHolder.img.setImageResource(
        if (expanded) R.drawable.baseline_folder_open_24 else R.drawable.baseline_folder_24
    )
    viewHolder.chevron.setImageResource(
        if (expanded) R.drawable.baseline_expand_less_24 else R.drawable.baseline_expand_more_24
    )
    viewHolder.itemView.contentDescription = activity.getString(
        if (expanded) {
            R.string.content_description_folder_collapse
        } else {
            R.string.content_description_folder_expand
        }
    )

    viewHolder.itemView.setOnClickListener { toggleFolder(entry.folder) }
    viewHolder.itemView.setOnLongClickListener {
        showFolderPopup(viewHolder, entry.folder)
    }
}

/**
 * Expands or collapses a folder in place.
 *
 * Only the block directly below the header changes, so the header itself stays
 * where the user tapped it and nothing above it moves. Rows below shift by the
 * size of the folder, which RecyclerView animates as an insertion / removal.
 */
internal fun AppsRecyclerAdapter.toggleFolder(folder: Folder) {
    val headerPosition = entries.indexOfFirst {
        it is AppListEntry.FolderEntry && it.folder.id == folder.id
    }
    if (headerPosition < 0) {
        return
    }

    if (!expandedFolders.remove(folder.id)) {
        expandedFolders.add(folder.id)
    }

    val previousSize = entries.size
    rebuildEntries()
    val delta = entries.size - previousSize

    notifyItemChanged(headerPosition)
    if (delta > 0) {
        notifyItemRangeInserted(headerPosition + 1, delta)
    } else if (delta < 0) {
        notifyItemRangeRemoved(headerPosition + 1, -delta)
    }
}

@Suppress("SameReturnValue")
internal fun AppsRecyclerAdapter.showFolderPopup(
    viewHolder: AppsRecyclerAdapter.FolderViewHolder,
    folder: Folder
): Boolean {
    val popup = PopupMenu(activity, viewHolder.img)
    popup.inflate(R.menu.menu_folder)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (LauncherPreferences.list().layout() != ListLayout.TEXT) {
            popup.setForceShowIcon(true)
        }
    }
    popup.setOnMenuItemClickListener {
        when (it.itemId) {
            R.id.folder_menu_rename -> {
                folder.showRenameDialog(activity); true
            }

            R.id.folder_menu_delete -> {
                folder.showDeleteDialog(activity, root); true
            }

            else -> false
        }
    }
    popup.show()
    return true
}
