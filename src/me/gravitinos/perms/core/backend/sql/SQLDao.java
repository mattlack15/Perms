package me.gravitinos.perms.core.backend.sql;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.cache.OwnerPermissionPair;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.*;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

public class SQLDao implements AutoCloseable{

    volatile int transactionCounter = 0;

    private Connection connection;

    private static final String TABLE_PERMISSIONS = "perms_permissions";
    private static final String TABLE_SUBJECTDATA = "perms_subjectdata";
    private static final String TABLE_INHERITANCE = "perms_inheritance";
    private static final String TABLE_SERVER_INDEX = "perms_server_index";

    volatile int holdOpen = 0;

    public SQLDao(SQLHandler handler) {
        this.connection = handler.getConnection();
    }

    /**
     * Executes Something which in an SQL transaction (It ensure that either ALL the commands send to the SQL server are executed or none of them are)
     *
     * @param func The function to run
     * @param <T>  Return type
     * @return Whatever you set it to return
     * @throws SQLException Because this is using SQL
     */
    public <T> T executeInTransaction(Supplier<T> func) throws SQLException {
        transactionCounter++;
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T out = func.get();
            if (--transactionCounter <= 0) {
                connection.commit();
            }
            return out;
        } finally {
            if (transactionCounter <= 0) {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    //Statement getters

    /**
     * Gets statement to create permission table
     *
     * @return String containing the statement
     */
    protected String getPermissionTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_PERMISSIONS + " (OwnerSubjectId varchar(48), Permission varchar(256), PermissionIdentifier varchar(48), Context varchar(256))";
    }

    /**
     * Gets statement to create Subject Data table
     *
     * @return String containing the statement
     */
    protected String getSubjectDataTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_SUBJECTDATA + " (SubjectId varchar(48), Type varchar(32), Data varchar(1024))";
    }

