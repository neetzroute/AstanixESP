# 📦 Astanix-ESP (Anti-Storage ESP Protection)

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.x-brightgreen.svg?style=for-the-badge&logo=minecraft" alt="Minecraft Version" />
  <img src="https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk" alt="Java Version" />
  <img src="https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur-blue.svg?style=for-the-badge" alt="Platform" />
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge" alt="License" />
</p>

**Astanix-ESP** — это высокопроизводительный серверный Paper-плагин нового поколения для полной защиты сервера от любых видов читов категории **Storage ESP / Chest ESP / Container ESP / FreeCam** (Meteor, LiquidBounce, Aristois, Wurst, Baritone и др.).

Плагин перехватывает отображение контейнеров на сетевом уровне протокола Paper. Если игрок находится на расстоянии более **25 блоков** (дистанция настраивается), контейнер для его клиента **физически подменяется на естественный блок окружения** (землю, камень, доски, снег), а сущности-хранилища скрываются. Чит-клиенты удаляют TileEntity из памяти и не могут подсветить сундуки сквозь стены.

---

## ✨ Ключевые возможности

* 📦 **Полный охват всех типов хранилищ:**
  * **Сундуки:** Обычные (`CHEST`), сундуки-ловушки (`TRAPPED_CHEST`), эндер-сундуки (`ENDER_CHEST`), бочки (`BARREL`).
  * **Печи:** Обычные (`FURNACE`), плавильни (`BLAST_FURNACE`), коптильни (`SMOKER`).
  * **Магия и варка:** Зельеварочные стойки (`BREWING_STAND`), столы зачарования (`ENCHANTING_TABLE`).
  * **Шалкеровые ящики:** Все 16 цветов + неокрашенный (`SHULKER_BOX`).
  * **Редстоун-хранилища:** Воронки (`HOPPER`), выбрасыватели (`DROPPER`), раздатчики (`DISPENSER`), авто-верстаки (`CRAFTER`), резные книжные полки (`CHISELED_BOOKSHELF`).
  * **Сущности-хранилища:** Вагонетки с сундуком/воронкой (`CHEST_MINECART`, `HOPPER_MINECART`), лодки с сундуком (`CHEST_BOAT`).

* 🎨 **Интеллектуальная контекстная маскировка (Contextual Disguise):**
  * Блок заменяется на блок **поверхности или опоры, на которой он стоит**.
  * Сундук на траве/тропинке (`GRASS_BLOCK`, `DIRT_PATH`, `PODZOL`) подменяется на **землю (`DIRT`)**.
  * Сундук на снегу или снежном блоке (`SNOW`, `SNOW_BLOCK`) подменяется на **`SNOW_BLOCK`**.
  * Сундук на досках, камне, булыжнике или глубоком сланце заменяется на соответствующий тип блока.
  * Парящие или подвешенные контейнеры: плагин трассирует вертикаль вниз до первого твёрдого блока поверхности.
  * Контейнеры над пустотой: берутся соседние стены, потолок или контекстный fallback (`STONE` / `DEEPSLATE` на `Y < 0` / `NETHERRACK` / `END_STONE`).

* ⚡ **Нулевая нагрузка на TPS (Zero TPS Loss):**
  * **Кэширование при загрузке чанка:** Используется сверхбыстрый вызов Paper API `chunk.getTileEntities(false)`, который не создаёт тяжёлых копий NBT инвентарей. Сервер не сканирует мир в тиках.
  * **Spatial Bounding Window (5x5 чанков):** Проверка дистанции 25 блоков ограничена радиусом всего в 2 чанка вокруг игрока. Блоки за пределами этого радиуса гарантированно скрыты без проверок.
  * **Tick Spreading:** Игроки равномерно распределяются по тикам через `(player.getEntityId() + tick) % interval`, исключая одновременную нагрузку на процессор.
  * **Movement Threshold:** Если игрок сместился менее чем на 0.5 блока (стоит на месте, сортирует вещи, копает), расчёт дистанции мгновенно пропускается.
  * **State Transition Only:** Пакеты отправляются **только** в момент пересечения радиуса 25 блоков (скрытие ⇄ открытие). Никакого спама пакетами.

* 🌐 **Мгновенный перехват чанков (`PlayerChunkLoadEvent`):**
  * В момент отправки чанка игроку контейнеры дальше 25 блоков маскируются немедленно. Чит не успевает отобразить даже один кадр подсветки.

* 🛠️ **Административный Bypass:**
  * Администраторы с правом `astanix.esp.bypass` могут командой `/esp bypass` мгновенно отключить маскировку для себя при проведении проверок.

