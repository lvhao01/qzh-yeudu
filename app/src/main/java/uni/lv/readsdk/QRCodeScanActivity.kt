package uni.lv.readsdk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.google.zxing.ResultPoint

/**
 * 二维码扫描Activity
 * 负责扫描二维码并将结果返回给调用者
 */
class QRCodeScanActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private var hasScanned = false // 防止重复扫描

    companion object {
        private const val TAG = "QRCodeScanActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        
        // 返回结果的Key
        const val EXTRA_SCAN_RESULT = "scan_result"
        
        // 结果码
        const val RESULT_SCAN_SUCCESS = Activity.RESULT_OK
        const val RESULT_SCAN_CANCELED = Activity.RESULT_CANCELED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode_scan)

        barcodeView = findViewById(R.id.barcode_scanner)

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            // 已有权限，开始扫描
            startScan()
        }
    }

    /**
     * 开始扫描二维码
     */
    private fun startScan() {
        // 设置扫描回调
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result != null && !hasScanned) {
                    hasScanned = true // 标记已扫描，防止重复
                    
                    val qrCodeContent = result.text
                    android.util.Log.d(TAG, "扫描成功: $qrCodeContent")
                    
                    // 返回扫描结果
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_SCAN_RESULT, qrCodeContent)
                    }
                    setResult(RESULT_SCAN_SUCCESS, resultIntent)
                    finish()
                }
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>?) {
                // 可能的结果点，可用于绘制扫描动画
            }
        })
        
        // 开始扫描
        barcodeView.resume()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，开始扫描
                android.util.Log.d(TAG, "相机权限已授予")
                startScan()
            } else {
                // 权限被拒绝
                android.util.Log.e(TAG, "相机权限被拒绝")
                Toast.makeText(this, "需要相机权限才能扫描二维码", Toast.LENGTH_SHORT).show()
                setResult(RESULT_SCAN_CANCELED)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) {
            barcodeView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::barcodeView.isInitialized) {
            barcodeView.pause()
        }
    }
}


