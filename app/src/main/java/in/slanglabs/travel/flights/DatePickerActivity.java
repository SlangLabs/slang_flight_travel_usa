package in.slanglabs.travel.flights;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;

import java.util.Calendar;

import in.slanglabs.platform.SlangBuddy;

public class DatePickerActivity extends AppCompatActivity {

    CalendarView calendarView;
    Button selectDate, cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_picker);

        calendarView = findViewById(R.id.calendar_view);
        selectDate = findViewById(R.id.select_date_btn);
        cancel = findViewById(R.id.cancel_btn);

        selectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long dateLong = calendarView.getDate();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(dateLong);

                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                int monthOfYear = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);

                String monthText;
                if ((monthOfYear + 1) < 10) {
                    monthText = "0" + (monthOfYear + 1);
                } else {
                    monthText = String.valueOf(monthOfYear + 1);
                }

                String dateText = dayOfMonth + "-" + monthText + "-" + year;

                Intent intent = getIntent();
                intent.putExtra("dateText", dateText);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                intent.putExtra("dateText", "");
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SlangBuddy.getBuiltinUI().hide();
    }
}
