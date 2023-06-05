package br.edu.ifsuldeminas.mch.alarmmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifsuldeminas.mch.alarmmanager.db.DatabaseHelper;
import br.edu.ifsuldeminas.mch.alarmmanager.model.Alarm;

public class TimeListActivity extends AppCompatActivity {

    public static String activeAlarm = "";
    private ListView listView;
    private static final int REQUEST_CODE = 1000;

    public static List<Alarm> alarmList = new ArrayList<>();
    private CustomAdapter customAdapter;
    private DatabaseHelper db = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_list);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = findViewById(R.id.listView);
        alarmList.clear();
        List<Alarm> list = db.getAllAlarms();
        alarmList.addAll(list);
        customAdapter = new CustomAdapter(getApplicationContext(), alarmList);
        listView.setAdapter(customAdapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Alarm alarm = alarmList.get(position);
                int rowsAffected = db.deleteAlarm(alarm);
                if (rowsAffected > 0) {
                    alarmList.remove(position);
                    customAdapter.notifyDataSetChanged();
                    Toast.makeText(TimeListActivity.this, "Alarme excluído", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TimeListActivity.this, "Erro ao excluir o alarme", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            boolean needRefresh = data.getExtras().getBoolean("needRefresh");
            if (needRefresh) {
                alarmList.clear();
                List<Alarm> list = db.getAllAlarms();
                alarmList.addAll(list);
                customAdapter.notifyDataSetChanged();
            }
        }
    }
}