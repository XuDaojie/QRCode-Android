QRCode
===
本项目依赖于[ZXing](https://github.com/zxing/zxing) 3.2.1
![Scan](https://github.com/XuDaojie/QRCode-Android/art/scan_qrcode.gif)

## Use

### Add permission
``` xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FLASHLIGHT" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

### Register activity
``` xml
<activity android:name="io.github.xudaojie.qrcodelib.CaptureActivity"/>
```

### Code
``` java
Intent i = new Intent(mContent, CaptureActivity.class);
startActivityForResult(i, REQUEST_QR_CODE);
```

``` java
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK
            && requestCode == REQUEST_QR_CODE
            && data != null) {
        String result = data.getStringExtra("result");
        Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
    }
}
```

## Including in your project

### Add repository
    
想引入你的项目，需要修改你的build.gradle
``` gradle
repositories {
    maven { url "http://repo1.maven.org/maven2" }
    maven { url 'https://jitpack.io' }
}
```

### Add dependency
``` gradle
```

### 吃水不忘挖井人
[ZXing](https://github.com/zxing/zxing)<br>
[zxing扫描二维码和识别图片二维码及其优化策略](http://iluhcm.com/2016/01/08/scan-qr-code-and-recognize-it-from-picture-fastly-using-zxing/)<br>