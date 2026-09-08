package de.jrpie.android.launcher.apps

import de.jrpie.android.launcher.preferences.LauncherPreferences
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * A named group of apps. Folders are rendered as expandable headers interspersed
 * alphabetically in the app list, see
 * [de.jrpie.android.launcher.ui.list.apps.AppsRecyclerAdapter].
 *
 * [apps] is a set, so the stored format can already express membership of an app in
 * more than one folder. The current user interface keeps membership exclusive (see
 * [put]), but lifting that restriction later requires no migration of stored data.
 *
 * This mirrors [de.jrpie.android.launcher.widgets.WidgetPanel]: an id-identified,
 * serializable object stored as a set in the preferences.
 */
@Serializable
@SerialName("folder")
class Folder(val id: Int, var label: String, var apps: Set<AbstractAppInfo> = setOf()) {

    override fun equals(other: Any?): Boolean {
        return (other as? Folder)?.id == id
    }

    override fun hashCode(): Int {
        return id
    }

    fun serialize(): String {
        return Json.encodeToString(this)
    }

    /**
     * Removes the folder. Apps that were in it are not touched, they simply become
     * ungrouped and return to the top level of the list.
     */
    fun delete() {
        setAll(all().filter { it.id != this.id })
    }

    fun rename(newLabel: String) {
        label = newLabel
        // Only the label is written back, onto the freshly read folder. Storing `this`
        // instead would also write back this instance's app set, which silently drops
        // members if the instance is an older copy held by a bound list row.
        setAll(all().map { folder ->
            folder.also { if (it.id == this.id) it.label = newLabel }
        })
    }

    companion object {
        fun deserialize(serialized: String): Folder {
            return Json.decodeFromString(serialized)
        }

        /** All folders, sorted by label - the same sort context as ungrouped apps. */
        fun all(): List<Folder> {
            return (LauncherPreferences.apps().folders() ?: setOf<Folder>())
                .sortedBy { it.label.lowercase(Locale.ROOT) }
        }

        fun setAll(folders: Collection<Folder>) {
            LauncherPreferences.apps().folders(folders.toSet())
        }

        fun byId(id: Int): Folder? {
            return all().firstOrNull { it.id == id }
        }

        fun allocateId(): Int {
            return (all().maxOfOrNull { it.id } ?: 0) + 1
        }

        /** The folder [app] belongs to, or null when it is ungrouped. */
        fun of(app: AbstractAppInfo): Folder? {
            return all().firstOrNull { it.apps.contains(app) }
        }

        fun create(label: String): Folder {
            val folder = Folder(allocateId(), label)
            setAll(all() + folder)
            return folder
        }

        /**
         * Moves [app] into [folder], or removes it from every folder when [folder] is null.
         *
         * Removing the app from all other folders first is what makes membership
         * exclusive; drop that step to allow an app in several folders.
         */
        fun put(app: AbstractAppInfo, folder: Folder?) {
            setAll(all().map {
                when {
                    it.id == folder?.id -> it.also { f -> f.apps = f.apps + app }
                    else -> it.also { f -> f.apps = f.apps - app }
                }
            })
        }
    }
}
