package app.familygem

import android.Manifest
import android.util.Log
import android.view.View
import androidx.test.core.app.takeScreenshot
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.rules.TestName
import org.junit.runner.RunWith
import com.moka.lib.assertions.WaitingAssertion
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.*

@RunWith(AndroidJUnit4::class)
class NewTreeActivityTest {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<NewTree>()

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
        Espresso.onView(ViewMatchers.withId(R.id.bottone_scarica_esempio)).perform(ViewActions.click())
        WaitingAssertion.assertVisibility(R.id.nuovo_circolo, View.VISIBLE, 300)

        for (i in 500L..2500L step 500) {
            Thread.sleep(i)
            Espresso.onView(ViewMatchers.isRoot())
                .captureToBitmap()
                .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-$i")
        }
        Intents.intended(IntentMatchers.hasComponent(TreesActivity::class.java.name))

        Espresso.onView(ViewMatchers.withId(R.id.lista_alberi)).perform(ViewActions.click())
        Thread.sleep(2000)
        Espresso.onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-click")

        Espresso.onData(allOf(instanceOf(HashMap::class.java))).atPosition(0).perform(ViewActions.click())
        Thread.sleep(2000)
        Espresso.onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-onData")

        Espresso.onView(allOf(withId(R.id.card_name), withText(PERSON))).check(matches(isDisplayed()))
        Espresso.onView(allOf(withId(R.id.card_name), withText(PERSON)))
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-Homer")

        // parent  View
        Espresso.onView(ViewMatchers.isRoot()).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-clickMain")
    }

    companion object {
        private val PERSON = "Homer\nSimpson"
    }
}
