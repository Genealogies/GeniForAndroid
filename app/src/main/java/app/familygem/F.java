// Funzioni statiche per gestire file e media

package app.familygem;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import com.google.gson.JsonPrimitive;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.Person;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import app.familygem.visitor.MediaList;

public class F {

	// Impacchettamento per ricavare una cartella in KitKat
	static String uriPercorsoCartellaKitKat( Context contesto, Uri uri ) {
		String percorso = uriPercorsoFile( uri );
		if( percorso != null && percorso.lastIndexOf('/') > 0 ) {
			return percorso.substring( 0, percorso.lastIndexOf('/') );
		} else {
			Toast.makeText(contesto, "Could not get this position.", Toast.LENGTH_SHORT).show();
			return null;
		}
	}

	// Riceve un Uri e cerca di restituire il percorso del file
	// Versione commentata in lab
	static String uriPercorsoFile( Uri uri ) {
		if( uri == null ) return null;
		if( uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("file") ) {
			// Toglie 'file://'
			return uri.getPath();
		}
		switch( uri.getAuthority() ) {
			case "com.android.externalstorage.documents":	// memoria interna e scheda SD
				String[] split = uri.getLastPathSegment().split(":");
				if( split[0].equalsIgnoreCase("primary")) {
					// Storage principale
					String percorso = Environment.getExternalStorageDirectory() + "/" + split[1];
					if( new File(percorso).canRead() )
						return percorso;
				} else if( split[0].equalsIgnoreCase("home") ) {
					// Cartella 'Documents' in Android 9 e 10
					return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + split[1];
				} else {
					// Tutti gli altri casi, tra cui schede SD
					File[] luoghi = Global.context.getExternalFilesDirs(null);
					for( File luogo : luoghi ) {
						if( luogo.getAbsolutePath().indexOf("/Android") > 0 ) {
							String dir = luogo.getAbsolutePath().substring(0, luogo.getAbsolutePath().indexOf("/Android"));
							File trovando = new File( dir, split[1] );
							if( trovando.canRead() )
								return trovando.getAbsolutePath();
						}
					}
				}
				break;
			case "com.android.providers.downloads.documents": // file dalla cartella Download
				String id = uri.getLastPathSegment();
				if( id.startsWith( "raw:/" ) )
					return id.replaceFirst("raw:", "");
				if( id.matches("\\d+") ) {
					String[] contentUriPrefixesToTry = new String[] {
							"content://downloads/public_downloads",
							"content://downloads/my_downloads"
					};
					for( String contentUriPrefix : contentUriPrefixesToTry ) {
						Uri uriRicostruito = ContentUris.withAppendedId( Uri.parse(contentUriPrefix), Long.parseLong(id) );
						try {
							String nomeFile = trovaNomeFile( uriRicostruito );
							if( nomeFile != null )
								return nomeFile;
						} catch(Exception e) {}
					}
				}
		}
		return trovaNomeFile( uri );
	}

	// Riceve l'URI (eventualmente ricostruito) di un file preso con SAF
	// Se riesce restituisce il percorso completo, altrimenti il singolo nome del file
	private static String trovaNomeFile( Uri uri ) {
		Cursor cursore = Global.context.getContentResolver().query( uri, null, null, null, null);
		if( cursore != null && cursore.moveToFirst() ) {
			int indice = cursore.getColumnIndex( MediaStore.Files.FileColumns.DATA );
			if( indice < 0 )
				indice = cursore.getColumnIndex( OpenableColumns.DISPLAY_NAME );
			String nomeFile = cursore.getString( indice );
			cursore.close();
			return nomeFile;
		}
		return null;
	}

