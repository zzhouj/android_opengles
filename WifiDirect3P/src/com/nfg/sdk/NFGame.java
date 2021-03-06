package com.nfg.sdk;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DialogListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.util.Log;

public class NFGame implements PeerListListener, ConnectionInfoListener, GroupInfoListener {

	public interface NFGameNotifyListener {
		public void onNFGameNotify(NFGame game);
	}

	public static final String TAG = "NFGame";

	private Context mContext;
	private WifiManager mWifiManager;
	private WifiP2pManager mWifiP2pManager;
	private WifiP2pManager.Channel mChannel;
	private NFGameNotifyListener mNFGameNotifyListener;

	private String appLabel = TAG;
	private String serviceType;
	private NFGameBroadcastReceiver mNFGameBroadcastReceiver;
	private NFGameDnsSdServiceResponseListener mNFGameDnsSdServiceResponseListener;
	private WifiP2pDnsSdServiceInfo mWifiP2pDnsSdServiceInfo;
	private WifiP2pDnsSdServiceRequest mWifiP2pDnsSdServiceRequest;
	private Handler mHandler;
	private Runnable mDiscoveryingRunnable;

	private boolean isWifiP2pEnable;
	private boolean isWifiP2pDiscoverying;
	private WifiP2pDevice me;
	private List<WifiP2pDevice> peers;
	private List<WifiP2pDevice> servicePeers;
	private WifiP2pInfo wifiP2pInfo;
	private WifiP2pGroup wifiP2pGroup;
	private int mNetId = -1;

	public NFGame(Context context, NFGameNotifyListener nFGameNotifyListener) {
		mContext = context;
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);
		mNFGameNotifyListener = nFGameNotifyListener;

		mNFGameBroadcastReceiver = new NFGameBroadcastReceiver();
		mNFGameDnsSdServiceResponseListener = new NFGameDnsSdServiceResponseListener();

