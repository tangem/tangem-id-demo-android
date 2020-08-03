package com.tangem.id.common.extensions

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


fun String.toQrCode(): Bitmap {
    val hintMap = Hashtable<EncodeHintType, Any>()
    hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M // H = 30% damage
    hintMap[EncodeHintType.MARGIN] = 2

    val qrCodeWriter = QRCodeWriter()

    val size = 256

    val bitMatrix = qrCodeWriter.encode(this, BarcodeFormat.QR_CODE, size, size, hintMap)
    val width = bitMatrix.width
    val bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until width) {
            bmp.setPixel(y, x, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    return bmp
}

fun String.toDate(): LocalDate? =
    try {
        LocalDate.parse(this, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    } catch (exception: Exception) {
        null
    }

fun LocalDate.toMillis(): Long {
    return this.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}


fun LocalDate.isOver21Years(): Boolean {
    val over21Date = this.plusYears(21)
    return over21Date < LocalDate.now()
}

fun LocalDate.isOver18Years(): Boolean {
    val over18Date = this.plusYears(18)
    return over18Date < LocalDate.now()
}

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 20, stream)
    return stream.toByteArray()
}