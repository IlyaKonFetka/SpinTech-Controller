package ru.ufa.spintechcontrol.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import ru.ufa.spintechcontrol.data.TurbineData
import ru.ufa.spintechcontrol.data.TurbineStatus

class DataParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * Парсит JSON ответ от ESP32 в объект TurbineData
     */
    fun parseTurbineData(jsonString: String): Result<TurbineData> {
        return try {
            val turbineData = json.decodeFromString<TurbineData>(jsonString)
            Result.success(turbineData)
        } catch (e: SerializationException) {
            Result.failure(Exception("Ошибка парсинга данных турбины: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Неизвестная ошибка при парсинге: ${e.message}"))
        }
    }
    
    /**
     * Парсит статус системы
     */
    fun parseSystemStatus(jsonString: String): Result<TurbineStatus> {
        return try {
            val status = json.decodeFromString<TurbineStatus>(jsonString)
            Result.success(status)
        } catch (e: SerializationException) {
            Result.failure(Exception("Ошибка парсинга статуса: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Неизвестная ошибка при парсинге статуса: ${e.message}"))
        }
    }
    
    /**
     * Создает JSON для команды управления клапаном
     */
    fun createValveCommand(position: Int): String {
        val clampedPosition = position.coerceIn(0, 100)
        return """{"position":$clampedPosition}"""
    }
    
    /**
     * Создает JSON для системной команды
     */
    fun createSystemCommand(action: String, parameters: Map<String, String> = emptyMap()): String {
        val parametersJson = if (parameters.isEmpty()) {
            ""
        } else {
            ""","parameters":${json.encodeToString(kotlinx.serialization.serializer(), parameters)}"""
        }
        return """{"action":"$action"$parametersJson}"""
    }
    
    /**
     * Валидирует корректность данных турбины
     */
    fun validateTurbineData(data: TurbineData): List<String> {
        val errors = mutableListOf<String>()
        
        if (data.rpm < 0 || data.rpm > 20000) {
            errors.add("Некорректные обороты: ${data.rpm}")
        }
        
        if (data.tempIn < -50 || data.tempIn > 500) {
            errors.add("Некорректная температура входа: ${data.tempIn}°C")
        }
        
        if (data.tempOut < -50 || data.tempOut > 500) {
            errors.add("Некорректная температура выхода: ${data.tempOut}°C")
        }
        
        if (data.valvePosition < 0 || data.valvePosition > 100) {
            errors.add("Некорректная позиция клапана: ${data.valvePosition}%")
        }
        
        if (data.power < 0) {
            errors.add("Некорректная мощность: ${data.power}W")
        }
        
        return errors
    }
    
    /**
     * Создает тестовые данные для демонстрации
     */
    fun createTestData(): TurbineData {
        return TurbineData(
            rpm = kotlin.random.Random.nextInt(1000, 5000),
            tempIn = kotlin.random.Random.nextFloat() * (250f - 180f) + 180f,
            tempOut = kotlin.random.Random.nextFloat() * (180f - 120f) + 120f,
            steamFlow = kotlin.random.Random.nextFloat() * (50f - 10f) + 10f,
            power = kotlin.random.Random.nextFloat() * (800f - 200f) + 200f,
            voltage = kotlin.random.Random.nextFloat() * (240f - 220f) + 220f,
            valvePosition = kotlin.random.Random.nextInt(30, 80),
            isRunning = true,
            efficiency = kotlin.random.Random.nextFloat() * (85f - 60f) + 60f
        )
    }
}