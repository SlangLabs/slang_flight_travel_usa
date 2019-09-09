package in.slanglabs.travel.flights;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

import in.slanglabs.platform.SlangBuddy;

import static in.slanglabs.travel.flights.AppTravelAction.resetCache;

public class MainActivity extends AppCompatActivity {

    private LinearLayout english;
    private EditText source, destination, startDate;
    private TextView contactUs;
    private Button search;
    private String locale;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        locale = "en";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SlangInterface.init(this);

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

        source = findViewById(R.id.main_source_edit);
        destination = findViewById(R.id.main_dest_edit);
        startDate = findViewById(R.id.main_start_edit);
        english = findViewById(R.id.main_help_english);

        startDate.setInputType(InputType.TYPE_NULL);
        source.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                enableSubmitIfReady();
            }
        });

        destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                enableSubmitIfReady();
            }
        });

        startDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                enableSubmitIfReady();
            }
        });

        startDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.performClick();
                }
            }
        });
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH);
                int mDay = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {

                                String dateText = dayOfMonth + "-" + (monthOfYear + 1) + "-" + year;

                                startDate.setText(dateText);

                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                datePickerDialog.show();
            }
        });

        search = findViewById(R.id.main_search);
        search.setEnabled(false);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                String searchCriteria =
                        "From:" + (!source.getText().toString().isEmpty() ? source.getText() : "NA")
                        + "\nTo:" + (!destination.getText().toString().isEmpty() ? destination.getText() : "NA")
                        + "\nLeaving On:" + (!startDate.getText().toString().isEmpty() ? startDate.getText() : "NA");
                intent.putExtra("search_criteria", searchCriteria);
                intent.putExtra("locale", "en");
                AppTravelAction.setTouchEntities(
                        source.getText().toString(),
                        destination.getText().toString(),
                        startDate.getText().toString()
                );
                startActivity(intent);
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(listener, new IntentFilter("localeChanged"));

        sharedPreferences = getSharedPreferences("SharedPref", MODE_PRIVATE);
        locale = sharedPreferences.getString("locale", "en");
        boolean ask = sharedPreferences.getBoolean("ask", true);
        if (ask) {
            Dialog dialog = createNoLocationDialog();
            dialog.show();
        }
    }

    private void sendEmailIntent() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "42@slanglabs.in", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hi, I tried your demo and have a feedback!");
        startActivity(Intent.createChooser(intent, "Send email..."));
    }

    @Override
    protected void onResume() {
        super.onResume();
        source.setText("");
        destination.setText("");
        startDate.setText("");
        resetCache();
        SlangBuddy.getBuiltinUI().show(this);
        SlangBuddy.getBuiltinUI().clearIntentFiltersForDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listener);
    }

    private Dialog createNoLocationDialog() {
        View view = View.inflate(MainActivity.this, R.layout.dialog_intro, null);
        CheckBox neverShowDialog = view.findViewById(R.id.location_never_ask_again);

        neverShowDialog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // Save the preference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("ask", !isChecked);
                editor.apply();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.welcome_title)
                .setCancelable(false)
                .setNeutralButton("Let's jump in", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setView(view);
        return builder.create();
    }

    private Dialog createEmailDialog() {
        View view = View.inflate(MainActivity.this, R.layout.dialog_contact_us, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
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

    private BroadcastReceiver listener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            locale = intent.getStringExtra("localeBroadcast");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("locale", locale);
            editor.apply();
            english.setVisibility(View.VISIBLE);
        }
    };

    private void enableSubmitIfReady() {
        boolean enable = !source.getText().toString().isEmpty() &&
                !destination.getText().toString().isEmpty() &&
                !startDate.getText().toString().isEmpty();
        search.setEnabled(enable);
    }

    public void setSource(String sourceText) {
        source.setText(sourceText);
    }

    public void setDestination(String destText) {
        destination.setText(destText);
    }

    public void setStartDate(String startDateText) {
        startDate.setText(startDateText);
    }
}
