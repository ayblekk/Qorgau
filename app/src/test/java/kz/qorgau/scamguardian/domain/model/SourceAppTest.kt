package kz.qorgau.scamguardian.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceAppTest {

    @Test
    fun `known packages map correctly`() {
        assertEquals(SourceApp.SMS, SourceApp.fromPackageName("com.google.android.apps.messaging"))
        assertEquals(SourceApp.WHATSAPP, SourceApp.fromPackageName("com.whatsapp"))
        assertEquals(SourceApp.TELEGRAM, SourceApp.fromPackageName("org.telegram.messenger"))
    }

    @Test
    fun `soft match catches oem variants`() {
        assertEquals(SourceApp.WHATSAPP, SourceApp.fromPackageName("com.whatsapp.w4b"))
        assertEquals(SourceApp.TELEGRAM, SourceApp.fromPackageName("org.telegram.messenger.beta"))
        assertEquals(SourceApp.SMS, SourceApp.fromPackageName("com.miui.mms"))
        assertEquals(SourceApp.SMS, SourceApp.fromPackageName("com.transsion.smartmessage"))
        assertEquals(SourceApp.SMS, SourceApp.fromPackageName("com.oplus.mms"))
        assertEquals(SourceApp.OTHER, SourceApp.fromPackageName("org.thoughtcrime.securesms"))
    }

    @Test
    fun `resolve accepts message category for unknown packages`() {
        assertEquals(
            SourceApp.OTHER,
            SourceApp.resolve(
                packageName = "com.weird.chat.app",
                isMessageCategory = true,
                hasMessagingStyle = false,
            ),
        )
        assertEquals(
            SourceApp.WHATSAPP,
            SourceApp.resolve(
                packageName = "com.whatsapp",
                isMessageCategory = false,
                hasMessagingStyle = false,
            ),
        )
        assertNull(
            SourceApp.resolve(
                packageName = "com.android.systemui",
                isMessageCategory = false,
                hasMessagingStyle = false,
            ),
        )
    }

    @Test
    fun `unknown non messaging package is null`() {
        assertNull(SourceApp.fromPackageName("com.android.systemui"))
        assertNull(SourceApp.fromPackageName("com.spotify.music"))
    }
}
