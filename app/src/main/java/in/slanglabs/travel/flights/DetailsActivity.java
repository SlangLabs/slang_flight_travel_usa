package in.slanglabs.travel.flights;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Set;

import in.slanglabs.platform.SlangBuddy;

public class DetailsActivity extends AppCompatActivity {

    private LinearLayout english, hindi;
    private TextView sort, updated, contactUs;
    private static String locale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Set<String> names = new ArraySet<>();
        names.add(SlangInterface.TravelAction.INTENT_SEARCH_FLIGHTS);
        names.add(SlangInterface.TravelAction.INTENT_SORT_FLIGHTS);
        SlangBuddy.getBuiltinUI().setIntentFiltersForDisplay(names);

        contactUs = findViewById(R.id.contact_us);
        contactUs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = createEmailDialog();
                dialog.show();
                TextView tv = dialog.findViewById(R.id.send_email);
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendEmailIntent();
                        dialog.dismiss();
                    }
                });
            }
        });

        sort = findViewById(R.id.details_sort_flights);
        updated = findViewById(R.id.details_search_updated);
        english = findViewById(R.id.details_help_english);

        String searchCriteria = getIntent().getStringExtra("search_criteria");
        if (null != searchCriteria && !searchCriteria.isEmpty()) {
            updateSearchCriteria(searchCriteria);
        }

        setHelp();
        LocalBroadcastManager.getInstance(this).registerReceiver(listener, new IntentFilter("localeChanged"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SlangBuddy.getBuiltinUI().show(this);
    }

    private void setHelp() {
        english.setVisibility(View.VISIBLE);
    }

    private BroadcastReceiver listener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            locale = intent.getStringExtra("localeBroadcast");
            setHelp();
        }
    };

    private Dialog createEmailDialog() {
        View view = View.inflate(DetailsActivity.this, R.layout.dialog_contact_us, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(DetailsActivity.this)
                .setTitle(R.string.contact_us)
                .setCancelable(true)
                .setNeutralButton("Send Email", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        sendEmailIntent();
                    }
                })
                .setView(view);
        return builder.create();
    }

    private void sendEmailIntent() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "42@slanglabs.in", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hi, I tried your demo and have a feedback!");
        startActivity(Intent.createChooser(intent, "Send email..."));
    }

    public void setSort(String sortText) {
        sort.setText(sortText);
    }

    public void setUpdated() {
        updated.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listener);
    }

    public void updateSearchCriteria(String searchCriteria) {
        ((TextView)findViewById(R.id.search_criteria)).setText(searchCriteria);
    }
}
