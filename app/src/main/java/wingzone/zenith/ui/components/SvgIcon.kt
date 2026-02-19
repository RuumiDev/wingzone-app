package wingzone.zenith.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest

/**
 * Composable for displaying SVG icons from assets folder using Coil
 * 
 * @param assetPath Path to SVG file in assets folder (e.g., "icons/wings.svg")
 * @param contentDescription Accessibility description for the icon
 * @param modifier Modifier for the Image composable
 * @param tint Optional color tint to apply to the icon
 * @param size Size of the icon (default 24.dp)
 */
@Composable
fun SvgIcon(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 24.dp
) {
    val context = LocalContext.current
    
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/$assetPath")
            .decoderFactory(SvgDecoder.Factory())
            .build()
    )
    
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.tint(it) },
        contentScale = ContentScale.Fit
    )
}
