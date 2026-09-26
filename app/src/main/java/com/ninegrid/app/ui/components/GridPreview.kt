package com.ninegrid.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ninegrid.app.core.image.GridGeometry
import com.ninegrid.app.core.model.AppLanguage
import com.ninegrid.app.core.model.CropMode
import com.ninegrid.app.core.model.EditorSettings
import com.ninegrid.app.core.model.SourceImage
import com.ninegrid.app.core.model.TilePreview
import kotlin.math.roundToInt

@Composable
fun ResultGrid(
    previews: List<TilePreview>,
    rows: Int,
    columns: Int,
    language: AppLanguage,
    onTileSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(columns) { column ->
                    val index = row * columns + column
                    val preview = previews.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (preview != null) Modifier.clickable { onTileSelected(index) }
                                else Modifier,
                            ),
                    ) {
                        if (preview != null && !preview.bitmap.isRecycled) {
                            Image(
                                bitmap = preview.bitmap.asImageBitmap(),
                                contentDescription = language.text(
                                    "第 ${index + 1} 张切图",
                                    "Image ${index + 1}",
                                ),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                            )
                            Surface(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .align(Alignment.TopStart),
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.62f),
                            ) {
                                Text(
                                    text = "%02d".format(index + 1),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompositionPreview(
    source: SourceImage,
    settings: EditorSettings,
    language: AppLanguage,
    onFocusChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = source.bitmap.asImageBitmap()
    val aspectRatio = settings.gridSpec.columns.toFloat() / settings.gridSpec.rows
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(4.dp))
            .background(settings.backgroundColor)
            .semantics {
                contentDescription = language.text(
                    "拖动调整照片主体位置",
                    "Drag to position the subject",
                )
            }
            .pointerInput(source.bitmap, settings.cropMode, settings.focusX, settings.focusY) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val direction = if (settings.cropMode == CropMode.COVER) -1f else 1f
                    onFocusChanged(
                        settings.focusX + direction * dragAmount.x / size.width.coerceAtLeast(1),
                        settings.focusY + direction * dragAmount.y / size.height.coerceAtLeast(1),
                    )
                }
            },
    ) {
        drawRect(settings.backgroundColor)
        val drawRect = GridGeometry.sourceDrawRect(
            sourceWidth = source.bitmap.width,
            sourceHeight = source.bitmap.height,
            compositionWidth = size.width.roundToInt(),
            compositionHeight = size.height.roundToInt(),
            cropMode = settings.cropMode,
            focusX = settings.focusX,
            focusY = settings.focusY,
        )
        clipRect {
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(drawRect.left.roundToInt(), drawRect.top.roundToInt()),
                dstSize = IntSize(drawRect.width.roundToInt(), drawRect.height.roundToInt()),
            )
        }

        val gridColor = Color.White.copy(alpha = 0.88f)
        repeat(settings.gridSpec.columns - 1) { index ->
            val x = size.width * (index + 1) / settings.gridSpec.columns
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
        }
        repeat(settings.gridSpec.rows - 1) { index ->
            val y = size.height * (index + 1) / settings.gridSpec.rows
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx())
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 5.dp.toPx(),
            center = Offset(size.width / 2, size.height / 2),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = 3.dp.toPx(),
            center = Offset(size.width / 2, size.height / 2),
        )
    }
}
