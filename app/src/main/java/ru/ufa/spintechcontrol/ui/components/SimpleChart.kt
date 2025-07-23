package ru.ufa.spintechcontrol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ufa.spintechcontrol.data.TurbineHistoryPoint
import kotlin.math.max
import kotlin.math.min

@Composable
fun SimpleChart(
    data: List<TurbineHistoryPoint>,
    title: String,
    valueExtractor: (TurbineHistoryPoint) -> Float,
    unit: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет данных для отображения",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                val values = data.map(valueExtractor)
                val minValue = values.minOrNull() ?: 0f
                val maxValue = values.maxOrNull() ?: 100f
                val currentValue = values.lastOrNull() ?: 0f
                
                // Отображение текущего значения
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Текущее: ${currentValue.toInt()} $unit",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                    Text(
                        text = "Мин: ${minValue.toInt()} | Макс: ${maxValue.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // График
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    drawChart(
                        values = values,
                        minValue = minValue,
                        maxValue = maxValue,
                        color = color
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawChart(
    values: List<Float>,
    minValue: Float,
    maxValue: Float,
    color: Color
) {
    if (values.size < 2) return
    
    val width = size.width
    val height = size.height
    val padding = 20f
    
    val chartWidth = width - 2 * padding
    val chartHeight = height - 2 * padding
    
    // Нормализуем значения
    val range = maxValue - minValue
    val normalizedValues = if (range > 0) {
        values.map { (it - minValue) / range }
    } else {
        values.map { 0.5f }
    }
    
    // Создаем путь для линии графика
    val path = Path()
    val stepX = chartWidth / (values.size - 1)
    
    // Начальная точка
    val startX = padding
    val startY = padding + chartHeight * (1 - normalizedValues[0])
    path.moveTo(startX, startY)
    
    // Добавляем остальные точки
    for (i in 1 until normalizedValues.size) {
        val x = padding + i * stepX
        val y = padding + chartHeight * (1 - normalizedValues[i])
        path.lineTo(x, y)
    }
    
    // Рисуем линию графика
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Рисуем точки
    for (i in normalizedValues.indices) {
        val x = padding + i * stepX
        val y = padding + chartHeight * (1 - normalizedValues[i])
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = Offset(x, y)
        )
    }
    
    // Рисуем сетку
    val gridColor = color.copy(alpha = 0.2f)
    
    // Горизонтальные линии сетки
    for (i in 0..4) {
        val y = padding + (chartHeight / 4) * i
        drawLine(
            color = gridColor,
            start = Offset(padding, y),
            end = Offset(padding + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // Вертикальные линии сетки
    val verticalLines = min(values.size, 5)
    for (i in 0 until verticalLines) {
        val x = padding + (chartWidth / (verticalLines - 1)) * i
        drawLine(
            color = gridColor,
            start = Offset(x, padding),
            end = Offset(x, padding + chartHeight),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun TurbineChartsSection(
    historyData: List<TurbineHistoryPoint>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Графики данных",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // График оборотов
        SimpleChart(
            data = historyData,
            title = "Обороты турбины",
            valueExtractor = { it.rpm.toFloat() },
            unit = "об/мин",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // График температуры
        SimpleChart(
            data = historyData,
            title = "Температура пара",
            valueExtractor = { it.temperature },
            unit = "°C",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // График мощности
        SimpleChart(
            data = historyData,
            title = "Мощность",
            valueExtractor = { it.power },
            unit = "Вт",
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}