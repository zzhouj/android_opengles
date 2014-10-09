package com.nfg.sdk;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

public class NFGame implements PeerListListener, ConnectionInfoListener, GroupInfoListener {

	public static final String TAG = "NFGame";

	private Context mContext;
	private WifiP2pManager mWifiP2pManager;
	private WifiP2pManager.Channel mChannel;

	private NFGameBroadcastReceiver mNFGameBroadcastReceiver;

	private boolean isWifiP2pEnable;
	private boolean isWifiP2pDiscoverying;
	private WifiP2pDevice me;
	private List<WifiP2pDevice> peers;
	private WifiP2pInfo wifiP2pInfo;
	private WifiP2pGroup wifiP2pGroup;

	public NFGame(Context context) {
		mContext = context;
		mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);
		peers = new ArrayList<WifiP2pDevice>();
	}

	public void init() {
		if (mNFGameBroadcastReceiver == null) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
			filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
			filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
			filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
			filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
			mNFGameBroadcastReceiver = new NFGameBroadcastReceiver();
			mContext.registerReceiver(mNFGameBroadcastReceiver, filter);
		}
		mWifiP2pManager.discoverPeers(mChannel, null);
		mWifiP2pManager.requestPeers(mChannel, this);
	}

	public void deinit() {
		if (mNFGameBroadcastReceiver != null) {
			mContext.unregisterReceiver(mNFGameBroadcastReceiver);
			mNFGameBroadcastReceiver = null;
		}
		mWifiP2pManager.stopPeerDiscovery(mChannel, null);
	}

	private class NFGameBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, action);

			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				isWifiP2pEnable = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
				Log.d(TAG, "isWifiP2pEnable = " + isWifiP2pEnable);

			} else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
				isWifiP2pDiscoverying = (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED);
				Log.d(TAG, "isWifiP2pDiscoverying = " + isWifiP2pDiscoverying);
				if (!isWifiP2pDiscoverying) {
					mWifiP2pManager.discoverPeers(mChannel, null);
				}

			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				me = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
				Log.d(TAG, "me = " + me);

			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
				mWifiP2pManager.requestPeers(mChannel, NFGame.this);

			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
				Log.d(TAG, "networkInfo = " + networkInfo);
				if (networkInfo.isConnected()) {
					mWifiP2pManager.requestConnectionInfo(mChannel, NFGame.this);
					mWifiP2pManager.requestGroupInfo(mChannel, NFGame.this);
				} else {
					wifiP2pInfo = null;
					wifiP2pGroup = null;
				}
			}
		}
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		this.peers.clear();
		for (WifiP2pDevice peer : peers.getDeviceList()) {
			this.peers.add(peer);
			Log.d(TAG, "peer = " + peer);
		}
		if (this.peers.size() == 0) {
			Log.d(TAG, "no peers");
		}
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		wifiP2pInfo = info;
		Log.d(TAG, "wifiP2pInfo = " + wifiP2pInfo);
	}

	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		wifiP2pGroup = group;
		Log.d(TAG, "wifiP2pGroup = " + wifiP2pGroup);
	}

}
