package ru.ufa.spintechcontrol.network

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import ru.ufa.spintechcontrol.data.TurbineData
import ru.ufa.spintechcontrol.data.TurbineStatus
import java.io.IOException
import java.util.concurrent.TimeUnit

class DeviceManager {
    
    private val dataParser = DataParser()
    private var deviceIp: String = "192.168.4.1" // Стандартный IP ESP32 в AP режиме
    private var isConnected = false
    private var pollingJob: Job? = null
    
    // HTTP клиент с логированием
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()
    
    // Коллбэки для получения данных
    var onDataReceived: ((TurbineData) -> Unit)? = null
    var onStatusChanged: ((TurbineStatus) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Устанавливает IP адрес ESP32
     */
    fun setDeviceIp(ip: String) {
        deviceIp = ip
    }
    
    /**
     * Проверяет подключение к устройству
     */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://$deviceIp/ping")
                .build()
                
            val response = httpClient.newCall(request).execute()
            isConnected = response.isSuccessful
            response.close()
            
            val status = if (isConnected) {
                TurbineStatus("online")
            } else {
                TurbineStatus("offline", "Нет ответа от устройства")
            }
            
            withContext(Dispatchers.Main) {
                onStatusChanged?.invoke(status)
            }
            
            isConnected
        } catch (e: IOException) {
            isConnected = false
            val status = TurbineStatus("offline", "Ошибка подключения: ${e.message}")
            
            withContext(Dispatchers.Main) {
                onStatusChanged?.invoke(status)
                onError?.invoke("Ошибка подключения: ${e.message}")
            }
            
            false
        }
    }
    
    /**
     * Получает текущие данные турбины
     */
    suspend fun getTurbineData(): Result<TurbineData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://$deviceIp/status")
                .build()
                
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val parseResult = dataParser.parseTurbineData(responseBody)
                if (parseResult.isSuccess) {
                    val turbineData = parseResult.getOrThrow()
                    withContext(Dispatchers.Main) {
                        onDataReceived?.invoke(turbineData)
                    }
                }
                parseResult
            } else {
                Result.failure(Exception("HTTP ошибка: ${response.code}"))
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                onError?.invoke("Ошибка получения данных: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    /**
     * Отправляет команду управления клапаном
     */
    suspend fun setValvePosition(position: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = dataParser.createValveCommand(position)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("http://$deviceIp/valve")
                .post(requestBody)
                .build()
                
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Result.success("Клапан установлен на $position%")
            } else {
                Result.failure(Exception("Ошибка управления клапаном: ${response.code}"))
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                onError?.invoke("Ошибка управления клапаном: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    /**
     * Отправляет системную команду (старт/стоп/перезапуск)
     */
    suspend fun sendSystemCommand(action: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = dataParser.createSystemCommand(action)
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("http://$deviceIp/system")
                .post(requestBody)
                .build()
                
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Result.success("Команда '$action' выполнена")
            } else {
                Result.failure(Exception("Ошибка выполнения команды: ${response.code}"))
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                onError?.invoke("Ошибка системной команды: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    /**
     * Запускает периодический опрос данных
     */
    fun startDataPolling(intervalMs: Long = 1000) {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                if (checkConnection()) {
                    getTurbineData()
                }
                delay(intervalMs)
            }
        }
    }
    
    /**
     * Останавливает периодический опрос
     */
    fun stopDataPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    /**
     * Возвращает статус подключения
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Получает тестовые данные для демонстрации
     */
    fun getTestData(): TurbineData {
        return dataParser.createTestData()
    }
    
    /**
     * Освобождает ресурсы
     */
    fun cleanup() {
        stopDataPolling()
        httpClient.dispatcher.executorService.shutdown()
    }
}