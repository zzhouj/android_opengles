package com.nfg.wifidirect3p;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.nfg.sdk.NFGameServer;

public class ChatActivity extends Activity implements OnClickListener {

	public static final String EXTRA_ADDRESS = "ChatActivity.EXTRA_ADDRESS";

	private TextView mTextView;
	private EditText mEditText;
	private Button mButton;

	private Handler mHandler;

	private ChatClient mChatClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.chat);
		mTextView = (TextView) findViewById(R.id.text);
		mEditText = (EditText) findViewById(R.id.edit);
		mButton = (Button) findViewById(R.id.button);
		mButton.setOnClickListener(this);

		mHandler = new Handler();

		String address = getIntent().getStringExtra(EXTRA_ADDRESS);
		try {
			mChatClient = new ChatClient(new URI("ws://" + address + ":" + NFGameServer.DEFAULT_PORT));
			mChatClient.connect();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mChatClient != null) {
			mChatClient.close();
			mChatClient = null;
		}
	}

	private class ChatClient extends WebSocketClient {

		public ChatClient(URI serverURI) {
			super(serverURI);
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
		}

		@Override
		public void onMessage(final String message) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					StringBuffer sb = new StringBuffer();
					sb.append(mTextView.getText());
					sb.append(message);
					sb.append("\n");
					mTextView.setText(sb.toString());
				}
			});
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			finish();
		}

		@Override
		public void onError(Exception ex) {
			ex.printStackTrace();
			finish();
		}
	}

	@Override
	public void onClick(View v) {
		if (v == mButton) {
			if (mChatClient != null) {
				String message = mEditText.getText().toString();
				if (!TextUtils.isEmpty(message)) {
					mChatClient.send(mEditText.getText().toString());
					mEditText.setText("");
				}
			}
		}
	}

}
