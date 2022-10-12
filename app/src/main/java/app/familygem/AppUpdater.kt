package app.familygem

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import info.hannes.github.AppUpdateHelper.checkForNewVersion

object AppUpdater {
    fun check(activity: AppCompatActivity) {
        checkForNewVersion(
            activity,
            BuildConfig.GIT_REPOSITORY,
            BuildConfig.VERSION_NAME,
            { msg -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show() }
        )
    }
}
