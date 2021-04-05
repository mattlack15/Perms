package me.gravitinos.perms.core.backend.mongo;

import com.mongodb.*;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.StorageCredentials;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.cache.OwnerPermissionPair;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.ladders.RankLadder;
import me.gravitinos.perms.core.subject.*;
import me.gravitinos.perms.core.util.GravSerializer;
import me.gravitinos.perms.core.util.MapUtil;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MongoHandler extends DataManager {
    private MongoClient client;
    private DB database;

    private static final String COLLECTION_SUBJECTDATA = "perms_subjectdata";
    private static final String COLLECTION_INHERITANCE = "perms_inheritance";
    private static final String COLLECTION_PERMISSIONS = "perms_permissions";
    private static final String COLLECTION_SERVER_INDEX = "perms_server_index";
    private static final String COLLECTION_RANK_LADDERS = "perms_rank_ladders";

    //Rank Ladders
    private static final String FIELD_LADDER_ID = "_id";
    private static final String FIELD_LADDER_DATA = "data";
    private static final String FIELD_LADDER_GROUPS = "groups";
    private static final String FIELD_LADDER_CONTEXT = "context";

    //Permission
    private static final String FIELD_PERMISSION_NAME = "permission";
    private static final String FIELD_PERMISSION_ID = "_id";
    private static final String FIELD_PERMISSION_OWNER_SUBJECT_ID = "ownerId";

    //SubjectData
    private static final String FIELD_SUBJECT_ID = "_id";
    private static final String FIELD_SUBJECT_DATA = "data";
    private static final String FIELD_SUBJECT_TYPE = "type";

    //Inheritance
    private static final String FIELD_INHERITANCE_CHILD = "child";
    private static final String FIELD_INHERITANCE_PARENT = "parent";
    private static final String FIELD_INHERITANCE_CHILD_TYPE = "childType";
    private static final String FIELD_INHERITANCE_PARENT_TYPE = "parentType";
    private static final String FIELD_INHERITANCE_ID = "_id"; //This will be a string that is child + parent so that no duplicates are made

    private boolean connected = false;

    //Common
    private static final String FIELD_CONTEXT = "context";

    public MongoHandler(StorageCredentials credentials, String database) {
        MongoCredential credential = MongoCredential.createMongoCRCredential(credentials.getUsername(), database, credentials.getPassword().toCharArray());
        try {
            this.client = new MongoClient(new ServerAddress(credentials.getHost(), credentials.getPort()), new ArrayList<MongoCredential>() {{
                add(credential);
            }}, MongoClientOptions.builder().build());
            this.database = client.getDB(database);
            if(this.database == null)
                throw new SQLException("CANNOT CONNECT");
            connected = true;
        } catch (Exception e) {
            PermsManager.instance.getImplementation().consoleLog("UNABLE TO CONNECT TO MONGO DATABASE ERROR:");
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        this.client.close();
    }

    @Override
    public CompletableFuture<Void> addSubject(Subject subject) {
        return runAsync(() -> {
            //Update or Add subject data
            this.database.getCollection(COLLECTION_SUBJECTDATA).update(new BasicDBObject(FIELD_SUBJECT_ID, subject.getSubjectId().toString()), subjectToFullSubjectDataObj(subject), true, false);

            //Remove previous (if existing) inheritances
            this.database.getCollection(COLLECTION_INHERITANCE).remove(new BasicDBObject(FIELD_INHERITANCE_CHILD, subject.getSubjectId().toString()));

            //Add inheritances
            ArrayList<BasicDBObject> objects = new ArrayList<>();
            for (Inheritance inheritance : Subject.getInheritances(subject)) {
                if (inheritance.isValid()) {
                    objects.add(inheritanceToObj(inheritance.toCachedInheritance()));
                }
            }
            this.database.getCollection(COLLECTION_INHERITANCE).insert(objects.toArray(new BasicDBObject[0]));

            //Remove previous (if existing) permissions
            this.database.getCollection(COLLECTION_PERMISSIONS).remove(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, subject.getSubjectId().toString()));

            //Add permissions
            objects.clear();
            for (PPermission permissions : Subject.getPermissions(subject)) {
                objects.add(permissionToObj(new OwnerPermissionPair(subject.getSubjectId(), permissions)));
            }
            this.database.getCollection(COLLECTION_PERMISSIONS).insert(objects.toArray(new BasicDBObject[0]));
            return null;
        });
    }

    @Override
    public CompletableFuture<CachedSubject> getSubject(UUID subjectId) {
        return runAsync(() -> {
            DBObject subjectData = this.database.getCollection(COLLECTION_SUBJECTDATA).findOne(new BasicDBObject(FIELD_SUBJECT_ID, subjectId.toString()));
            if (subjectData == null)
                return null;
            String type = (String) subjectData.get(FIELD_SUBJECT_TYPE);
            SubjectData data = objToSubjectData((DBObject) subjectData.get(FIELD_SUBJECT_DATA));

            //Permissions
            ArrayList<PPermission> permissions = new ArrayList<>();
            this.database.getCollection(COLLECTION_PERMISSIONS).find(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, subjectId.toString())).forEach(object -> permissions.add(objToPermission(object)));

            //Inheritances
            ArrayList<CachedInheritance> inheritances = new ArrayList<>();
            this.database.getCollection(COLLECTION_INHERITANCE).find(new BasicDBObject(FIELD_INHERITANCE_CHILD, subjectId.toString())).forEach(object -> inheritances.add(objToInheritance(object)));

            return new CachedSubject(subjectId, type, data, permissions, inheritances);
        });
    }

    @Override
    public CompletableFuture<Void> updateSubject(Subject subject) {
        return this.addSubject(subject);
    }

    @Override
    public CompletableFuture<Void> removeSubject(UUID subjectId) {
        return runAsync(() -> {
            //Subject Data
            this.database.getCollection(COLLECTION_SUBJECTDATA).remove(new BasicDBObject(FIELD_SUBJECT_ID, subjectId.toString()));

            //Inheritances
            List<BasicDBObject> criteria = new ArrayList<>();
            criteria.add(new BasicDBObject(FIELD_INHERITANCE_CHILD, subjectId.toString()));
            criteria.add(new BasicDBObject(FIELD_INHERITANCE_PARENT, subjectId.toString()));
            this.database.getCollection(COLLECTION_INHERITANCE).remove(new BasicDBObject("$or", criteria));

            //Permissions
            this.database.getCollection(COLLECTION_PERMISSIONS).remove(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, subjectId.toString()));

            return null;
        });
    }

