package de.jrpie.android.launcher.ui.list.apps

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import de.jrpie.android.launcher.R
import de.jrpie.android.launcher.apps.AbstractDetailedAppInfo
import de.jrpie.android.launcher.apps.Folder

/**
 * Dialogs used to put apps into folders and to manage the folders themselves.
 *
 * These follow the same pattern as the app actions in [ContextMenuActions]:
 * reached by long pressing a row, and built with [R.style.AlertDialogCustom].
 */

/**
 * Lets the user move this app into a folder, remove it from its folder,
 * or create a new folder for it.
 */
fun AbstractDetailedAppInfo.showFolderDialog(context: Context) {
    val folders = Folder.all()
    val current = Folder.of(getRawInfo())

    // "No folder", every existing folder, then "New folder…".
    val labels = mutableListOf(context.getString(R.string.dialog_folder_none))
    folders.mapTo(labels) { it.label }
    labels.add(context.getString(R.string.dialog_folder_new))

    val checked = current?.let { folders.indexOf(it) + 1 } ?: 0

    AlertDialog.Builder(context, R.style.AlertDialogCustom).apply {
        setTitle(R.string.dialog_folder_choose_title)
        setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, which ->
            dialog.dismiss()
            when (which) {
                0 -> Folder.put(getRawInfo(), null)
                labels.size - 1 -> showNewFolderDialog(context) { folder ->
                    Folder.put(getRawInfo(), folder)
                }

                else -> Folder.put(getRawInfo(), folders[which - 1])
            }
        }
        setNegativeButton(android.R.string.cancel) { d, _ -> d.cancel() }
    }.create().show()
}

/**
 * Shared name entry dialog for creating and renaming folders.
 *
 * Cancel is the negative button and OK the positive one, so they render as
 * [Cancel] [OK]: the platform convention is the dismissive action on the left and
 * the confirming action on the right. Rather than reversing that, the keyboard's
 * done key commits the name directly, which removes the trip to the button
 * entirely. A blank name is ignored, so neither path can create an unnamed folder.
 */
private fun showFolderNameDialog(
    context: Context,
    title: CharSequence,
    initialName: String?,
    onCommit: (String) -> Unit
) {
    val dialog = AlertDialog.Builder(context, R.style.AlertDialogCustom).apply {
        setTitle(title)
        setView(R.layout.dialog_folder_name)
        setNegativeButton(android.R.string.cancel) { d, _ -> d.cancel() }
        setPositiveButton(android.R.string.ok) { d, _ ->
            val name = (d as? AlertDialog)
                ?.findViewById<EditText>(R.id.dialog_folder_name_edit_text)
                ?.text?.toString()?.trim()
            if (!name.isNullOrEmpty()) {
                onCommit(name)
            }
        }
    }.create()

    dialog.show()

    dialog.findViewById<EditText>(R.id.dialog_folder_name_edit_text)?.apply {
        setHint(R.string.dialog_folder_name_hint)
        initialName?.let {
            setText(it)
            setSelection(it.length)
        }
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val name = text.toString().trim()
                if (name.isNotEmpty()) {
                    onCommit(name)
                }
                dialog.dismiss()
                true
            } else {
                false
            }
        }
        requestFocus()
    }
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
}

/**
 * Asks for a name and creates a folder, then hands it to [onCreated].
 */
fun showNewFolderDialog(context: Context, onCreated: (Folder) -> Unit) {
    showFolderNameDialog(
        context,
        context.getString(R.string.dialog_folder_new_title),
        null
    ) { name -> onCreated(Folder.create(name)) }
}

fun Folder.showRenameDialog(context: Context) {
    showFolderNameDialog(
        context,
        context.getString(R.string.dialog_folder_rename_title, label),
        label
    ) { name -> rename(name) }
}

/**
 * Confirms and deletes the folder. Apps in it are kept and return to the
 * top level of the list, so this is undoable from a snackbar.
 *
 * @param view used to show the snackbar letting the user undo the deletion
 */
fun Folder.showDeleteDialog(context: Context, view: View) {
    AlertDialog.Builder(context, R.style.AlertDialogCustom).apply {
        setTitle(R.string.list_folder_delete)
        setMessage(context.getString(R.string.dialog_folder_delete_message, label))
        setNegativeButton(android.R.string.cancel) { d, _ -> d.cancel() }
        setPositiveButton(android.R.string.ok) { _, _ ->
            val restore = this@showDeleteDialog
            delete()
            Snackbar.make(view, R.string.snackbar_folder_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    Folder.setAll(Folder.all() + restore)
                }.show()
        }
    }.create().show()
}
