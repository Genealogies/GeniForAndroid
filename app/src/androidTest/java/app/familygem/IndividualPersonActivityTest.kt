package app.familygem

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.takeScreenshot
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class IndividualPersonActivityTest {

    private val intent = Intent(ApplicationProvider.getApplicationContext(), IndividualPersonActivity::class.java).apply {
        this.putExtra(IndividualPersonActivity.KEY_ID, "I1")
    }

    @get:Rule
    val activityScenarioRule = activityScenarioRule<IndividualPersonActivity>(intent)

    // a handy JUnit rule that stores the method name, so it can be used to generate unique screenshot files per test method
    @get:Rule
    var nameRule = TestName()

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
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}1")
        Thread.sleep(1000)
        takeScreenshot()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}2")
    }

}
