package com.byoutline.kickmaterial.espressohelpers

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.base.DefaultFailureHandler
import android.support.test.rule.ActivityTestRule
import com.byoutline.kickmaterial.KickMaterialApp
import com.byoutline.kickmaterial.dagger.GlobalComponent
import com.byoutline.kickmaterial.features.projectlist.MainActivity
import com.squareup.spoon.Spoon
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Methods returning custom [ActivityTestRule]s that set test [AppComponent].

 * @author Sebastian Kacprzak <sebastian.kacprzak at byoutline.com>
 */
object DaggerRules {

    fun userFirstLaunchRule(): ActivityTestRule<MainActivity>
            = getActivityRule({ TestComponents.getFirstRunComponent(it) }, MainActivity::class.java)

    fun userNextLaunchRule(): ActivityTestRule<MainActivity>
            = getActivityRule({ TestComponents.getNextRunComponent(it) }, MainActivity::class.java)

    private fun <ACTIVITY : Activity> getActivityRule(mainComponentProv: (KickMaterialApp) -> GlobalComponent,
                                                      clazz: Class<ACTIVITY>): ActivityTestRule<ACTIVITY> {
        val mainHandler = Handler(Looper.getMainLooper())
        return DaggerActivityTestRule(clazz, beforeActivityLaunchedAction = { application ->
            val app = application as KickMaterialApp
            val globalComponent = mainComponentProv(app)
            mainHandler.post { app.setComponents(globalComponent) }
        })
    }
}

class DaggerActivityTestRule<T : Activity>(activityClass: Class<T>, initialTouchMode: Boolean = false,
                                           launchActivity: Boolean = true,
                                           private val beforeActivityLaunchedAction: (Application) -> Unit = {}) : ActivityTestRule<T>(activityClass, initialTouchMode, launchActivity) {

    override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        beforeActivityLaunchedAction(InstrumentationRegistry.getInstrumentation()
                .targetContext.applicationContext as Application)
    }

    override fun apply(base: Statement, description: Description): Statement {
        // On Ci take screenshot if test fails
        if (System.getenv("CIRCLECI") != null) {
            Espresso.setFailureHandler { error, viewMatcher ->
                Spoon.screenshot(activity, error.javaClass.simpleName, description.className, description.methodName)
                DefaultFailureHandler(activity).handle(error, viewMatcher)
            }
        }
        return super.apply(base, description)
    }
}