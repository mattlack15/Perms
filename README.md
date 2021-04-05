# Perms
This is a permissions plugin.

## Extensive in-game GUI

## API
### Getting a user object
```java
User user = UserManager.instance.getUser(player.getUniqueId());
```
### Adding a permission
```java
ContextSet contextSet = new MutableContextSet(Context.CONTEXT_SERVER_LOCAL);
user.addPermission(new PPermission("hey.wassup", contextSet);
```
### Getting a visible group object
```java
Group group = GroupManager.instance.getVisibleGroup("myGroupName");
```

### Creating a group
```java
Group g = new GroupBuilder("default").setPrefix("[&7Default&f] ").setDescription("The default group")
                .addPermission(new PPermission("modifyworld.*")).build();
GroupManager.instance.addGroup(g);
```
