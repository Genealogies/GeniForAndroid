package app.familygem

import android.Manifest
import android.util.Log
import androidx.test.core.app.takeScreenshot
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreesActivityTest {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<FacadeActivity>()

    // a handy JUnit rule that stores the method name, so it can be used to generate unique screenshot files per test method
    @get:Rule
    var nameRule = TestName()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Before
    fun before() {
        Intents.init()
    }

    @After
    fun after() {
        Intents.release()
    }

    @Test
    fun treesSimplyStart() {
        takeScreenshot()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}")
        try {
            Espresso.onView(ViewMatchers.isRoot())
                .captureToBitmap()
                .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}")
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
        }
    }

    @Test
    fun importStart() {
        Espresso.onView(ViewMatchers.withId(R.id.fab)).perform(ViewActions.click())
        intended(hasComponent(NewTree::class.java.name))
        takeScreenshot()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}")
        try {
            Espresso.onView(ViewMatchers.isRoot())
                .captureToBitmap()
                .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}")
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.message.toString())
        }
    }
}
