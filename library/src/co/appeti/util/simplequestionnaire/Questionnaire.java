package co.appeti.util.simplequestionnaire;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Opens HTML questionnaires, such as Google forms. When the user answers the questionnaire, this activity closes itself.
 * 
 * Needs the internet permission, also don't forget to declare this activity in your manifest.
 *
 * You can use {@link openQuestionnaire(Context, String)} to open a questionnaire.
 * If you want to open the questionnaire only once, use {@link openQuestionnaireIfUnanswered(Context, String)}.
 * 
 * If you want to show a questionnaire when the user closes your app via the back button, you can implement this in your main activity:
 * 
 *  @Override
 *  public void onBackPressed() {
 *  	QuestionnaireActivity.openQuestionnaireIfUnanswered(this, "https://docs.google.com/spreadsheet/viewform?formkey=YOURFORM");
 *  	finish();
 *  	return;
 *  }
 *  
 *  If you want to use something else than Google forms, overwrite {@link detectAnsweredQuestionnaire(String)} and set replaceCSS to false.
 */
public class Questionnaire extends Activity {
	public static final String PREFERENCE_NAME = "co.appeti.util.simplequestionnaire";
	public static final String INTENT_FORM_URL = "co.appeti.util.simplequestionnaire.formurl";
	public static final String INTENT_REPLACE_CSS = "co.appeti.util.simplequestionnaire.replaceCSS";

	private static SharedPreferences getPreferences(Context context) {
		return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
	}
	
	/**
	 * Checks if the user has answered the questionnaire already.
	 * The questionnaire is only answered when the user submitted it. If it has been shown, but the user dismissed it, this returns false.
	 * @param context
	 * @param url
	 * @return
	 */
	public static boolean questionnaireAnswered(Context context, String url) {
		return getPreferences(context).getBoolean(url, false);
	}
	
	/**
	 * Sets the questionnaire as answered.
	 * @param context
	 * @param url
	 */
	public static void setQuestionnaireAnswered(Context context, String url) {
		getPreferences(context).edit().putBoolean(url, true).commit();
	}
	
	/**
	 * Opens the questionnaire by loading the url.
	 * @param context A valid context.
	 * @param url The url of the questionnaire, e.g. https://docs.google.com/spreadsheet/viewform?formkey=YOURFORM
	 * @param replaceCSS If true, the plain google docs CSS will be replaced with a more mobile friendly one.
	 */
	public static void openQuestionnaire(Context context, String url, boolean replaceCSS) {
		Intent intent = new Intent(context, Questionnaire.class);
		intent.putExtra(INTENT_FORM_URL, url);
		intent.putExtra(INTENT_REPLACE_CSS, replaceCSS);
    	context.startActivity(intent);
	}

	/**
	 * Opens the questionnaire by loading the url.
	 * @param context A valid context.
	 * @param url The url of the questionnaire, e.g. https://docs.google.com/spreadsheet/viewform?formkey=YOURFORM
	 */
	public static void openQuestionnaire(Context context, String url) {
		openQuestionnaire(context, url, true);
	}
	
	/**
	 * Checks if the user has completed the questionnaire. If not, the questionnaire is opened.
	 * @param context A valid context.
	 * @param url The url of the questionnaire, e.g. https://docs.google.com/spreadsheet/viewform?formkey=YOURFORM
	 * @param replaceCSS If true, the plain google docs CSS will be replaced with a more mobile friendly one.
	 * @return True if the questionnaire was opened.
	 */
	public static boolean openQuestionnaireIfUnanswered(Context context, String url, boolean replaceCSS) {
		if (!questionnaireAnswered(context, url)) {
			openQuestionnaire(context, url, replaceCSS);
			return true;
		}
		return false;
	}

	/**
	 * Checks if the user has completed the questionnaire. If not, the questionnaire is opened.
	 * @param context A valid context.
	 * @param url The url of the questionnaire, e.g. https://docs.google.com/spreadsheet/viewform?formkey=YOURFORM
	 * @return True if the questionnaire was opened.
	 */
	public static boolean openQuestionnaireIfUnanswered(Context context, String url) {
		return openQuestionnaireIfUnanswered(context, url, true);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the url to the form that we will display
		final String formUrl = getIntent().getStringExtra(INTENT_FORM_URL);
		final boolean replaceCSS = getIntent().getBooleanExtra(INTENT_REPLACE_CSS, true);
		
		// Create a webview
		WebView webView = new WebView(this);
		
		setAnswerDetection(formUrl, webView);
		
		// Load the url and show the webview
		if (replaceCSS) replaceCSS(webView, formUrl);
		else webView.loadUrl(formUrl);
		setContentView(webView);
	}
	
	/**
	 * This replaces the standard Google Forms CSS with one that is more mobile friendly.
	 * The HTML is loaded asynchronously.
	 * 
	 * @param webView
	 * @param url
	 */
	protected void replaceCSS(WebView webView, String url) {
		// Display something while the data loads asynchronously
		webView.loadData("<html><body>" + getString(R.string.simplequestionnaire_loading) + "</body></html>", "text/html", null);
		
		final String formUrl = url;
		// Download the html asynchronously and replace the css
		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				String url = params[0];
				try {
					// Download html and convert to a string
					HttpClient client = new DefaultHttpClient();
					HttpGet request = new HttpGet(url);
			        HttpResponse response = client.execute(request);
			        String html = EntityUtils.toString(response.getEntity());
			        // Replace the css
			        return GoogleFormsMobileCSS.insertMobileCSS(html);
				}
				catch (Exception e) {
					Log.e("QuestionnaireActivity", "Loading questionnaire failed: " + url, e);
					return null;
				}
			}
			@Override
			protected void onPostExecute(String html) {
				if (html == null) {
					// Loading failed... close the activity
					finish();
				}
				else {
					// Set the new data into a new web view
					// Re-using the existing web view crashes it... don't know why.
					WebView webView = new WebView(getApplicationContext());
					webView.loadData(html, "text/html; charset=UTF-8", null);
					setAnswerDetection(formUrl, webView);
					setContentView(webView);
				}
		     }
		}.execute(url);
	}
	
	/**
	 * Detects if the questionnaire has been answered by the user. With google forms, it simply checks whether the url contains "formResponse".
	 * If you want to use a custom questionnaire, you can overwrite this method (or make sure your url also contains "formResponse").
	 * @param url
	 * @return
	 */
	protected boolean detectAnsweredQuestionnaire(String url) {
		return url.startsWith("http") && url.contains("formResponse");
	}

	/**
	 * The web view will detect an answered questionnaire. 
	 * It will then save that the questionnaire has been completed and close this activity.
	 * @param formUrl
	 * @param webView
	 */
	protected void setAnswerDetection(final String formUrl, WebView webView) {
		final Context context = getApplicationContext();
		webView.setWebViewClient(new WebViewClient() {
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				// When the user answers ...
				if (detectAnsweredQuestionnaire(url)) {
					// ... save that she completed the questionnaire
					setQuestionnaireAnswered(context, formUrl);
					// ...and close this activity
					finish();
				}
			}
		});
	}
}
