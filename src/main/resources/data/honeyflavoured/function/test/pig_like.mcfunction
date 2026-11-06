# Spawns the pig_like test mobs around the command block. NoAI = they cannot move. Tag hf_test lets you find/kill them.
summon minecraft:pig ~-3 ~ ~0 {NoAI:1b,PersistenceRequired:1b,Tags:["hf_test"]}
summon minecraft:hoglin ~0 ~ ~-3 {NoAI:1b,PersistenceRequired:1b,Tags:["hf_test"]}
summon minecraft:zoglin ~0 ~ ~3 {NoAI:1b,PersistenceRequired:1b,Tags:["hf_test"]}
summon minecraft:piglin ~3 ~ ~0 {NoAI:1b,PersistenceRequired:1b,Tags:["hf_test"]}
summon minecraft:piglin_brute ~-3 ~ ~-3 {NoAI:1b,PersistenceRequired:1b,Tags:["hf_test"]}
summon minecraft:zombified_piglin ~-3 ~ ~3 {NoAI:1b,PersistenceRequired:1b,Tags:["hf_test"]}
