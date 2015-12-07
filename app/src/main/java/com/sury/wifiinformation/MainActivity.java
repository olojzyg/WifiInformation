package com.sury.wifiinformation;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private List<ScanResult> scanResult;
    private ListView mListView;
    private Button refreshBtn;
    private Button markBtn;
    private EditText countNum;
    private RadioGroup interval;
    private ListViewAdapter mAdapter;
    public static HashMap<Integer, Boolean> mCheckBoxStatusHashMap = null;
    private Map<String, String> mMap = new HashMap<String, String>();
    private Map<String, String> select_status = new HashMap<String, String>();
    private ArrayList<Map<String, String>> tempList = new ArrayList<Map<String, String>>();
    private Handler handler;
    private Runnable getMsg;
    private Runnable markMsg;
    private int time = 0;
    private int count = 0;
    private ProgressDialog mProgressDialog;
    private String fileName = "wifiInformation.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        init();

    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter rssiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(myRssiChangeReceiver, rssiFilter);
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        wifiInfo = wifiManager.getConnectionInfo();
        scanResult = wifiManager.getScanResults();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.unregisterReceiver(myRssiChangeReceiver);
    }

    private BroadcastReceiver myRssiChangeReceiver
            = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            wifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
            wifiManager.startScan();
            wifiInfo = wifiManager.getConnectionInfo();
            scanResult = wifiManager.getScanResults();
        }};

    void init() {
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        wifiInfo = wifiManager.getConnectionInfo();
        scanResult = wifiManager.getScanResults();
        mCheckBoxStatusHashMap = new HashMap<Integer, Boolean>();
        countNum = (EditText)findViewById(R.id.countNum);
        interval = (RadioGroup)findViewById(R.id.interval);

        refreshBtn = (Button)findViewById(R.id.refreshBtn);

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "刷新！", Toast.LENGTH_SHORT).show();
                scanResult = wifiManager.getScanResults();
                tempList = new ArrayList<Map<String, String>>();
                new Thread(getMsg).start();
            }
        });

        markBtn = (Button)findViewById(R.id.markBtn);

        markBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time = 0;
                count = -1;
                if (interval.getCheckedRadioButtonId() == R.id.threeSec) {
                    time = 3;
                } else if (interval.getCheckedRadioButtonId() == R.id.fiveSec){
                    time = 5;
                }

                try {
                    count = Integer.valueOf(countNum.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (time == 0) {
                    Toast.makeText(MainActivity.this, "请选择统计的频率 Orz", Toast.LENGTH_SHORT).show();
                } else if (count < 0) {
                    Toast.makeText(MainActivity.this, "请输入统计次数 Orz", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "统计" + count + "次", Toast.LENGTH_SHORT).show();
                    mProgressDialog = ProgressDialog.show(MainActivity.this, "记录中", "请等待");
                    try {
                        String i = "";
                        File file = new File(Environment.getExternalStorageDirectory(), fileName);
                        FileOutputStream os = new FileOutputStream(file);
                        os.write(i.getBytes());
                        os.flush();
                        os.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    new Thread(markMsg).start();
                }
            }
        });

        handler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    Log.i("tempList", tempList.toString());

                    mListView = (ListView)findViewById(R.id.listView);
                    mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                    mAdapter = new ListViewAdapter(MainActivity.this,
                            tempList, R.layout.list_item,
                            new String[]{"wifiName", "RSSI"},
                            new int[]{R.id.wifiName, R.id.wifiRSSI});

                    mListView.setAdapter(mAdapter);

                    for (int i = 0; i < tempList.size(); i++) {
                        mCheckBoxStatusHashMap.put(i, false);
                    }

                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String name_s = ((TextView)view.findViewById(R.id.wifiName)).getText().toString();
                            String RSSI_s = ((TextView)view.findViewById(R.id.wifiRSSI)).getText().toString();
                            if (select_status.containsKey(name_s)) {
                                select_status.remove(name_s);
                                ((CheckBox)view.findViewById(R.id.wifiCheckBox)).setChecked(false);
                                mCheckBoxStatusHashMap.put(position, false);
                            } else {
                                select_status.put(name_s, RSSI_s);
                                ((CheckBox)view.findViewById(R.id.wifiCheckBox)).setChecked(true);
                                mCheckBoxStatusHashMap.put(position, true);
                            }
                        }
                    });
                }
            }
        };

        getMsg = new Runnable() {
            @Override
            public void run() {
                if (tempList.size() > 0) {
                    handler.obtainMessage(1).sendToTarget();
                } else {
                    tempList = new ArrayList<Map<String, String>>();
                    for (ScanResult r : scanResult) {
                        mMap = new HashMap<String, String>();
                        mMap.put("wifiName", r.SSID.toString());
                        mMap.put("wifiRSSI", r.level + "db");
                        mMap.put("check", "" + 1);
                        tempList.add(mMap);
                    }
                    handler.postDelayed(getMsg, 100);
                }
            }
        };

        markMsg = new Runnable() {
            @Override
            public void run() {
                if (count > 0) {
                    wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                    wifiInfo = wifiManager.getConnectionInfo();
                    scanResult = wifiManager.getScanResults();
                    String inf = "";
                    for (Map.Entry<String, String> entry : select_status.entrySet()) {
                        int flag = 1;
                        for (ScanResult r : scanResult) {
                            if (entry.getKey().toString().equals(r.SSID.toString())) {
                                inf += r.SSID.toString() + ": " + r.level + " ";
                                flag = 0;
                            }
                        }
                        if (flag == 1) {
                            inf += entry.getKey().toString() + ": " + "0 ";
                        }
                    }
                    print(inf);
                    --count;
                    handler.postDelayed(markMsg, time * 1000);
                } else {
                    mProgressDialog.dismiss();
                }
            }
        };
    }

    public void print(String str) {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(Environment.getExternalStorageDirectory().toString() + "/" + fileName, true);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(str + "\n");
            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                bufferedWriter.close();
                fileWriter.close();
            } catch (IOException e1) {
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
