package br.edu.ifsuldeminas.mch.alarmmanager;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Calendar;
import java.util.List;

import br.edu.ifsuldeminas.mch.alarmmanager.db.DatabaseHelper;
import br.edu.ifsuldeminas.mch.alarmmanager.model.Alarm;

public class CustomAdapter extends BaseAdapter {

    private Context context;
    private List<Alarm> alarmList;
    private LayoutInflater layoutInflater;

    public CustomAdapter(Context context, List<Alarm> alarmList) {
        this.context = context;
        this.alarmList = alarmList;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return alarmList.size();
    }

    @Override
    public Object getItem(int i) {
        return alarmList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = layoutInflater.inflate(R.layout.row_item, null);
        final Alarm selectedAlarm = alarmList.get(position);
        final TextView alarmTV = convertView.findViewById(R.id.timeAgended);
        final TextView nameTV = convertView.findViewById(R.id.timeDescription);
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        nameTV.setText(selectedAlarm.getName());
        alarmTV.setText(selectedAlarm.toString());

        final Intent serviceIntent = new Intent(context, AlarmReceiver.class);

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedAlarm.getHour());
        calendar.set(Calendar.MINUTE, selectedAlarm.getMinute());
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DATE, 1);;
        }

        ToggleButton toggleButton = convertView.findViewById(R.id.toggle);
        toggleButton.setChecked(selectedAlarm.getStatus());
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                selectedAlarm.setStatus(isChecked);
                DatabaseHelper db = new DatabaseHelper(context);
                db.updateAlarm(selectedAlarm);

                TimeListActivity.alarmList.clear();
                List<Alarm> list = db.getAllAlarms();
                TimeListActivity.alarmList.addAll(list);
                notifyDataSetChanged();

                if (!isChecked && selectedAlarm.toString().equals(TimeListActivity.activeAlarm)) {
                    serviceIntent.putExtra("extra", "off");
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, position, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    alarmManager.cancel(pendingIntent);
                    context.sendBroadcast(serviceIntent);
                } else if (isChecked) {
                    serviceIntent.putExtra("extra", "on");
                    serviceIntent.putExtra("active", selectedAlarm.toString());
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, position, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }
        });

        return convertView;
    }
}
