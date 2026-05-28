# Future Work

## Reinforcement Learning with Q-Table

The next step for the simulation is to add reinforcement learning so each vehicle can learn better decisions over time instead of only relying on fixed steering rules.

### Core idea

- Treat each vehicle as an agent.
- Use a Q-table to learn which action is best for a given state.
- Reward the vehicle for moving toward the target.
- Penalize the vehicle for bad outcomes, especially blackhole collisions.

### State design

Because the simulation is continuous, the environment should be converted into discrete states before using a Q-table. Useful state inputs could include:

- distance to the target
- angle to the target
- distance to the nearest blackhole
- current speed or motion direction

These values can be grouped into bins such as near / medium / far or left / ahead / right.

### Action design

Keep the action space small and discrete. Example actions:

- turn left
- go straight
- turn right
- accelerate
- brake

### Reward design

A simple reward structure could be:

- `+100` for reaching the target
- `+1` for moving closer to the target
- `-1` for moving away from the target
- `-10` for getting too close to a blackhole
- `-100` for touching a blackhole and dying
- `-0.1` per time step to encourage faster solutions

If a vehicle dies, it should respawn at the spawn point and receive a strong negative reward so the model learns to avoid blackholes.

### Learning update

The Q-value update rule would be:

$$
Q(s,a) \leftarrow Q(s,a) + \alpha \left[r + \gamma \max_{a'} Q(s',a') - Q(s,a)\right]
$$

Where:

- `s` is the current state
- `a` is the chosen action
- `r` is the reward
- `s'` is the next state
- `\alpha` is the learning rate
- `\gamma` is the discount factor

### Suggested implementation path

1. Detect when a vehicle touches a blackhole.
2. Mark the vehicle as dead and apply a negative reward.
3. Respawn the vehicle at the spawn point.
4. Update the Q-table from the transition.
5. Repeat over many episodes so the swarm gradually learns safer behavior.

### Implementation checklist

- Add a `dead` state to each vehicle.
- Detect blackhole collisions every simulation step.
- On collision, apply the penalty and reset the vehicle position to the spawn point.
- Discretize state values into bins before indexing the Q-table.
- Implement `chooseAction`, `applyAction`, and `updateQValue` helpers.
- Store and reuse the Q-table across vehicles if using a shared learning policy.
- Start with a small action set and expand only if needed.

### Roadmap

#### Phase 1: Basic learning loop

- Add the Q-table data structure.
- Add state discretization.
- Add reward handling for target success, movement, and blackhole death.

#### Phase 2: Respawn and training

- Respawn dead vehicles at the spawn point.
- Track episode resets and running reward totals.
- Tune learning rate, discount factor, and exploration rate.

#### Phase 3: Behavior improvement

- Compare learned behavior against the current rule-based steering.
- Adjust bins and rewards if the swarm gets stuck or ignores the target.
- Consider moving to a more advanced model later if the Q-table becomes too large.

### Notes

- A shared Q-table for all vehicles is the simplest starting point.
- The state space should stay small enough that the table remains manageable.
- More advanced methods like DQN can be explored later if the Q-table becomes too large.
