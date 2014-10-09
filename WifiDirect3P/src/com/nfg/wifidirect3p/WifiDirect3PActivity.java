package com.nfg.wifidirect3p;

import android.app.Activity;
import android.os.Bundle;

import com.nfg.sdk.NFGame;

public class WifiDirect3PActivity extends Activity {

	private NFGame mNFGame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mNFGame = new NFGame(this);
		mNFGame.init();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mNFGame.deinit();
	}

}
