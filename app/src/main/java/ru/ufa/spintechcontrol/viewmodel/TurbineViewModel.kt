package ru.ufa.spintechcontrol.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.ufa.spintechcontrol.data.TurbineData
import ru.ufa.spintechcontrol.data.TurbineStatus
import ru.ufa.spintechcontrol.data.TurbineHistoryPoint
import ru.ufa.spintechcontrol.network.DeviceManager
import ru.ufa.spintechcontrol.network.ValveController

class TurbineViewModel : ViewModel() {
    
    private val deviceManager = DeviceManager()
    private val valveController = ValveController(deviceManager)
    
    // Состояние UI
    private val _turbineData = mutableStateOf(TurbineData())
    val turbineData: State<TurbineData> = _turbineData
    
    private val _turbineStatus = mutableStateOf(TurbineStatus())
    val turbineStatus: State<TurbineStatus> = _turbineStatus
    
    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected
    
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _valvePosition = mutableStateOf(0)
    val valvePosition: State<Int> = _valvePosition
    
    private val _isValveAdjusting = mutableStateOf(false)
    val isValveAdjusting: State<Boolean> = _isValveAdjusting
    
    private val _historyData = mutableStateOf<List<TurbineHistoryPoint>>(emptyList())
    val historyData: State<List<TurbineHistoryPoint>> = _historyData
    
    private val _isDemoMode = mutableStateOf(false)
    val isDemoMode: State<Boolean> = _isDemoMode
    
    init {
        setupDeviceManagerCallbacks()
        setupValveControllerCallbacks()
    }
    
    private fun setupDeviceManagerCallbacks() {
        deviceManager.onDataReceived = { data ->
            _turbineData.value = data
            _isConnected.value = true
            _errorMessage.value = null
            
            // Добавляем точку в историю
            addHistoryPoint(data)
            
            // Обновляем позицию клапана
            valveController.updateCurrentPosition(data.valvePosition)
            _valvePosition.value = data.valvePosition
        }
        
        deviceManager.onStatusChanged = { status ->
            _turbineStatus.value = status
            _isConnected.value = status.status == "online"
        }
        
        deviceManager.onError = { error ->
            _errorMessage.value = error
            _isConnected.value = false
        }
    }
    
    private fun setupValveControllerCallbacks() {
        valveController.onPositionChanged = { position ->
            _valvePosition.value = position
        }
        
        valveController.onAdjustmentComplete = { position ->
            _isValveAdjusting.value = false
        }
        
        valveController.onError = { error ->
            _errorMessage.value = error
            _isValveAdjusting.value = false
        }
    }
    
    /**
     * Подключается к устройству
     */
    fun connectToDevice(ip: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            if (ip != null) {
                deviceManager.setDeviceIp(ip)
            }
            
            val connected = deviceManager.checkConnection()
            if (connected) {
                deviceManager.startDataPolling(1000) // Опрос каждую секунду
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Отключается от устройства
     */
    fun disconnectFromDevice() {
        deviceManager.stopDataPolling()
        _isConnected.value = false
        _turbineStatus.value = TurbineStatus("offline")
    }
    
    /**
     * Устанавливает позицию клапана
     */
    fun setValvePosition(position: Int) {
        viewModelScope.launch {
            _isValveAdjusting.value = true
            _errorMessage.value = null
            
            if (_isDemoMode.value) {
                // В демо режиме просто обновляем значение
                _valvePosition.value = position
                _isValveAdjusting.value = false
            } else {
                valveController.setPosition(position, smooth = true)
            }
        }
    }
    
    /**
     * Экстренная остановка турбины
     */
    fun emergencyStop() {
        viewModelScope.launch {
            _errorMessage.value = null
            
            if (_isDemoMode.value) {
                _turbineData.value = _turbineData.value.copy(isRunning = false, rpm = 0)
                _valvePosition.value = 0
            } else {
                valveController.emergencyClose()
                deviceManager.sendSystemCommand("emergency_stop")
            }
        }
    }
    
    /**
     * Запускает турбину
     */
    fun startTurbine() {
        viewModelScope.launch {
            _errorMessage.value = null
            
            if (_isDemoMode.value) {
                _turbineData.value = _turbineData.value.copy(isRunning = true)
            } else {
                deviceManager.sendSystemCommand("start")
            }
        }
    }
    
    /**
     * Останавливает турбину
     */
    fun stopTurbine() {
        viewModelScope.launch {
            _errorMessage.value = null
            
            if (_isDemoMode.value) {
                _turbineData.value = _turbineData.value.copy(isRunning = false, rpm = 0)
            } else {
                deviceManager.sendSystemCommand("stop")
            }
        }
    }
    
    /**
     * Включает/выключает демо режим
     */
    fun toggleDemoMode() {
        _isDemoMode.value = !_isDemoMode.value
        
        if (_isDemoMode.value) {
            disconnectFromDevice()
            startDemoDataGeneration()
        } else {
            stopDemoDataGeneration()
        }
    }
    
    private fun startDemoDataGeneration() {
        viewModelScope.launch {
            _isConnected.value = true
            _turbineStatus.value = TurbineStatus("online")
            
            // Генерируем тестовые данные каждую секунду
            while (_isDemoMode.value) {
                val testData = deviceManager.getTestData().copy(
                    valvePosition = _valvePosition.value,
                    isRunning = _turbineData.value.isRunning
                )
                _turbineData.value = testData
                addHistoryPoint(testData)
                
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    private fun stopDemoDataGeneration() {
        // Демо режим отключается автоматически через флаг _isDemoMode
    }
    
    /**
     * Добавляет точку в историю данных
     */
    private fun addHistoryPoint(data: TurbineData) {
        val newPoint = TurbineHistoryPoint(
            timestamp = data.timestamp,
            rpm = data.rpm,
            temperature = data.tempIn,
            power = data.power
        )
        
        val currentHistory = _historyData.value.toMutableList()
        currentHistory.add(newPoint)
        
        // Ограничиваем историю последними 100 точками
        if (currentHistory.size > 100) {
            currentHistory.removeAt(0)
        }
        
        _historyData.value = currentHistory
    }
    
    /**
     * Очищает сообщение об ошибке
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Получает текущие данные вручную
     */
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            if (_isDemoMode.value) {
                val testData = deviceManager.getTestData()
                _turbineData.value = testData
                addHistoryPoint(testData)
            } else {
                deviceManager.getTurbineData()
            }
            
            _isLoading.value = false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        deviceManager.cleanup()
        valveController.cleanup()
    }
}