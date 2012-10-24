package co.appeti.util.simplequestionnaire.sample;

import co.appeti.util.simplequestionnaire.Questionnaire;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LoremIpsum extends Activity {
	private static final String inAppQuestionaire = "https://docs.google.com/spreadsheet/viewform?formkey=dEdOSGlnMUh4V2kxNFpCNk0wcEY3QWc6MQ#gid=0";
	private static final String onCloseQuestionnaire = "https://docs.google.com/spreadsheet/viewform?formkey=dHlHaVdORzZSNjZWVUQ5dUVYWFhiOWc6MQ#gid=0";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lorem_ipsum);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_lorem_ipsum, menu);
        return true;
    }
    
    public void next(View v) {
    	// Show a questionnaire about the previous text if the user hasn't answered it before
    	if (!Questionnaire.questionnaireAnswered(this, inAppQuestionaire)) {
    		// Create a dialog to ask whether the user wants to answer now
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setTitle(R.string.questionnaire_question);
	    	final Activity act = this;
	    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					// Open questionnaire
					Questionnaire.openQuestionnaire(act, inAppQuestionaire);
				}
			});
	    	builder.setNegativeButton("Not now", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
	    	builder.create().show();
    	}
    	// Display another text
    	TextView tv = (TextView) findViewById(R.id.lorem_ipsum);
    	tv.setText(R.string.hipster_lorem_ipsum);
    	Button b = (Button) findViewById(R.id.button);
    	b.setVisibility(View.INVISIBLE);
    }
    
    @Override
    public void onBackPressed() {
    	// Show the questionnaire
    	Questionnaire.openQuestionnaire(this, onCloseQuestionnaire);
    	// Close self (framework doesn't handle that automatically anymore when we implement this method)
    	finish();
    	return;
    }
}