//    @Override
//    public CompletableFuture<ImmutablePermissionList> getPermissions(UUID subjectId) {
//        return runAsync(() -> {
//
//            //Permissions
//            ArrayList<PPermission> permissions = new ArrayList<>();
//            this.database.getCollection(COLLECTION_PERMISSIONS).find(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, subjectId.toString())).forEach(object -> permissions.add(objToPermission(object)));
//
//            return new ImmutablePermissionList(permissions);
//        });
//    }

//    @Override
//    public CompletableFuture<Void> updatePermissions(Subject subject) {
//        return runAsync(() -> {
//            //Remove previous (if existing) permissions
//            this.database.getCollection(COLLECTION_PERMISSIONS).remove(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, subject.getSubjectId().toString()));
//
//            //Add permissions
//            ArrayList<BasicDBObject> objects = new ArrayList<>();
//            for (PPermission permissions : Subject.getPermissions(subject)) {
//                objects.add(permissionToObj(new OwnerPermissionPair(subject.getSubjectId(), permissions)));
//            }
//            this.database.getCollection(COLLECTION_PERMISSIONS).insert(objects.toArray(new BasicDBObject[0]));
//
//            return null;
//        });
//    }
//
//    @Override
//    public CompletableFuture<Void> addPermission(Subject subject, PPermission permission) {
//        return runAsync(() -> {
//            this.database.getCollection(COLLECTION_PERMISSIONS).insert(permissionToObj(new OwnerPermissionPair(subject.getSubjectId(), permission)));
//            return null;
//        });
//    }
//
//    @Override
//    public CompletableFuture<Void> removePermission(Subject subject, String permission) {
//        return runAsync(() -> {
//            this.database.getCollection(COLLECTION_PERMISSIONS).remove(new BasicDBObject("$and", new ArrayList<BasicDBObject>() {{
//                add(new BasicDBObject(FIELD_PERMISSION_NAME, permission));
//                add(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, subject.getSubjectId().toString()));
//            }})); //This executes a statement that removes permissions if their permission is permission AND their owner is the subject's subject id
//            return null;
//        });
//    }

