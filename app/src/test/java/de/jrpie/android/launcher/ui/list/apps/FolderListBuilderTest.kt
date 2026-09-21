package de.jrpie.android.launcher.ui.list.apps

import de.jrpie.android.launcher.apps.AppInfo
import de.jrpie.android.launcher.apps.Folder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [foldersToRender] decides which folders get a header row in the favorites
 * view (and, unfiltered, in the main list). It is pulled out of
 * [AppsRecyclerAdapter.rebuildEntries] specifically so this decision - the
 * actual bit of new behaviour this feature adds - can be tested on the JVM,
 * without an [android.app.Activity] to build the adapter with.
 */
class FolderListBuilderTest {

    private fun app(packageName: String) =
        AppInfo(packageName, "$packageName.Main", 0)

    private val browser = app("com.example.browser")
    private val mail = app("com.example.mail")
    private val notes = app("com.example.notes")
    private val camera = app("com.example.camera")

    @Test
    fun `outside the favorites view every folder is kept, including empty ones`() {
        val work = Folder(1, "Work", setOf(browser, mail))
        val empty = Folder(2, "Empty")

        val rendered = foldersToRender(listOf(work, empty), onlyFavorites = false, favorites = setOf())

        assertEquals(listOf(work, empty), rendered)
    }

    @Test
    fun `favorites view drops a folder with no favorited members`() {
        val work = Folder(1, "Work", setOf(browser, mail))
        val hobby = Folder(2, "Hobby", setOf(camera))

        // Only an app from "Work" is favorited.
        val rendered = foldersToRender(
            listOf(work, hobby),
            onlyFavorites = true,
            favorites = setOf(browser)
        )

        assertEquals(listOf(work), rendered)
    }

    @Test
    fun `favorites view keeps a folder as soon as one member is favorited`() {
        // "Work" has an unfavorited member (mail) alongside the favorited one -
        // it must still render, the unfavorited member just won't be listed
        // under it (that filtering happens earlier, via AppFilter).
        val work = Folder(1, "Work", setOf(browser, mail))

        val rendered = foldersToRender(
            listOf(work),
            onlyFavorites = true,
            favorites = setOf(browser)
        )

        assertEquals(listOf(work), rendered)
    }

    @Test
    fun `favorites view drops every folder when nothing is favorited`() {
        val work = Folder(1, "Work", setOf(browser, mail))
        val hobby = Folder(2, "Hobby", setOf(camera))

        val rendered = foldersToRender(listOf(work, hobby), onlyFavorites = true, favorites = setOf())

        assertTrue(rendered.isEmpty())
    }

    @Test
    fun `an empty folder never survives the favorites view`() {
        val empty = Folder(1, "Empty")

        val rendered = foldersToRender(listOf(empty), onlyFavorites = true, favorites = setOf(notes))

        assertTrue(rendered.isEmpty())
    }
}
