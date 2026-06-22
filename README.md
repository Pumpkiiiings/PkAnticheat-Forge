# PkAnticheat (Forge)

PkAnticheat is a high-performance, server-side-only AntiCheat specifically designed for Minecraft Forge 1.20.1. It utilizes advanced heuristics, predictive math, and dynamic latency buffers to detect and prevent hacks without relying on client-side mods.

## ✨ Features
- **Server-Side Only**: Does not require players to install any client-side mods. Just drop it into your server's `mods` folder.
- **Dynamic Ping Compensation (Lag Buffers)**: Uses real-time latency (ping) and jitter tracking to expand bounding boxes and math limits automatically. Prevents false positives for lagging players.
- **Silent Mitigation Philosophy**: Prioritizes neutralizing the hack (e.g., cancelling the hit, forcing fall damage) over kicking players out of the blue, while quietly accumulating Violation Levels (VL) for admins.
- **Smart Chain-Reaction Prevention**: Movement and Combat modules communicate to prevent false positive chain-reactions (e.g., teleporting a player won't trigger speed flags).

## 🛡️ Active Modules (Checks)

### ⚔️ Combat Checks
- **Reach / Hitbox**: Calculates true 3D distances between attacker and target. Dynamically expands the allowed reach threshold based on the attacker's live ping to prevent false flags on desynchronized hits.
- **KillAura / AutoClicker**: Detects abnormal rotation speeds, impossible attack angles, and inhuman clicking consistency.

### 🏃 Movement Checks
- **Fly**: Pure mathematical gravity check. Detects hovering, slow falling, or flying without false-flagging legitimate jumps or explosions.
- **Bhop / Strafe**: Tracks mid-air velocity. Ignores the first 3 ticks of a legitimate sprint-jump, but flags players who maintain illegal high speeds (>0.34 blocks/tick) indefinitely in the air.
- **WaterWalk (Jesus)**: Cancels all horizontal movement and forces the player to sink if they attempt to walk on liquid surfaces.
- **NoFall**: Uses raw physical Y-axis tracking to calculate true fall distance instead of trusting the client's `OnGround` packet. Forces fall damage mathematically.
- **NoSlow**: Prevents players from sprinting at full speed while eating or blocking. Smartly ignores mid-air eating to allow legitimate Vanilla jump-eating mechanics.
- **SafeWalk**: Prevents players from walking over the edge of blocks without sneaking.

### 🌐 World & Packet Checks
- **FastBreak / Nuker**: Calculates the theoretical break speed of a block based on the player's tool and enchantments. If a block breaks faster than mathematically possible (accounting for a 30% lag margin), the event is cancelled.
- **Timer / Blink**: Simulates an internal economy of packets (Inspired by Intave). If packets arrive too fast, the player is set back. If packets arrive too slow (>80ms delay for 10 ticks), the player is dropped to prevent floating/Blink exploits.
- **Damage Indicator Spoof**: Injects fake, randomized health data (5.0, 10.0, 15.0) into outgoing packets for surrounding entities. This breaks hacker HUDs and Damage Indicator mods without affecting the client's own health bar.

## 💻 Commands & Permissions
All commands require Operator Permission Level 2.
- `/pk clear <player>`: Clears all accumulated Violation Levels (VL) for the specified player.
- `/pk history <player>`: Displays a detailed log of which modules the player has failed and how many times.
- `/pk ping <player>`: Checks the real-time latency (in milliseconds) of the player.

## 🛠️ Installation
1. Download the `PkAnticheat-1.0.jar` file.
2. Place it inside your Forge server's `mods` directory.
3. Start the server. (Works perfectly on Forge 1.20.1 / 47.3.0).

---

## ❓ Frequently Asked Questions (Q&A)

**Q: Do my players need to install this mod to join the server?**
**A:** No. PkAnticheat is strictly Server-Side. Your players can join using Vanilla Minecraft or any regular client.

**Q: Why isn't the AntiCheat banning hackers immediately?**
**A:** PkAnticheat uses a "Silent Mitigation" approach. If someone tries to use Jesus, they simply sink. If they try to Reach, the hit doesn't register. It accumulates "Violation Levels" (VL) quietly so admins can review them with `/pk history`.

**Q: Will this kick players who have a bad internet connection?**
**A:** No. We specifically developed a "Lag Buffer" system. The AntiCheat reads the player's live Ping and Jitter, and mathematically expands its tolerance limits to accommodate lag spikes without flagging them.

**Q: Why does the health bar of other players look glitched on my screen?**
**A:** That means the "Damage Indicator Spoof" is working! It sends randomized health data to clients to completely break illegal Damage Indicator mods.
