package ru.ufa.spintechcontrol.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.*
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.Color
import androidx.core.net.toUri

private const val TAG = "SceneModelViewer"

/**
 * 3D просмотрщик на базе SceneView. Грузит Duck.glb из assets
 */
@Composable
fun SceneModelViewer(
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
    rpm: Int = 0
) {
    var isModelLoaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var sceneView by remember { mutableStateOf<SceneView?>(null) }
    var modelNode by remember { mutableStateOf<Node?>(null) }
    
    // Состояния для управления моделью жестами
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(0.5f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Функция для обновления трансформации модели
    fun updateModelTransform() {
        modelNode?.let { node ->
            // Применяем масштаб (ограничиваем разумными пределами)
            val clampedScale = scale.coerceIn(0.1f, 3.0f)
            node.localScale = Vector3(clampedScale, clampedScale, clampedScale)
            
            // ПРАВИЛЬНЫЙ порядок вращений для естественного управления:
            // 1. Сначала поворот по Y (вертикальная ось) - всегда фиксирована
            // 2. Потом поворот по X (горизонтальная ось) - относительно уже повернутой модели
            val rotY = com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(0f, 1f, 0f), rotationY)
            val rotX = com.google.ar.sceneform.math.Quaternion.axisAngle(Vector3(1f, 0f, 0f), rotationX)
            
            // Важно: сначала Y, потом X - это даёт правильное поведение камеры
            node.localRotation = com.google.ar.sceneform.math.Quaternion.multiply(rotY, rotX)
            
            // Позиция модели остается в центре сцены для правильного вращения
            node.localPosition = Vector3(offsetX * 0.005f, offsetY * 0.005f, 0f)
            
            Log.d(TAG, "Трансформация: scale=$clampedScale, rotX=$rotationX, rotY=$rotationY")
        }
    }

    Column(modifier = modifier) {
        // 3D View
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures(
                                panZoomLock = false
                            ) { centroid, pan, zoom, rotation ->
                                // Обрабатываем жесты только если модель загружена
                                if (isModelLoaded) {
                                    // Масштабирование щипками
                                    if (zoom != 1.0f) {
                                        scale *= zoom
                                        Log.d(TAG, "Щипковое масштабирование: scale=$scale, zoom=$zoom")
                                        updateModelTransform()
                                    }
                                    // Вращение от панорамирования (когда не масштабируем)
                                    else if (pan != Offset.Zero) {
                                        // Горизонтальное движение - вращение вокруг вертикальной оси (Y)
                                        rotationY += pan.x * 0.3f
                                        
                                        // Вертикальное движение - вращение вокруг ТЕКУЩЕЙ горизонтальной оси
                                        // Применяем поворот по X к уже существующему повороту
                                        val deltaRotX = pan.y * 0.3f
                                        rotationX += deltaRotX
                                        
                                        Log.d(TAG, "Вращение: rotY=$rotationY, rotX=$rotationX, pan=$pan")
                                        updateModelTransform()
                                    }
                                }
                            }
                        },
                    factory = { context ->
                        Log.d(TAG, "=== Создаём SceneView ===")
                        
                        SceneView(context).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Устанавливаем белый фон для лучшей видимости
                            setBackgroundColor(android.graphics.Color.WHITE)
                            
                            sceneView = this
                            
                            // КРИТИЧНО: Подключаем жизненный цикл для рендеринга!
                            try {
                                // В некоторых версиях Sceneform может не быть этого метода
                                val method = this::class.java.getMethod("setLifecycleOwner", androidx.lifecycle.LifecycleOwner::class.java)
                                method.invoke(this, lifecycleOwner)
                                Log.d(TAG, "LifecycleOwner подключён к SceneView!")
                            } catch (e: Exception) {
                                Log.w(TAG, "setLifecycleOwner недоступен: ${e.message}")
                                // Пробуем альтернативный способ
                                try {
                                    resume()
                                    Log.d(TAG, "SceneView resume() вызван вручную")
                                } catch (e2: Exception) {
                                    Log.w(TAG, "resume() тоже недоступен: ${e2.message}")
                                }
                            }
                            
                            Log.d(TAG, "SceneView создан, размеры: ${width}x${height}")
                            
                            // Настройка камеры - упрощённая версия
                            scene.camera.apply {
                                localPosition = Vector3(0f, 0f, 2f)  // Камера дальше
                                setLookDirection(Vector3(0f, 0f, -1f))  // Смотрим на модель
                                nearClipPlane = 0.1f
                                farClipPlane = 30f
                            }
                            
                            Log.d(TAG, "Камера настроена: позиция=${scene.camera.localPosition}")
                            
                            // Простое освещение - только основной свет
                            Log.d(TAG, "Настройка освещения...")
                            
                            // Дополнительный направленный свет
                            val mainLight = Node().apply {
                                setParent(scene)
                                localPosition = Vector3(0f, 4f, 4f)
                                light = Light.builder(Light.Type.DIRECTIONAL)
                                    .setColor(Color(1f, 1f, 1f))
                                    .setIntensity(50000f)
                                    .build()
                            }
                            
                            Log.d(TAG, "Освещение настроено")
                            
                            // Загружаем модель
                            Log.d(TAG, "=== Начинаем загрузку Duck.glb ===")
                            
                            try {
                                // Пробуем разные способы загрузки
                                val assetUri = "Duck.glb".toUri()

                                
                                Log.d(TAG, "Пытаемся загрузить модель по URI: $assetUri")
                                
                                ModelRenderable.builder()
                                    .setSource(context, assetUri)
                                    .setIsFilamentGltf(true)
                                    .build()
                                    .thenAccept { renderable ->
                                        Log.d(TAG, "=== МОДЕЛЬ ЗАГРУЖЕНА УСПЕШНО! ===")
                                        
                                        // Создаем узел модели - размещаем в центре сцены
                                        val node = Node().apply {
                                            this.renderable = renderable
                                            
                                            // КРИТИЧНО: модель точно в центре сцены для правильного вращения
                                            localPosition = Vector3(0f, 0f, 0f)  // Центр сцены
                                            localScale = Vector3(scale, scale, scale)  // Используем текущий масштаб
                                            localRotation = com.google.ar.sceneform.math.Quaternion.identity()  // Без поворота
                                            
                                            Log.d(TAG, "Узел создан в центре: позиция=$localPosition, масштаб=$localScale")
                                        }
                                        
                                        // КРИТИЧНО: Правильно добавляем узел в сцену
                                        scene.addChild(node)
                                        modelNode = node
                                        
                                        // Проверяем что модель действительно в сцене
                                        Log.d(TAG, "Дочерних узлов в сцене: ${scene.children.size}")
                                        Log.d(TAG, "Модель в узле: ${node.renderable != null}")
                                        Log.d(TAG, "Родитель узла: ${node.parent}")
                                        Log.d(TAG, "Узел в сцене: ${scene.children.contains(node)}")
                                        
                                        isModelLoaded = true
                                        errorMessage = null
                                        
                                        // Применяем начальную трансформацию
                                        updateModelTransform()
                                        
                                        // Принудительно обновляем рендеринг
                                        try {
                                            scene.camera.setLookDirection(Vector3(0f, 0f, -1f))
                                            Log.d(TAG, "Камера перенастроена для обновления рендеринга")
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Не удалось обновить камеру: ${e.message}")
                                        }
                                        
                                    }
                                    .exceptionally { throwable ->
                                        Log.e(TAG, "=== ОШИБКА ЗАГРУЗКИ МОДЕЛИ ===", throwable)
                                        errorMessage = "Ошибка: ${throwable.message}"
                                        null
                                    }
                            } catch (e: Exception) {
                                Log.e(TAG, "=== ИСКЛЮЧЕНИЕ ПРИ ЗАГРУЗКЕ ===", e)
                                errorMessage = "Исключение: ${e.message}"
                            }
                        }
                    },
                    update = { view ->
                        // Обновления если нужны
                    }
                )



                // Показываем ошибку если есть
                errorMessage?.let { error ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Показываем ошибку внизу если есть
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}