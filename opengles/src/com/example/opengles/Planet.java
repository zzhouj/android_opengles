package com.example.opengles;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Planet {
	FloatBuffer mVertexData;
	FloatBuffer mNormalData;
	FloatBuffer mColorData;

	float mScale;
	float mSquash;
	float mRadius;

	int mStacks;
	int mSlices;

	public Planet(int stacks, int slices, float radius, float squash) {
		this.mStacks = stacks;
		this.mSlices = slices + 1;
		this.mRadius = radius;
		this.mSquash = squash;
		init();
	}

	private void init() {
		float[] vertexData;
		float[] colorData;
		float colorIncrement = 1.0f / (float) mStacks;

		float blue = 0f;
		float red = 1.0f;
		int numVertices = 0;
		int vIndex = 0;
		int cIndex = 0;

		mScale = mRadius;

		vertexData = new float[3 * mSlices * 2 * mStacks];
		colorData = new float[4 * mSlices * 2 * mStacks];

		for (int i = 0; i < mStacks; i++) {
			float phi0 = (float) Math.PI * ((float) (i + 0) * (1.0f / (float) mStacks) - 0.5f);
			float phi1 = (float) Math.PI * ((float) (i + 1) * (1.0f / (float) mStacks) - 0.5f);
			float cosPhi0 = (float) Math.cos(phi0);
			float sinPhi0 = (float) Math.sin(phi0);
			float cosPhi1 = (float) Math.cos(phi1);
			float sinPhi1 = (float) Math.sin(phi1);
			for (int j = 0; j < mSlices; j++) {
				float theta = -2.0f * (float) Math.PI * (float) j * (1.0f / (float) (mSlices - 1));
				float costheta = (float) Math.cos(theta);
				float sintheta = (float) Math.sin(theta);
				vertexData[vIndex + 0] = mScale * cosPhi0 * costheta;
				vertexData[vIndex + 1] = mScale * sinPhi0 * mSquash;
				vertexData[vIndex + 2] = mScale * cosPhi0 * sintheta;

				vertexData[vIndex + 3] = mScale * cosPhi1 * costheta;
				vertexData[vIndex + 4] = mScale * sinPhi1 * mSquash;
				vertexData[vIndex + 5] = mScale * cosPhi1 * sintheta;

				colorData[cIndex + 0] = red;
				colorData[cIndex + 1] = 0f;
				colorData[cIndex + 2] = blue;
				colorData[cIndex + 3] = 1f;

				colorData[cIndex + 4] = red;
				colorData[cIndex + 5] = 0f;
				colorData[cIndex + 6] = blue;
				colorData[cIndex + 7] = 1f;

				vIndex += 6;
				cIndex += 8;
			}
			blue += colorIncrement;
			red -= colorIncrement;
		}
		mVertexData = makeFloatBuffer(vertexData);
		mColorData = makeFloatBuffer(colorData);
	}

	public void draw(GL10 gl) {
		gl.glFrontFace(GL10.GL_CW);

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexData);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorData);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mSlices * 2 * mStacks);
	}

	private FloatBuffer makeFloatBuffer(float[] data) {
		ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(data);
		fb.position(0);
		return fb;
	}

}
