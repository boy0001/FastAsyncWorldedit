package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.intellectualcrafters.plot.util.StringMan;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.BlockChangeDelegate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Difficulty;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * Modify the world from an async thread <br>
 *  - Any Chunk/Block/BlockState objects returned should also be thread safe <br>
 *  - Use world.commit() to execute the changes <br>
 *  - Don't rely on autoQueue as behavior is determined by the settings.yml <br>
 */
public class AsyncWorld implements World {

    public final World parent;
    public final FaweQueue queue;
    private BukkitImplAdapter adapter;

    public AsyncWorld(World parent, boolean autoQueue) {
        this(parent, FaweAPI.createQueue(parent.getName(), autoQueue));
    }

    public AsyncWorld(World parent, FaweQueue queue) {
        this.parent = parent;
        this.queue = queue;
        if (queue instanceof BukkitQueue_0) {
            this.adapter = (BukkitImplAdapter) ((BukkitQueue_0) queue).adapter;
        } else {
            try {
                WorldEditPlugin instance = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
                Field fieldAdapter = WorldEditPlugin.class.getDeclaredField("bukkitAdapter");
                fieldAdapter.setAccessible(true);
                this.adapter = (BukkitImplAdapter) fieldAdapter.get(instance);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static AsyncWorld wrap(World world) {
        if (world instanceof AsyncWorld) {
            return (AsyncWorld) world;
        }
        return new AsyncWorld(world, false);
    }

    public void commit() {
        queue.enqueue();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return TaskManager.IMP.sync(new RunnableVal<WorldBorder>() {
            @Override
            public void run(WorldBorder value) {
                this.value = parent.getWorldBorder();
            }
        });
    }

    @Override
    public Block getBlockAt(int x, int y, int z) {
        return new AsyncBlock(this, queue, x, y, z);
    }

    @Override
    public Block getBlockAt(Location loc) {
        return getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    @Deprecated
    public int getBlockTypeIdAt(int x, int y, int z) {
        return queue.getCombinedId4Data(x, y, z) >> 4;
    }

    @Override
    @Deprecated
    public int getBlockTypeIdAt(Location loc) {
        return getBlockTypeIdAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        for (int y = 255; y >= 0; y--) {
            if (queue.getCombinedId4Data(x, y, z) != 0) {
                return y;
            }
        }
        return 0;
    }

    @Override
    public int getHighestBlockYAt(Location loc) {
        return getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
    }

    @Override
    public Block getHighestBlockAt(int x, int z) {
        int y = getHighestBlockYAt(x, z);
        return getBlockAt(x, y, z);
    }

    @Override
    public Block getHighestBlockAt(Location loc) {
        return getHighestBlockAt(loc.getBlockX(), loc.getBlockZ());
    }

    @Override
    public Chunk getChunkAt(int x, int z) {
        return new AsyncChunk(this, queue, x, z);
    }

    @Override
    public Chunk getChunkAt(Location location) {
        return getChunkAt(location.getBlockX(), location.getBlockZ());
    }

    @Override
    public Chunk getChunkAt(Block block) {
        return getChunkAt(block.getX(), block.getZ());
    }

    @Override
    public boolean isChunkLoaded(Chunk chunk) {
        return chunk.isLoaded();
    }

    @Override
    public Chunk[] getLoadedChunks() {
        return parent.getLoadedChunks();
    }

    @Override
    public void loadChunk(final Chunk chunk) {
        if (!chunk.isLoaded()) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    parent.loadChunk(chunk);
                }
            });
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof World)) {
            return false;
        }
        World other = (World) obj;
        return StringMan.isEqual(other.getName(), getName());
    }

    @Override
    public int hashCode() {
        return this.getUID().hashCode();
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        return parent.isChunkLoaded(x, z);
    }

    @Override
    public boolean isChunkInUse(int x, int z) {
        return parent.isChunkInUse(x, z);
    }

    @Override
    public void loadChunk(int x, int z) {
        if (!isChunkLoaded(x, z)) {
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    parent.loadChunk(x, z);
                }
            });
        }
    }

    @Override
    public boolean loadChunk(int x, int z, boolean generate) {
        if (!isChunkLoaded(x, z)) {
            return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
                @Override
                public void run(Boolean value) {
                    this.value = parent.loadChunk(x, z, generate);
                }
            });
        }
        return true;
    }

    @Override
    public boolean unloadChunk(Chunk chunk) {
        if (chunk.isLoaded()) {
            return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
                @Override
                public void run(Boolean value) {
                    this.value = parent.unloadChunk(chunk);
                }
            });
        }
        return true;
    }

    @Override
    public boolean unloadChunk(int x, int z) {
        return unloadChunk(x, z, true);
    }

    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        return unloadChunk(x, z, save, false);
    }

    @Override
    public boolean unloadChunk(int x, int z, boolean save, boolean safe) {
        if (isChunkLoaded(x, z)) {
            return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
                @Override
                public void run(Boolean value) {
                    this.value = parent.unloadChunk(x, z, save, safe);
                }
            });
        }
        return true;
    }

    @Override
    public boolean unloadChunkRequest(int x, int z) {
        return unloadChunk(x, z);
    }

    @Override
    public boolean unloadChunkRequest(int x, int z, boolean safe) {
        return unloadChunk(x, z, safe);
    }

    @Override
    public boolean regenerateChunk(int x, int z) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
               this.value = parent.regenerateChunk(x, z);
            }
        });
    }

    @Override
    @Deprecated
    public boolean refreshChunk(int x, int z) {
        queue.sendChunk(queue.getFaweChunk(x, z), FaweQueue.RelightMode.NONE);
        return true;
    }

    @Override
    public Item dropItem(Location location, ItemStack item) {
        return TaskManager.IMP.sync(new RunnableVal<Item>() {
            @Override
            public void run(Item value) {
                this.value = parent.dropItem(location, item);
            }
        });
    }

    @Override
    public Item dropItemNaturally(Location location, ItemStack item) {
        return TaskManager.IMP.sync(new RunnableVal<Item>() {
            @Override
            public void run(Item value) {
                this.value = parent.dropItemNaturally(location, item);
            }
        });
    }

    @Override
    public Arrow spawnArrow(Location location, Vector direction, float speed, float spread) {
        return TaskManager.IMP.sync(new RunnableVal<Arrow>() {
            @Override
            public void run(Arrow value) {
                this.value = parent.spawnArrow(location, direction, speed, spread);
            }
        });
    }

    @Override
    public boolean generateTree(Location location, TreeType type) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.generateTree(location, type);
            }
        });
    }

    @Override
    public boolean generateTree(Location loc, TreeType type, BlockChangeDelegate delegate) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.generateTree(loc, type, delegate);
            }
        });
    }

    @Override
    public Entity spawnEntity(Location loc, EntityType type) {
        return spawn(loc, type.getEntityClass());
    }

    @Override
    @Deprecated
    public LivingEntity spawnCreature(Location loc, EntityType type) {
        return (LivingEntity)this.spawnEntity(loc, type);
    }

    @Override
    @Deprecated
    public LivingEntity spawnCreature(Location loc, CreatureType type) {
        return this.spawnCreature(loc, type.toEntityType());
    }

    @Override
    public LightningStrike strikeLightning(Location loc) {
        return TaskManager.IMP.sync(new RunnableVal<LightningStrike>() {
            @Override
            public void run(LightningStrike value) {
                this.value = parent.strikeLightning(loc);
            }
        });
    }

    @Override
    public LightningStrike strikeLightningEffect(Location loc) {
        return TaskManager.IMP.sync(new RunnableVal<LightningStrike>() {
            @Override
            public void run(LightningStrike value) {
                this.value = parent.strikeLightningEffect(loc);
            }
        });
    }

    @Override
    public List<Entity> getEntities() {
        return TaskManager.IMP.sync(new RunnableVal<List<Entity>>() {
            @Override
            public void run(List<Entity> value) {
                this.value = parent.getEntities();
            }
        });
    }

    @Override
    public List<LivingEntity> getLivingEntities() {
        return TaskManager.IMP.sync(new RunnableVal<List<LivingEntity>>() {
            @Override
            public void run(List<LivingEntity> value) {
                this.value = parent.getLivingEntities();
            }
        });
    }

    @Override
    @Deprecated
    public <T extends Entity> Collection<T> getEntitiesByClass(Class<T>... classes) {
        return TaskManager.IMP.sync(new RunnableVal<Collection<T>>() {
            @Override
            public void run(Collection<T> value) {
                this.value = (Collection<T>) parent.getEntitiesByClass(classes);
            }
        });
    }

    @Override
    public <T extends Entity> Collection<T> getEntitiesByClass(Class<T> cls) {
        return TaskManager.IMP.sync(new RunnableVal<Collection<T>>() {
            @Override
            public void run(Collection<T> value) {
                this.value = (Collection<T>) parent.getEntitiesByClass(cls);
            }
        });
    }

    @Override
    public Collection<Entity> getEntitiesByClasses(Class<?>... classes) {
        return TaskManager.IMP.sync(new RunnableVal<Collection<Entity>>() {
            @Override
            public void run(Collection<Entity> value) {
                this.value = parent.getEntitiesByClasses(classes);
            }
        });
    }

    @Override
    public List<Player> getPlayers() {
        return TaskManager.IMP.sync(new RunnableVal<List<Player>>() {
            @Override
            public void run(List<Player> value) {
                this.value = parent.getPlayers();
            }
        });
    }

    @Override
    public Collection<Entity> getNearbyEntities(Location location, double x, double y, double z) {
        return TaskManager.IMP.sync(new RunnableVal<Collection<Entity>>() {
            @Override
            public void run(Collection<Entity> value) {
                this.value = parent.getNearbyEntities(location, x, y, z);
            }
        });
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public UUID getUID() {
        return parent.getUID();
    }

    @Override
    public Location getSpawnLocation() {
        return parent.getSpawnLocation();
    }

    @Override
    public boolean setSpawnLocation(int x, int y, int z) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.setSpawnLocation(x, y, z);
            }
        });
    }

    @Override
    public long getTime() {
        return parent.getTime();
    }

    @Override
    public void setTime(long time) {
        parent.setTime(time);
    }

    @Override
    public long getFullTime() {
        return parent.getFullTime();
    }

    @Override
    public void setFullTime(long time) {
        parent.setFullTime(time);
    }

    @Override
    public boolean hasStorm() {
        return parent.hasStorm();
    }

    @Override
    public void setStorm(boolean hasStorm) {
        parent.setStorm(hasStorm);
    }

    @Override
    public int getWeatherDuration() {
        return parent.getWeatherDuration();
    }

    @Override
    public void setWeatherDuration(int duration) {
        parent.setWeatherDuration(duration);
    }

    @Override
    public boolean isThundering() {
        return parent.isThundering();
    }

    @Override
    public void setThundering(boolean thundering) {
        parent.setThundering(thundering);
    }

    @Override
    public int getThunderDuration() {
        return parent.getThunderDuration();
    }

    @Override
    public void setThunderDuration(int duration) {
        parent.setThunderDuration(duration);
    }

    public boolean createExplosion(double x, double y, double z, float power) {
        return this.createExplosion(x, y, z, power, false, true);
    }

    public boolean createExplosion(double x, double y, double z, float power, boolean setFire) {
        return this.createExplosion(x, y, z, power, setFire, true);
    }

    public boolean createExplosion(double x, double y, double z, float power, boolean setFire, boolean breakBlocks) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.createExplosion(x, y, z, power, setFire, breakBlocks);
            }
        });
    }

    public boolean createExplosion(Location loc, float power) {
        return this.createExplosion(loc, power, false);
    }

    public boolean createExplosion(Location loc, float power, boolean setFire) {
        return this.createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, setFire);
    }

    @Override
    public Environment getEnvironment() {
        return parent.getEnvironment();
    }

    @Override
    public long getSeed() {
        return parent.getSeed();
    }

    @Override
    public boolean getPVP() {
        return parent.getPVP();
    }

    @Override
    public void setPVP(boolean pvp) {
        parent.setPVP(pvp);
    }

    @Override
    public ChunkGenerator getGenerator() {
        return parent.getGenerator();
    }

    @Override
    public void save() {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.save();
            }
        });
    }

    @Override
    public List<BlockPopulator> getPopulators() {
        return parent.getPopulators();
    }

    @Override
    public <T extends Entity> T spawn(Location location, Class<T> clazz) throws IllegalArgumentException {
        return TaskManager.IMP.sync(new RunnableVal<T>() {
            @Override
            public void run(T value) {
                this.value = parent.spawn(location, clazz);
            }
        });
    }

    @Override
    @Deprecated
    public FallingBlock spawnFallingBlock(Location location, Material material, byte data) throws IllegalArgumentException {
        return this.spawnFallingBlock(location, material.getId(), data);
    }

    @Override
    @Deprecated
    public FallingBlock spawnFallingBlock(Location location, int blockId, byte blockData) throws IllegalArgumentException {
        return TaskManager.IMP.sync(new RunnableVal<FallingBlock>() {
            @Override
            public void run(FallingBlock value) {
                this.value = parent.spawnFallingBlock(location, blockId, blockData);
            }
        });
    }

    @Override
    public void playEffect(Location location, Effect effect, int data) {
        this.playEffect(location, effect, data, 64);
    }

    @Override
    public void playEffect(Location location, Effect effect, int data, int radius) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playEffect(location, effect, data, radius);
            }
        });
    }

    @Override
    public <T> void playEffect(Location loc, Effect effect, T data) {
        this.playEffect(loc, effect, data, 64);
    }

    @Override
    public <T> void playEffect(Location location, Effect effect, T data, int radius) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playEffect(location, effect, data, radius);
            }
        });
    }

    @Override
    public ChunkSnapshot getEmptyChunkSnapshot(int x, int z, boolean includeBiome, boolean includeBiomeTempRain) {
        return TaskManager.IMP.sync(new RunnableVal<ChunkSnapshot>() {
            @Override
            public void run(ChunkSnapshot value) {
                this.value = parent.getEmptyChunkSnapshot(x, z, includeBiome, includeBiomeTempRain);
            }
        });
    }

    @Override
    public void setSpawnFlags(boolean allowMonsters, boolean allowAnimals) {
        parent.setSpawnFlags(allowMonsters, allowAnimals);
    }

    @Override
    public boolean getAllowAnimals() {
        return parent.getAllowAnimals();
    }

    @Override
    public boolean getAllowMonsters() {
        return parent.getAllowMonsters();
    }

    @Override
    public Biome getBiome(int x, int z) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public void setBiome(int x, int z, Biome bio) {
        int id = adapter.getBiomeId(bio);
        queue.setBiome(x, z, new BaseBiome(id));
    }

    @Override
    public double getTemperature(int x, int z) {
        return parent.getTemperature(x, z);
    }

    @Override
    public double getHumidity(int x, int z) {
        return parent.getHumidity(x, z);
    }

    @Override
    public int getMaxHeight() {
        return parent.getMaxHeight();
    }

    @Override
    public int getSeaLevel() {
        return parent.getSeaLevel();
    }

    @Override
    public boolean getKeepSpawnInMemory() {
        return parent.getKeepSpawnInMemory();
    }

    @Override
    public void setKeepSpawnInMemory(boolean keepLoaded) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.setKeepSpawnInMemory(keepLoaded);
            }
        });
    }

    @Override
    public boolean isAutoSave() {
        return parent.isAutoSave();
    }

    @Override
    public void setAutoSave(boolean value) {
        parent.setAutoSave(value);
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        parent.setDifficulty(difficulty);
    }

    @Override
    public Difficulty getDifficulty() {
        return parent.getDifficulty();
    }

    @Override
    public File getWorldFolder() {
        return parent.getWorldFolder();
    }

    @Override
    public WorldType getWorldType() {
        return parent.getWorldType();
    }

    @Override
    public boolean canGenerateStructures() {
        return parent.canGenerateStructures();
    }

    @Override
    public long getTicksPerAnimalSpawns() {
        return parent.getTicksPerAnimalSpawns();
    }

    @Override
    public void setTicksPerAnimalSpawns(int ticksPerAnimalSpawns) {
        parent.setTicksPerAnimalSpawns(ticksPerAnimalSpawns);
    }

    @Override
    public long getTicksPerMonsterSpawns() {
        return parent.getTicksPerMonsterSpawns();
    }

    @Override
    public void setTicksPerMonsterSpawns(int ticksPerMonsterSpawns) {
        parent.setTicksPerMonsterSpawns(ticksPerMonsterSpawns);
    }

    @Override
    public int getMonsterSpawnLimit() {
        return parent.getMonsterSpawnLimit();
    }

    @Override
    public void setMonsterSpawnLimit(int limit) {
        parent.setMonsterSpawnLimit(limit);
    }

    @Override
    public int getAnimalSpawnLimit() {
        return parent.getAnimalSpawnLimit();
    }

    @Override
    public void setAnimalSpawnLimit(int limit) {
        parent.setAnimalSpawnLimit(limit);
    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return parent.getWaterAnimalSpawnLimit();
    }

    @Override
    public void setWaterAnimalSpawnLimit(int limit) {
        parent.setWaterAnimalSpawnLimit(limit);
    }

    @Override
    public int getAmbientSpawnLimit() {
        return parent.getAmbientSpawnLimit();
    }

    @Override
    public void setAmbientSpawnLimit(int limit) {
        parent.setAmbientSpawnLimit(limit);
    }

    @Override
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                parent.playSound(location, sound, volume, pitch);
            }
        });
    }

    @Override
    public String[] getGameRules() {
        return parent.getGameRules();
    }

    @Override
    public String getGameRuleValue(String rule) {
        return parent.getGameRuleValue(rule);
    }

    @Override
    public boolean setGameRuleValue(String rule, String value) {
        return parent.setGameRuleValue(rule, value);
    }

    @Override
    public boolean isGameRule(String rule) {
        return parent.isGameRule(rule);
    }

    @Override
    public void setMetadata(String key, MetadataValue value) {
        parent.setMetadata(key, value);
    }

    @Override
    public List<MetadataValue> getMetadata(String key) {
        return parent.getMetadata(key);
    }

    @Override
    public boolean hasMetadata(String key) {
        return parent.hasMetadata(key);
    }

    @Override
    public void removeMetadata(String key, Plugin plugin) {
        parent.removeMetadata(key, plugin);
    }

    @Override
    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        parent.sendPluginMessage(source, channel, message);
    }

    @Override
    public Set<String> getListeningPluginChannels() {
        return parent.getListeningPluginChannels();
    }
}
