package dev.johnoreilly.confetti.auto.utils

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.HostException
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import coil.imageLoader
import coil.request.ImageRequest
import dev.johnoreilly.confetti.R
import dev.johnoreilly.confetti.utils.format
import kotlinx.datetime.LocalDateTime
import java.time.format.DateTimeFormatter

const val METERS_TO_KMS = 1000

suspend fun fetchImage(carContext: CarContext, link: String): Bitmap? {
    val coil = carContext.imageLoader

    val request = ImageRequest.Builder(carContext)
        .data(link)
        .fallback(R.drawable.ic_filled_person)
        .allowHardware(false)
        .build()

    val response = coil.execute(request)
    return (response.drawable as? BitmapDrawable)?.bitmap
}

fun colorize(str: String, color: CarColor, index: Int, length: Int): CharSequence {
    return SpannableString(str).apply {
        setSpan(
            ForegroundCarColorSpan.create(color),
            index,
            index + length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

fun formatDateTime(time: LocalDateTime): String {
    return DateTimeFormatter.ofPattern("MMM d, HH:mm").format(time)
}

fun navigateTo(carContext: CarContext, latitude: Double, longitude: Double) {
    val uri = Uri.parse("geo:0,0?q=$latitude,$longitude")
    val intent = Intent(CarContext.ACTION_NAVIGATE, uri)

    try {
        carContext.startCarApp(intent)
    } catch (e: HostException) {
        CarToast.makeText(
            carContext,
            carContext.getString(R.string.auto_navigate_to_failed),
            CarToast.LENGTH_SHORT
        ).show()
    }
}