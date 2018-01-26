package com.sploit.messageterror20;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;


public class MessageActivity extends AppCompatActivity {

    int sentMessageCount = 0;
    int sendNumber;
    boolean sucessCount = false;

    ProgressBar progressBar;
    Button btnContact;
    Button btnCancle;
    String preMessage = "";

    EditText editTextPhoneNumber;
    EditText editTextMessage;
    EditText editTextByteCount;
    EditText editTextSendnumber;
    EditText editTextProgressValue;
    String phoneNumbers;
    String messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(MessageActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Toast.makeText(MessageActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }


        };

        new TedPermission(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE,Manifest.permission.RECEIVE_SMS)
                .check();




        btnContact = (Button) findViewById(R.id.contact_Button);
        btnCancle = (Button) findViewById(R.id.cancle_Button);

        editTextMessage = (EditText) findViewById(R.id.message_editText);
        editTextPhoneNumber = (EditText) findViewById(R.id.phoneNumber_editText);
        editTextByteCount = (EditText) findViewById(R.id.messageByte_editText);
        editTextSendnumber = (EditText) findViewById(R.id.sendNumber_editText);
        editTextProgressValue = (EditText) findViewById(R.id.count_editText) ;
        progressBar = (ProgressBar)findViewById(R.id.send_progressBar);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                int byteCount = getLength(editTextMessage.getText().toString());
                if (byteCount > 140) {
                    editTextMessage.setText(preMessage);
                }
                editTextByteCount.setText(getLength(editTextMessage.getText().toString()) + "/140");
                preMessage = editTextMessage.getText().toString();
            }
        };
        editTextMessage.addTextChangedListener(watcher);


        //리스너 등록
        btnContact.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(intent, 0);
            }
        });

        btnCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sucessCount = true;
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                phoneNumbers = editTextPhoneNumber.getText().toString();
                messages = editTextMessage.getText().toString();
                sendNumber = Integer.parseInt(editTextSendnumber.getText().toString());
                if (phoneNumbers.length() > 0 && messages.length() > 0 && sendNumber > 0) {
                    sentMessageCount = 0;
                    sucessCount = false;
                    progressBar.setMax(sendNumber);
                    progressBar.setProgress(0);
                    editTextProgressValue.setText("0/0");

                    multiSendThread abc = new multiSendThread(phoneNumbers, messages);
                    Thread thread = new Thread(abc);
                    thread.setDaemon(true);
                    thread.start();

                  //  sendEggSMS(phoneNumbers, messages);
                } else {
                    Toast.makeText(getBaseContext(), "빈칸을 모두 채워주세요,",
                            Toast.LENGTH_SHORT).show();

                }
            }
        });
    }
    // editText byte 계산
    public static int getLength(String string) {
        int strLength = 0;
        char tempChar[] = new char[string.length()];
        for (int i = 0; i < tempChar.length; i++) {
            tempChar[i] = string.charAt(i);
            if (tempChar[i] < 128) {
                strLength++;
            } else {
                strLength += 2;
            }
        }
        return strLength;
    }

    private void sendEggSMS(final String phoneNumber, final String message) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        if (sucessCount == false) {
                            SystemClock.sleep(1000);
                            sentMessageCount++;
                            progressBar.setProgress(sentMessageCount);
                            Toast.makeText(getBaseContext(), sentMessageCount + "번째 전송 성공!",
                                    Toast.LENGTH_SHORT).show();
                             editTextProgressValue.setText(sentMessageCount + "/" + sendNumber);
                            if (sentMessageCount == sendNumber) {
                                sucessCount = true;
                                return;
                            }
                           sendEggSMS(phoneNumbers, messages);
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;

                }
            }
        }, new IntentFilter(SENT));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    class multiSendThread implements Runnable{
        String phoneNumbers;
        String messages;
        multiSendThread(String p, String m){
            phoneNumbers = p;
            messages = m;
        }
        public void run(){
            sendEggSMS(phoneNumbers, messages);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK)
        {
            Cursor cursor = getContentResolver().query(data.getData(),
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
            cursor.moveToFirst();
            editTextPhoneNumber.setText(cursor.getString(1));     //번호 얻어오기
            cursor.close();
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message, menu);
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
