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
### Saving
All users and groups will queue updates to send to the data storage system automatically (auto-queueing), however this can be disabled using setAutoQueue(false).
You may manually queue a save by calling queueSave().

### User/Group data
User/Group data may be accessed by called getData() on either a User or a Group, it will return the corresponding SubjectData object.
