/*
 * Copyright (C) 2008 ZXing authors
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

package io.github.xudaojie.qrcodelib.zxing.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

import java.util.Collection;
import java.util.HashSet;

import io.github.xudaojie.qrcodelib.R;
import io.github.xudaojie.qrcodelib.zxing.camera.CameraManager;


/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    public static int RECT_OFFSET_X; // 扫描区域偏移量 默认位于屏幕中间
    public static int RECT_OFFSET_Y;

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final int OPAQUE = 0xFF;
    private static long ANIMATION_DELAY = 10L;


    private final Paint paint;
    private final int maskColor;
    private final int resultColor;
    private final int frameColor;
    private final int laserColor;
    private final int resultPointColor;
    private final int angleColor;
    private String hint;
    private int hintColor;
    private String errorHint;
    private int errorHintColor;
    private boolean showPossiblePoint;
    private Bitmap resultBitmap;
    private int scannerAlpha;
    private Collection<ResultPoint> possibleResultPoints;
    private Collection<ResultPoint> lastPossibleResultPoints;

    private float translateY = 5f;
    private int cameraPermission = PackageManager.PERMISSION_DENIED;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.qr_ViewfinderView);
        angleColor = typedArray.getColor(R.styleable.qr_ViewfinderView_qr_angleColor, Color.WHITE);
        hint = typedArray.getString(R.styleable.qr_ViewfinderView_qr_hint);
        hintColor = typedArray.getColor(R.styleable.qr_ViewfinderView_qr_textHintColor, Color.GRAY);
        errorHint = typedArray.getString(R.styleable.qr_ViewfinderView_qr_errorHint);
        errorHintColor = typedArray.getColor(R.styleable.qr_ViewfinderView_qr_textErrorHintColor, Color.WHITE);
        showPossiblePoint = typedArray.getBoolean(R.styleable.qr_ViewfinderView_qr_showPossiblePoint, false);

        RECT_OFFSET_X = typedArray.getInt(R.styleable.qr_ViewfinderView_qr_offsetX, 0);
        RECT_OFFSET_Y = typedArray.getInt(R.styleable.qr_ViewfinderView_qr_offsetY, 0);

        if (TextUtils.isEmpty(hint)) {
            hint = "将二维码/条形码置于框内即自动扫描";
        }
        if (TextUtils.isEmpty(errorHint)) {
            errorHint = "请允许访问摄像头后重试";
        }
        if (showPossiblePoint) {
            ANIMATION_DELAY = 100L;
        }

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        frameColor = resources.getColor(R.color.viewfinder_frame);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new HashSet<ResultPoint>(5);

        typedArray.recycle();
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = null;
        if (!isInEditMode()) {
            if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
                cameraPermission = CameraManager.get().checkCameraPermission();
            }
            frame = CameraManager.get().getFramingRect(RECT_OFFSET_X, RECT_OFFSET_Y);
        }

        if (frame == null) {
            // Android Studio中预览时和未获得相机权限时都为null
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int width = 675;
            int height = 675;
            int leftOffset = (screenWidth - width) / 2;
            int topOffset = (screenHeight - height) / 2;
            frame = new Rect(leftOffset + RECT_OFFSET_X,
                    topOffset + RECT_OFFSET_Y,
                    leftOffset + width + RECT_OFFSET_X,
                    topOffset + height + RECT_OFFSET_Y);
//            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        drawText(canvas, frame);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {
            // Draw a two pixel solid black border inside the framing rect
//            paint.setColor(frameColor);
            paint.setColor(Color.GRAY);
            canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
            canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
            canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
            canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);

            drawAngle(canvas, frame);
            drawScanner(canvas, frame);
            if (showPossiblePoint) {
                drawPossiblePoint(canvas, frame);
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }

    private void drawAngle(Canvas canvas, Rect frame) {
        int angleLength = 50;
        int angleWidth = 10;
        int top = frame.top;
        int bottom = frame.bottom;
        int left = frame.left;
        int right = frame.right;

        paint.setColor(angleColor);
        // 左上
        canvas.drawRect(left - angleWidth, top - angleWidth, left + angleLength, top, paint);
        canvas.drawRect(left - angleWidth, top - angleWidth, left, top + angleLength, paint);
        // 左下
        canvas.drawRect(left - angleWidth, bottom, left + angleLength, bottom + angleWidth, paint);
        canvas.drawRect(left - angleWidth, bottom - angleLength, left, bottom + angleWidth, paint);
        // 右上
        canvas.drawRect(right - angleLength, top - angleWidth, right + angleWidth, top, paint);
        canvas.drawRect(right, top - angleWidth, right + angleWidth, top + angleLength, paint);
        // 右下
        canvas.drawRect(right - angleLength, bottom, right, bottom + angleWidth, paint);
        canvas.drawRect(right, bottom - angleLength, right + angleWidth, bottom + angleWidth, paint);
    }

    private void drawText(Canvas canvas, Rect frame) {
        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
            paint.setColor(hintColor);
            paint.setTextSize(36);
            String text = hint;
            canvas.drawText(hint, frame.centerX() - text.length() * 36 / 2, frame.bottom + 35 + 20, paint);
        } else {
            paint.setColor(errorHintColor);
            paint.setTextSize(36);
            String text = errorHint;
            canvas.drawText(errorHint, frame.centerX() - text.length() * 36 / 2, frame.bottom + 35 + 20, paint);
        }
    }

    // 绘制扫描线
    // 如果允许绘制 `possible points`则显示居中的红线
    private void drawScanner(Canvas canvas, Rect frame) {
        if (showPossiblePoint) {
            // Draw a red "laser scanner" line through the middle to show decoding is active
            paint.setColor(laserColor);
            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            int middle = frame.height() / 2 + frame.top;
            canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
        } else {
            paint.setColor(Color.parseColor("#03A9F4"));
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            canvas.translate(0, translateY);
            canvas.drawRect(frame.left + 10, frame.top, frame.right - 10, frame.top + 10, paint);

            translateY += 5f;
            if (translateY >= 670) {
                translateY = 5f;
            }
        }
    }

    // Draw a yellow "possible points"
    private void drawPossiblePoint(Canvas canvas, Rect frame) {
        Collection<ResultPoint> currentPossible = possibleResultPoints;
        Collection<ResultPoint> currentLast = lastPossibleResultPoints;
        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new HashSet<ResultPoint>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(OPAQUE);
            paint.setColor(resultPointColor);
            for (ResultPoint point : currentPossible) {
                canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
            }
        }
        if (currentLast != null) {
            paint.setAlpha(OPAQUE / 2);
            paint.setColor(resultPointColor);
            for (ResultPoint point : currentLast) {
                canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
            }
        }
    }
}
