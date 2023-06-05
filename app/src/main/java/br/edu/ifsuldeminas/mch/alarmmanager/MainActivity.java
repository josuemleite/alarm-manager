package br.edu.ifsuldeminas.mch.alarmmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

import br.edu.ifsuldeminas.mch.alarmmanager.db.DatabaseHelper;
import br.edu.ifsuldeminas.mch.alarmmanager.model.Alarm;

public class MainActivity extends AppCompatActivity {

    private int notificationId = 1;

    private int hour = 1;
    private int minute = 35;

    private TextView selectedTime;
    private Button setTime;
    private Button showTime;

    private static final String PREFS_KEY = "time";
    private static final String SELECTED_TIME = "selected_time";

    private SharedPreferences preferences;

    private Alarm alarmDatabase;
    private boolean needRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);

        createNotificationChannel();

        selectedTime = findViewById(R.id.selectedTime);

        showTime = findViewById(R.id.showButton);
        showTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TimeListActivity.class);
                startActivity(intent);
            }
        });
    }

    public void popTimePicker(View view) {
        // Obtenha a hora atual do sistema
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                hour =  selectedHour;
                minute = selectedMinute;
                selectedTime.setText(String.format(Locale.getDefault(), "%02d : %02d", hour, minute));
            }
        };

        // Use a hora atual do sistema como valor inicial para o TimePicker
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, onTimeSetListener, currentHour, currentMinute, true);

        timePickerDialog.setTitle("Seleciona o Tempo");
        timePickerDialog.show();
    }

    public void setAlarm(View view) {
        EditText alarmDescription = findViewById(R.id.alarmDescription);

        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
        intent.putExtra("notificationId", notificationId);
        intent.putExtra("todo", alarmDescription.getText().toString());

        PendingIntent alarmIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);

        switch (view.getId()) {
            case R.id.setButton: {
                String selectedTime = this.selectedTime.getText().toString();
                String[] timeParts = selectedTime.split(":");
                int hour = Integer.parseInt(timeParts[0].trim());
                int minute = Integer.parseInt(timeParts[1].trim());
                String description = alarmDescription.getText().toString();

                Calendar startTime = Calendar.getInstance();
                startTime.set(Calendar.HOUR_OF_DAY, hour);
                startTime.set(Calendar.MINUTE, minute);
                startTime.set(Calendar.SECOND, 0);
                long alarmStartTime = startTime.getTimeInMillis();

                DatabaseHelper db = new DatabaseHelper(getApplicationContext());

                if (db.isAlarmExists(hour, minute)) {
                    Toast.makeText(this, "Alarme já existe", Toast.LENGTH_SHORT).show();
                    return;
                }

                alarm.set(AlarmManager.RTC_WAKEUP, alarmStartTime, alarmIntent);

                Toast.makeText(this, "Feito!", Toast.LENGTH_SHORT).show();

                alarmDatabase = new Alarm(hour, minute, true, description);
                db.addAlarm(alarmDatabase);

                needRefresh = true;

                break;
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "alarmReminderChannel";
            String description = "Channel for Alarm Manager";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("alarm", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences.Editor editor = preferences.edit();

        String time = selectedTime.getText().toString();
        if (!time.equals(R.string.default_time)) {
            editor.putString(SELECTED_TIME, time);
            editor.apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (preferences.contains(SELECTED_TIME)) {
            String saveSelectedTime = preferences.getString(SELECTED_TIME, "");
            selectedTime.setText(saveSelectedTime);
        }
    }

    @Override
    public void finish() {
        super.finish();
        Intent data = new Intent();
        data.putExtra("needRefresh", needRefresh);
        this.setResult(RESULT_OK, data);
    }
}