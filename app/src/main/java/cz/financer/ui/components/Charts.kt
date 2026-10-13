package cz.financer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Paleta pro grafy. Barvy se střídají, takže jsou rozlišitelné i v legendě. */
val ChartPalette: List<Color> = listOf(
    Color(0xFF1E3A5F),
    Color(0xFF0F766E),
    Color(0xFFB7791F),
    Color(0xFFB3261E),
    Color(0xFF6D4C9F),
    Color(0xFF3F7D20),
    Color(0xFF8A94A6),
    Color(0xFFC2185B),
)

data class ChartSlice(
    val label: String,
    val value: Double,
    val color: Color,
)

/** Donut graf. Každý výseč odpovídá podílu na celku v procentech. */
@Composable
fun DonutChart(slices: List<ChartSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }
    Canvas(modifier.size(180.dp)) {
        if (total <= 0.0) return@Canvas
        val stroke = size.minDimension * 0.22f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        var start = -90f
        slices.forEach { slice ->
            val sweep = (slice.value / total * 360.0).toFloat()
            drawArc(
                color = slice.color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            start += sweep
        }
    }
}

/** Legenda ke grafu: barva, jméno a procenta. */
@Composable
fun ChartLegend(slices: List<ChartSlice>, percentOf: (ChartSlice) -> String) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(slice.color))
                Text(
                    slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Text(percentOf(slice), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Sloupcový graf: jeden sloupec na položku, popisek pod ním. */
@Composable
fun BarChart(bars: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val max = (bars.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Row(
        modifier.fillMaxWidth().height(180.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        bars.forEachIndexed { index, (label, value) ->
            Column(
                Modifier.weight(1f).height(180.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(value.toString(), style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((120f * value / max).dp.coerceAtLeast(4.dp))
                        .background(ChartPalette[index % ChartPalette.size]),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Čárový graf: hodnoty v pořadí (např. měsíce) spojené lomenou čarou. */
@Composable
fun LineChart(points: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val max = (points.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            if (points.size < 2) return@Canvas
            val stepX = size.width / (points.size - 1)
            val path = Path()
            points.forEachIndexed { i, (_, value) ->
                val x = stepX * i
                val y = size.height - (size.height * (value / max)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 3.dp.toPx()))
            points.forEachIndexed { i, (_, value) ->
                val x = stepX * i
                val y = size.height - (size.height * (value / max)).toFloat()
                drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(x, y))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            points.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
