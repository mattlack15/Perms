# Perms
This is a permissions plugin.

## Extensive in-game GUI

## API
### Getting a user object
```java
User user = UserManager.instance.getUser(player.getUniqueId());
```
### Contexts
Contexts are conditions in which something will apply. A ContextSet is a set of one or more contexts that define when something should apply. ContextSets can be satisfied by other ContextSets. For satisfaction of a ContextSet, the set is divided into groups of like-keys (eg. server) and the set is satisfied if each group in set A has at least one match with a context in set B. For example, if set A is {server:sv1, server:sv2, world:w1} and B is {server:sv1), then A is NOT satisfied by B because the group "world" does not have at least one match in set B. However, if set B is instead, {server:sv1, world:w1} then A IS satisfied by B because B contains a matching context for both groups (server and world).<br><br>
Contexts can be created like this
```java
Context context = new Context("key", "value");
Context serverContext = new Context(Context.SERVER_IDENTIFIER, "prison");
ContextSet contexts = new MutableContextSet(context, serverContext);
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
User/Group data may be accessed by calling getData() on either a User or a Group, it will return the corresponding SubjectData object.
