package simplepets.brainsynder.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.simple.JSONObject;
import simple.brainsynder.api.ItemBuilder;
import simple.brainsynder.nms.DataConverter;
import simple.brainsynder.utils.MatType;
import simple.brainsynder.utils.Reflection;
import simple.brainsynder.utils.ServerVersion;
import simple.brainsynder.utils.SkullType;
import simplepets.brainsynder.PetCore;
import simplepets.brainsynder.errors.SimplePetsException;
import simplepets.brainsynder.player.PetOwner;
import simplepets.brainsynder.reflection.FieldAccessor;
import simplepets.brainsynder.reflection.ReflectionUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utilities {
    private static DataConverter converter;
    private static Map<String, Long> startTimeMap = new HashMap<>();

    public static void init () {
        converter = Reflection.getConverter();
    }

    public static List<Material> getBlacklistedMaterials() {
        List<Material> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            String name = material.name();

            if (name.contains("GLASS_PANE")) materials.add(material);
            if (name.contains("FENCE")) materials.add(material);
            if (name.contains("DOOR") && (!name.contains("TRAP"))) materials.add(material);
            if (name.contains("FENCE_GATE")) materials.add(material);
            if (name.contains("THIN")) materials.add(material);
        }

        return materials;
    }

    public static DataConverter.Data getSkull(SkullType type) {
        return converter.getSkullMaterial(type);
    }

    public static DataConverter.Data getColored(MatType type, int data) {
        return converter.getColoredMaterial(type, data);
    }

    /**
     * Translates the name to a {@link org.bukkit.Material}
     *
     * @param name The new/old {@link org.bukkit.Material} name
     * @return
     */
    public static Material findMaterial(String name) {
        return converter.findMaterial(name);

    }

    public static ItemBuilder translate113 (ItemBuilder builder, JSONObject json) {
        if (ServerVersion.getVersion().getIntVersion() < ServerVersion.v1_13_R1.getIntVersion()) return builder;
        Material material = findMaterial(String.valueOf(json.get("material")));
        int data = Integer.parseInt(String.valueOf(json.getOrDefault("data", "0")));

        try {
            MatType type = MatType.valueOf(material.name().replace("LEGACY_", ""));
            DataConverter.Data data1 = getColored(type, data);
            json.put("material", data1.getMaterial().name());
            if (data1.getData() == -1) json.remove("data");
            return ItemBuilder.fromJSON(json);
        }catch (Exception ignored){}

        if (json.containsKey("skullData")) {
            try {
                DataConverter.Data data1 = getSkull(simple.brainsynder.utils.SkullType.values()[data]);
                json.put("material", data1.getMaterial().name());
                if (data1.getData() == -1) json.remove("data");
                return ItemBuilder.fromJSON(json);
            }catch (Exception ignored){}
        }

        if (json.containsKey("entity")) {
            try {
                material = findMaterial(json.get("entity")+"_SPAWN_EGG");
                json.put("material", material.name());
                json.remove("data");
                json.remove("entity");
                return ItemBuilder.fromJSON(json);
            }catch (Exception ignored){}
        }

        return builder;
    }

    public static Material fetchMaterial(String... names) {
        for (String name : names) {
            try {
                return converter.findMaterial(name);
            } catch (Exception ignored) {
            }
        }
        return Material.AIR;
    }

    /**
     * Finds how many Milliseconds it took to run a task
     *
     * @param clazz    - Class where the task is being called from
     * @param taskName - A name to give the task (use the same name for start/finish)
     */
    public static long findDelay(Class clazz, String taskName) {
        return findDelay(clazz, taskName, true);
    }

    /**
     * Finds how many Milliseconds it took to run a task
     *
     * @param clazz    - Class where the task is being called from
     * @param taskName - A name to give the task (use the same name for start/finish)
     * @param output   - Should the data be broadcast to the server?
     */
    public static long findDelay(Class clazz, String taskName, boolean output) {
        String key = clazz.getSimpleName() + "|" + taskName;
        if (startTimeMap.containsKey(key)) {
            long start = startTimeMap.get(key);
            long end = System.nanoTime();
            long diff = (end - start) / 1000000;
            if (output) Bukkit.broadcastMessage(key + " -   Took: " + diff + "ms");
            startTimeMap.remove(key);
            return diff;
        }
        startTimeMap.put(key, System.nanoTime());
        return 0;
    }

    public static String saveTextToHastebin(String text) {
        try {
            String url = "https://hastebin.com/documents";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(text);
            wr.flush();
            wr.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) response.append(inputLine);
            in.close();

            JsonElement json = new JsonParser().parse(response.toString());
            if (!json.isJsonObject()) throw new IOException("Cannot parse JSON");
            return "https://hastebin.com/" + json.getAsJsonObject().get("key").getAsString();
        } catch (IOException ignored) {
        }
        return null;
    }

    public void setPassenger(Entity entity, Entity passenger) {
        try {
            entity.setPassenger(passenger);
            if (passenger instanceof Player) {
                sendMountPacket((Player) passenger, entity);
            }
            if (entity instanceof Player) {
                sendMountPacket((Player) entity, passenger);
            }
        } catch (Exception e) {
            PetCore.get().debug(2, "Could not run method IEntityPet#setPassenger");
            e.printStackTrace();
        }
    }

    public void removePassenger(Entity entity, Entity passenger) {
        try {
            entity.eject();
            if (entity instanceof Player) {
                resetRideCooldown(passenger);
                sendMountPacket((Player) entity, passenger);
            }
        } catch (Exception e) {
            PetCore.get().debug(2, "Could not run method IEntityPet#removePassenger");
            e.printStackTrace();
        }
    }

    public void resetRideCooldown(Entity entity) {
        FieldAccessor<Integer> field;
        if (ServerVersion.getVersion() == ServerVersion.v1_13_R1 || ServerVersion.getVersion() == ServerVersion.v1_13_R2) {
            field = FieldAccessor.getField(Reflection.getNmsClass("Entity"), "k", Integer.TYPE);
        } else {
            field = FieldAccessor.getField(Reflection.getNmsClass("Entity"), "j", Integer.TYPE);
        }
        field.set(Reflection.getHandle(entity), 0);
    }

    public void sendMountPacket(Player player, Entity entity) {
        if (ServerVersion.getVersion() == ServerVersion.v1_8_R3) return;
        Class<?> outMount = ReflectionUtil.getNmsClass("PacketPlayOutMount");
        Constructor<?> constructor = ReflectionUtil.fillConstructor(outMount, ReflectionUtil.getNmsClass("Entity"));
        Object packet = ReflectionUtil.initiateClass(constructor, ReflectionUtil.getEntityHandle(entity));
        ReflectionUtil.sendPacket(packet, player);
    }

    public void handlePathfinders(Player player, Entity entity, double speed) {
        try {
            Class<?> clazz = Class.forName("simplepets.brainsynder.nms." + ReflectionUtil.getVersion() + ".pathfinders.HandlePathfinders");
            if (clazz == null) throw new SimplePetsException("HandlePathfinders not found");
            Constructor<?> con = clazz.getDeclaredConstructor(Player.class, Entity.class, double.class);
            con.newInstance(player, entity, speed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearPathfinders(Entity entity) {
        try {
            Class<?> clazz = Class.forName("simplepets.brainsynder.nms."+ReflectionUtil.getVersion()+".pathfinders.ClearPathfinders");
            if (clazz == null) throw new SimplePetsException("ClearPathfinders not found");
            Constructor<?> con = clazz.getDeclaredConstructor(Entity.class);
            con.newInstance(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hidePet(Player player) {
        managePetVisibility(player, "PacketPlayOutEntityDestroy", Integer.TYPE, PetOwner.getPetOwner(player).getPet().getEntity().getEntity().getEntityId());
    }

    public void showPet(Player player) {
        managePetVisibility(player, "PacketPlayOutSpawnEntityLiving", ReflectionUtil.getNmsClass("EntityLiving"), ReflectionUtil.getEntityHandle(PetOwner.getPetOwner(player).getPet().getEntity().getEntity()));
    }

    private void managePetVisibility(Player p, String nmsClass, Class<?> o1, Object o2) {
        Class<?> entity = ReflectionUtil.getNmsClass(nmsClass);
        Constructor<?> constructor = ReflectionUtil.fillConstructor(entity, o1);
        Object packet = ReflectionUtil.initiateClass(constructor, o2);
        ReflectionUtil.sendPacket(packet, p);
    }

    public boolean isSimilar(ItemStack main, ItemStack check) {
        List<Boolean> values = new ArrayList<>();
        if ((main == null) || (check == null)) return false;
        //if (main.isSimilar(check)) return true;

        if (main.getType() == check.getType()) {
            if (main.hasItemMeta() && check.hasItemMeta()) {
                ItemMeta mainMeta = main.getItemMeta();
                ItemMeta checkMeta = check.getItemMeta();
                if (mainMeta.hasDisplayName() && checkMeta.hasDisplayName()) {
                    values.add(mainMeta.getDisplayName().equals(checkMeta.getDisplayName()));
                }

                if (mainMeta.hasLore() && checkMeta.hasLore()) {
                    values.add(mainMeta.getLore().equals(checkMeta.getLore()));
                }

                if (mainMeta.hasEnchants() && checkMeta.hasEnchants()) {
                    values.add(mainMeta.getEnchants().equals(checkMeta.getEnchants()));
                }

                if (!values.isEmpty()) return !values.contains(false);
            }
        }

        return main.isSimilar(check);
    }

    public String itemToString(ItemStack itemStack) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("i", itemStack);
        return config.saveToString();
    }

    public ItemStack stringToItem(String stringBlob) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(stringBlob);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return config.getItemStack("i", null);
    }
}
