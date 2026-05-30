# SwarmSimulation Project Context

## Project Overview

SwarmSimulation is a Java Swing-based 2D vehicle swarm simulation that combines classic boids flocking behavior with Q-learning for adaptive navigation. The swarm learns to navigate obstacles, avoid black holes, and hunt targets through shared reinforcement learning.

**Key Technologies:**
- Java 11+
- Swing (GUI)
- Q-Learning (Shared State-Action Table)
- Boids Algorithm (Separation, Alignment, Cohesion)

**Canvas Size:** 1475×800 pixels (with 0.4 scaling factor)
**Vehicle Count:** 30 (configurable)

---

## Project Structure

```
src/
├── main/
│   ├── java/com/dgx/
│   │   ├── Simulation.java       (Main JFrame, simulation loop, UI controls)
│   │   ├── Vehicle.java          (Individual agent, movement, forces)
│   │   ├── Canvas.java           (Rendering, visualization overlays)
│   │   ├── QEngine.java          (Q-Learning state/action/reward logic)
│   │   ├── Obstacle.java         (Rectangular obstacles)
│   │   ├── BlackHole.java        (Circular hazards)
│   │   └── VectorCalculation.java (Math utilities)
│   └── resources/
│       ├── obstacles.txt         (Obstacle positions/sizes)
│       └── blackholes.txt        (Black hole positions/radii)
└── test/                          (Test suite placeholder)
```

---

## Core Components

### 1. **Simulation.java** (Main Controller)
**Role:** JFrame that orchestrates the entire simulation loop

**Key Methods:**
- `Simulation()` - Initializes canvas, UI controls, loads obstacles/black holes, spawns vehicles
- `checkTargetStatus()` - Manages target lifecycle (spawn → detect → consume → disperse)
- `spawnNextTarget()` - Creates target avoiding obstacles/black holes with safety buffers
- `placeVehicleInSpawnCircle()` - Respawns dead vehicles in the spawn zone

**Key Fields:**
- `allVehicles` - ArrayList of 30 Vehicle objects
- `allObstacles` - Loaded from file, clipped to world bounds
- `allBlackHoles` - Loaded from file, used for collision/spawn checks
- `currentTarget` - [targetX, targetY] or null
- `isConsuming` / `isDispersing` - Target phase states

**Simulation Loop:**
```
Timer.start() → checkTargetStatus() → For each vehicle:
  1. Capture old position (oldX, oldY)
  2. Query Q-engine for action: chooseAction(oldX, oldY)
  3. Execute vehicle movement: v.move(...)
  4. Check collisions (black holes, obstacles, targets)
  5. Compute reward (-100, -10, -1, +100)
  6. Update Q-table: updateQ(oldX, oldY, action, reward, newX, newY)
  7. Respawn if hit black hole
  8. Repaint canvas
```

---

### 2. **QEngine.java** (Reinforcement Learning Brain)
**Role:** Shared Q-learning table for the entire swarm

**Architecture:**
- **State:** Grid discretization of world position (40.0 units per cell)
- **Action:** 4 directions (Up, Down, Left, Right)
- **Reward:** -100 (black hole), -10 (obstacle), +100 (target), -1 (step)

**Key Methods:**
- `chooseAction(x, y)` - ε-greedy policy (25% random, 75% best Q-value)
- `updateQ(oldX, oldY, action, reward, newX, newY)` - Bellman equation update
- `toGridX(x)` / `toGridY(y)` - Convert world coords to grid cell

**Data Structures:**
- `Q[Q_WIDTH][Q_HEIGHT][ACTIONS]` - 3D Q-table (discretized position × 4 actions)
- `visitCounts[Q_WIDTH][Q_HEIGHT]` - Exploration bonus tracking (encourages coverage)

**Q-Learning Parameters:**
- `ALPHA = 0.1` - Learning rate
- `GAMMA = 0.9` - Discount factor
- `EPSILON = 0.25` - Exploration rate
- `VISIT_BONUS = 1.0` - Exploration bonus per unvisited cell

---

### 3. **Vehicle.java** (Individual Agent)
**Role:** Single swarm member with physics and behavior

**Key Methods:**
- `move(allVehicles, allObstacles, currentTarget, isConsuming, isDispersing)` - Computes movement via forces:
  1. Separation force (avoid crowding, radius = 5 units)
  2. Alignment force (match local heading)
  3. Cohesion force (move toward flock center, radius = 25 units)
  4. Obstacle avoidance (steered away from rectangles)
  5. Q-learning correction (guides toward learned high-value states)

**Physics:**
- Position: `pos[0]`, `pos[1]` (world coordinates)
- Velocity: `vel[0]`, `vel[1]` (direction × speed)
- Max acceleration: 0.2 units/frame
- Max velocity: 1.0 units/frame

**Tunable Weights (adjustable via UI sliders):**
- `BASE_AVOIDANCE_RADIUS` - Distance used for obstacle sensing
- `OBS_WEIGHT = 0.4` - Obstacle avoidance force strength
- `F_ZUS_WEIGHT = 0.25` - Cohesion weight
- `F_SEP_WEIGHT = 1.2` - Separation weight
- `F_AUS_WEIGHT = 0.4` - Alignment weight

---

### 4. **Canvas.java** (Renderer)
**Role:** Swing JPanel that draws vehicles, obstacles, targets, and overlays

