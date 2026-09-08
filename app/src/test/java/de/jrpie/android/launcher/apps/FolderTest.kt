package de.jrpie.android.launcher.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Storage round trip for [Folder].
 *
 * Folders are the only user data this fork adds, and they survive a restart only
 * by surviving this round trip, so it is worth guarding without a device. These
 * run on the JVM: serialization touches no Android APIs.
 */
class FolderTest {

    private fun app(packageName: String) =
        AppInfo(packageName, "$packageName.Main", 0)

    @Test
    fun `round trip preserves id label and members`() {
        val folder = Folder(
            3,
            "Work",
            setOf(app("com.example.one"), app("com.example.two"))
        )

        val restored = Folder.deserialize(folder.serialize())

        assertEquals(3, restored.id)
        assertEquals("Work", restored.label)
        assertEquals(folder.apps, restored.apps)
    }

    @Test
    fun `round trip preserves an empty folder`() {
        val restored = Folder.deserialize(Folder(1, "Empty").serialize())

        assertEquals(1, restored.id)
        assertEquals("Empty", restored.label)
        assertTrue(restored.apps.isEmpty())
    }

    /**
     * Renaming must not disturb membership. This is the pure part of the bug that
     * showed up when a rename wrote a stale instance back over the stored folder.
     */
    @Test
    fun `relabelling keeps members intact through a round trip`() {
        val folder = Folder(7, "Old", setOf(app("com.example.kept")))
        folder.label = "New"

        val restored = Folder.deserialize(folder.serialize())

        assertEquals("New", restored.label)
        assertEquals(setOf(app("com.example.kept")), restored.apps)
    }

    @Test
    fun `folders compare by id so a set replaces rather than duplicates`() {
        val original = Folder(2, "Media", setOf(app("com.example.player")))
        val renamed = Folder(2, "Music")

        assertEquals(original, renamed)
        assertEquals(1, setOf(original, renamed).size)
    }
}