    /**
     * Gets statement to create Inheritance table
     *
     * @return String containing the statement
     */
    protected String getInheritanceTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_INHERITANCE + " (Child varchar(48), Parent varchar(48), ChildType varchar(64), ParentType varchar(64), Context varchar(256))";
    }

    /**
     * Gets statement to create Server Index table
     *
     * @return String containing the statement
     */
    protected String getServerIndexTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_SERVER_INDEX + " (ServerId int, ServerName varchar(128))";
    }

    protected String getSubjectDataFromSubjectIdQuery() {
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " WHERE SubjectId=?";
    }

    /**
     * Gets the statement to query subject data from a type
     *
     * @return
     */
    protected String getSubjectDataFromTypeQuery() {
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " WHERE Type=?";
    }

    /**
     * Gets the statement to delete subject data by some subject id
     *
     * @return
     */
    protected String getDeleteSubjectDataBySubjectIdUpdate() {
        return "DELETE FROM " + TABLE_SUBJECTDATA + " WHERE SubjectId=?";
    }

    /**
     * Gets the statement to delete subject data by some type
     *
     * @return
     */
    protected String getDeleteSubjectDataByTypeUpdate() {
        return "DELETE FROM " + TABLE_SUBJECTDATA + " WHERE Type=?";
    }

    /**
     * Gets the statement to insert subject data
     *
     * @return
     */
    protected String getInsertSubjectDataUpdate() {
        return "INSERT INTO " + TABLE_SUBJECTDATA + " (SubjectId, Type, Data) VALUES (?, ?, ?)";
    }

    /**
     * Gets the statement to query inheritance from a specific child
     *
     * @return
     */
    protected String getInheritancesFromChildQuery() {
        return "SELECT * FROM " + TABLE_INHERITANCE + " WHERE Child=?";
    }

    /**
     * Gets the statement to delete inheritance by a child and parent
     *
     * @return
     */
    protected String getDeleteInheritanceByChildAndParentUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE Child=? AND Parent=?";
    }

    /**
     * Gets the statement to delete inheritance by a child
     *
     * @return
     */
    protected String getDeleteInheritanceByChildUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE Child=?";
    }

    /**
     * Gets the statement to delete inheritance by a parent
     *
     * @return
     */
    protected String getDeleteInheritanceByParentUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE Parent=?";
    }

    /**
     * Gets the statement to delete inheritance by some child or parent
     *
     * @return
     */
    protected String getDeleteInheritanceByChildOrParentUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE Child=? OR Parent=?";
    }

    /**
     * Self explanatory
     * @return
     */
    protected String getDeleteInheritanceByChildTypeUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE ChildType=?";
    }

    /**
     * Self explanatory
     * @return
     */
    protected String getDeleteInheritanceByParentTypeUpdate() {
        return "DELETE FROM " + TABLE_INHERITANCE + " WHERE ParentType=?";
    }

    /**
     * Gets the statement to insert inheritance
     *
     * @return
     */
    protected String getInsertInheritanceUpdate() {
        return "INSERT INTO " + TABLE_INHERITANCE + " (Child, Parent, ChildType, ParentType, Context) VALUES (?, ?, ?, ?, ?)";
    }

    /**
     * Gets the statement to clear subject data table
     * @return
     */
    protected String getClearSubjectDataTableUpdate(){
        return "DELETE FROM " + TABLE_SUBJECTDATA;
    }

    /**
     * Gets the statement to clear inheritance table
     * @return
     */
    protected String getClearInheritanceTableUpdate(){
        return "DELETE FROM " + TABLE_INHERITANCE;
    }

    /**
     * Gets the statement to clear permissions table
     * @return
     */
    protected String getClearPermissionsTableUpdate(){
        return "DELETE FROM " + TABLE_PERMISSIONS;
    }

    /**
     * Gets the statement to get permissions from an owner subjectId
     *
     * @return
     */
    protected String getPermissionsFromOwnerSubjectIdQuery() {
        return "SELECT * FROM " + TABLE_PERMISSIONS + " WHERE OwnerSubjectId=?";
    }

    /**
     * Gets the statement to delete permissions by an owner subjectId and a permission string
     *
     * @return
     */
    protected String getDeletePermissionByOwnerSubjectIdAndPermissionUpdate() {
        return "DELETE FROM " + TABLE_PERMISSIONS + " WHERE OwnerSubjectId=? AND Permission=?";
    }

    protected String getDeletePermissionByPermissionIdentifierUpdate(){
        return "DELETE FROM " + TABLE_PERMISSIONS + " WHERE PermissionIdentifier=?";
    }

    /**
     * Gets the statement to delete permissions by an owner subjectId
     *
     * @return
     */
    protected String getDeletePermissionByOwnerSubjectIdUpdate() {
        return "DELETE FROM " + TABLE_PERMISSIONS + " WHERE OwnerSubjectId=?";
    }

    protected String getInsertPermissionUpdate() {
        return "INSERT INTO " + TABLE_PERMISSIONS + " (OwnerSubjectId, Permission, PermissionIdentifier, Context) VALUES (?, ?, ?, ?)";
    }

    protected String getPermissionsFromTypeJoinSubjectData(){
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " LEFT JOIN " + TABLE_PERMISSIONS + " ON SubjectId=OwnerSubjectId WHERE Type=?";
    }

    protected String getInheritancesFromTypeJoinSubjectData(){
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " LEFT JOIN " + TABLE_INHERITANCE + " ON SubjectId=Child WHERE Type=?";
    }

    //OUTDATED -> No longer using identifiers, so no need to rename