//    @Override
//    public CompletableFuture<Void> removePermissionExact(Subject subject, PPermission permission) {
//        return runAsync(() -> {
//            this.database.getCollection(COLLECTION_PERMISSIONS).remove(new BasicDBObject(FIELD_PERMISSION_ID, permission.getPermissionIdentifier().toString()));
//            return null;
//        });
//    }
//
//    @Override
//    public CompletableFuture<List<CachedInheritance>> getInheritances(UUID subjectId) {
//        return runAsync(() -> {
//            //Inheritances
//            ArrayList<CachedInheritance> inheritances = new ArrayList<>();
//            this.database.getCollection(COLLECTION_INHERITANCE).find(new BasicDBObject(FIELD_INHERITANCE_CHILD, subjectId.toString())).forEach(object -> inheritances.add(objToInheritance(object)));
//            return inheritances;
//        });
//    }

//    @Override
//    public CompletableFuture<Void> updateInheritances(Subject subject) {
//        return runAsync(() -> {
//
//            //Remove previous
//            this.database.getCollection(COLLECTION_INHERITANCE).remove(new BasicDBObject(FIELD_INHERITANCE_CHILD, subject.getSubjectId().toString()));
//
//            //Add current
//            ArrayList<DBObject> toAdd = new ArrayList<>();
//            ArrayList<Inheritance> inheritances = Subject.getInheritances(subject);
//            inheritances.forEach(i -> toAdd.add(inheritanceToObj(i.toCachedInheritance())));
//            this.database.getCollection(COLLECTION_INHERITANCE).insert(toAdd.toArray(new DBObject[0]));
//
//            return null;
//        });
//    }

//    @Override
//    public CompletableFuture<Void> addInheritance(@NotNull CachedInheritance inheritance) {
//        return runAsync(() -> {
//            this.database.getCollection(COLLECTION_INHERITANCE).insert(inheritanceToObj(inheritance));
//            return null;
//        });
//    }

//    @Override
//    public CompletableFuture<Void> removeInheritance(Subject subject, UUID parent) {
//        return runAsync(() -> {
//            this.database.getCollection(COLLECTION_INHERITANCE).remove(new BasicDBObject(FIELD_INHERITANCE_CHILD, subject.getSubjectId().toString())
//                    .append(FIELD_INHERITANCE_PARENT, parent.toString()));
//            return null;
//        });
//    }

    @Override
    public CompletableFuture<Void> updateSubjectData(Subject subject) {
        return runAsync(() -> {
            this.database.getCollection(COLLECTION_SUBJECTDATA).update(new BasicDBObject(FIELD_SUBJECT_ID, subject.getSubjectId().toString()), subjectToFullSubjectDataObj(subject), true, false);
            return null;
        });
    }

    @Override
    public CompletableFuture<GenericSubjectData> getSubjectData(UUID subjectId) {
        return runAsync(() -> {
            DBObject object = this.database.getCollection(COLLECTION_SUBJECTDATA).findOne(new BasicDBObject(FIELD_SUBJECT_ID, subjectId.toString()));
            if(object == null)
                return null;
            GenericSubjectData subjectData = objToSubjectData(object);
            subjectData.setType((String) object.get(FIELD_SUBJECT_TYPE));
            return subjectData;
        });
    }

    @Override
    public CompletableFuture<Void> addOrUpdateSubjects(List<Subject<?>> subjects) {
        subjects.forEach(s -> {
            s.getOwnLoggedPermissions().resetLog();
            s.getOwnLoggedInheritances().resetLog();
        });
        return addSubjects(subjects);
    }

//    @Override
//    public CompletableFuture<Void> addPermissions(Subject subject, ImmutablePermissionList list) {
//        return runAsync(() -> {
//            ArrayList<DBObject> objects = new ArrayList<>();
//            list.forEach(p -> objects.add(permissionToObj(new OwnerPermissionPair(subject.getSubjectId(), p))));
//            this.database.getCollection(COLLECTION_PERMISSIONS).insert(objects.toArray(new DBObject[0]));
//            return null;
//        });
//    }