	// Riceve un tree Uri ricavato con ACTION_OPEN_DOCUMENT_TREE e cerca di restituire il percorso della cartella
	// altrimenti tranquillamente restituisce null
	public static String uriPercorsoCartella( Uri uri ) {
		if( uri == null ) return null;
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
			String treeDocId = DocumentsContract.getTreeDocumentId( uri );
			switch( uri.getAuthority() ) {
				case "com.android.externalstorage.documents": // memoria interna e scheda SD
					String[] split = treeDocId.split(":");
					String percorso = null;
					// Storage principale
					if( split[0].equalsIgnoreCase("primary") ) {
						percorso = Environment.getExternalStorageDirectory().getAbsolutePath();
					}
					// Documents in Android 9 e 10
					else if( split[0].equalsIgnoreCase("home") ) {
						percorso = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
					}
					// Tutti gli altri, type la scheda SD
					else {
						File[] filesDirs = Global.context.getExternalFilesDirs(null);
						for( File dir : filesDirs ) {
							String altro = dir.getAbsolutePath();
							if( altro.contains(split[0])) {
								percorso = altro.substring( 0, altro.indexOf("/Android") );
								break;
							}
						}
					}
					if( percorso != null ) {
						if( split.length > 1 && !split[1].isEmpty() )
							percorso += "/" + split[1];
						return percorso;
					}
					break;
				case "com.android.providers.downloads.documents": // provider Downloads e sottocartelle
					if( treeDocId.equals("downloads") )
						return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
					if( treeDocId.startsWith("raw:/") )
						return treeDocId.replaceFirst("raw:", "");
			}
		}
		return null;
	}

	// Fa salvare un documento (PDF, GEDCOM, ZIP) con SAF
	static void saveDocument(Activity attivita, Fragment frammento, int idAlbero, String mime, String ext, int requestCode ) {
		String nome = Global.settings.getTree(idAlbero).title;
		// GEDCOM deve esplicitare l'estensione, gli altri la mettono in base al mime type
		ext = ext.equals("ged") ? ".ged" : "";
		// rimpiazza caratteri pericolosi per il filesystem di Android che non vengono ripiazzati da Android stesso
		nome = nome.replaceAll( "[$']", "_" );
		Intent intent = new Intent( Intent.ACTION_CREATE_DOCUMENT )
				.addCategory( Intent.CATEGORY_OPENABLE )
				.setType( mime )
				.putExtra( Intent.EXTRA_TITLE, nome  + ext );
		if( attivita != null )
			attivita.startActivityForResult( intent, requestCode );
		else
			frammento.startActivityForResult( intent, requestCode );
	}

	// Metodi per mostrare immagini:

	// Riceve una Person e sceglie il Media principale da cui ricavare l'immagine
	static void unaFoto( Gedcom gc, Person p, ImageView img ) {
		MediaList visita = new MediaList( gc, 0 );
		p.accept( visita );
		boolean trovatoQualcosa = false;
		for( Media med : visita.list) { // Cerca un media contrassegnato Primario Y
			if( med.getPrimary() != null && med.getPrimary().equals("Y") ) {
				showImage( med, img, null );
				trovatoQualcosa = true;
				break;
			}
		}
		if( !trovatoQualcosa ) { // In alternativa restituisce il primo che trova
			for( Media med : visita.list) {
				showImage( med, img, null );
				trovatoQualcosa = true;
				break;
			}
		}
		img.setVisibility( trovatoQualcosa ? View.VISIBLE : View.GONE );
	}

	/**
	 * Show pictures with Picasso
	 * */
	public static void showImage(Media media, ImageView imageView, ProgressBar circo ) {
		int idAlbero;
		// Confrontatore ha bisogno dell'id dell'albero nuovo per cercare nella sua cartella
		View probabile = null;
		if( imageView.getParent() != null && imageView.getParent().getParent() != null )
			probabile = (View) imageView.getParent().getParent().getParent();
		if( probabile != null && probabile.getId() == R.id.confronto_nuovo )
			idAlbero = Global.treeId2;
		else idAlbero = Global.settings.openTree;
		String percorso = mediaPath(idAlbero, media);
		Uri[] uri = new Uri[1];
		if( percorso == null )
			uri[0] = uriMedia(idAlbero, media);
		if( circo != null ) circo.setVisibility(View.VISIBLE);
		imageView.setTag(R.id.tag_tipo_file, 0);
		if( percorso != null || uri[0] != null ) {
			RequestCreator creator;
			if( percorso != null )
				creator = Picasso.get().load("file://" + percorso);
			else
				creator = Picasso.get().load(uri[0]);
			creator.placeholder(R.drawable.image)
					.fit()
					.centerInside()
					.into(imageView, new Callback() {
						@Override
						public void onSuccess() {
							if( circo != null ) circo.setVisibility(View.GONE);
							imageView.setTag(R.id.tag_tipo_file, 1);
							imageView.setTag(R.id.tag_percorso, percorso); // 'percorso' o 'uri' uno dei 2 è valido, l'altro è null
							imageView.setTag(R.id.tag_uri, uri[0]);
							// Nella pagina Dettaglio Immagine ricarica il menu opzioni per mostrare il comando Crop
							if( imageView.getId() == R.id.immagine_foto ) {
								if( imageView.getContext() instanceof Activity ) // In KitKat è instance di TintContextWrapper
									((Activity)imageView.getContext()).invalidateOptionsMenu();
							}
						}
						@Override
						public void onError( Exception e ) {
							// Magari è un video da cui ricavare una thumbnail
							Bitmap bitmap = null;
							try { // Ultimamente questi generatori di thumbnail inchiodano, quindi meglio pararsi il culo
								bitmap = ThumbnailUtils.createVideoThumbnail( percorso, MediaStore.Video.Thumbnails.MINI_KIND );
								// Tramite l'URI
								if( bitmap == null && uri[0] != null ) {
									MediaMetadataRetriever mMR = new MediaMetadataRetriever();
									mMR.setDataSource( Global.context, uri[0] );
									bitmap = mMR.getFrameAtTime();
								}
							} catch( Exception excpt ) {}
							imageView.setTag(R.id.tag_tipo_file, 2);
							if( bitmap == null ) {
								// un File locale senza anteprima
								String formato = media.getFormat();
								if( formato == null )
									formato = percorso != null ? MimeTypeMap.getFileExtensionFromUrl(percorso.replaceAll("[^a-zA-Z0-9./]", "_")) : "";
									// Rimuove gli spazi bianchi che non fanno trovare l'estensione
								if( formato.isEmpty() && uri[0] != null )
									formato = MimeTypeMap.getFileExtensionFromUrl( uri[0].getLastPathSegment() );
								bitmap = generaIcona( imageView, R.layout.media_file, formato );
								imageView.setScaleType( ImageView.ScaleType.FIT_CENTER );
								if( imageView.getParent() instanceof RelativeLayout && // brutto ma efficace
										((RelativeLayout)imageView.getParent()).findViewById(R.id.media_testo) != null ) {
									RelativeLayout.LayoutParams parami = new RelativeLayout.LayoutParams(
											RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT );
									parami.addRule( RelativeLayout.ABOVE, R.id.media_testo );
									imageView.setLayoutParams( parami );
								}
								imageView.setTag( R.id.tag_tipo_file, 3 );
							}
							imageView.setImageBitmap(bitmap);
							imageView.setTag( R.id.tag_percorso, percorso );
							imageView.setTag( R.id.tag_uri, uri[0] );
							if( circo!=null ) circo.setVisibility( View.GONE );
						}
					});
		} else if( media.getFile() != null && !media.getFile().isEmpty() ) { // magari è un'immagine in internet
			String percorsoFile = media.getFile();
			Picasso.get().load(percorsoFile).fit()
					.placeholder(R.drawable.image).centerInside()
					.into(imageView, new Callback() {
						@Override
						public void onSuccess() {
							if( circo != null ) circo.setVisibility(View.GONE);
							imageView.setTag(R.id.tag_tipo_file, 1);
							try {
								new ImboscaImmagine(media).execute(new URL(percorsoFile));
							} catch( Exception e ) {}
						}
						@Override
						public void onError( Exception e ) {
							// Proviamo con una pagina web
							new ZuppaMedia(imageView, circo, media).execute(percorsoFile);
						}
					});
		} else { // Media privo di collegamento a un file
			if( circo != null ) circo.setVisibility(View.GONE);
			imageView.setImageResource(R.drawable.image);
		}
	}

	// Riceve un Media, cerca il file in locale con diverse combinazioni di percorso e restituisce l'indirizzo
	public static String mediaPath(int idAlbero, Media m ) {
		String file = m.getFile();
		if( file != null && !file.isEmpty() ) {
			String nome = file.replace("\\", "/");
			// Percorso FILE (quello nel gedcom)
			if( new File(nome).canRead() )
				return nome;
			for( String dir : Global.settings.getTree( idAlbero ).dirs ) {
				// Cartella media + percorso FILE
				String percorso = dir + '/' + nome;
				File prova = new File(percorso);
				/* Todo Talvolta File.isFile() produce un ANR, type https://stackoverflow.com/questions/224756
				   Ho provato con vari percorsi inesistenti, type la scheda SD rimossa, o con caratteri assurdi,
				   ma tutti restituiscono semplicemente false.
				   Probabilmente l'ANR è quando il percorso punta a una risorsa esistente che però attende per tempo indefinito. */
				if( prova.isFile() && prova.canRead() )
					return percorso;
				// Cartella media + nome del FILE
				percorso = dir + '/' + new File(nome).getName();
				prova = new File(percorso);
				if( prova.isFile() && prova.canRead() )
					return percorso;
			}
			Object stringa = m.getExtension("cache");
			// A volte è String a volte JsonPrimitive, non ho capito bene perché
			if( stringa != null ) {
				String percorsoCache;
				if( stringa instanceof String )
					percorsoCache = (String) stringa;
				else
					percorsoCache = ((JsonPrimitive)stringa).getAsString();
				if( new File(percorsoCache).isFile() )
					return percorsoCache;
			}
		}
		return null;
	}

	// Riceve un Media, cerca il file in locale negli eventuali tree-URI e restituisce l'URI
	public static Uri uriMedia( int idAlbero, Media m ) {
		String file = m.getFile();
		if( file != null && !file.isEmpty() ) {
			// OBJE.FILE non è mai un Uri, sempre un percorso (Windows o Android)
			String nomeFile = new File(file.replace("\\", "/")).getName();
			for( String uri : Global.settings.getTree(idAlbero).uris ) {
				DocumentFile documentDir = DocumentFile.fromTreeUri( Global.context, Uri.parse(uri) );
				DocumentFile docFile = documentDir.findFile( nomeFile );
				if( docFile != null && docFile.isFile() )
					return docFile.getUri();
			}
		}
		return null;
	}

	static Bitmap generaIcona( ImageView vista, int icona, String testo ) {
		LayoutInflater inflater = (LayoutInflater) vista.getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		View inflated = inflater.inflate( icona, null );
		RelativeLayout frameLayout = inflated.findViewById( R.id.icona );
		((TextView)frameLayout.findViewById( R.id.icona_testo ) ).setText( testo );
		frameLayout.setDrawingCacheEnabled( true );
		frameLayout.measure( View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ),
				View.MeasureSpec.makeMeasureSpec( 0, View.MeasureSpec.UNSPECIFIED ) );
		frameLayout.layout( 0, 0, frameLayout.getMeasuredWidth(), frameLayout.getMeasuredHeight() );
		frameLayout.buildDrawingCache( true );
		return frameLayout.getDrawingCache();
	}

	// Salva in cache un'immagine trovabile in internet per poi riusarla
	// todo? forse potrebbe anche non essere un task asincrono ma una semplice funzione
	static class ImboscaImmagine extends AsyncTask<URL,Void,String> {
		Media media;
		ImboscaImmagine( Media media ) {
			this.media = media;
		}
		protected String doInBackground( URL... url ) {
			try {
				File cartellaCache = new File( Global.context.getCacheDir().getPath() + "/" + Global.settings.openTree);
				if( !cartellaCache.exists() ) {
					// Elimina extension "cache" da tutti i Media
					MediaList visitaMedia = new MediaList( Global.gc, 0 );
					Global.gc.accept( visitaMedia );
					for( Media media : visitaMedia.list)
						if( media.getExtension("cache") != null )
							media.putExtension( "cache", null );
					cartellaCache.mkdir();
				}
				String estensione = FilenameUtils.getName( url[0].getPath() );
				if( estensione.lastIndexOf('.') > 0 )
					estensione = estensione.substring( estensione.lastIndexOf('.')+1 );
				String ext;
				switch( estensione ) {
					case "png":
						ext = "png";
						break;
					case "gif":
						ext = "gif";
						break;
					case "bmp":
						ext = "bmp";
						break;
					case "jpg":
					case "jpeg":
					default:
						ext = "jpg";
				}
				File cache = nextAvailableFileName( cartellaCache.getPath(), "img." + ext );
				FileUtils.copyURLToFile( url[0], cache );
				return cache.getPath();
			} catch( Exception e ) {
				e.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute( String percorso) {
			if( percorso != null )
				media.putExtension( "cache", percorso );
		}
	}

	// Scarica asincronicamente un'immagine da una pagina internet
	static class ZuppaMedia extends AsyncTask<String, Integer, Bitmap> {
		ImageView vistaImmagine;
		ProgressBar circo;
		Media media;
		URL url;
		int tagTipoFile = 0; // setTag deve stare nel thread principale, non nel doInBackground
		int vistaImmagineWidth; // idem
		ZuppaMedia( ImageView vistaImmagine, ProgressBar circo, Media media ) {
			this.vistaImmagine = vistaImmagine;
			this.circo = circo;
			this.media = media;
			vistaImmagineWidth = vistaImmagine.getWidth();
		}
		@Override
		protected Bitmap doInBackground(String... parametri) {
			Bitmap bitmap;
			try {
				Connection connessione = Jsoup.connect(parametri[0]);
				//if (connessione.equals(bitmap)) {	// TODO: verifica che un address sia associato all'hostname
				Document doc = connessione.get();
				List<Element> lista = doc.select("img");
				if( lista.isEmpty() ) { // Pagina web trovata ma senza immagini
					tagTipoFile = 3;
					url = new URL( parametri[0] );
					return generaIcona( vistaImmagine, R.layout.media_mondo, url.getProtocol() );	// ritorna una bitmap
				}
				int maxDimensioniConAlt = 1;
				int maxDimensioni = 1;
				int maxLunghezzaAlt = 0;
				int maxLunghezzaSrc = 0;
				Element imgGrandeConAlt = null;
				Element imgGrande = null;
				Element imgAltLungo = null;
				Element imgSrcLungo = null;
				for( Element img : lista ) {
					int larga, alta;
					if (img.attr("width").isEmpty()) larga = 1;
					else larga = Integer.parseInt(img.attr("width"));
					if (img.attr("height").isEmpty()) alta = 1;
					else alta = Integer.parseInt(img.attr("height"));
					if( larga * alta > maxDimensioniConAlt && !img.attr("alt").isEmpty() ) {	// la più grande con alt
						imgGrandeConAlt = img;
						maxDimensioniConAlt = larga * alta;
					}
					if( larga * alta > maxDimensioni ) {	// la più grande anche senza alt
						imgGrande = img;
						maxDimensioni = larga * alta;
					}
					if( img.attr("alt").length() > maxLunghezzaAlt ) { // quella con l'alt più lungo
						imgAltLungo = img;
						maxLunghezzaAlt = img.attr( "alt" ).length();
					}
					if( img.attr("src").length() > maxLunghezzaSrc ) { // quella col src più lungo
						imgSrcLungo = img;
						maxLunghezzaSrc = img.attr("src").length();
					}
				}
				String percorso = null;
				if( imgGrandeConAlt != null )
					percorso = imgGrandeConAlt.absUrl( "src" );  //absolute URL on src
				else if( imgGrande != null )
					percorso = imgGrande.absUrl( "src" );
				else if( imgAltLungo != null )
					percorso = imgAltLungo.absUrl( "src" );
				else if( imgSrcLungo != null )
					percorso = imgSrcLungo.absUrl( "src" );
				url = new URL(percorso);
				InputStream inputStream = url.openConnection().getInputStream();
				BitmapFactory.Options opzioni = new BitmapFactory.Options();
				opzioni.inJustDecodeBounds = true; // prende solo le info dell'immagine senza scaricarla
				BitmapFactory.decodeStream(inputStream, null, opzioni);
				// Infine cerca di caricare l'immagine vera e propria ridimensionandola
				if( opzioni.outWidth > vistaImmagineWidth )
					opzioni.inSampleSize = opzioni.outWidth / (vistaImmagineWidth+1);
				inputStream = url.openConnection().getInputStream();
				opzioni.inJustDecodeBounds = false;	// Scarica l'immagine
				bitmap = BitmapFactory.decodeStream( inputStream, null, opzioni );
				tagTipoFile = 1;
			} catch( Exception e ) {
				return null;
			}
			return bitmap;
		}
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			vistaImmagine.setTag(R.id.tag_tipo_file, tagTipoFile);
			if( bitmap != null ) {
				vistaImmagine.setImageBitmap(bitmap);
				vistaImmagine.setTag(R.id.tag_percorso, url.toString());    // usato da Immagine
				if( tagTipoFile == 1 )
					new ImboscaImmagine(media).execute(url);
			}
			if( circo != null ) // può arrivare molto in ritardo quando la pagina non esiste più
				circo.setVisibility(View.GONE);
		}
	}

	public static int checkMultiplePermissions(final Context context, final String... permissions) {
        int result = PackageManager.PERMISSION_GRANTED;
        for (String permission: permissions) {
            result |= ContextCompat.checkSelfPermission(context, permission);
        }

        return result;
    }

    // Methods for image acquisition:

    /**
     * Offers a nice list of apps for capturing images
     */
    public static void displayImageCaptureDialog(Context context, Fragment fragment, int code, MediaContainer container) {
        // Request permission to access device memory
        final String[] requiredPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions = new String[] {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
            };
        } else {
            requiredPermissions = new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            };
        }
        final int perm = checkMultiplePermissions(context, requiredPermissions);
        if (perm == PackageManager.PERMISSION_DENIED) {
            if (fragment != null) { // MediaFragment
                fragment.requestPermissions(requiredPermissions, code);
            } else
                ActivityCompat.requestPermissions((AppCompatActivity)context, requiredPermissions, code);
            return;
        }
        // Collect intents useful to capture images
        List<ResolveInfo> resolveInfos = new ArrayList<>();
        final List<Intent> intents = new ArrayList<>();
        // Cameras
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(cameraIntent, 0)) {
            Intent finalIntent = new Intent(cameraIntent);
            finalIntent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intents.add(finalIntent);
            resolveInfos.add(info);
        }
        // Galleries
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        String[] mimeTypes = {"image/*", "audio/*", "video/*", "application/*", "text/*"};
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
            mimeTypes[0] = "*/*"; // Otherwise KitKat does not see the 'application / *' in Downloads
        galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        for (ResolveInfo info : context.getPackageManager().queryIntentActivities(galleryIntent, 0)) {
            Intent finalIntent = new Intent(galleryIntent);
            finalIntent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intents.add(finalIntent);
            resolveInfos.add(info);
        }
        // Empty Media
        // Doesn't appear when choosing a file in MediaActivity
        if (Global.settings.expert && code != 5173) {
            Intent intent = new Intent(context, MediaFoldersActivity.class);
            ResolveInfo info = context.getPackageManager().resolveActivity(intent, 0);
            intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intents.add(intent);
            resolveInfos.add(info);
        }
        new AlertDialog.Builder(context).setAdapter(createAdapter(context, resolveInfos),
                (dialog, id) -> {
                    Intent intent = intents.get(id);
                    // Set up a URI in which to put the photo taken by the camera app
                    if (intent.getAction() != null && intent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE)) {
                        File dir = context.getExternalFilesDir(String.valueOf(Global.settings.openTree));
                        if (!dir.exists())
                            dir.mkdir();
                        File photoFile = nextAvailableFileName(dir.getAbsolutePath(), "image.jpg");
                        Global.pathOfCameraDestination = photoFile.getAbsolutePath(); // Saves it to retake it after the photo is taken
                        Uri photoUri;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            photoUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", photoFile);
                        else // KitKat
                            photoUri = Uri.fromFile(photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    }
                    if (intent.getComponent().getPackageName().equals("app.familygem")) { // TODO: extract to build property
                        // Create an empty Media
                        Media med;
                        if (code == 4173 || code == 2173) { // Simple media
                            med = new Media();
                            med.setFileTag("FILE");
                            container.addMedia(med);
                            Memory.add(med);
                        } else { // Shared media
                            med = GalleryFragment.newMedia(container);
                            Memory.setFirst(med);
                        }
                        med.setFile("");
                        context.startActivity(intent);
                        U.save(true, Memory.firstObject());
                    } else if (fragment != null)
                        fragment.startActivityForResult(intent, code); // Thus the result returns to the fragment
                    else
                        ((AppCompatActivity)context).startActivityForResult(intent, code);
                }).show();
    }

    /**
     * Closely related to the one above.
     */
    private static ArrayAdapter<ResolveInfo> createAdapter(final Context context, final List<ResolveInfo> resolveInfos) {
        return new ArrayAdapter<ResolveInfo>(context, R.layout.piece_app, R.id.app_title, resolveInfos) {
            @Override
            public View getView(int position, View view1, ViewGroup parent) {
                View view = super.getView(position, view1, parent);
                ResolveInfo info = resolveInfos.get(position);
                ImageView image = view.findViewById(R.id.app_icon);
                TextView textview = view.findViewById(R.id.app_title);
                if (info.activityInfo.packageName.equals("app.familygem")) {
                    image.setImageResource(R.drawable.image);
                    textview.setText(R.string.empty_media);
                } else {
                    image.setImageDrawable(info.loadIcon(context.getPackageManager()));
                    textview.setText(info.loadLabel(context.getPackageManager()).toString());
                }
                return view;
            }
        };
    }

	/**
	 * Save the scanned file and propose to crop it if it is an image
	 * @return true if it opens the dialog and therefore the updating of the activity must be blocked
	 * */
	static boolean proposeCropping(Context contesto, Fragment frammento, Intent data, Media media ) {
		// Trova il percorso dell'immagine
		Uri uri = null;
		String percorso;
		// Contenuto preso con SAF
		if( data != null && data.getData() != null ) {
			uri = data.getData();
			percorso = uriPercorsoFile( uri );
		} // Foto da app camera
		else if( Global.fotoCamera != null ) {
			percorso = Global.fotoCamera;
			Global.fotoCamera = null; // lo resetta
		} // Niente di utilizzabile
		else {
			Toast.makeText( contesto, R.string.something_wrong, Toast.LENGTH_SHORT ).show();
			return false;
		}

		// Crea il file
		File[] fileMedia = new File[1];  // perché occorre final
		if( percorso != null && percorso.lastIndexOf('/') > 0 ) { // se è un percorso completo del file
			// Punta direttamente il file
			fileMedia[0] = new File( percorso );
		} else { // È solo il nome del file 'mioFile.ext' o più raramente null
			// Memoria esterna dell'app: /storage/emulated/0/Android/data/app.familygem/files/12
			File dirMemoria = contesto.getExternalFilesDir( String.valueOf(Global.settings.openTree) );
			try { // Usiamo l'URI
				InputStream input = contesto.getContentResolver().openInputStream( uri );
				// Todo se il file esiste già identico non duplicarlo ma riutilizzarlo: come in Conferma.vediSeCopiareFile()
				if( percorso == null ) { // Nome del file null, va inventato
					String type = contesto.getContentResolver().getType(uri);
					percorso = type.substring(0, type.indexOf('/')) + "."
							+ MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
				}
				fileMedia[0] = nextAvailableFileName( dirMemoria.getAbsolutePath(), percorso );
				FileUtils.copyInputStreamToFile( input, fileMedia[0] ); // Crea la cartella se non esiste
			} catch( Exception e ) {
				String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : contesto.getString(R.string.something_wrong);
				Toast.makeText( contesto, msg, Toast.LENGTH_LONG ).show();
			}
		}
		// Aggiunge il percorso della cartella nel Tree in preferenze
		if( Global.settings.getCurrentTree().dirs.add( fileMedia[0].getParent() ) ) // true se ha aggiunto la cartella
			Global.settings.save();
		// Imposta nel Media il percorso trovato
		media.setFile( fileMedia[0].getAbsolutePath() );

		// Se si tratta di un'immagine apre il diaogo di proposta ritaglio
		String tipoMime = URLConnection.guessContentTypeFromName( fileMedia[0].getName() );
		if( tipoMime != null && tipoMime.startsWith("image/") ) {
			ImageView vistaImmagine = new ImageView( contesto );
			showImage( media, vistaImmagine, null );
			Global.croppedMedia = media; // Media parcheggiato in attesa di essere aggiornato col nuovo percorso file
			Global.edited = false; // per non innescare il recreate() che negli Android nuovi non fa comparire l'AlertDialog
			new AlertDialog.Builder( contesto )
					.setView(vistaImmagine)
					.setMessage( R.string.want_crop_image )
					.setPositiveButton( R.string.yes, (dialog, id) -> cropImage( contesto, fileMedia[0], null, frammento ) )
					.setNeutralButton( R.string.no, (dialog, which) -> {
						concludiProponiRitaglio( contesto, frammento );
					}).setOnCancelListener( dialog -> { // click fuori dal dialogo
						concludiProponiRitaglio( contesto, frammento );
					}).show();
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams( FrameLayout.LayoutParams.MATCH_PARENT, U.dpToPx(320) );
			vistaImmagine.setLayoutParams( params ); // l'assegnazione delle dimensioni deve venire DOPO la creazione del dialogo
			return true;
		}
		return false;
	}
	// Conclusione negativa della proposta di ritaglio dell'immagine: aggiorna semplicemente la pagina per mostrare l'immagine
	static void concludiProponiRitaglio( Context contesto, Fragment frammento ) {
		if( frammento instanceof GalleryFragment)
			((GalleryFragment)frammento).ricrea();
		else if( contesto instanceof DetailActivity)
			((DetailActivity)contesto).refresh();
		else if( contesto instanceof IndividualPersonActivity) {
			IndividualMediaFragment indiMedia = (IndividualMediaFragment) ((AppCompatActivity)contesto).getSupportFragmentManager()
					.findFragmentByTag( "android:switcher:" + R.id.schede_persona + ":0" );
			indiMedia.refresh();
		}
		Global.edited = true; // per rinfrescare le pagine precedenti
	}

	// Avvia il ritaglio di un'immagine con CropImage
	// 'fileMedia' e 'uriMedia': uno dei due è valido, l'altro è null
	static void cropImage(Context contesto, File fileMedia, Uri uriMedia, Fragment frammento ) {
		// Partenza
		if( uriMedia == null )
			uriMedia = Uri.fromFile(fileMedia);
		// Destinazione
		File dirMemoria = contesto.getExternalFilesDir( String.valueOf(Global.settings.openTree) );
		if( !dirMemoria.exists() )
			dirMemoria.mkdir();
		File fileDestinazione;
		if( fileMedia != null && fileMedia.getAbsolutePath().startsWith(dirMemoria.getAbsolutePath()) )
			fileDestinazione = fileMedia; // File già nella cartella memoria vengono sovrascritti
		else {
			String nome;
			if( fileMedia != null )
				nome = fileMedia.getName();
			else // Uri
				nome = DocumentFile.fromSingleUri( contesto, uriMedia ).getName();
			fileDestinazione = nextAvailableFileName( dirMemoria.getAbsolutePath(), nome );
		}
		Intent intent = CropImage.activity( uriMedia )
				.setOutputUri( Uri.fromFile(fileDestinazione) ) // cartella in memoria esterna
				.setGuidelines( CropImageView.Guidelines.OFF )
				.setBorderLineThickness( 1 )
				.setBorderCornerThickness( 6 )
				.setBorderCornerOffset( -3 )
				.setCropMenuCropButtonTitle( contesto.getText(R.string.done) )
				.getIntent( contesto );
		if( frammento != null )
			frammento.startActivityForResult( intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE );
		else
			((AppCompatActivity)contesto).startActivityForResult( intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE );
	}

	/**
	 * If a file with that name already exists in that folder, increment it with 1 2 3 ...
	 * Se in quella cartella esiste già un file con quel nome lo incrementa con 1 2 3...
	 * */
	static File nextAvailableFileName(String dir, String nome ) {
		File file = new File( dir, nome );
		int increment = 0;
		while( file.exists() ) {
			increment++;
			file = new File( dir, nome.substring(0,nome.lastIndexOf('.'))
					+ increment + nome.substring(nome.lastIndexOf('.')) );
		}
		return file;
	}

	/**
	 * Ends the cropping procedure of an image
	 * */
	static void endImageCropping(Intent data ) {
		CropImage.ActivityResult risultato = CropImage.getActivityResult(data);
		Uri uri = risultato.getUri(); // ad es. 'file:///storage/emulated/0/Android/data/app.familygem/files/5/anna.webp'
		Picasso.get().invalidate( uri ); // cancella dalla cache l'eventuale immagine prima del ritaglio che ha lo stesso percorso
		String percorso = uriPercorsoFile( uri );
		Global.croppedMedia.setFile( percorso );
	}

	/**
	 * Answering all permission requests for Android 6+
	 * Risposta a tutte le richieste di permessi per Android 6+
	 * */
	static void permissionsResult(Context contesto, Fragment frammento, int codice, String[] permessi, int[] accordi, MediaContainer contenitore ) {
		if( accordi.length > 0 && accordi[0] == PackageManager.PERMISSION_GRANTED ) {
			displayImageCaptureDialog( contesto, frammento, codice, contenitore );
		} else {
			String permesso = permessi[0].substring(permessi[0].lastIndexOf('.') + 1);
			Toast.makeText( contesto, contesto.getString(R.string.not_granted,permesso), Toast.LENGTH_LONG ).show();
		}
	}
}