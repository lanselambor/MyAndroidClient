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

		/* 严苛模式（StrictMode） */
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
		button_connect = (Button) findViewById(R.id.button_connect);// 登录按钮
		button_send = (Button) findViewById(R.id.button_send);// 发送信息按钮
		sw_light = (ToggleButton) findViewById(R.id.sw01); // 灯光开关
		sw_air = (ToggleButton) findViewById(R.id.sw02); // 空调开关
		tv_temp = (TextView) findViewById(R.id.textview_temp);// 温度显示
		tv_wet = (TextView) findViewById(R.id.textview_wet);// 湿度显示
		tv_connect = (TextView) findViewById(R.id.tv_connect);// 连接状态信息
		edittext_send = (EditText) findViewById(R.id.edittext_send);// 要发送的信息
		textview_receive = (TextView) findViewById(R.id.textview_receive);// 收到的信息
//		textview_receive.setMovementMethod(ScrollingMovementMethod.getInstance());//滚动模式
		//textview_receive.setMarqueeRepeatLimit(1);
	}

	public void init_action() {
		button_connect.setOnClickListener(startConnect);// 启动联网
		sw_light.setOnCheckedChangeListener(operatingLight);// 灯光控制
		sw_air.setOnCheckedChangeListener(operatingAir);// 空调控制
		button_send.setOnClickListener(SendClickListenerClient);// 发送信息
	}

	// 按钮事件---启动联网
	private OnClickListener startConnect = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// 断开联网线程
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

				button_connect.setText("开始连接");
				IPText.setEnabled(true);
				textview_receive.setText("信息:\n");

				isConnecting = false;
				button_connect.setText("启动联网");
			} else {
				// 启动联网线程
				mThreadClient = new Thread(mRunnable);
				mThreadClient.start();

				isConnecting = true;
				IPText.setEnabled(false);
				button_connect.setText("断开联网");
			}

		}
	};

	// 按钮事件---灯光控制
	private OnCheckedChangeListener operatingLight = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {				
				mPrintWriterClient.print("$client:openLight");// 发送给服务器
				mPrintWriterClient.flush();
				Log.v("Android_Home", "light_up");
			} else {
				mPrintWriterClient.print("$client:closeLight");// 发送给服务器
				mPrintWriterClient.flush();
				Log.v("Android_Home", "light_down");
			}
		}
	};

	// 按钮事件---空调控制
	private OnCheckedChangeListener operatingAir = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {

			if (isChecked) {
				mPrintWriterClient.print("$client:openAir");// 发送给服务器
				mPrintWriterClient.flush();
				Log.v("Android_Home", "air_up");
			} else {
				Log.v("Android_Home", "air_down");
				mPrintWriterClient.print("$client:closeAir");// 发送给服务器
				mPrintWriterClient.flush();
			}
		}
	};

	// 按钮事件---发送信息
	private OnClickListener SendClickListenerClient = new OnClickListener() {
		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			if (isConnecting && mSocketClient != null) {
				
				msgText_send = edittext_send.getText().toString();// 取得编辑框中我们输入的内容

				if (msgText_send.length() <= 0) {
					Toast.makeText(mContext, "发送内容不能为空！", Toast.LENGTH_SHORT)
							.show();
				} else {
					try {
						
						mPrintWriterClient.print(msgText_send);// 发送给服务器
						mPrintWriterClient.flush();
					} catch (Exception e) {
						// TODO: handle exception
						Toast.makeText(mContext, "发送异常：" + e.getMessage(),
								Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
			}
			
		}
	};

	@SuppressLint("HandlerLeak") Handler mHandler = new Handler() {
		@SuppressLint("HandlerLeak")
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 0) {
				textview_receive.append("Server: " + recvMessageServer); // 刷新
			} else if (msg.what == 1) {
				textview_receive.append("Client: " + recvMessageClient); // 刷新

			}
		}
	};

	// 线程:监听服务器发来的消息
	private Runnable mRunnable = new Runnable() {

		@Override
		public void run() {
			String msgText = IPText.getText().toString();
			if (msgText.length() <= 0) {
				Toast.makeText(mContext, "IP不能为空！", Toast.LENGTH_SHORT).show();// 土司，ip地址不能为空
				recvMessageClient = "IP不能为空！\n";// 消息换行
				Message msg = new Message();
				msg.what = 1;
				mHandler.sendMessage(msg);
				return;
			}
			int start = msgText.indexOf(":");
			if ((start == -1) || (start + 1 >= msgText.length())) {
				recvMessageClient = "IP地址不合法\n";// 消息换行
				Message msg = new Message();
				msg.what = 1;
				mHandler.sendMessage(msg);
				return;
			}
			String sIP = msgText.substring(0, start);
			String sPort = msgText.substring(start + 1);
			int port = Integer.parseInt(sPort);

			Log.d("访问地址", "IP:" + sIP + ":" + port);

			try {
				// 连接服务器
				mSocketClient = new Socket(sIP, port); // portnum
				// 取得输入、输出流
				mBufferedReaderClient = new BufferedReader(
						new InputStreamReader(mSocketClient.getInputStream()));

				mPrintWriterClient = new PrintWriter(
						mSocketClient.getOutputStream(), true);

				recvMessageClient = "已经连接server!\n";// 消息换行
				Message msg = new Message();
				msg.what = 1;
				mHandler.sendMessage(msg);
				// break;
			} catch (Exception e) {
				recvMessageClient = "连接IP异常:" + e.toString() + e.getMessage()
						+ "\n";// 消息换行
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
					}	 //清空数组													
					if ((count = mBufferedReaderClient.read(buffer)) > 0) {					
						recvMessageClient = getInfoBuff(buffer, count) + "\n";// 消息换行
						Message msg = new Message();
						msg.what = 1;
						mHandler.sendMessage(msg);
					}
				} catch (Exception e) {
					recvMessageClient = "接收异常:" + e.getMessage() + "\n";// 消息换行
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