//    @Override
//    public CompletableFuture<Void> removePermissions(Subject subject, ArrayList<String> list) {
//        return runAsync(() -> {
//            BulkWriteOperation op = this.database.getCollection(COLLECTION_PERMISSIONS).initializeUnorderedBulkOperation();
//            list.forEach(p -> op.find(new BasicDBObject(FIELD_PERMISSION_NAME, p)).remove());
//            op.execute();
//            return null;
//        });
//    }

//    @Override
//    public CompletableFuture<Void> removePermissionsExact(Subject subject, ArrayList<PPermission> list) {
//        return runAsync(() -> {
//            BulkWriteOperation op = this.database.getCollection(COLLECTION_PERMISSIONS).initializeUnorderedBulkOperation();
//            list.forEach(p -> op.find(new BasicDBObject(FIELD_PERMISSION_ID, p.getPermissionIdentifier())).remove());
//            op.execute();
//            return null;
//        });
//    }

    public CompletableFuture<Void> addSubjects(List<Subject<?>> subjects) {
        return runAsync(() -> {

            //Initialize ORDERED (because of removing and THEN adding) bulk operations
            BulkWriteOperation opSubjectData = this.database.getCollection(COLLECTION_SUBJECTDATA).initializeOrderedBulkOperation();
            BulkWriteOperation opInheritances = this.database.getCollection(COLLECTION_INHERITANCE).initializeOrderedBulkOperation();
            BulkWriteOperation opPermissions = this.database.getCollection(COLLECTION_PERMISSIONS).initializeOrderedBulkOperation();


            //2 loops so that the operations are neatly grouped in the bulk operation
            //Because the implementation will group CONSECUTIVE operations of the same type
            //NOT DOING THIS MIGHT RESULT IN LONG EXECUTION TIME

            //First loop for removing of the previous values, if they exist
            subjects.forEach(s -> {
                opSubjectData.find(new BasicDBObject(FIELD_SUBJECT_ID, s.getSubjectId().toString())).remove();
                opInheritances.find(new BasicDBObject(FIELD_INHERITANCE_CHILD, s.getSubjectId().toString())).remove();
                opPermissions.find(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, s.getSubjectId().toString())).remove();
            });

            //Second loop for adding data
            subjects.forEach(s -> {
                opSubjectData.insert(subjectToFullSubjectDataObj(s));
                for(Inheritance inheritance : Subject.getInheritances(s)) {
                    opInheritances.insert(inheritanceToObj(inheritance.toCachedInheritance()));
                }
                for(PPermission permission : Subject.getPermissions(s)){
                    opPermissions.insert(permissionToObj(new OwnerPermissionPair(s.getSubjectId(), permission)));
                }
            });

            //Execute operations
            opSubjectData.execute();
            opInheritances.execute();
            opPermissions.execute();

            return null;
        });
    }

    @Override
    public CompletableFuture<Void> removeSubjects(List<UUID> subjects) {
        return runAsync(() -> {

            //Initialized UNORDERED bulk operations
            BulkWriteOperation opSubjectData = this.database.getCollection(COLLECTION_SUBJECTDATA).initializeUnorderedBulkOperation();
            BulkWriteOperation opInheritances = this.database.getCollection(COLLECTION_INHERITANCE).initializeUnorderedBulkOperation();
            BulkWriteOperation opPermissions = this.database.getCollection(COLLECTION_PERMISSIONS).initializeUnorderedBulkOperation();

            subjects.forEach(s -> {
                opSubjectData.find(new BasicDBObject(FIELD_SUBJECT_ID, s.toString())).remove();

                BasicDBList criteria = new BasicDBList();
                criteria.add(new BasicDBObject(FIELD_INHERITANCE_CHILD, s.toString()));
                criteria.add(new BasicDBObject(FIELD_INHERITANCE_PARENT, s.toString()));
                opInheritances.find(new BasicDBObject("$or", criteria)).remove();

                opPermissions.find(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, s.toString()));
            });

            opSubjectData.execute();
            opInheritances.execute();
            opPermissions.execute();

            return null;
        });
    }

    @Override
    public CompletableFuture<Void> removeInheritances(Subject<?> subject, List<UUID> parents) {
        return runAsync(() -> {
            BulkWriteOperation op = this.database.getCollection(COLLECTION_INHERITANCE).initializeUnorderedBulkOperation();
            parents.forEach(p -> op.find(new BasicDBObject(FIELD_INHERITANCE_CHILD, subject.getSubjectId().toString()).append(FIELD_INHERITANCE_PARENT, p.toString())).remove());
            op.execute();
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> addInheritances(List<Inheritance> inheritances) {
        return runAsync(() -> {
            ArrayList<DBObject> toAdd = new ArrayList<>();
            inheritances.forEach(i -> toAdd.add(inheritanceToObj(i.toCachedInheritance())));
            this.database.getCollection(COLLECTION_INHERITANCE).insert(toAdd.toArray(new DBObject[0]));
            return null;
        });
    }

    @Override
    public CompletableFuture<List<CachedSubject>> getAllSubjectsOfType(String type) {
        return runAsync(() -> {

            ArrayList<CachedSubject> subjects = new ArrayList<>();


            //The next 33 lines are basically gonna do a manual aggregation $lookup operation
            //Get matching subject data objects
            List<DBObject> objects = this.database.getCollection(COLLECTION_SUBJECTDATA).find(new BasicDBObject(FIELD_SUBJECT_TYPE, type)).toArray();

            //Map them by subject id and add their ids to a list
            Map<String, DBObject> mappedObjects = new HashMap<>();
            List<String> localFields = new ArrayList<>();
            Iterator<DBObject> it = objects.iterator();
            while(it.hasNext()) {
                DBObject o = it.next();
                it.remove();
                mappedObjects.put((String)o.get(FIELD_SUBJECT_ID), o);
                localFields.add((String)o.get(FIELD_SUBJECT_ID));
            }

            //Get things using the list of subject ids

            //Get inheritances
            Map<String, BasicDBList> inheritances = new HashMap<>();
            this.database.getCollection(COLLECTION_INHERITANCE).find(new BasicDBObject(FIELD_INHERITANCE_CHILD, new BasicDBObject("$in", localFields.toArray(new String[0])))).forEach(o -> {
                inheritances.computeIfAbsent((String) o.get(FIELD_INHERITANCE_CHILD), k -> new BasicDBList());
                inheritances.get((String) o.get(FIELD_INHERITANCE_CHILD)).add(o);
            });
            //Add inheritances to subject objects
            inheritances.forEach((s, o) -> mappedObjects.get(s).put("inheritances", o));

            //Get permissions
            Map<String, BasicDBList> permissions = new HashMap<>();
            this.database.getCollection(COLLECTION_PERMISSIONS).find(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, new BasicDBObject("$in", localFields.toArray(new String[0])))).forEach(o -> {
                permissions.computeIfAbsent((String) o.get(FIELD_PERMISSION_OWNER_SUBJECT_ID), k -> new BasicDBList());
                permissions.get((String) o.get(FIELD_PERMISSION_OWNER_SUBJECT_ID)).add(o);
            });
            //Add permissions to subject objects
            permissions.forEach((s, o) -> mappedObjects.get(s).put("permissions", o));

            mappedObjects.values().forEach(result -> {

                //Get subject data
                UUID subjectId = UUID.fromString((String) result.get(FIELD_SUBJECT_ID));
                String subjectType = (String) result.get(FIELD_SUBJECT_TYPE);
                SubjectData subjectData = objToSubjectData((DBObject) result.get(FIELD_SUBJECT_DATA));

                //Parse inheritances and permissions
                ArrayList<CachedInheritance> inheritances1 = new ArrayList<>();
                ArrayList<PPermission> permissions1 = new ArrayList<>();

                if (result.containsField("inheritances")) {
                    BasicDBList inheritanceObjs = (BasicDBList) result.get("inheritances");
                    inheritanceObjs.forEach(i -> inheritances1.add(objToInheritance((DBObject) i)));
                }

                if (result.containsField("permissions")) {
                    BasicDBList permissionObjs = (BasicDBList) result.get("permissions");
                    permissionObjs.forEach(i -> permissions1.add(objToPermission((DBObject) i)));
                }

                subjects.add(new CachedSubject(subjectId, subjectType, subjectData, permissions1, inheritances1));
            });

            return subjects;
        });
    }

    @Override
    public CompletableFuture<Void> clearAllData() {
        return runAsync(() -> {
            this.database.getCollection(COLLECTION_SUBJECTDATA).drop();
            this.database.getCollection(COLLECTION_PERMISSIONS).drop();
            this.database.getCollection(COLLECTION_INHERITANCE).drop();
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> clearSubjectsOfType(String type) {
        return runAsync(() -> {
            BulkWriteOperation opIn = this.database.getCollection(COLLECTION_INHERITANCE).initializeUnorderedBulkOperation();
            BulkWriteOperation opPerm = this.database.getCollection(COLLECTION_PERMISSIONS).initializeUnorderedBulkOperation();
            this.database.getCollection(COLLECTION_SUBJECTDATA).find(new BasicDBObject(FIELD_SUBJECT_TYPE, type)).forEach(s -> {
                String id = (String) s.get(FIELD_SUBJECT_ID);
                List<BasicDBObject> criteria = new ArrayList<>();
                criteria.add(new BasicDBObject(FIELD_INHERITANCE_CHILD, id));
                criteria.add(new BasicDBObject(FIELD_INHERITANCE_PARENT, id));
                opIn.find(new BasicDBObject("$or", criteria)).remove();
                opPerm.find(new BasicDBObject(FIELD_PERMISSION_OWNER_SUBJECT_ID, id)).remove();
            });
            this.database.getCollection(COLLECTION_SUBJECTDATA).remove(new BasicDBObject(FIELD_SUBJECT_TYPE, type));
            opIn.execute();
            opPerm.execute();
            return null;
        });
    }

    @Override
    public CompletableFuture<List<RankLadder>> getRankLadders() {
        return runAsync(() -> {
            List<RankLadder> ladders = new ArrayList<>();
            this.database.getCollection(COLLECTION_RANK_LADDERS).find().forEach(lo -> ladders.add(objToRankLadder(lo)));
            return ladders;
        });
    }

    @Override
    public CompletableFuture<RankLadder> getRankLadder(UUID id) {
        return runAsync(() -> {
            DBObject object = this.database.getCollection(COLLECTION_RANK_LADDERS).findOne(new BasicDBObject(FIELD_LADDER_ID, id.toString()));
            if (object == null)
                return null;
            return objToRankLadder(object);
        });
    }

    @Override
    public CompletableFuture<Void> removeRankLadder(UUID id) {
        return runAsync(() -> {
            this.database.getCollection(COLLECTION_RANK_LADDERS).remove(new BasicDBObject(FIELD_LADDER_ID, id.toString()));
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> addRankLadder(RankLadder ladder) {
        return runAsync(() -> {
            this.database.getCollection(COLLECTION_RANK_LADDERS).update(new BasicDBObject(FIELD_LADDER_ID, ladder.getId().toString()), rankLadderToObj(ladder), true, false);
            return null;
        });

    }

    @Override
    public CompletableFuture<Void> updateRankLadder(RankLadder ladder) {
        return addRankLadder(ladder);
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getServerIndex() {
        return runAsync(() -> {
            Map<Integer, String> map = new HashMap<>();
            this.database.getCollection(COLLECTION_SERVER_INDEX).find().forEach(o -> map.put((int) o.get("_id"), (String) o.get("name")));
            return map;
        });
    }

    @Override
    public CompletableFuture<Void> putServerIndex(int serverId, String serverName) {
        return runAsync(() -> {
            this.database.getCollection(COLLECTION_SERVER_INDEX).update(new BasicDBObject("_id", serverId), new BasicDBObject("_id", serverId).append("name", serverName), true, false);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> removeServerIndex(int serverId) {
        return runAsync(() -> {
            this.database.getCollection(COLLECTION_SERVER_INDEX).remove(new BasicDBObject("_id", serverId));
            return null;
        });
    }

    public BasicDBObject subjectToFullSubjectDataObj(Subject subject) {
        return new BasicDBObject(FIELD_SUBJECT_ID, subject.getSubjectId().toString())
                .append(FIELD_SUBJECT_TYPE, subject.getType())
                .append(FIELD_SUBJECT_DATA, subjectDataToObj(subject.getData()));
    }

    public BasicDBObject permissionToObj(OwnerPermissionPair permissionPair) {
        return new BasicDBObject(FIELD_PERMISSION_ID, permissionPair.getPermissionIdentifier().toString())
                .append(FIELD_PERMISSION_OWNER_SUBJECT_ID, permissionPair.getOwnerSubjectId().toString())
                .append(FIELD_PERMISSION_NAME, permissionPair.getPermissionString())
                .append(FIELD_CONTEXT, permissionPair.getPermission().getContext().toString());
    }

    public BasicDBObject inheritanceToObj(CachedInheritance inheritance) {
        return new BasicDBObject(FIELD_INHERITANCE_ID, inheritance.getChild().toString() + inheritance.getParent().toString())
                .append(FIELD_INHERITANCE_CHILD, inheritance.getChild().toString())
                .append(FIELD_INHERITANCE_PARENT, inheritance.getParent().toString())
                .append(FIELD_INHERITANCE_CHILD_TYPE, inheritance.getChildType())
                .append(FIELD_INHERITANCE_PARENT_TYPE, inheritance.getParentType())
                .append(FIELD_CONTEXT, inheritance.getContext().toString());
    }

    public BasicDBObject subjectDataToObj(SubjectData subjectData) {
        BasicDBObject object = new BasicDBObject();
        Map<String, String> data = SubjectData.getData(subjectData);
        for (String keys : data.keySet()) {
            object.append(keys, data.get(keys));
        }
        return object;
    }

    public GenericSubjectData objToSubjectData(DBObject object) {
        GenericSubjectData subjectData = new GenericSubjectData();
        for (String keys : object.keySet()) {
            subjectData.setData(keys, (String) object.get(keys));
        }
        return subjectData;
    }

    public CachedInheritance objToInheritance(DBObject object) {
        UUID child = UUID.fromString((String) object.get(FIELD_INHERITANCE_CHILD));
        UUID parent = UUID.fromString((String) object.get(FIELD_INHERITANCE_PARENT));
        String childType = (String) object.get(FIELD_INHERITANCE_CHILD_TYPE);
        String parentType = (String) object.get(FIELD_INHERITANCE_PARENT_TYPE);
        ContextSet context = ContextSet.fromString((String) object.get(FIELD_CONTEXT));
        return new CachedInheritance(child, parent, childType, parentType, context);
    }

    public PPermission objToPermission(DBObject object) {
        String permission = (String) object.get(FIELD_PERMISSION_NAME);
        UUID id = UUID.fromString((String) object.get(FIELD_PERMISSION_ID));
        ContextSet context = ContextSet.fromString((String) object.get(FIELD_CONTEXT));
        return new PPermission(permission, context, id);
    }

    public RankLadder objToRankLadder(DBObject object){
        UUID id = UUID.fromString((String) object.get(FIELD_LADDER_ID));
        Map<String, String> data = MapUtil.stringToMap((String) object.get(FIELD_LADDER_DATA));
        ConcurrentMap<String, String> concData = new ConcurrentHashMap<>(data);
        ContextSet contexts = ContextSet.fromString((String) object.get(FIELD_LADDER_CONTEXT));
        GravSerializer serializer = new GravSerializer((String) object.get(FIELD_LADDER_GROUPS));
        List<UUID> groups = new ArrayList<>();
        int num = serializer.readInt();
        while(num-- > 0){
            groups.add(serializer.readUUID());
        }
        return new RankLadder(id, groups, contexts, null, concData);
    }

    public BasicDBObject rankLadderToObj(RankLadder ladder){
        BasicDBObject object = new BasicDBObject(FIELD_LADDER_ID, ladder.getId().toString());
        object.put(FIELD_LADDER_DATA, ladder.getDataEncoded());
        GravSerializer serializer = new GravSerializer();
        List<UUID> groups = ladder.getGroups();
        serializer.writeInt(groups.size());
        groups.forEach(serializer::writeUUID);
        object.put(FIELD_LADDER_GROUPS, serializer.toString());
        object.put(FIELD_LADDER_CONTEXT, ladder.getContext().toString());
        return object;
    }

    @Override
    public CompletableFuture<Boolean> testBackendConnection() {
        return runAsync(() -> connected);
    }

    @Override
    public boolean isRemote() {
        return true;
    }
}