//    protected String getRenameIdentifiersInPermissionsUpdate(){
//        return "UPDATE " + TABLE_PERMISSIONS + " SET OwnerSubjectId=? WHERE OwnerSubjectId=?";
//    }
//
//    protected String getRenameChildsInInheritancesUpdate(){
//        return "UPDATE " + TABLE_INHERITANCE + " SET Child=? WHERE Child=?";
//    }
//
//    protected String getRenameParentsInInheritancesUpdate(){
//        return "UPDATE " + TABLE_INHERITANCE + " SET Parent=? WHERE Parent=?";
//    }

    protected String getServerIndexQuery(){
        return "SELECT * FROM " + TABLE_SERVER_INDEX;
    }

    protected String getDeleteServerIndexQuery(){
        return "DELETE FROM " + TABLE_SERVER_INDEX + " WHERE ServerId=?";
    }


    protected String getPutServerIndexUpdate(){
        return "INSERT INTO " + TABLE_SERVER_INDEX + " (ServerId, ServerName) VALUES (?, ?)";
    }

    //

    /**
     * Prepares a statement
     * @param statement SQL String
     * @return Prepared Statement
     * @throws SQLException
     */
    protected PreparedStatement prepareStatement(String statement) throws SQLException {
        return connection.prepareStatement(statement);
    }

    //

        //Bulk Ops

    public void clearTables() throws SQLException{

        SQLException e = executeInTransaction(() -> {
            try {
                prepareStatement(this.getClearInheritanceTableUpdate()).executeUpdate();
                prepareStatement(this.getClearPermissionsTableUpdate()).executeUpdate();
                prepareStatement(this.getClearSubjectDataTableUpdate()).executeUpdate();
                return null;
            }catch(SQLException ex){
                ex.printStackTrace();
                return ex;
            }
        });
        if(e == null){
            return;
        } else {
            throw e;
        }

    }

    public CachedSubject getSubject(@NotNull UUID subjectId) throws SQLException {
        GenericSubjectData data = this.getSubjectData(subjectId);
        if(data == null){
            return null;
        }
        String type = data.getType();
        return new CachedSubject(subjectId, type, data, this.getPermissions(subjectId), this.getInheritances(subjectId));
    }

    public void addSubject(@NotNull Subject subject) throws SQLException {
        if(subjectExists(subject.getSubjectId())){
            this.removeSubject(subject.getSubjectId(), false);
        }

        this.setSubjectData(subject.getSubjectId(), subject.getData(), subject.getType());
        this.addPermissions(subject.getSubjectId(), Subject.getPermissions(subject));
        ArrayList<CachedInheritance> inheritances = new ArrayList<>();
        Subject.getInheritances(subject).forEach(i -> inheritances.add(new CachedInheritance(i.getChild().getSubjectId(), i.getParent().getSubjectId(), i.getChild().getType(), i.getParent().getType(), i.getContext())));
        this.addInheritances(inheritances);
    }

    public boolean subjectExists(@NotNull UUID subjectId) throws SQLException {
        PreparedStatement s = prepareStatement(getSubjectDataFromSubjectIdQuery());
        s.setString(1, subjectId.toString());
        ResultSet r = s.executeQuery();
        return r.next();
    }

    public void removeSubject(@NotNull UUID subjectId, boolean deleteInheritanceToChildren) throws SQLException {
        this.removeAllInheritances(subjectId);

        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerSubjectIdUpdate());
        s.setString(1, subjectId.toString());
        s.executeUpdate();

        s = prepareStatement(this.getDeleteSubjectDataBySubjectIdUpdate());
        s.setString(1, subjectId.toString());
        s.executeUpdate();

        s = prepareStatement(deleteInheritanceToChildren ? this.getDeleteInheritanceByChildOrParentUpdate() : this.getDeleteInheritanceByChildTypeUpdate());
        s.setString(1, subjectId.toString());
        s.executeUpdate();

    }

    public void removePermission(@NotNull UUID permissionIdentifier) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByPermissionIdentifierUpdate());

        s.setString(1, permissionIdentifier.toString());

        s.executeUpdate();
    }

    public void removePermissionsExact(ArrayList<UUID> permissionIdentifiers) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByPermissionIdentifierUpdate());

        permissionIdentifiers.removeIf(Objects::isNull);
        for(UUID id : permissionIdentifiers) {
            s.setString(1, id.toString());
            s.addBatch();
        }

        s.executeBatch();
    }

    public ArrayList<CachedSubject> getAllSubjectsOfType(String type) throws SQLException{
        PreparedStatement s = prepareStatement(this.getPermissionsFromTypeJoinSubjectData());
        s.setString(1, type);
        ResultSet results = s.executeQuery();

        Map<UUID, CachedSubject> subjectMap = new HashMap<>();

        while(results.next()){
            String subIdStr = results.getString("SubjectId");
            if(subIdStr == null){
                continue;
            }
            UUID subjectId = UUID.fromString(results.getString("SubjectId"));
            CachedSubject sub = subjectMap.get(subjectId);
            if(sub == null){
                sub = new CachedSubject(subjectId, type, SubjectData.fromString(results.getString("Data")), new ArrayList<>(), new ArrayList<>());
                subjectMap.put(subjectId, sub);
            }
            if(results.getString("Permission") == null){
                continue;
            }
            sub.getPermissions().add(new PPermission(results.getString("Permission"), Context.fromString(results.getString("Context")), UUID.fromString(results.getString("PermissionIdentifier") != null ? results.getString("PermissionIdentifier") : UUID.randomUUID().toString())));
        }

        s = prepareStatement(this.getInheritancesFromTypeJoinSubjectData());
        s.setString(1, type);
        results = s.executeQuery();
        
        while(results.next()){
            String subIdStr = results.getString("SubjectId");
            if(subIdStr == null){
                continue;
            }
            UUID subjectId = UUID.fromString(results.getString("SubjectId"));
            CachedSubject sub = subjectMap.get(subjectId);
            if(sub == null){
                sub = new CachedSubject(subjectId, type, SubjectData.fromString(results.getString("Data")), new ArrayList<>(), new ArrayList<>());
                subjectMap.put(subjectId, sub);
            }
            sub.getInheritances().add(new CachedInheritance(UUID.fromString(results.getString("Child")), UUID.fromString(results.getString("Parent")), results.getString("ChildType"), results.getString("ParentType"), Context.fromString(results.getString("Context"))));
        }

        return Lists.newArrayList(subjectMap.values());
    }

    public Map<Integer, String> getServerIndex() throws SQLException {
        PreparedStatement s = prepareStatement(this.getServerIndexQuery());
        ResultSet results = s.executeQuery();
        Map<Integer, String> index = new HashMap<>();
        while(results.next()){
            index.put(results.getInt(1), results.getString(2));
        }
        return index;
    }

    public void putServerIndex(int serverId, String serverName) throws SQLException {
        PreparedStatement s = prepareStatement(this.getPutServerIndexUpdate());
        s.setInt(1, serverId);
        s.setString(2, serverName);
        s.executeUpdate();
    }

    public void removeServerIndex(int serverId) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteServerIndexQuery());
        s.setInt(1, serverId);
        s.executeUpdate();
    }

    public void addSubjects(ArrayList<Subject> subjects) throws SQLException {
        SQLException e = executeInTransaction(() -> {
            try{
                PreparedStatement sData = prepareStatement(this.getInsertSubjectDataUpdate());
                PreparedStatement sInheritances = prepareStatement(this.getInsertInheritanceUpdate());
                PreparedStatement sPermissions = prepareStatement(this.getInsertPermissionUpdate());

                for(Subject subject : subjects){
                    if(subject == null){
                        continue;
                    }
                    sData.setString(1, subject.getSubjectId().toString());
                    sData.setString(2, subject.getType());
                    sData.setString(3, subject.getData().toString());
                    sData.addBatch();

                    for(Inheritance inheritances : Subject.getInheritances(subject)){
                        if(inheritances == null){
                            continue;
                        }
                        sInheritances.setString(1, subject.getSubjectId().toString());
                        sInheritances.setString(2, inheritances.getParent().getSubjectId().toString());
                        sInheritances.setString(3, inheritances.getChild().getType());
                        sInheritances.setString(4, inheritances.getParent().getType());
                        sInheritances.setString(5, inheritances.getContext().toString());
                        sInheritances.addBatch();
                    }

                    for(PPermission permissions : Subject.getPermissions(subject)){
                        if(permissions == null){
                            continue;
                        }
                        sPermissions.setString(1, subject.getSubjectId().toString());
                        sPermissions.setString(2, permissions.getPermission());
                        sPermissions.setString(3, permissions.getPermissionIdentifier().toString());
                        sPermissions.setString(4, permissions.getContext().toString());
                        sPermissions.addBatch();
                    }
                }

                sData.executeBatch();
                sInheritances.executeBatch();
                sPermissions.executeBatch();

            } catch(SQLException ex){
                return ex;
            }
            return null;
        });
        if(e == null){
            return;
        } else {
            throw e;
        }
    }

    public void removeSubjects(ArrayList<UUID> subjects) throws SQLException {
        SQLException e = executeInTransaction(() -> {
            try {
                PreparedStatement sData = prepareStatement(this.getDeleteSubjectDataBySubjectIdUpdate());
                PreparedStatement sInheritances = prepareStatement(this.getDeleteInheritanceByChildOrParentUpdate());
                PreparedStatement sPermissions = prepareStatement(this.getDeletePermissionByOwnerSubjectIdUpdate());

                for (UUID subject : subjects) {
                    sData.setString(1, subject.toString());
                    sData.addBatch();

                    sInheritances.setString(1, subject.toString());
                    sInheritances.addBatch();

                    sPermissions.setString(1, subject.toString());
                    sPermissions.addBatch();
                }
                sData.executeBatch();
                sInheritances.executeBatch();
                sPermissions.executeBatch();

                return null;

            }catch(SQLException ex){
                return ex;
            }
        });
        if(e == null){
            return;
        } else {
            throw e;
        }
    }

    public void removeInheritances(ArrayList<CachedInheritance> inheritances) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteInheritanceByChildAndParentUpdate());

        for(CachedInheritance inheritance : inheritances){
            s.setString(1, inheritance.getChild().toString());
            s.setString(2, inheritance.getParent().toString());

            s.addBatch();
        }

        s.executeBatch();
    }

    public void addInheritances(@NotNull ArrayList<CachedInheritance> inheritances) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertInheritanceUpdate());

        inheritances.removeIf(Objects::isNull);
        for(CachedInheritance inheritance : inheritances){
            s.setString(1, inheritance.getChild().toString());
            s.setString(2, inheritance.getParent().toString());
            s.setString(3, inheritance.getChildType());
            s.setString(4, inheritance.getParentType());
            s.setString(5, inheritance.getContext().toString());

            s.addBatch();
        }

        s.executeBatch();
    }

    public void addPermissions(@NotNull ArrayList<OwnerPermissionPair> permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertPermissionUpdate());

        permissions.removeIf(Objects::isNull);
        for(OwnerPermissionPair pair : permissions){
            s.setString(1, pair.getOwnerSubjectId().toString());
            s.setString(2, pair.getPermission().getPermission());
            s.setString(3, pair.getPermissionIdentifier().toString());
            s.setString(4, pair.getPermission().getContext().toString());

            s.addBatch();
        }

        s.executeBatch();
    }

    public void removeAllPermissions(@NotNull UUID subjectId) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerSubjectIdUpdate());
        s.setString(1, subjectId.toString());
        s.executeUpdate();
    }

    public void removePermissions(@NotNull ArrayList<OwnerPermissionPair> permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerSubjectIdAndPermissionUpdate());

        permissions.removeIf(Objects::isNull);
        for(OwnerPermissionPair pair : permissions){
            s.setString(1, pair.getOwnerSubjectId().toString());
            s.setString(2, pair.getPermissionString());

            s.addBatch();
        }

        s.executeBatch();
    }

        //Subject data

    public void setSubjectData(@NotNull UUID subjectId, @NotNull SubjectData data, @NotNull String type) throws SQLException {
        this.removeSubjectData(subjectId);

        PreparedStatement s = prepareStatement(this.getInsertSubjectDataUpdate());

        s.setString(1, subjectId.toString());
        s.setString(2, type);
        s.setString(3, data.toString());

        s.executeUpdate();
    }

    public void removeSubjectData(@NotNull UUID subjectId) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteSubjectDataBySubjectIdUpdate());

        s.setString(1, subjectId.toString());

        s.executeUpdate();
    }

    public GenericSubjectData getSubjectData(@NotNull UUID subjectId) throws SQLException {
        PreparedStatement s = prepareStatement(this.getSubjectDataFromSubjectIdQuery());

        s.setString(1, subjectId.toString());

        ResultSet results = s.executeQuery();

        if(!results.next()){
            return null;
        }

        GenericSubjectData data = SubjectData.fromString(results.getString("Data"));
        if(data == null){
            return null;
        }

        data.setType(results.getString("Type"));

        return data;
    }

        //Inheritances

    public void removeAllInheritances (@NotNull UUID childOrParent) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteInheritanceByChildUpdate());

        s.setString(1, childOrParent.toString());

        s.executeUpdate();
    }

    public void removeAllInheritancesIncludingChilds (@NotNull UUID childOrParent) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteInheritanceByChildOrParentUpdate());

        s.setString(1, childOrParent.toString());
        s.setString(2, childOrParent.toString());

        s.executeUpdate();
    }

    public void removeInheritance(@NotNull UUID child, @NotNull UUID parent) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteInheritanceByChildAndParentUpdate());

        s.setString(1, child.toString());
        s.setString(2, parent.toString());

        s.executeUpdate();
    }

    public void addInheritance(@NotNull UUID child, @NotNull UUID parent, @NotNull String childType, @NotNull String parentType, @NotNull Context context) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertInheritanceUpdate());

        s.setString(1, child.toString());
        s.setString(2, parent.toString());
        s.setString(3, childType);
        s.setString(4, parentType);
        s.setString(5, context.toString());

        s.executeUpdate();
    }

    public ArrayList<CachedInheritance> getInheritances(@NotNull UUID child) throws SQLException {
        ArrayList<CachedInheritance> out = new ArrayList<>();

        PreparedStatement s = prepareStatement(this.getInheritancesFromChildQuery());

        s.setString(1, child.toString());

        ResultSet results = s.executeQuery();

        while(results.next()){
            out.add(new CachedInheritance(child, UUID.fromString(results.getString("Parent")), results.getString("ChildType"), results.getString("ParentType"), Context.fromString(results.getString("Context"))));
        }

        return out;
    }


        //Permissions

    public ArrayList<PPermission> getPermissions(@NotNull UUID ownerSubjectId) throws SQLException {
        ArrayList<PPermission> perms = new ArrayList<>();

        PreparedStatement s = prepareStatement(this.getPermissionsFromOwnerSubjectIdQuery());

        s.setString(1, ownerSubjectId.toString());

        ResultSet r = s.executeQuery();

        while(r.next()){
            PPermission perm = new PPermission(r.getString("Permission"), Context.fromString(r.getString("Context")), UUID.fromString(r.getString("PermissionIdentifier") != null ? r.getString("PermissionIdentifier") : UUID.randomUUID().toString()));
            perms.add(perm);
        }

        return perms;
    }

    public void addPermissions(@NotNull UUID ownerSubjectId, @NotNull ImmutablePermissionList permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertPermissionUpdate());

        for (PPermission perm : permissions) {
            s.setString(1, ownerSubjectId.toString());
            s.setString(2, perm.getPermission());
            s.setString(3, perm.getPermissionIdentifier().toString());
            s.setString(4, perm.getContext().toString());

            s.addBatch();
        }

        s.executeBatch();
    }

    public void addPermission(@NotNull UUID ownerSubjectId, @NotNull PPermission permission) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertPermissionUpdate());

        s.setString(1, ownerSubjectId.toString());
        s.setString(2, permission.getPermission());
        s.setString(3, permission.getPermissionIdentifier().toString());
        s.setString(4, permission.getContext().toString());

        s.executeUpdate();
    }

    public void removePermissions(@NotNull UUID ownerSubjectId, @NotNull ArrayList<String> permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerSubjectIdAndPermissionUpdate());

        permissions.removeIf(Objects::isNull);
        for (String perms : permissions) {
            s.setString(1, ownerSubjectId.toString());
            s.setString(2, perms);

            s.addBatch();
        }

        s.executeBatch();
    }

    public void removePermission(@NotNull UUID ownerSubjectId, @NotNull String permission) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerSubjectIdAndPermissionUpdate());

        s.setString(1, ownerSubjectId.toString());
        s.setString(2, permission);

        s.executeUpdate();
    }

    public void removeSubjectsOfType(@NotNull String type) throws SQLException{
        PreparedStatement s = prepareStatement(this.getSubjectDataFromTypeQuery());
        s.setString(1, type);
        ResultSet set = s.executeQuery();

        s = prepareStatement(this.getDeleteSubjectDataByTypeUpdate());
        s.setString(1, type);
        s.executeUpdate();

        s = prepareStatement(this.getDeletePermissionByOwnerSubjectIdUpdate());
        while(set.next()){
            s.setString(1, set.getString("SubjectId"));
            s.addBatch();
        }
        s.executeBatch();

        s = prepareStatement(this.getDeleteInheritanceByChildTypeUpdate());
        s.setString(1, type);
        s.executeUpdate();

        s = prepareStatement(this.getDeleteInheritanceByParentTypeUpdate());
        s.setString(1, type);
        s.executeUpdate();
    }

    public boolean checkConverterIdentifierToSubjectId() throws SQLException {
        PreparedStatement statement = prepareStatement(this.getSubjectDataFromTypeQuery());
        statement.setString(1, Subject.GROUP);
        ResultSet set = statement.executeQuery();
        if(set.next()){
            try{
                set.getRef("Identifier");
            } catch(SQLException e){
                return true;
            }
        }
        return false;
    }

    public boolean convertIdentifierToSubjectId() throws SQLException {

        //Table modifications
        PreparedStatement changeIdentifierSD = prepareStatement("ALTER TABLE " + TABLE_SUBJECTDATA + " CHANGE Identifier SubjectId varchar(48)");
        PreparedStatement changeTypeSD = prepareStatement("ALTER TABLE " + TABLE_SUBJECTDATA + " MODIFY Type varchar(32)");
        PreparedStatement changeDataSD = prepareStatement("ALTER TABLE " + TABLE_SUBJECTDATA + " MODIFY Data varchar(1024)");


        PreparedStatement changePermIdentifierP = prepareStatement("ALTER TABLE " + TABLE_PERMISSIONS + " MODIFY PermissionIdentifier varchar(48)");
        PreparedStatement changeOwnerIdentifierP = prepareStatement("ALTER TABLE " + TABLE_PERMISSIONS + " CHANGE OwnerIdentifier OwnerSubjectId varchar(48)");

        PreparedStatement changeChildI = prepareStatement("ALTER TABLE " + TABLE_INHERITANCE + " MODIFY Child varchar(48)");
        PreparedStatement changeParentI = prepareStatement("ALTER TABLE " + TABLE_INHERITANCE + " MODIFY Parent varchar(48)");

        changeIdentifierSD.executeUpdate();
        changeTypeSD.executeUpdate();
        changeDataSD.executeUpdate();

        changePermIdentifierP.executeUpdate();
        changeOwnerIdentifierP.executeUpdate();

        changeChildI.executeUpdate();
        changeParentI.executeUpdate();

        //Changing identifiers to subject ids in inheritances and permissions
        Map<String, UUID> mapIdentifierSubjectId = new HashMap<>();

        //Get and change all GROUP data, permissions, and inheritances, no need for user data, because their identifiers just become their subject ids
        PreparedStatement getAllGroupData = prepareStatement(this.getSubjectDataFromTypeQuery());

        PreparedStatement changeGroupData = prepareStatement("UPDATE " + TABLE_SUBJECTDATA + " SET SubjectId=? WHERE SubjectId=?");
        PreparedStatement changeInheritanceData1 = prepareStatement("UPDATE " + TABLE_INHERITANCE + " SET Child=? WHERE Child=?");
        PreparedStatement changeInheritanceData2 = prepareStatement("UPDATE " + TABLE_INHERITANCE + " SET Parent=? WHERE Parent=?");
        PreparedStatement changePermissionData = prepareStatement("UPDATE " + TABLE_PERMISSIONS + " SET OwnerSubjectId=? WHERE OwnerSubjectId=?");

        getAllGroupData.setString(1, Subject.GROUP);

        ResultSet set = getAllGroupData.executeQuery();

        while(set.next()){
            String identifier = set.getString(1);
            if(!mapIdentifierSubjectId.containsKey(identifier)) {
                UUID id = UUID.randomUUID(); //Create new group id
                mapIdentifierSubjectId.put(identifier, id);

                changeGroupData.setString(1, id.toString());
                changeGroupData.setString(2, identifier);
                changeGroupData.addBatch();

                changeInheritanceData1.setString(1, id.toString());
                changeInheritanceData1.setString(2, identifier);
                changeInheritanceData1.addBatch();

                changeInheritanceData2.setString(1, id.toString());
                changeInheritanceData2.setString(2, identifier);
                changeInheritanceData2.addBatch();

                changePermissionData.setString(1, id.toString());
                changePermissionData.setString(2, identifier);
                changePermissionData.addBatch();
            }
        }

        return !checkConverterIdentifierToSubjectId();
    }

    public void initializeTables() throws SQLException {
        this.executeInTransaction(() -> {
            try {
                PreparedStatement s = this.prepareStatement(this.getInheritanceTableCreationUpdate());
                s.executeUpdate();
                s.close();
                s = this.prepareStatement(this.getPermissionTableCreationUpdate());
                s.executeUpdate();
                s.close();
                s = this.prepareStatement(this.getSubjectDataTableCreationUpdate());
                s.executeUpdate();
                s.close();
                s = this.prepareStatement(this.getServerIndexTableCreationUpdate());
                s.executeUpdate();
                s.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public void close() throws SQLException {
        if(this.holdOpen <= 0) {
            connection.close();
        }
    }
}
