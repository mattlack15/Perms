package me.gravitinos.perms.spigot.file;

import me.gravitinos.perms.spigot.SpigotPerms;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Files {
	
	//
	@PluginFile
	public static final File GROUPS_FILE = new File(SpigotPerms.instance.getDataFolder() + File.separator + "groups.yml");

	@PluginFile
	public static final File USERS_FILE = new File(SpigotPerms.instance.getDataFolder() + File.separator + "users.yml");

	@PluginFile
	public static final File LOG_FILE = new File(SpigotPerms.instance.getDataFolder() + File.separator + "log.txt");

	@PluginFile
	public static final File PERMISSION_INDEX_FILE = new File(SpigotPerms.instance.getDataFolder() + File.separator + "permission_index.yml");

	
	public Files() {
		Class<?> c = this.getClass();
		Field fields[] = c.getDeclaredFields();
		for(Field f : fields) {
			if(Modifier.isStatic(f.getModifiers())){
				if(f.getType() == File.class) {
					if(f.getAnnotation(PluginFile.class) != null) {
						try {
							File file = (File) f.get(null);
							try {
								SpigotPerms.instance.saveResource(file.getPath(), false);
							} catch(Exception e) {
							
							}
							if(!file.exists()) {
								new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator))).mkdirs();
								file.createNewFile();
							}
						} catch (IllegalArgumentException | IllegalAccessException | IOException e) {
							e.printStackTrace();
						}
					} else if(f.getAnnotation(PluginFolder.class) != null) {
						try {
							File file = (File) f.get(null);
							file.mkdirs();
						} catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
		}
	}
}
