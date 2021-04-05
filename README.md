# Perms
This is a permissions plugin.

## Extensive in-game GUI

## API
### Getting a user object
```java
User user = UserManager.getUser(player.getUniqueId());
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
