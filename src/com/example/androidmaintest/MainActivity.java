package com.example.androidmaintest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	//private static String HOST = "192.168.1.123";
	private static String HOST = "120.24.101.184";
	private static int PORT = 50000;

	private Thread mThreadClient = null;
	private Socket mSocketClient = null;
	

	private EditText IPText = null;
	private String recvMessageClient = "";
	private String recvMessageServer = "";

	private BufferedReader mBufferedReaderClient = null;
	private PrintWriter mPrintWriterClient = null;
	
	private TextView textview_receive = null;
	private EditText edittext_send = null;
	private String msgText_send = null;	

	private Button button_connect = null;
	private Button button_send = null;
	private Context mContext;

	private TextView tv_temp = null;
	private TextView tv_wet = null;
	private TextView tv_connect = null;

	

	private ToggleButton sw_light = null;
	private ToggleButton sw_air = null;
	
	private boolean isConnecting = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = this;

		/* �Ͽ�ģʽ��StrictMode�� */
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()        
        .detectDiskReads()        
        .detectDiskWrites()        
        .detectNetwork()   // or .detectAll() for all detectable problems       
        .penaltyLog()        
        .build());        
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()        
        .detectLeakedSqlLiteObjects()     
        .penaltyLog()        
        .penaltyDeath()        
        .build()); 

		initView();
		init_action();

	}

	public void initView() {
		IPText = (EditText) findViewById(R.id.IPText);
		IPText.setText(HOST + ":" + PORT);
		button_connect = (Button) findViewById(R.id.button_connect);// ��¼��ť
		button_send = (Button) findViewById(R.id.button_send);// ������Ϣ��ť
		sw_light = (ToggleButton) findViewById(R.id.sw01); // �ƹ⿪��
		sw_air = (ToggleButton) findViewById(R.id.sw02); // �յ�����
		tv_temp = (TextView) findViewById(R.id.textview_temp);// �¶���ʾ
		tv_wet = (TextView) findViewById(R.id.textview_wet);// ʪ����ʾ
		tv_connect = (TextView) findViewById(R.id.tv_connect);// ����״̬��Ϣ
		edittext_send = (EditText) findViewById(R.id.edittext_send);// Ҫ���͵���Ϣ
		textview_receive = (TextView) findViewById(R.id.textview_receive);// �յ�����Ϣ
//		textview_receive.setMovementMethod(ScrollingMovementMethod.getInstance());//����ģʽ
		//textview_receive.setMarqueeRepeatLimit(1);
	}

	public void init_action() {
		button_connect.setOnClickListener(startConnect);// ��������
		sw_light.setOnCheckedChangeListener(operatingLight);// �ƹ����
		sw_air.setOnCheckedChangeListener(operatingAir);// �յ�����
		button_send.setOnClickListener(SendClickListenerClient);// ������Ϣ
	}

	// ��ť�¼�---��������
	private OnClickListener startConnect = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// �Ͽ������߳�
			if (isConnecting) {
				try {
					if (mSocketClient != null) {
						mSocketClient.close();
						mSocketClient = null;

						mPrintWriterClient.close();
						mPrintWriterClient = null;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mThreadClient.interrupt();

				button_connect.setText("��ʼ����");
				IPText.setEnabled(true);
				textview_receive.setText("��Ϣ:\n");

				isConnecting = false;
				button_connect.setText("��������");
			} else {
				// ���������߳�
				mThreadClient = new Thread(mRunnable);
				mThreadClient.start();

				isConnecting = true;
				IPText.setEnabled(false);
				button_connect.setText("�Ͽ�����");
			}

		}
	};

	// ��ť�¼�---�ƹ����
	private OnCheckedChangeListener operatingLight = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {				
				mPrintWriterClient.print("$client:openLight");// ���͸�������
				mPrintWriterClient.flush();
				Log.v("Android_Home", "light_up");
			} else {
				mPrintWriterClient.print("$client:closeLight");// ���͸�������
				mPrintWriterClient.flush();
				Log.v("Android_Home", "light_down");
			}
		}
	};

	// ��ť�¼�---�յ�����
	private OnCheckedChangeListener operatingAir = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {

			if (isChecked) {
				mPrintWriterClient.print("$client:openAir");// ���͸�������
				mPrintWriterClient.flush();
				Log.v("Android_Home", "air_up");
			} else {
				Log.v("Android_Home", "air_down");
				mPrintWriterClient.print("$client:closeAir");// ���͸�������
				mPrintWriterClient.flush();
			}
		}
	};

	// ��ť�¼�---������Ϣ
	private OnClickListener SendClickListenerClient = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			if (isConnecting && mSocketClient != null) {
				
				msgText_send = edittext_send.getText().toString();// ȡ�ñ༭�����������������

				if (msgText_send.length() <= 0) {
					Toast.makeText(mContext, "�������ݲ���Ϊ�գ�", Toast.LENGTH_SHORT)
							.show();
				} else {
					try {
						
						mPrintWriterClient.print(msgText_send);// ���͸�������
						mPrintWriterClient.flush();
					} catch (Exception e) {
						// TODO: handle exception
						Toast.makeText(mContext, "�����쳣��" + e.getMessage(),
								Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				Toast.makeText(mContext, "û������", Toast.LENGTH_SHORT).show();
			}
			
		}
	};

	@SuppressLint("HandlerLeak") Handler mHandler = new Handler() {
		@SuppressLint("HandlerLeak")
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 0) {
				textview_receive.append("Server: " + recvMessageServer); // ˢ��
			} else if (msg.what == 1) {
				textview_receive.append("Client: " + recvMessageClient); // ˢ��

			}
		}
	};

	// �߳�:������������������Ϣ
	private Runnable mRunnable = new Runnable() {

		@Override
		public void run() {
			String msgText = IPText.getText().toString();
			if (msgText.length() <= 0) {
				Toast.makeText(mContext, "IP����Ϊ�գ�", Toast.LENGTH_SHORT).show();// ��˾��ip��ַ����Ϊ��
				recvMessageClient = "IP����Ϊ�գ�\n";// ��Ϣ����
				Message msg = new Message();
				msg.what = 1;
				mHandler.sendMessage(msg);
				return;
			}
			int start = msgText.indexOf(":");
			if ((start == -1) || (start + 1 >= msgText.length())) {
				recvMessageClient = "IP��ַ���Ϸ�\n";// ��Ϣ����
				Message msg = new Message();
				msg.what = 1;
				mHandler.sendMessage(msg);
				return;
			}
			String sIP = msgText.substring(0, start);
			String sPort = msgText.substring(start + 1);
			int port = Integer.parseInt(sPort);

			Log.d("���ʵ�ַ", "IP:" + sIP + ":" + port);

			try {
				// ���ӷ�����
				mSocketClient = new Socket(sIP, port); // portnum
				// ȡ�����롢�����
				mBufferedReaderClient = new BufferedReader(
						new InputStreamReader(mSocketClient.getInputStream()));

				mPrintWriterClient = new PrintWriter(
						mSocketClient.getOutputStream(), true);

				recvMessageClient = "�Ѿ�����server!\n";// ��Ϣ����
				Message msg = new Message();
				msg.what = 1;
				mHandler.sendMessage(msg);
				// break;
			} catch (Exception e) {
				recvMessageClient = "����IP�쳣:" + e.toString() + e.getMessage()
						+ "\n";// ��Ϣ����
				Message msg = new Message();
				msg.what = 1;
				mHandler.sendMessage(msg);
				return;
			}

			char[] buffer = new char[100];
			int count = 0;
			while (isConnecting) {
				try {
					 //if ( (recvMessageClient = mBufferedReaderClient.readLine()) != null ) {
					
					int i = 100;
					for(;i == 0;i--) {
						buffer[i] = '0';
					}	 //�������													
					if ((count = mBufferedReaderClient.read(buffer)) > 0) {					
						recvMessageClient = getInfoBuff(buffer, count) + "\n";// ��Ϣ����
						Message msg = new Message();
						msg.what = 1;
						mHandler.sendMessage(msg);
					}
				} catch (Exception e) {
					recvMessageClient = "�����쳣:" + e.getMessage() + "\n";// ��Ϣ����
					Message msg = new Message();
					msg.what = 1;
					mHandler.sendMessage(msg);
					
				}
				
			}

		}

	};

	private String getInfoBuff(char[] buff, int count) {
		char[] temp = new char[count];
		for (int i = 0; i < count; i++) {
			temp[i] = buff[i];
		}
		return new String(temp);
	}

}
