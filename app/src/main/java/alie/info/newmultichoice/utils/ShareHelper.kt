package alie.info.newmultichoice.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareHelper {
    
    fun createResultImage(
        context: Context,
        correct: Int,
        total: Int,
        percentage: Float
    ): Bitmap {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background gradient
        val paint = Paint().apply {
            isAntiAlias = true
        }
        
        // Dark blue background
        canvas.drawColor(Color.parseColor("#1a1a2e"))
        
        // Logo area
        paint.color = Color.WHITE
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("KLCP Quiz", width / 2f, 200f, paint)
        
        // Score circle
        paint.color = Color.parseColor("#00ff88")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 20f
        canvas.drawCircle(width / 2f, height / 2f, 250f, paint)
        
        // Percentage text
        paint.style = Paint.Style.FILL
        paint.textSize = 150f
        paint.color = Color.WHITE
        canvas.drawText("${percentage.toInt()}%", width / 2f, height / 2f + 50f, paint)
        
        // Stats
        paint.textSize = 50f
        paint.color = Color.parseColor("#CCFFFFFF")
        canvas.drawText("$correct / $total correct", width / 2f, height / 2f + 200f, paint)
        
        // Bottom text
        paint.textSize = 40f
        paint.color = Color.parseColor("#00ff88")
        canvas.drawText("I passed the KLCP exam! 🎉", width / 2f, height - 150f, paint)
        
        return bitmap
    }
    
    fun shareResult(context: Context, correct: Int, total: Int, percentage: Float) {
        val bitmap = createResultImage(context, correct, total, percentage)
        
        // Save to cache
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "klcp_result.png")
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "I scored ${percentage.toInt()}% on the KLCP Quiz! 🎯")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share your result"))
    }
}

