package com.example.opengles;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;

public class SolarSystemRenderer implements Renderer {

	private boolean mTranslucentBackground;
	private Planet mPlanet;
	private float mTransY;
	private float mAngle;

	public SolarSystemRenderer(boolean translucentBackground) {
		mTranslucentBackground = translucentBackground;
		mPlanet = new Planet(20, 20, 1.0f, 1.0f);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(0.0f, (float) Math.sin(mTransY), -7.0f);
		gl.glRotatef(mAngle, 0.0f, 1.0f, 0.0f);
		gl.glRotatef(mAngle, 1.0f, 0.0f, 0.0f);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

		mPlanet.draw(gl);

		// mTransY += 0.075f;
		mAngle += 0.4f;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		float ratio = (float) width / (float) height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		float zNear = .1f;
		float zFar = 1000;
		float fieldOfView = 30.0f / 57.3f;
		float size = zNear * (float) Math.tan(fieldOfView / 2.0f);
		gl.glFrustumf(-size, size, -size / ratio, size / ratio, zNear, zFar);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glDisable(GL10.GL_DITHER);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
		if (mTranslucentBackground) {
			gl.glClearColor(0, 0, 0, 0);
		} else {
			gl.glClearColor(0.0f, 0.5f, 0.5f, 1.0f);
		}
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glEnable(GL10.GL_DEPTH_TEST);
	}

}
