# 🛡️ AstanixESP (Anti-Storage ESP & Silent Wall-Occlusion Suite)

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.x-brightgreen.svg?style=for-the-badge&logo=minecraft" alt="Minecraft Version" />
  <img src="https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk" alt="Java Version" />
  <img src="https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur-blue.svg?style=for-the-badge" alt="Platform" />
  <img src="https://img.shields.io/badge/Mode-Silent%20%26%20Zero--Spam-success.svg?style=for-the-badge" alt="Mode" />
</p>

**AstanixESP** — это объединённый высокопроизводительный Paper-плагин для комплексной бесшумной защиты сервера от **Storage ESP, Chest ESP, Container ESP и FreeCam** (Meteor, LiquidBounce, Aristois, Wurst, Baritone и др.).

Плагин работает в **полностью бесшумном режиме**: никаких алертов в чат, никаких звуковых сигналов или спама в консоль. Он просто и надежно **прячет сундуки** от читеров и блокирует любые нелегитимные взаимодействия!

---

## 🎯 Принцип работы: «Просто прячем сундуки»

1. **Дистанционная маскировка (Anti-Storage ESP, от 25 блоков):**
   * Все целевые контейнеры (сундуки, бочки, шалкеры, печи и т.д.) за пределами дистанции видимости подменяются сетевыми пакетами Paper на **блок, на котором они стоят**.
   * Сундук на дёрне (`GRASS_BLOCK`) подменяется на `GRASS_BLOCK`.
   * Сундук на снегу подменяется на `SNOW_BLOCK`.
   * Сундук на досках, камне или сланце — на соответствующий материал поверхности.
   * Двойные сундуки синхронизированы атомарно: обе половины открываются и закрываются одновременно в одном пакете, текстуры не ломаются.

2. **Скрытие контейнеров за стенами (Wall Occlusion, вблизи игрока):**
   * Даже если читер подлетает на расстояние 5–15 блоков через **FreeCam**, контейнеры, скрытые за глухими стенами, перекрытиями и полом, **визуально подменяются на окружающую породу/пол** (`STONE`, `DEEPSLATE`, `PLANKS` и т.д.).
   * Летающая камера FreeCam видит только сплошной массив стен и пола — тайные комнаты, базы и сундуки обнаружить невозможно!
   * Как только игрок легитимно входит в дверной проём или получает прямую линию взгляда (Line of Sight), сундуки мгновенно отображаются в своём исходном виде.

3. **Бесшумная блокировка взаимодействия сквозь стены (Silent Denial):**
   * Попытки открыть контейнер через стену, нажать рычаг, кликнуть по сундуку или сломать блок без прямой видимости **молча отменяются** на уровне событий ядра.
   * Чат администраторов остаётся чистым, сервер не нагружается вычислениями ложных алертов.

---

## 🎮 Команды и права

| Команда | Алиасы | Описание | Право доступа |
|---|---|---|---|
| `/esp status` | `/esp info` | Статистика кэша: отслеживаемые чанки, блоки, сущности | `astanix.esp.admin` |
| `/esp reload` | — | Перезагрузить конфигурацию `config.yml` | `astanix.esp.admin` |
| `/esp bypass` | — | Переключить режим обхода маскировки (видеть все контейнеры) | `astanix.esp.bypass` |
| `/afc info [игрок]` | `/freecam info` | Статус проверок и количество скрытых за стенами контейнеров | `astanix.freecam.admin` |
| `/afc bypass` | — | Переключить персональный обход проверок видимости | `astanix.freecam.bypass` |
| `/afc reload` | — | Перезагрузить конфигурацию плагина | `astanix.freecam.admin` |

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

# Список целевых контейнеров для маскировки
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
  - "DISPENSER"
  - "DROPPER"
  - "HOPPER"
  - "CRAFTER"
  - "CHISELED_BOOKSHELF"
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

target-entities:
  - "CHEST_MINECART"
  - "HOPPER_MINECART"
  - "CHEST_BOAT"

# Настройки защиты от FreeCam и скрытия за стенами
freecam:
  reach:
    enabled: true
    survival-max: 4.8
    creative-max: 5.5
  raytrace:
    enabled: true
    check-blocks: true
    check-entities: true
    check-breaking: true
    check-air-clicks: true
    check-inventory-open: true
    multi-ray-samples: 5
  occlusion:
    enabled: true
    radius: 18.0
```

---

## 🔨 Сборка

```bash
mvn clean package -DskipTests
```
Готовый артефакт:
`target/astanix-esp-1.0.0.jar`
