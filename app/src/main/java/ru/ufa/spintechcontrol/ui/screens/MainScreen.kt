package ru.ufa.spintechcontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.ufa.spintechcontrol.viewmodel.TurbineViewModel
import ru.ufa.spintechcontrol.ui.components.TurbineChartsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    turbineViewModel: TurbineViewModel = viewModel()
) {
    val turbineData by turbineViewModel.turbineData
    val isConnected by turbineViewModel.isConnected
    val errorMessage by turbineViewModel.errorMessage
    val isLoading by turbineViewModel.isLoading
    val valvePosition by turbineViewModel.valvePosition
    val isValveAdjusting by turbineViewModel.isValveAdjusting
    val isDemoMode by turbineViewModel.isDemoMode
    val historyData by turbineViewModel.historyData
    
    var showConnectionDialog by remember { mutableStateOf(false) }
    var ipAddress by remember { mutableStateOf("192.168.4.1") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Заголовок и статус подключения
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SpinTech Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDemoMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(
                            text = "ДЕМО",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                StatusIndicator(
                    isConnected = isConnected,
                    isLoading = isLoading
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Сообщение об ошибке
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Ошибка",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { turbineViewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Основные параметры турбины
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ParameterCard(
                title = "Обороты",
                value = "${turbineData.rpm}",
                unit = "об/мин",
                icon = Icons.Default.Refresh,
                color = if (turbineData.rpm > 10000) MaterialTheme.colorScheme.error 
                       else MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            ParameterCard(
                title = "Мощность",
                value = "${turbineData.power.toInt()}",
                unit = "Вт",
                icon = Icons.Default.ElectricBolt,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Температурные параметры
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ParameterCard(
                title = "T вход",
                value = "${turbineData.tempIn.toInt()}",
                unit = "°C",
                icon = Icons.Default.Thermostat,
                color = if (turbineData.tempIn > 250) MaterialTheme.colorScheme.error 
                       else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            
            ParameterCard(
                title = "T выход",
                value = "${turbineData.tempOut.toInt()}",
                unit = "°C",
                icon = Icons.Default.Thermostat,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Управление клапаном
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Управление подачей пара",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (isValveAdjusting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Позиция клапана: $valvePosition%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = valvePosition.toFloat(),
                    onValueChange = { newValue ->
                        turbineViewModel.setValvePosition(newValue.toInt())
                    },
                    valueRange = 0f..100f,
                    steps = 19, // 5% шаги
                    enabled = isConnected && !isValveAdjusting
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = { turbineViewModel.setValvePosition(0) },
                        enabled = isConnected && !isValveAdjusting
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Закрыть")
                    }
                    
                    FilledTonalButton(
                        onClick = { turbineViewModel.setValvePosition(50) },
                        enabled = isConnected && !isValveAdjusting
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("50%")
                    }
                    
                    FilledTonalButton(
                        onClick = { turbineViewModel.setValvePosition(100) },
                        enabled = isConnected && !isValveAdjusting
                    ) {
                        Icon(Icons.Default.OpenInFull, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Открыть")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Управление системой
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Управление турбиной",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (turbineData.isRunning) {
                        Button(
                            onClick = { turbineViewModel.stopTurbine() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("СТОП")
                        }
                    } else {
                        Button(
                            onClick = { turbineViewModel.startTurbine() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("СТАРТ")
                        }
                    }
                    
                    Button(
                        onClick = { turbineViewModel.emergencyStop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("АВАРИЙНЫЙ СТОП")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Кнопки подключения и режимов
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showConnectionDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Подключение")
            }
            
            OutlinedButton(
                onClick = { turbineViewModel.toggleDemoMode() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isDemoMode) Icons.Default.WifiOff else Icons.Default.DeveloperMode,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isDemoMode) "Выйти из демо" else "Демо режим")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Графики данных
        if (historyData.isNotEmpty()) {
            TurbineChartsSection(
                historyData = historyData,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    // Диалог подключения
    if (showConnectionDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            title = { Text("Подключение к ESP32") },
            text = {
                Column {
                    Text("Введите IP адрес ESP32:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("IP адрес") },
                        placeholder = { Text("192.168.4.1") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        turbineViewModel.connectToDevice(ipAddress)
                        showConnectionDialog = false
                    }
                ) {
                    Text("Подключить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectionDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ParameterCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun StatusIndicator(
    isConnected: Boolean,
    isLoading: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isConnected) Color.Green else Color.Red,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = when {
                isLoading -> "Подключение..."
                isConnected -> "Подключено"
                else -> "Отключено"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}