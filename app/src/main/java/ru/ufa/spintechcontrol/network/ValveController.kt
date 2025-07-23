package ru.ufa.spintechcontrol.network

import kotlinx.coroutines.*

class ValveController(private val deviceManager: DeviceManager) {
    
    private var currentPosition = 0
    private var targetPosition = 0
    private var isAdjusting = false
    private var adjustmentJob: Job? = null
    
    // Коллбэки
    var onPositionChanged: ((Int) -> Unit)? = null
    var onAdjustmentComplete: ((Int) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Устанавливает позицию клапана с плавным переходом
     */
    suspend fun setPosition(position: Int, smooth: Boolean = true) {
        val clampedPosition = position.coerceIn(0, 100)
        targetPosition = clampedPosition
        
        if (smooth && kotlin.math.abs(clampedPosition - currentPosition) > 5) {
            startSmoothAdjustment()
        } else {
            setPositionDirect(clampedPosition)
        }
    }
    
    /**
     * Устанавливает позицию клапана напрямую
     */
    private suspend fun setPositionDirect(position: Int) {
        try {
            isAdjusting = true
            val result = deviceManager.setValvePosition(position)
            
            if (result.isSuccess) {
                currentPosition = position
                onPositionChanged?.invoke(position)
                onAdjustmentComplete?.invoke(position)
            } else {
                onError?.invoke("Ошибка установки клапана: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            onError?.invoke("Ошибка управления клапаном: ${e.message}")
        } finally {
            isAdjusting = false
        }
    }
    
    /**
     * Запускает плавную регулировку позиции клапана
     */
    private fun startSmoothAdjustment() {
        adjustmentJob?.cancel()
        adjustmentJob = CoroutineScope(Dispatchers.IO).launch {
            isAdjusting = true
            
            try {
                while (currentPosition != targetPosition && isActive) {
                    val step = if (targetPosition > currentPosition) {
                        minOf(5, targetPosition - currentPosition)
                    } else {
                        maxOf(-5, targetPosition - currentPosition)
                    }
                    
                    val newPosition = currentPosition + step
                    val result = deviceManager.setValvePosition(newPosition)
                    
                    if (result.isSuccess) {
                        currentPosition = newPosition
                        withContext(Dispatchers.Main) {
                            onPositionChanged?.invoke(currentPosition)
                        }
                        
                        // Пауза между шагами для плавности
                        delay(200)
                    } else {
                        withContext(Dispatchers.Main) {
                            onError?.invoke("Ошибка плавной регулировки: ${result.exceptionOrNull()?.message}")
                        }
                        break
                    }
                }
                
                withContext(Dispatchers.Main) {
                    onAdjustmentComplete?.invoke(currentPosition)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError?.invoke("Ошибка плавной регулировки: ${e.message}")
                }
            } finally {
                isAdjusting = false
            }
        }
    }
    
    /**
     * Экстренное закрытие клапана
     */
    suspend fun emergencyClose() {
        adjustmentJob?.cancel()
        targetPosition = 0
        setPositionDirect(0)
    }
    
    /**
     * Экстренное открытие клапана
     */
    suspend fun emergencyOpen() {
        adjustmentJob?.cancel()
        targetPosition = 100
        setPositionDirect(100)
    }
    
    /**
     * Увеличивает позицию клапана на заданное значение
     */
    suspend fun increasePosition(step: Int = 5) {
        val newPosition = (currentPosition + step).coerceIn(0, 100)
        setPosition(newPosition, smooth = false)
    }
    
    /**
     * Уменьшает позицию клапана на заданное значение
     */
    suspend fun decreasePosition(step: Int = 5) {
        val newPosition = (currentPosition - step).coerceIn(0, 100)
        setPosition(newPosition, smooth = false)
    }
    
    /**
     * Возвращает текущую позицию клапана
     */
    fun getCurrentPosition(): Int = currentPosition
    
    /**
     * Возвращает целевую позицию клапана
     */
    fun getTargetPosition(): Int = targetPosition
    
    /**
     * Проверяет, выполняется ли регулировка
     */
    fun isAdjusting(): Boolean = isAdjusting
    
    /**
     * Устанавливает позицию без отправки команды (для синхронизации с реальным состоянием)
     */
    fun updateCurrentPosition(position: Int) {
        currentPosition = position.coerceIn(0, 100)
    }
    
    /**
     * Останавливает все операции с клапаном
     */
    fun stopAdjustment() {
        adjustmentJob?.cancel()
        isAdjusting = false
    }
    
    /**
     * Валидирует безопасность установки позиции
     */
    private fun validatePosition(position: Int, currentRpm: Int, currentTemp: Float): String? {
        return when {
            position > 90 && currentRpm > 10000 -> 
                "Опасно: высокие обороты при большом открытии клапана"
            position > 80 && currentTemp > 280 -> 
                "Опасно: высокая температура при большом открытии клапана"
            position < 10 && currentRpm < 500 -> 
                "Предупреждение: слишком малое открытие может остановить турбину"
            else -> null
        }
    }
    
    /**
     * Устанавливает позицию с проверкой безопасности
     */
    suspend fun setPositionWithSafetyCheck(
        position: Int, 
        currentRpm: Int, 
        currentTemp: Float,
        forceUnsafe: Boolean = false
    ) {
        val warning = validatePosition(position, currentRpm, currentTemp)
        
        if (warning != null && !forceUnsafe) {
            onError?.invoke("Предупреждение безопасности: $warning")
            return
        }
        
        setPosition(position)
    }
    
    /**
     * Освобождает ресурсы
     */
    fun cleanup() {
        stopAdjustment()
    }
}