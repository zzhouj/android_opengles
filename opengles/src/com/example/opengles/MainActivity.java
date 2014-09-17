package com.example.opengles;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		GLSurfaceView glView = new GLSurfaceView(this);
		// glView.setRenderer(new SquareRenderer(true));
		// glView.setRenderer(new CubeRenderer(false));
		glView.setRenderer(new SolarSystemRenderer(true));
		setContentView(glView);
	}

}
