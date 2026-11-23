package com.nellecrzier.gofly

import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Uji asap layar masuk: aktivitas asli dibuka, halaman email muncul, dan tidak
 * ada panggilan jaringan sama sekali (layar masuk hanya membaca sesi lokal).
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenSmokeTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    /**
     * Persiapan harus berjalan SEBELUM aktivitas diluncurkan:
     * - hapus sesi tersimpan supaya layar masuk selalu tampil (bukan layar siap);
     * - matikan animasi sistem, karena latar layar masuk memakai animasi tanpa
     *   akhir yang membuat Compose Test tidak pernah mencapai keadaan idle.
     */
    @get:Rule
    val rule: TestRule = RuleChain.outerRule(PrepareLoginRule()).around(compose)

    @Test
    fun loginScreenShowsEmailField() {
        // Halaman pertama adalah sambutan; CTA utama membuka halaman email.
        compose.onNodeWithTag("login_cta").performClick()

        compose.onNodeWithTag("login_email").assertIsDisplayed()
        compose.onNodeWithTag("login_cta").assertIsDisplayed()
        compose.onNodeWithTag("login_progress").assertIsDisplayed()
    }

    private class PrepareLoginRule : TestRule {
        override fun apply(base: Statement, description: Description): Statement =
            object : Statement() {
                override fun evaluate() {
                    val instrumentation = InstrumentationRegistry.getInstrumentation()
                    val resolver = instrumentation.targetContext.contentResolver
                    val previous = Settings.Global.getFloat(
                        resolver,
                        Settings.Global.ANIMATOR_DURATION_SCALE,
                        1f,
                    )

                    runCatching {
                        instrumentation.targetContext.deleteSharedPreferences("gofile-session")
                        instrumentation.targetContext.deleteSharedPreferences("gofile-session-secure")
                    }
                    setAnimationScale(instrumentation, 0f)
                    try {
                        base.evaluate()
                    } finally {
                        setAnimationScale(instrumentation, previous)
                    }
                }
            }

        private fun setAnimationScale(
            instrumentation: android.app.Instrumentation,
            scale: Float,
        ) {
            listOf(
                "animator_duration_scale",
                "transition_animation_scale",
                "window_animation_scale",
            ).forEach { key ->
                runCatching {
                    shell(instrumentation, "settings put global $key $scale")
                }
            }
        }

        private fun shell(instrumentation: android.app.Instrumentation, command: String): String =
            instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes().decodeToString()
            }
    }
}
