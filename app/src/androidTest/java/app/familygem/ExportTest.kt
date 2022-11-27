package app.familygem

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.lang.Exception

import android.Manifest
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import junit.framework.TestCase.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.nio.file.Paths

@RunWith(AndroidJUnit4::class)
class ExportTest {

//    @get:Rule
//    val activityScenarioRule = activityScenarioRule<PreviewActivity>()

    // a handy JUnit rule that stores the method name, so it can be used to generate unique screenshot files per test method
    @get:Rule
    var nameRule = TestName()

    //https://developer.android.com/reference/androidx/test/rule/GrantPermissionRule
    //Note: As per the documentation this rule will automatically grant READ_EXTERNAL_STORAGE when WRITE_EXTERNAL_STORAGE is requested.
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
//        Manifest.permission.READ_EXTERNAL_STORAGE,
//        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @Test
    @Suppress
    @Throws(Exception::class)
    fun startTest() {
        loadFileGedcom()
        //loadUriMedia()
        //exportGedcom()
        //exportBackup()
    }

    // load /assets/media.ged in /GEDCOM/media.ged from device
    @Throws(Exception::class)
    fun loadFileGedcom() {
        val gedcomDir = File(Environment.getDownloadCacheDirectory(), "/GEDCOM")

        val inputStream = InstrumentationRegistry.getInstrumentation().context.assets.open("media.ged")
        assertNotNull(inputStream)
        val resultMkdir: Boolean = File(Environment.getDownloadCacheDirectory(), "/GEDCOM").mkdir()
        if (!resultMkdir)
            throw Exception("gedcomDir not exists")
        gedcomDir.mkdir()
        if (!gedcomDir.exists())
            throw Exception("gedcomDir not exists")
//        assertTrue(gedcomDir.exists())
        val gedcomFile = File(
            Environment.getExternalStorageDirectory().toString() + "/GEDCOM",
            "media.ged"
        )
        gedcomFile.mkdirs()
        assertNotNull(gedcomFile)
        inputStream.use { it ->
            gedcomFile.outputStream().use { output ->
                it.copyTo(output)
            }
        }
        assertTrue(gedcomFile.isFile)
        for (alb in Global.settings.trees) {
            if (alb.title == "media")
                Global.settings.deleteTree(alb.id)
        }
    }

    @Throws(Exception::class)
    fun loadUriMedia(appContext: Context) {
        val ultimoAlb = Global.settings.trees[Global.settings.trees.size - 1]

        // PDF in external storage
        val percorso0 = appContext.getExternalFilesDir(ultimoAlb.id.toString())!!.path
        var input = InstrumentationRegistry.getInstrumentation().targetContext.assets.open("È Carmelo.pdf")
        val external = File(percorso0, "È Carmelo.pdf")
        input.use { it ->
            external.outputStream().use { output ->
                it.copyTo(output)
            }
        }
        assertTrue(external.isFile)
        val percorso1: String = Environment.getExternalStorageDirectory().path + "/Percorso"
        val pathFile = File(percorso1, "path.txt")
        assertNotNull(pathFile)
        assertTrue(pathFile.canWrite())
        pathFile.writeText(pathFile.path)
        assertTrue(pathFile.isFile)

        val percorso2: String = Environment.getExternalStorageDirectory().path + "/Percorso Bis"

        val pathFilePrimo = File("$percorso2/primo", "omonimo.txt")
        assertNotNull(pathFilePrimo)
        pathFilePrimo.writeText(pathFilePrimo.path)
        assertTrue(pathFilePrimo.isFile)

        val pathFileSecondo = File("$percorso2/secondo", "omonimo.txt")
        assertNotNull(pathFileSecondo)
        pathFileSecondo.writeText(pathFileSecondo.path)
        assertTrue(pathFileSecondo.isFile)

        assertEquals(ultimoAlb.title, "media")
        //if( !ultimoAlb.cartelle.contains(percorso1) )
        ultimoAlb.dirs.add(percorso0)
        assertTrue(ultimoAlb.dirs.contains(percorso0))
        ultimoAlb.dirs.add(percorso1)
        assertTrue(ultimoAlb.dirs.contains(percorso1))
        ultimoAlb.dirs.add(percorso2)
        assertTrue(ultimoAlb.dirs.contains(percorso2))
        Global.settings.openTree = ultimoAlb.id
        Global.settings.save()

        val percorso3: String = Environment.getExternalStorageDirectory().path + "/Uri"
        val uriFile = File(percorso3, "uri.txt")
        assertNotNull(uriFile)
        uriFile.writeText(DocumentFile.fromFile(uriFile).uri.toString())
        assertTrue(uriFile.isFile)

        val luoghi = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDirs(null)
        for (luogo in luoghi) {
            if (!luogo.path.startsWith(Environment.getExternalStorageDirectory().path)) {
                val sdFolder = luogo.path.substring(0, luogo.path.indexOf("Android/")) + "Privata"
                input = InstrumentationRegistry.getInstrumentation().targetContext.assets.open("anna.webp")
                assertNotNull(input)
                val file = File(sdFolder, "anna.webp")
                assertNotNull(file)

                input.use { it ->
                    file.outputStream().use { output ->
                        it.copyTo(output)
                    }
                }

                assertTrue(file.isFile)
            }
        }
    }

    fun exportGedcom() {
        val ultimoAlb = Global.settings.trees[Global.settings.trees.size - 1]
        //assertEquals( ultimoAlb.nome, "media" );
        val idAlbero = ultimoAlb.id
        val documentsDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists()) documentsDir.mkdir()
        val fileGedcom = File(documentsDir, "Küçük ağaç.ged")
        val esp = Exporter(InstrumentationRegistry.getInstrumentation().targetContext)
        assertTrue(esp.apriAlbero(idAlbero))
        assertNull(esp.messaggioSuccesso)
        assertNull(esp.messaggioErrore)
        assertTrue(esp.exportGedcom(Uri.fromFile(fileGedcom)))
        assertTrue(fileGedcom.isFile)
        assertEquals(esp.messaggioSuccesso, InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.gedcom_exported_ok))
        s.l(esp.messaggioSuccesso)
        val fileGedcomZip = File(documentsDir, "ਸੰਕੁਚਿਤ.zip")
        val esp2 = Exporter(InstrumentationRegistry.getInstrumentation().targetContext)
        assertTrue(esp2.apriAlbero(idAlbero))
        val result = esp2.exportGedcomZip(Uri.fromFile(fileGedcomZip))
        s.l(esp2.messaggioErrore)
        assertTrue(result)
        assertEquals(esp2.messaggioSuccesso, InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.zip_exported_ok))
        assertTrue(fileGedcomZip.isFile)
        s.l(esp2.messaggioSuccesso)
    }

    // export in /Documents l'ultimo albero come backup ZIP
    fun exportBackup() {
        val documentsDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists()) documentsDir.mkdir()
        val fileBackup = File(documentsDir, "Becàp olè.zip")
        val esp = Exporter(InstrumentationRegistry.getInstrumentation().targetContext)
        val ultimoAlb = Global.settings.trees[Global.settings.trees.size - 1]
        assertTrue(esp.apriAlbero(ultimoAlb.id))
        val result = esp.exportBackupZip(null, -1, Uri.fromFile(fileBackup))
        s.l(esp.messaggioErrore)
        assertTrue(result)
        assertEquals(esp.messaggioSuccesso, InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.zip_exported_ok))
        assertTrue(fileBackup.isFile)
        s.l(esp.messaggioSuccesso)
    }
}
