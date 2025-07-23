package ru.ufa.spintechcontrol.data

import kotlinx.serialization.Serializable

@Serializable
data class TurbineData(
    val rpm: Int = 0,                    // Обороты вала
    val tempIn: Float = 0f,              // Температура пара на входе (°C)
    val tempOut: Float = 0f,             // Температура пара на выходе (°C)
    val steamFlow: Float = 0f,           // Расход пара (кг/ч)
    val power: Float = 0f,               // Мощность (Вт)
    val voltage: Float = 0f,             // Напряжение (В)
    val valvePosition: Int = 0,          // Положение клапана (0-100%)
    val isRunning: Boolean = false,      // Работает ли турбина
    val efficiency: Float = 0f,          // КПД (%)
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TurbineStatus(
    val status: String = "offline",      // "online", "offline", "error"
    val errorMessage: String? = null,
    val lastUpdate: Long = System.currentTimeMillis()
)

@Serializable
data class ValveCommand(
    val position: Int                    // Позиция клапана 0-100%
)

@Serializable
data class SystemCommand(
    val action: String,                  // "start", "stop", "restart", "emergency_stop"
    val parameters: Map<String, String> = emptyMap()
)

// Модель для отображения исторических данных
data class TurbineHistoryPoint(
    val timestamp: Long,
    val rpm: Int,
    val temperature: Float,
    val power: Float
)

// Модель проекта для экрана "О проекте"
data class ProjectInfo(
    val title: String = "SpinTech Control",
    val description: String = "Мобильное приложение для мониторинга и управления компактной паровой турбиной",
    val authors: List<String> = listOf("Команда SpinTech"),
    val goals: List<String> = listOf(
        "Демонстрация высокотехнологичного энергомодуля",
        "Умное управление турбинной установкой",
        "Применение в образовании и малом энергоснабжении"
    ),
    val specifications: Map<String, String> = mapOf(
        "Максимальные обороты" to "7 000 об/мин",
        "Мощность" to "до 2000 Вт",
        "Рабочая температура пара" to "150-200°C",
        "Тип управления" to "Wi-Fi дистанционное"
    )
)