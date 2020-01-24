package me.gravitinos.perms.spigot;

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
