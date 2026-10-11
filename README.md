# AgroValle Cosecha - Sprint 1

## 📱 Descripción

**AgroValle Cosecha** es una aplicación móvil Android para el control de cosecha de arándanos en campo. Permite registrar asistencia de jornaleros (por QR o DNI), gestionar el contexto agronómico (cuartel, CLP, variedad), calibrar la tara de jabas vacías y registrar pesajes con cálculo automático de peso neto.

## 🛠️ Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Navegación | Navigation Compose |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Persistencia | Room (entidades y DAOs preparados, mock en Sprint 1) |
| Lectura QR | CameraX + ML Kit (placeholder simulado) |
| Feedback | ToneGenerator + Vibrator |
| SDK Mínimo | API 24 (Android 7.0) |

## 📂 Estructura del Proyecto

```
com.agrovalle.cosecha/
├── AgroValleApp.kt                    # Application class
├── MainActivity.kt                     # Single Activity (Compose host)
├── navigation/
│   ├── Screen.kt                      # Rutas tipadas
│   └── NavGraph.kt                    # Grafo de navegación
├── data/
│   ├── model/
│   │   └── Models.kt                  # Modelos de dominio y enums
│   ├── local/
│   │   ├── AgroValleDatabase.kt       # Room Database
│   │   ├── entities/                  # Entidades Room (6 archivos)
│   │   └── dao/                       # DAOs Room (5 archivos)
│   └── repository/
│       ├── AgroValleRepository.kt     # Interfaz del repositorio
│       └── MockAgroValleRepository.kt # Implementación mock
├── ui/
│   ├── theme/
│   │   ├── Color.kt                   # Paleta de alto contraste
│   │   ├── Type.kt                    # Tipografía grande
│   │   └── Theme.kt                   # Tema Material 3
│   ├── components/                    # Componentes reutilizables
│   │   ├── BigButton.kt               # Botones (Primary, Dark, Outlined, Small)
│   │   ├── DropdownSelector.kt        # Selector desplegable
│   │   ├── NumericKeypad.kt           # Teclado numérico táctil
│   │   ├── QRScannerPlaceholder.kt    # Visor QR simulado
│   │   ├── ProgressSteps.kt           # Indicador de 5 pasos
│   │   ├── OfflineFooter.kt           # Footer "Sin conexión"
│   │   └── HeaderBar.kt              # Barra superior con volver/cerrar
│   ├── screens/                       # 8 pantallas del Sprint 1
│   │   ├── LoginScreen.kt
│   │   ├── MainMenuScreen.kt
│   │   ├── AttendanceQRScreen.kt
│   │   ├── SearchByDNIScreen.kt
│   │   ├── AgronomicContextScreen.kt
│   │   ├── TareCalibrationScreen.kt
│   │   ├── WeighingScreen.kt
│   │   └── WeighingConfirmationScreen.kt
│   └── viewmodels/                    # ViewModels con StateFlow
│       ├── AuthViewModel.kt
│       ├── AttendanceViewModel.kt
│       ├── HarvestContextViewModel.kt
│       ├── TareCalibrationViewModel.kt
│       └── WeighingViewModel.kt
└── util/
    └── SonidoVibracionHelper.kt       # Feedback sonoro y háptico
```

## 🚀 Cómo Ejecutar

### Prerrequisitos
- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- SDK de Android API 34 instalado

### Pasos

1. **Clonar el repositorio:**
   ```bash
   git clone <url-del-repo>
   cd Agrovalle-cosecha-android
   git checkout feature/sprint-1-implementacion
   ```

2. **Abrir en Android Studio:**
   - File → Open → Seleccionar la carpeta del proyecto
   - Esperar a que Gradle sincronice (puede tardar la primera vez)

3. **Ejecutar la app:**
   - Conectar un dispositivo Android (API 24+) o iniciar un emulador
   - Click en Run ▶️ o `Shift + F10`

## 🧪 Flujos de Prueba

### Flujo Capataz
1. En Login, seleccionar **Capataz** → Jornada → Fundo → Sector → "Iniciar Turno"
2. En Menú Principal, pulsar **"Registrar Asistencia"**
3. Pulsar **"Escanear QR"** varias veces → el contador se incrementa
4. Pulsar **"Buscar por DNI"** → ingresar `1234` → "Buscar" → "Confirmar Asistencia"
5. Verificar que el contador se actualiza al volver

### Flujo Asistente de Pesaje
1. En Login, seleccionar **Asistente de Pesaje** → completar campos → "Iniciar Turno"
2. Pulsar **"Contexto Agronómico"** → seleccionar Cuartel, CLP, Variedad → "Abrir Parihuela"
3. Volver y pulsar **"Calibrar Tara"** → registrar 5 pesos → "Confirmar Calibración" (o "Omitir")
4. Volver y pulsar **"Registrar Pesaje"** → "Simular Escaneo" → ingresar peso (ej: 9.50) → agregar jabas (+4) → verificar cálculo → "Guardar Pesaje"
5. En Confirmación: verificar check verde (o ⚠️ si tara por defecto) → "Siguiente Jornalero →"
6. Verificar que los campos se limpiaron para el siguiente registro

## 📋 Datos Mock

| Jornalero | DNI | Código QR |
|---|---|---|
| Juan Pérez | 1234 | QR-001 |
| María López | 5678 | QR-002 |
| Pedro García | 9012 | QR-003 |

- **Tara por defecto:** 0.375 kg
- **Acumulado diario inicial:** 45.50 kg
- **Total cuadrilla:** 35 trabajadores

## 🔧 Notas para Coder 1 (Room)

Para reemplazar el MockRepository por Room:

1. Inicializar `AgroValleDatabase` en `AgroValleApp.kt`
2. Crear `RoomAgroValleRepository` implementando `AgroValleRepository`
3. Cambiar la instanciación en `MainActivity.kt` (o usar Hilt/Koin)
4. Las entidades y DAOs ya están definidos y listos para usar

## 📄 Licencia

Proyecto académico - UPAO 2026