**Key Overlays (toggle via UI):**
- **Q-Grid** - Shows discretized grid cells (40×40 units each)
- **Q Shade** - Heatmap tint based on best Q-value per cell
- **Q-Values** - Numeric labels of best Q-value in each cell
- **Obstacle/Black hole radius** - Influence zones
- **Target radius** - Detection zone around current target
- **Type1 circle** - Debug circles for type-1 vehicles

---

### 5. **Obstacle.java** & **BlackHole.java** (Environment Objects)

**Obstacle:**
- Rectangular hazards: `position[x,y]`, width, height, name
- Loaded from `obstacles.txt`
- Format: `x y width height name`

**BlackHole:**
- Circular hazards: `position[x,y]`, radius, name
- Loaded from `blackholes.txt`
- Format: `x y radius name`
- Heavy penalty: `-100` reward on collision

---

## Recent Changes (Simplified Q-Learning)

### Previous Bias Issue
The Q-learning previously used **target-relative state encoding**:
- When target existed: `QEngine.chooseAction(x, y, targetX, targetY)`
- When no target: `QEngine.chooseAction(x, y, 0, 0)`

This created two separate learned policies and biased the swarm toward assuming targets always exist.

### Current Fix
- **Removed overloaded methods** with target parameters from `QEngine`
- **Unified state space** to use absolute world coordinates only: `chooseAction(x, y)`
- **Simplified `updateQ`** to single method: `updateQ(oldX, oldY, action, reward, newX, newY)`
- **Consistent behavior** whether target exists or not
- **Better generalization** - one unified policy for all scenarios

---

## Control Panel Layout

### Row 1: Sliders + Button
| Control | Range | Purpose |
|---------|-------|---------|
| AvoidRadius | 0–200 | Obstacle sensing distance |
| ObsWeight (F_OBS) | 0.0–2.0 | Obstacle avoidance strength |
| F_zus (Cohesion) | 0.0–2.0 | Flock cohesion strength |
| F_sep (Separation) | 0.0–2.0 | Vehicle separation strength |
| F_aus (Alignment) | 0.0–2.0 | Heading alignment strength |
| Target DetectRadius | 1–50 | Target detection range |
| New Target | Button | Spawn target immediately |

### Row 2: Toggles + Timer
| Control | Purpose |
|---------|---------|
| Show obstacle radius | Toggle obstacle zones |
| Show black hole radius | Toggle black hole zones |
| Show Q-Grid | Toggle discretization grid |
| Q Shade | Toggle Q-value heatmap |
| Show Q-Values | Toggle numeric Q-labels |
| Show target radius | Toggle target zone |
| Show type1 circle | Toggle debug circles |
| Search time / Last capture | Timer display |

---

## Execution Flow (Per Frame @ 2ms intervals)

```
1. checkTargetStatus()
   ├─ Update targetSearchElapsedMillis
   ├─ Detect swarm near target → isConsuming = true
   ├─ Wait 3s → spawn next target, start dispersal
   └─ isDispersing for 2s before search resumes

2. For each Vehicle v:
   a. oldX, oldY = capture current position
   b. action = QEngine.chooseAction(oldX, oldY)
   c. v.move(allVehicles, allObstacles, currentTarget, isConsuming, isDispersing)
   d. Check collisions → compute reward
   e. QEngine.updateQ(oldX, oldY, action, reward, newX, newY)
   f. If hitBlackHole → respawn in spawn circle

3. myCanvas.repaint()
```

---

## File Formats

### obstacles.txt
```
<count>
x1 y1 width1 height1 name1
x2 y2 width2 height2 name2
...
```

### blackholes.txt
```
<count>
x1 y1 radius1 name1
x2 y2 radius2 name2
...
```

---

## Configuration Constants

| Constant | Value | Purpose |
|----------|-------|---------|
| WIDTH | 1475 | Canvas width (pixels) |
| HEIGHT | 800 | Canvas height (pixels) |
| pix | 0.4 | Scaling factor (world units/pixel) |
| sleep | 2 | Frame delay (ms) |
| SPAWN_POINT_RADIUS | 40 | Vehicle spawn circle radius |
| WORLD_MARGIN | 10 | Border margin for world bounds |
| targetDetectionRadius | 15 | Default target detection range |

---

## Build & Run

**Build:**
```bash
mvn clean compile
```

**Run:**
```bash
mvn exec:java -Dexec.mainClass="com.dgx.Simulation"
```

---

## Debugging Tips

1. **Enable Q-Grid overlay** - Visualize state discretization
2. **Enable Q Shade** - See learned high-value regions
3. **Check visit counts** - Ensure exploration bonus is working
4. **Monitor timer** - Check target capture and search times
5. **Adjust weights** - Use sliders to test behavior changes

---

## Future Enhancements

- [ ] Persistent Q-table saves/loads
- [ ] Multi-target scenarios
- [ ] Adaptive epsilon decay (less exploration over time)
- [ ] Heuristic reward shaping (distance-to-target bonus)
- [ ] Swarm diversity metrics
- [ ] Performance profiling (FPS, learning speed)

---

## Key Design Principles

1. **Shared Intelligence** - Single Q-table benefits entire swarm
2. **Simple Discretization** - 40×40 grid cells for state space
3. **Reward Shaping** - Exploration bonus + terminal rewards
4. **Tunable Parameters** - All force weights adjustable at runtime
5. **Modular Architecture** - Clear separation of concerns (Physics, Learning, Rendering)

---

*Last Updated: May 30, 2026*
*Project: SwarmSimulation v2.1*
