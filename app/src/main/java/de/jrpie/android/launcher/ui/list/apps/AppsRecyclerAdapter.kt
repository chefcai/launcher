package de.jrpie.android.launcher.ui.list.apps

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.graphics.drawable.DrawableWrapper
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import de.jrpie.android.launcher.Application
import de.jrpie.android.launcher.R
import de.jrpie.android.launcher.actions.Action
import de.jrpie.android.launcher.actions.Gesture
import de.jrpie.android.launcher.apps.AbstractDetailedAppInfo
import de.jrpie.android.launcher.apps.AppFilter
import de.jrpie.android.launcher.apps.AppInfo
import de.jrpie.android.launcher.apps.DetailedAppInfo
import de.jrpie.android.launcher.apps.Folder
import de.jrpie.android.launcher.getAppShortcuts
import de.jrpie.android.launcher.preferences.LauncherPreferences
import de.jrpie.android.launcher.preferences.list.AppNameFormat
import de.jrpie.android.launcher.preferences.list.ListLayout
import de.jrpie.android.launcher.ui.list.AbstractListActivity
import de.jrpie.android.launcher.ui.transformMonochrome

/**
 * One row of the app list: either an app, or a folder header that can be
 * expanded to reveal the apps inside it.
 */
/**
 * A [RecyclerView] (efficient scrollable list) containing all apps on the users device.
 * The apps details are represented by [AppInfo].
 *
 * Apps that belong to a [Folder] are collapsed behind a folder header. Headers are
 * sorted into the list alphabetically alongside ungrouped apps, and expand inline.
 * Grouping is skipped while searching and in the filtered lists (favorites, hidden,
 * private space), so those keep behaving exactly as before.
 *
 * @param activity - the activity this is in
 * @param intention - why the list is displayed ("view", "pick")
 * @param forGesture - the action which an app is chosen for (when the intention is "pick")
 */
