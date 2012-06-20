/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tools;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

public class ColorPickerDialogDeprecated
extends Dialog {

    public static abstract class OnColorChangedListener {
        public abstract void colorChanged(int color, int red, int green, int blue);
        void colorChanged(int color){
        	int red = Color.red(color);
        	int green = Color.green(color);
        	int blue = Color.blue(color);
        	colorChanged(color, red, green, blue);
        }
    }

    private OnColorChangedListener mListener;
    private int mInitialColor;

    private static class ColorPickerView extends View {
        private Paint mPaint;
        private Paint mCenterPaint;
        private final int[] mColors;
        private OnColorChangedListener mListener;

        ColorPickerView(Context c, OnColorChangedListener l, int color) {
            super(c);
            mListener = l;
            mColors = new int[] {
                0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
                0xFFFFFF00, 0xFFFF0000
            };
            Shader s = new SweepGradient(0, 0, mColors, null);

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setShader(s);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(32);

            mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCenterPaint.setColor(color);
            mCenterPaint.setStrokeWidth(5);
        }

        private boolean mTrackingCenter;
        private boolean mHighlightCenter;

        @Override
        protected void onDraw(Canvas canvas) {
            float r = RADIUS - mPaint.getStrokeWidth()*0.5f;

            canvas.translate(CENTER_X, CENTER_Y);

            canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
            canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

            if (mTrackingCenter) {
                int c = mCenterPaint.getColor();
                mCenterPaint.setStyle(Paint.Style.STROKE);

                if (mHighlightCenter) {
                    mCenterPaint.setAlpha(0xFF);
                } else {
                    mCenterPaint.setAlpha(0x80);
                }
                canvas.drawCircle(0, 0,
                                  CENTER_RADIUS + mCenterPaint.getStrokeWidth(),
                                  mCenterPaint);

                mCenterPaint.setStyle(Paint.Style.FILL);
                mCenterPaint.setColor(c);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(CENTER_X*2, CENTER_Y*2);
        }

        private static final int CENTER_X = 200;
        private static final int CENTER_Y = 100;
        private static final int RADIUS = 100;
        private static final int CENTER_RADIUS = 32;

        private int floatToByte(float x) {
            int n = java.lang.Math.round(x);
            return n;
        }
        private int pinToByte(int n) {
            if (n < 0) {
                n = 0;
            } else if (n > 255) {
                n = 255;
            }
            return n;
        }

        private int ave(int s, int d, float p) {
            return s + java.lang.Math.round(p * (d - s));
        }

        private int interpColor(int colors[], float unit, float brightness) {
            if (unit <= 0) {
                return colors[0];
            }
            if (unit >= 1) {
                return colors[colors.length - 1];
            }

            float p = unit * (colors.length - 1);
            int i = (int)p;
            p -= i;

            // now p is just the fractional part [0...1) and i is the index
            int c0 = colors[i];
            int c1 = colors[i+1];
            int a = ave(Color.alpha(c0), Color.alpha(c1), p);
            int r = ave(Color.red(c0), Color.red(c1), p);
            int g = ave(Color.green(c0), Color.green(c1), p);
            int b = ave(Color.blue(c0), Color.blue(c1), p);
            
            // convert to hsv to add brightness
            float[] hsv = new float[3];
            Color.RGBToHSV(r, g, b, hsv);
            hsv[2] = hsv[2]*brightness;
            float extraBrightness = 0;
            if (hsv[2] > 1){
            	extraBrightness = hsv[2]-1;
            	hsv[2] = 1;
            }
            
            // convert back to rgb
            int color = Color.HSVToColor(hsv);
            
            // add any extra brightness factor
            r = (int) (Color.red(color) + 255*extraBrightness);
            g = (int) (Color.green(color) + 255*extraBrightness);
            b = (int) (Color.blue(color) + 255*extraBrightness);
            r = Math.min(Math.max(0, r), 255);
            g = Math.min(Math.max(0, g), 255);
            b = Math.min(Math.max(0, b), 255);
            
            // now convert back to rgb
            return Color.argb(a, r, g, b);
        }

        private int rotateColor(int color, float rad) {
            float deg = rad * 180 / 3.1415927f;
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);

            ColorMatrix cm = new ColorMatrix();
            ColorMatrix tmp = new ColorMatrix();

            cm.setRGB2YUV();
            tmp.setRotate(0, deg);
            cm.postConcat(tmp);
            tmp.setYUV2RGB();
            cm.postConcat(tmp);

            final float[] a = cm.getArray();

            int ir = floatToByte(a[0] * r +  a[1] * g +  a[2] * b);
            int ig = floatToByte(a[5] * r +  a[6] * g +  a[7] * b);
            int ib = floatToByte(a[10] * r + a[11] * g + a[12] * b);

            return Color.argb(Color.alpha(color), pinToByte(ir),
                              pinToByte(ig), pinToByte(ib));
        }

        private static final float PI = 3.1415926f;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX() - CENTER_X;
            float y = event.getY() - CENTER_Y;
            boolean inCenter = java.lang.Math.sqrt(x*x + y*y) <= CENTER_RADIUS;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTrackingCenter = inCenter;
                    if (inCenter) {
                        mHighlightCenter = true;
                        invalidate();
                        break;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mTrackingCenter) {
                        if (mHighlightCenter != inCenter) {
                            mHighlightCenter = inCenter;
                            invalidate();
                        }
                    } else {
                        float angle = (float)java.lang.Math.atan2(y, x);
                        // need to turn angle [-PI ... PI] into unit [0....1]
                        float unit = angle/(2*PI);
                        if (unit < 0) {
                            unit += 1;
                        }
                        float radiusValue = (float) ((Math.sqrt(x*x + y*y)-CENTER_RADIUS)/(RADIUS-CENTER_RADIUS));
                        mCenterPaint.setColor(interpColor(mColors, unit, radiusValue));
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (mTrackingCenter) {
                        if (inCenter) {
                            mListener.colorChanged(mCenterPaint.getColor());
                        }
                        mTrackingCenter = false;    // so we draw w/o halo
                        invalidate();
                    }
                    break;
            }
            return true;
        }
    }

    public ColorPickerDialogDeprecated(
    		Context context,
    		OnColorChangedListener listener,
    		int initialColor) {
    	super(context, R.style.CenteredDialog);

    	mListener = listener;
    	mInitialColor = initialColor;
    }

    public ColorPickerDialogDeprecated(
    		Context context,
    		OnColorChangedListener listener,
    		int initialRed,
    		int initialGreen,
    		int initialBlue) {
    	super(context, R.style.CenteredDialog);

    	mListener = listener;
    	mInitialColor = Color.rgb(initialRed, initialGreen, initialBlue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnColorChangedListener l = new OnColorChangedListener() {
            public void colorChanged(int color, int red, int green, int blue) {
                mListener.colorChanged(color);
                dismiss();
            }
        };

        setTitle("Touch Center to Select Color");
        setContentView(new ColorPickerView(getContext(), l, mInitialColor));
    }
}