package com.nfg.wifidirect3p;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.nfg.sdk.NFGame;
import com.nfg.sdk.NFGame.NFGameNotifyListener;

import fi.iki.elonen.HelloServer;
import fi.iki.elonen.ServerRunner;

public class WifiDirect3PActivity extends Activity implements NFGameNotifyListener, OnClickListener {

	static {
		ServerRunner.run(HelloServer.class);
	}

	private NFGame mNFGame;

	private TextView mTextView;
	private Button mButton1;
	private Button mButton2;
	private Button mButton3;
	private Button mButton4;
	private Button mButton5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mTextView = (TextView) findViewById(R.id.text1);
		mButton1 = (Button) findViewById(R.id.button1);
		mButton2 = (Button) findViewById(R.id.button2);
		mButton3 = (Button) findViewById(R.id.button3);
		mButton4 = (Button) findViewById(R.id.button4);
		mButton5 = (Button) findViewById(R.id.button5);

		mButton1.setOnClickListener(this);
		mButton2.setOnClickListener(this);
		mButton3.setOnClickListener(this);
		mButton4.setOnClickListener(this);
		mButton5.setOnClickListener(this);

		mNFGame = new NFGame(this, this);
		mNFGame.init();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mNFGame.deinit();
	}

	@Override
	public void onNFGameNotify(NFGame game) {
		StringBuffer sb = new StringBuffer();

		sb.append("P2P Enable: ");
		sb.append(game.isWifiP2pEnable());
		sb.append("\n");

		sb.append("P2P Discoverying: ");
		sb.append(game.isWifiP2pDiscoverying());
		sb.append("\n");

		sb.append("Me: ");
		if (game.getMe() != null) {
			sb.append(game.getMe().deviceName);
			sb.append(":");
			sb.append(statusAbbr(game.getMe().status));
		}
		sb.append("\n");

		sb.append("Peers: \n");
		int availablePeerCount = 0;
		int connectedPeerCount = 0;
		for (int i = 0; i < game.getPeers().size(); i++) {
			WifiP2pDevice device = game.getPeers().get(i);
			sb.append("  ");
			sb.append(i + 1);
			sb.append(". ");
			sb.append(device.deviceName);
			sb.append(":");
			sb.append(statusAbbr(device.status));
			sb.append("\n");
			if (device.status == WifiP2pDevice.AVAILABLE) {
				availablePeerCount++;
			} else if (device.status == WifiP2pDevice.CONNECTED) {
				connectedPeerCount++;
			}
		}

		sb.append("Service Peers: \n");
		for (int i = 0; i < game.getServicePeers().size(); i++) {
			WifiP2pDevice device = game.getServicePeers().get(i);
			sb.append("  ");
			sb.append(i + 1);
			sb.append(". ");
			sb.append(device.deviceName);
			sb.append(":");
			sb.append(statusAbbr(device.status));
			sb.append("\n");
		}

		sb.append("Connection info: \n");
		boolean groupFormed = game.getWifiP2pInfo() != null && game.getWifiP2pInfo().groupFormed;
		boolean isGroupOwner = game.getWifiP2pInfo() != null && game.getWifiP2pInfo().isGroupOwner;
		if (groupFormed) {
			sb.append("  isGO: ");
			sb.append(game.getWifiP2pInfo().isGroupOwner);
			sb.append("\n");

			sb.append("  ip: ");
			sb.append(game.getWifiP2pInfo().groupOwnerAddress.getHostAddress());
			sb.append("\n");
		}

		sb.append("Group info: \n");
		if (game.getWifiP2pGroup() != null) {
			sb.append("  ssid: ");
			sb.append(game.getWifiP2pGroup().getNetworkName());
			sb.append("\n");

			sb.append("  pass: ");
			sb.append(game.getWifiP2pGroup().getPassphrase());
			sb.append("\n");

			sb.append("  clients: ");
			sb.append(game.getWifiP2pGroup().getClientList().size());
			sb.append("\n");
		}

		sb.append("Network ID: ");
		sb.append(game.getNetId());
		sb.append("\n");

		mTextView.setText(sb.toString());

		mButton1.setEnabled(!groupFormed);
		mButton2.setEnabled(availablePeerCount >= 1);
		mButton3.setEnabled(!game.isWifiP2pDiscoverying());
	}

	@Override
	public void onClick(View view) {
		if (view == mButton1) {
			mNFGame.createGroup();
		} else if (view == mButton2) {
			for (int i = 0; i < mNFGame.getPeers().size(); i++) {
				WifiP2pDevice peer = mNFGame.getPeers().get(i);
				if (peer.status == WifiP2pDevice.AVAILABLE) {
					mNFGame.connect(i);
					break;
				}
			}
		} else if (view == mButton3) {
			mNFGame.discoverPeers();
		} else if (view == mButton4) {
			startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
		} else if (view == mButton5) {
			mNFGame.reset();
		}
	}

	private String statusAbbr(int status) {
		if (status == WifiP2pDevice.AVAILABLE) {
			return "A";
		} else if (status == WifiP2pDevice.CONNECTED) {
			return "C";
		} else if (status == WifiP2pDevice.FAILED) {
			return "F";
		} else if (status == WifiP2pDevice.INVITED) {
			return "I";
		} else if (status == WifiP2pDevice.UNAVAILABLE) {
			return "U";
		} else {
			return String.valueOf(status);
		}
	}

}