---

## 📂 Структура пакетов (`dev.astanix.esp`)

- `command` — обработчик административных команд `/esp` (`/astanixesp`) и автодополнение аргументов.
- `config` — загрузка и валидация настроек из `config.yml`.
- `disguise` — интеллектуальный движок `DisguiseResolver` для контекстного подбора маскировочных блоков.
- `listener` — высокопроизводительные слушатели чанков, блоков, сущностей и перемещения игроков.
- `manager` — пространственный кэш `StorageManager`, индексирующий чанки по `World` и `ChunkPos`.
- `model` — легковесные структуры данных: `BlockPos`, `ChunkPos`, `StorageBlock`, `StorageChunk`, `PlayerState`.
- `tracker` — сервис `PlayerTrackerService` для управления дистанцией, батчингом пакетов и тик-спредингом.
- `util` — форматирование компонентов Kyori Adventure MiniMessage (`TextUtil`).

---

## 🎮 Команды и права

| Команда | Алиасы | Описание | Право доступа |
|---|---|---|---|
| `/esp status` | `/esp info` | Статистика кэша: отслеживаемые чанки, блоки, сущности, память | `astanix.esp.admin` |
| `/esp reload` | — | Перезагрузить конфигурацию и переиндексировать чанки | `astanix.esp.admin` |
| `/esp bypass` | — | Включить/выключить режим обхода маскировки (видеть все блоки) | `astanix.esp.bypass` |
| `/esp debug` | — | Переключить режим отладочного логирования в консоли | `astanix.esp.admin` |

> По умолчанию право `astanix.esp.admin` выдано операторам сервера (`OP`).

---

## ⚙️ Конфигурация (`config.yml`)

```yaml
# Дистанция (в блоках), начиная с которой хранилища маскируются.
hide-distance: 25.0

# Интервал проверки дистанции (в тиках сервера). 2 тика = 100 мс.
check-interval-ticks: 2

# Максимальная глубина поиска вниз для парящих контейнеров.
max-downward-search: 32

# Блоки по умолчанию при отсутствии опоры (над пустотой)
fallback-disguise:
  overworld: "STONE"
  overworld-deepslate: "DEEPSLATE" # Высота Y < 0
  nether: "NETHERRACK"
  the-end: "END_STONE"

# Список типов блоков, подлежащих маскировке (Material)
target-blocks:
  - "CHEST"
  - "TRAPPED_CHEST"
  - "ENDER_CHEST"
  - "BARREL"
  - "FURNACE"
  - "BLAST_FURNACE"
  - "SMOKER"
  - "BREWING_STAND"
  - "ENCHANTING_TABLE"
  - "SHULKER_BOX"
  - "WHITE_SHULKER_BOX"
  - "ORANGE_SHULKER_BOX"
  - "MAGENTA_SHULKER_BOX"
  - "LIGHT_BLUE_SHULKER_BOX"
  - "YELLOW_SHULKER_BOX"
  - "LIME_SHULKER_BOX"
  - "PINK_SHULKER_BOX"
  - "GRAY_SHULKER_BOX"
  - "LIGHT_GRAY_SHULKER_BOX"
  - "CYAN_SHULKER_BOX"
  - "PURPLE_SHULKER_BOX"
  - "BLUE_SHULKER_BOX"
  - "BROWN_SHULKER_BOX"
  - "GREEN_SHULKER_BOX"
  - "RED_SHULKER_BOX"
  - "BLACK_SHULKER_BOX"
  - "HOPPER"
  - "DISPENSER"
  - "DROPPER"
  - "CRAFTER"
  - "CHISELED_BOOKSHELF"

# Список сущностей-хранилищ, скрываемых на дистанции
target-entities:
  - "CHEST_MINECART"
  - "HOPPER_MINECART"
  - "CHEST_BOAT"

debug: false
```

---

## 🔨 Сборка проекта

### Сборка через Maven:
```bash
mvn clean package -DskipTests
```
Готовый jar-файл появится в `target/astanix-esp-1.0.0.jar`.

### Сборка через Gradle:
```bash
gradle shadowJar
```
Готовый jar-файл появится в `build/libs/astanix-esp-1.0.0.jar`.

---

## 🚀 Установка на сервер
1. Скомпилируйте плагин или скачайте готовый jar-файл из вкладки **Releases**.
2. Поместите `astanix-esp-1.0.0.jar` в папку `plugins/` вашего Paper/Purpur 1.21.x сервера.
3. Перезапустите сервер или загрузите плагин.
4. Настройте `plugins/Astanix-ESP/config.yml` при необходимости.