@SuppressLint("NotifyDataSetChanged")
class AppsRecyclerAdapter(
    val activity: Activity,
    val root: View,
    internal val intention: AbstractListActivity.Companion.Intention = AbstractListActivity.Companion.Intention.VIEW,
    private val forGesture: String? = "",
    internal var appFilter: AppFilter = AppFilter(activity, ""),
    internal val layout: ListLayout,
    internal val nameFormat: AppNameFormat
) :
    RecyclerView.Adapter<AppsRecyclerAdapter.BaseViewHolder>() {


    internal val apps = (activity.applicationContext as Application).apps
    internal val entries: MutableList<AppListEntry> = mutableListOf()
    private val theme = LauncherPreferences.theme()
    private val colorTheme = theme.colorTheme()
    private val grayscale = colorTheme.monochromeIcons()

    /**
     * Ids of the folders that are currently open. Several folders may be open at
     * once: expanding one never moves the rows above it, which keeps the list
     * predictable while browsing.
     *
     * This is deliberately not persisted - the list opens with every folder closed.
     */
    internal val expandedFolders = mutableSetOf<Int>()

    private val folderIndentPx = (24 * activity.resources.displayMetrics.density).toInt()

    // Indenting members of a folder only makes sense in the linear layouts.
    private val indentFolderMembers =
        layout == ListLayout.DEFAULT || layout == ListLayout.TEXT

    // The grid layouts cannot indent, and worse, they complete a folder's last
    // partially filled row with the apps that follow it. They get a full span
    // marker after the last member instead, which breaks the row and closes the
    // block. The linear layouts need neither, one item per row already.
    internal val markFolderEnd =
        layout == ListLayout.GRID || layout == ListLayout.GRID_ONLY_ICONS

    // temporarily disable auto launch
    var disableAutoLaunch: Boolean = false

    init {
        apps.observe(this.activity as AppCompatActivity) {
            updateAppsList()
        }
        updateAppsList()
    }


    abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class AppViewHolder(itemView: View) : BaseViewHolder(itemView),
        View.OnClickListener {
        var textView: TextView = itemView.findViewById(R.id.list_apps_row_name)
        var img: ImageView = itemView.findViewById(R.id.list_apps_row_icon)

        override fun onClick(v: View) {
            val rect = Rect()
            img.getGlobalVisibleRect(rect)
            selectItem(bindingAdapterPosition, rect)
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    inner class FolderEndViewHolder(itemView: View) : BaseViewHolder(itemView)

    inner class FolderViewHolder(itemView: View) : BaseViewHolder(itemView) {
        var textView: TextView = itemView.findViewById(R.id.list_apps_folder_row_name)
        var img: ImageView = itemView.findViewById(R.id.list_apps_folder_row_icon)
        var chevron: ImageView = itemView.findViewById(R.id.list_apps_folder_row_chevron)
    }


    override fun getItemViewType(position: Int): Int {
        return when (entries[position]) {
            is AppListEntry.FolderEntry -> VIEW_TYPE_FOLDER
            is AppListEntry.FolderEndEntry -> VIEW_TYPE_FOLDER_END
            is AppListEntry.AppEntry -> VIEW_TYPE_APP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_FOLDER ->
                FolderViewHolder(inflater.inflate(layout.folderLayoutResource, parent, false))

            VIEW_TYPE_FOLDER_END ->
                FolderEndViewHolder(
                    inflater.inflate(R.layout.list_apps_folder_end, parent, false)
                )

            else -> AppViewHolder(inflater.inflate(layout.layoutResource, parent, false))
        }
    }

    override fun onBindViewHolder(viewHolder: BaseViewHolder, i: Int) {
        when (val entry = entries[i]) {
            is AppListEntry.FolderEntry -> bindFolder(viewHolder as FolderViewHolder, entry)
            is AppListEntry.AppEntry -> bindApp(viewHolder as AppViewHolder, entry)
            is AppListEntry.FolderEndEntry -> { /* static rule, nothing to bind */ }
        }
    }

    private fun bindApp(viewHolder: AppViewHolder, entry: AppListEntry.AppEntry) {
        val appInfo = entry.app
        var appLabel = appInfo.getCustomLabel(activity)

        val appIcon = appInfo.getIcon(activity)

        viewHolder.img.transformMonochrome(grayscale, colorTheme)
        viewHolder.img.setImageDrawable(appIcon.constantState?.newDrawable() ?: appIcon)

        if (layout.useBadgedText) {
            appLabel = activity.packageManager.getUserBadgedLabel(
                appLabel,
                appInfo.getUser(activity)
            ).toString()
        }
        viewHolder.textView.text = nameFormat.format(appLabel)

        // Apps revealed by an open folder are indented, so the list shows what
        // belongs to the header above it.
        viewHolder.itemView.setPaddingRelative(
            if (entry.inFolder && indentFolderMembers) folderIndentPx else 0,
            0, 0, 0
        )

        // decide when to show the options popup menu about
        if (intention == AbstractListActivity.Companion.Intention.VIEW) {
            viewHolder.textView.setOnLongClickListener {
                showOptionsPopup(viewHolder, appInfo)
            }
            viewHolder.img.setOnLongClickListener {
                showOptionsPopup(viewHolder, appInfo)
            }
            // ensure onClicks are actually caught
            viewHolder.textView.setOnClickListener { viewHolder.onClick(viewHolder.textView) }
            viewHolder.img.setOnClickListener { viewHolder.onClick(viewHolder.img) }
        }
    }

    @Suppress("SameReturnValue")
    private fun showOptionsPopup(
        viewHolder: AppViewHolder,
        appInfo: AbstractDetailedAppInfo
    ): Boolean {

        /* TODO:
            This popup menu needs to be replaced by a custom solution.
            The following should be implemented, but are not possible using android.widget.PopupMenu:
                - display a separator before shortcuts
                - show static shortcuts in one line (small icons, no text)
                - add long press handler to bind shortcuts to gestures
         */
        val popup = PopupMenu(activity, viewHolder.img)
        popup.inflate(R.menu.menu_app)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (LauncherPreferences.list().layout() != ListLayout.TEXT) {
                popup.setForceShowIcon(true)
            }
        }

        if (!appInfo.isRemovable()) {
            popup.menu.findItem(R.id.app_menu_delete).isVisible = false
        }

        if (appInfo !is DetailedAppInfo) {
            popup.menu.findItem(R.id.app_menu_info).isVisible = false
        }

        if (LauncherPreferences.apps().hidden()?.contains(appInfo.getRawInfo()) == true) {
            popup.menu.findItem(R.id.app_menu_hidden).apply {
                setTitle(R.string.list_app_hidden_remove)
                setIcon(R.drawable.baseline_visibility_24)
            }

        }

        if (LauncherPreferences.apps().favorites()?.contains(appInfo.getRawInfo()) == true) {
            popup.menu.findItem(R.id.app_menu_favorite).apply {
                setTitle(R.string.list_app_favorite_remove)
                setIcon(R.drawable.baseline_favorite_24)
            }
        }

        if (Folder.of(appInfo.getRawInfo()) != null) {
            popup.menu.findItem(R.id.app_menu_folder).setTitle(R.string.list_app_folder_change)
        }

        // shortcuts
        if (appInfo is DetailedAppInfo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val iconSize = (24 * activity.resources.displayMetrics.density).toInt()
            val launcherApps =
                activity.getSystemService(Service.LAUNCHER_APPS_SERVICE) as LauncherApps
            getAppShortcuts(appInfo.getRawInfo(), activity)
                .forEach { shortcutInfo ->
                    // getShortcutBadgedIconDrawable is documented to return null when
                    // the shortcut is invalid or its icon cannot be loaded. Dereferencing
                    // it unconditionally crashed the whole long press menu for such an
                    // app; show the shortcut without an icon instead, so it stays usable.
                    val fixedIcon =
                        launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0)
                            ?.also { it.transformMonochrome(grayscale, colorTheme) }
                            ?.let { shortcutIcon ->
                                object : DrawableWrapper(shortcutIcon) {
                                    override fun getIntrinsicWidth(): Int = iconSize
                                    override fun getIntrinsicHeight(): Int = iconSize
                                }
                            }
                    popup.menu.add(shortcutInfo.shortLabel).apply {
                        icon = fixedIcon
                        setOnMenuItemClickListener {
                            launcherApps.startShortcut(shortcutInfo, null, null)
                            true
                        }
                    }
                }
        }

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.app_menu_delete -> {
                    appInfo.getRawInfo().uninstall(activity); true
                }

                R.id.app_menu_info -> {
                    (appInfo.getRawInfo() as? AppInfo)?.openSettings(activity); true
                }

                R.id.app_menu_favorite -> {
                    appInfo.getRawInfo().toggleFavorite(); true
                }

                R.id.app_menu_hidden -> {
                    appInfo.getRawInfo().toggleHidden(root); true
                }

                R.id.app_menu_rename -> {
                    appInfo.showRenameDialog(activity); true
                }

                R.id.app_menu_folder -> {
                    appInfo.showFolderDialog(activity); true
                }

                else -> false
            }
        }

        popup.show()
        return true
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    fun selectItem(pos: Int, rect: Rect = Rect()) {
        when (val entry = entries.getOrNull(pos) ?: return) {
            is AppListEntry.FolderEntry -> toggleFolder(entry.folder)
            is AppListEntry.AppEntry -> selectApp(entry.app, rect)
            is AppListEntry.FolderEndEntry -> { /* not interactive */ }
        }
    }

    private fun selectApp(appInfo: AbstractDetailedAppInfo, rect: Rect) {
        when (intention) {
            AbstractListActivity.Companion.Intention.VIEW -> {
                appInfo.getAction().invoke(activity, rect)
            }

            AbstractListActivity.Companion.Intention.PICK -> {
                activity.finish()
                forGesture ?: return
                val gesture = Gesture.byId(forGesture) ?: return
                Action.setActionForGesture(gesture, appInfo.getAction())
            }
        }
    }

    fun updateAppsList(triggerAutoLaunch: Boolean = false) {
        rebuildEntries()

        val singleApp = entries.singleOrNull() as? AppListEntry.AppEntry

        if (triggerAutoLaunch &&
            singleApp != null
            && intention == AbstractListActivity.Companion.Intention.VIEW
            && !disableAutoLaunch
            && LauncherPreferences.functionality().searchAutoLaunch()
        ) {
            singleApp.app.getAction().invoke(activity)

            val inputMethodManager =
                activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(View(activity).windowToken, 0)
        }

        notifyDataSetChanged()
    }

    /**
     * The function [setSearchString] is used to search elements within this [RecyclerView].
     */
    fun setSearchString(search: String) {
        appFilter.query = search
        updateAppsList(true)

    }

    fun setFavoritesVisibility(v: AppFilter.Companion.AppSetVisibility) {
        appFilter.favoritesVisibility = v
        updateAppsList()
    }

    companion object {
        private const val VIEW_TYPE_APP = 0
        private const val VIEW_TYPE_FOLDER = 1
        private const val VIEW_TYPE_FOLDER_END = 2
    }
}
