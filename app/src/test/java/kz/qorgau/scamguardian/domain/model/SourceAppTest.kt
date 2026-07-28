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
    }

    @Test
    fun `unknown package is null`() {
        assertNull(SourceApp.fromPackageName("com.instagram.android"))
    }
}
