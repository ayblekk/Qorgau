package kz.qorgau.scamguardian.data.capability

import android.app.ActivityManager
import android.content.Context
import kz.qorgau.scamguardian.domain.capability.DeviceCapability
import kz.qorgau.scamguardian.domain.capability.DeviceCapabilityDetector

/**
 * Reads device RAM and maps to [DeviceCapability].
 */
class AndroidDeviceCapabilityReader(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun read(): DeviceCapability {
        val am = appContext.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(info)
        val totalMb = if (info.totalMem > 0L) {
            info.totalMem / (1024L * 1024L)
        } else {
            // Unknown RAM — stay conservative.
            DeviceCapabilityDetector.RULES_ONLY_MAX_RAM_MB - 1
        }
        return DeviceCapabilityDetector.fromTotalRamMb(totalMb)
    }
}
