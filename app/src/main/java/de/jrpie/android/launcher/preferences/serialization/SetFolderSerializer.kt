@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package de.jrpie.android.launcher.preferences.serialization

import de.jrpie.android.launcher.apps.Folder
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializationException
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer

/**
 * Stores the folders this fork adds. Kept out of PreferenceSerializers.kt so that
 * file stays byte identical to upstream and does not become a merge conflict.
 *
 * Follows the same shape as the serializers next to it, in particular
 * SetWidgetPanelSerializer.
 */
@Suppress("UNCHECKED_CAST")
class SetFolderSerializer :
    PreferenceSerializer<java.util.Set<Folder>?, java.util.Set<java.lang.String>?> {
    @Throws(PreferenceSerializationException::class)
    override fun serialize(value: java.util.Set<Folder>?): java.util.Set<java.lang.String>? {
        return value?.map(Folder::serialize)
            ?.toHashSet() as? java.util.Set<java.lang.String>
    }

    @Throws(PreferenceSerializationException::class)
    override fun deserialize(value: java.util.Set<java.lang.String>?): java.util.Set<Folder>? {
        return value?.map(java.lang.String::toString)?.map(Folder::deserialize)
            ?.toHashSet() as? java.util.Set<Folder>
    }
}
