# FunTimeDuels

Minecraft Paper 1.21.4 duel plugin (Java 21).

## Build
GitHub Actions builds the plugin on every push to `main`. Download the `FunTimeDuels-1.0.0` artifact from the Actions run.

## Installation
1. Put the built JAR into `plugins/`.
2. Install PlaceholderAPI if you want placeholders in DeluxeMenus / DecentHolograms.
3. Restart the server.
4. Give admin access only to trusted operators. Kit and arena management commands explicitly require OP.

## Admin setup
Stand at the desired location before each command:

```
/dueladmin setspawn
/dueladmin arena create arena1
/dueladmin arena setspawn1 arena1
/dueladmin arena setspawn2 arena1
```

Repeat the arena commands for every arena. An arena is available once both spawn points are configured. Optional markers:
```
/dueladmin arena pos1 arena1
/dueladmin arena pos2 arena1
```

Create kits by putting the exact items in your inventory, then:
```
/dueladmin kit create Sword
```
The kit is saved with Bukkit item serialization, retaining item metadata and custom NBT/PDC data. Recreate a kit after changing its contents; overwriting existing kits is not currently supported.

## Player queue
```
/duel Sword
```
Run the same command again to leave that kit's queue. When two players are queued, the plugin selects a free configured arena.

## DeluxeMenus example
Use a command action on the kit icon:
```yaml
left_click_commands:
  - '[player] duel Sword'
``
Queue count placeholder:
```
%funtimeduels_queue_sword%
```

## PlaceholderAPI
Player stats:
- `%funtimeduels_wins%`
- `%funtimeduels_losses%`
- `%funtimeduels_kills%`
- `%funtimeduels_deaths%`
- `%funtimeduels_streak%`

Top entries (rank starts at 1):
- `%funtimeduels_top_wins_1_name%`
- `%funtimeduels_top_wins_1_value%`
- Replace `wins` with `kills`, `losses`, `deaths` or `streak`.

## Notes
- Player inventories, armor, offhand, game mode, XP, food, health and potion effects are restored after a match.
- Match data and statistics currently use SQLite.
- Initial release is a functional foundation; test on a staging server before production. Arena region protection, full GUI, richer top hologram formatting and extra disconnect edge cases are follow-up work.