		try {
			ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), 0);
			appLabel = mContext.getPackageManager().getApplicationLabel(appInfo).toString();
		} catch (NameNotFoundException e) {
			// e.printStackTrace();
		}
		serviceType = mContext.getPackageName();
		mWifiP2pDnsSdServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(appLabel, serviceType, null);
		mWifiP2pDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
		mHandler = new Handler();
		mDiscoveryingRunnable = new Runnable() {
			@Override
			public void run() {
				discoverPeers();
			}
		};

		peers = new ArrayList<WifiP2pDevice>();
		servicePeers = new ArrayList<WifiP2pDevice>();
	}

	public void init() {
		initData();
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mContext.registerReceiver(mNFGameBroadcastReceiver, filter);

		mWifiP2pManager.setDnsSdResponseListeners(mChannel, mNFGameDnsSdServiceResponseListener, null);
		mWifiP2pManager.addLocalService(mChannel, mWifiP2pDnsSdServiceInfo,
				new NFGameActionListener("addLocalService"));
		mWifiP2pManager.addServiceRequest(mChannel, mWifiP2pDnsSdServiceRequest,
				new NFGameActionListener("addServiceRequest"));

		discoverPeers();
		mWifiP2pManager.requestPeers(mChannel, this);

		setDialogListener();
	}

	public void deinit() {
		mContext.unregisterReceiver(mNFGameBroadcastReceiver);

		mWifiP2pManager.clearLocalServices(mChannel, new NFGameActionListener("clearLocalServices"));
		mWifiP2pManager.clearServiceRequests(mChannel, new NFGameActionListener("clearServiceRequests"));

		stopPeerDiscovery();

		mWifiP2pManager.cancelConnect(mChannel, new NFGameActionListener("cancelConnect"));
		mWifiP2pManager.removeGroup(mChannel, new NFGameActionListener("removeGroup"));
		if (mNetId != -1) {
			try {
				Method deletePersistentGroup = mWifiP2pManager.getClass().getMethod("deletePersistentGroup",
						Channel.class, int.class, ActionListener.class);
				deletePersistentGroup.setAccessible(true);
				deletePersistentGroup.invoke(mWifiP2pManager, mChannel, mNetId,
						new NFGameActionListener("deletePersistentGroup"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void initData() {
		isWifiP2pEnable = false;
		isWifiP2pDiscoverying = false;
		me = null;
		peers.clear();
		servicePeers.clear();
		wifiP2pInfo = null;
		wifiP2pGroup = null;
		mNetId = -1;
	}

	private void setDialogListener() {
		try {
			mWifiP2pManager.setDialogListener(mChannel, new DialogListener() {
				@Override
				public void onShowPinRequested(String arg0) {
				}

				@Override
				public void onDetached(int arg0) {
				}

				@Override
				public void onConnectionRequested(WifiP2pDevice arg0, WifiP2pConfig arg1) {
					connect(arg0.deviceAddress, false);
				}

				@Override
				public void onAttached() {
				}
			});
		} catch (NoSuchMethodError e) {
			e.printStackTrace();
		}
	}

	public void reset() {
		deinit();
		init();
	}

	public void connect(String deviceAddress, boolean beGO) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = deviceAddress;
		if (beGO) {
			config.groupOwnerIntent = 15;
		}
		config.wps.setup = WpsInfo.PBC;
		mWifiP2pManager.connect(mChannel, config, new NFGameActionListener("connect"));
	}

	public boolean createGroup() {
		mWifiP2pManager.createGroup(mChannel, new NFGameActionListener("createGroup"));
		return true;
	}

	public void discoverPeers() {
		int invitedPeerCount = 0;
		for (int i = 0; i < peers.size(); i++) {
			WifiP2pDevice device = peers.get(i);
			if (device.status == WifiP2pDevice.INVITED) {
				invitedPeerCount++;
			}
		}
		if (invitedPeerCount == 0) {
			if (!isWifiP2pDiscoverying) {
				mWifiP2pManager.discoverPeers(mChannel, new NFGameActionListener("discoverPeers"));
				// mWifiP2pManager.discoverServices(mChannel, new NFGameActionListener("discoverServices"));
			}
		}
		mHandler.removeCallbacks(mDiscoveryingRunnable);
		mHandler.postDelayed(mDiscoveryingRunnable, 8000);
	}

	public void stopPeerDiscovery() {
		mWifiP2pManager.stopPeerDiscovery(mChannel, new NFGameActionListener("stopPeerDiscovery"));
		mHandler.removeCallbacks(mDiscoveryingRunnable);
	}

	public boolean isWifiP2pEnable() {
		return isWifiP2pEnable;
	}

	public boolean isWifiP2pDiscoverying() {
		return isWifiP2pDiscoverying;
	}

	public WifiP2pDevice getMe() {
		return me;
	}

	public List<WifiP2pDevice> getPeers() {
		return peers;
	}

	public List<WifiP2pDevice> getServicePeers() {
		return servicePeers;
	}

	public WifiP2pInfo getWifiP2pInfo() {
		return wifiP2pInfo;
	}

	public WifiP2pGroup getWifiP2pGroup() {
		return wifiP2pGroup;
	}

	public int getNetId() {
		return mNetId;
	}

	private void onNFGameNotify() {
		if (mNFGameNotifyListener != null) {
			mNFGameNotifyListener.onNFGameNotify(this);
		}
	}

	private class NFGameBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, action);

			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				isWifiP2pEnable = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
				if (!isWifiP2pEnable) {
					mWifiManager.setWifiEnabled(true);
				} else {
					discoverPeers();
				}
				onNFGameNotify();
				Log.d(TAG, "isWifiP2pEnable = " + isWifiP2pEnable);

			} else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);
				isWifiP2pDiscoverying = (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED);
				onNFGameNotify();
				Log.d(TAG, "isWifiP2pDiscoverying = " + isWifiP2pDiscoverying);

			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				me = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
				onNFGameNotify();
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
					onNFGameNotify();
				}
			}
		}
	}

	private class NFGameDnsSdServiceResponseListener implements DnsSdServiceResponseListener {

		@Override
		public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
			if (instanceName.equalsIgnoreCase(appLabel)) {
				for (WifiP2pDevice servicePeer : servicePeers) {
					if (servicePeer.deviceAddress.equals(srcDevice.deviceAddress)) {
						servicePeers.remove(servicePeer);
						break;
					}
				}
				servicePeers.add(srcDevice);
				onNFGameNotify();
				Log.d(TAG, "srcDevice = " + srcDevice);
			}
		}
	}

	private class NFGameActionListener implements ActionListener {
		private String funcName;

		public NFGameActionListener(String funcName) {
			this.funcName = funcName;
		}

		@Override
		public void onFailure(int reason) {
			Log.d(TAG, funcName + " onFailure " + reasonName(reason));
		}

		@Override
		public void onSuccess() {
			Log.d(TAG, funcName + " onSuccess ");
		}

		private String reasonName(int reason) {
			if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
				return "P2P_UNSUPPORTED";
			} else if (reason == WifiP2pManager.ERROR) {
				return "ERROR";
			} else if (reason == WifiP2pManager.BUSY) {
				return "BUSY";
			} else if (reason == WifiP2pManager.NO_SERVICE_REQUESTS) {
				return "NO_SERVICE_REQUESTS";
			} else {
				return String.valueOf(reason);
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
		mergeServicePeers();
		onNFGameNotify();
		if (this.peers.size() == 0) {
			Log.d(TAG, "no peers");
		}
	}

	private void mergeServicePeers() {
		List<WifiP2pDevice> newServicePeers = new ArrayList<WifiP2pDevice>();
		for (WifiP2pDevice servicePeer : servicePeers) {
			for (WifiP2pDevice peer : peers) {
				if (servicePeer.deviceAddress.equals(peer.deviceAddress)) {
					newServicePeers.add(peer);
					break;
				}
			}
		}
		servicePeers = newServicePeers;
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		wifiP2pInfo = info;
		onNFGameNotify();
		Log.d(TAG, "wifiP2pInfo = " + wifiP2pInfo);
	}

	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		wifiP2pGroup = group;
		try {
			Method getNetworkId = group.getClass().getMethod("getNetworkId");
			getNetworkId.setAccessible(true);
			mNetId = (Integer) getNetworkId.invoke(group);
		} catch (Exception e) {
			e.printStackTrace();
		}
		onNFGameNotify();
		Log.d(TAG, "wifiP2pGroup = " + wifiP2pGroup);
	}

}
