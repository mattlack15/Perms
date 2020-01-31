package me.gravitinos.perms.core.backend.sql;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.cache.OwnerPermissionPair;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class SQLDao {

    volatile int transactionCounter = 0;

    private Connection connection;

    private static final String TABLE_PERMISSIONS = "perms_permissions";
    private static final String TABLE_SUBJECTDATA = "perms_subjectdata";
    private static final String TABLE_INHERITANCE = "perms_inheritance";

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
        return "CREATE TABLE IF NOT EXISTS " + TABLE_PERMISSIONS + " (OwnerIdentifier varchar(512), Permission varchar(512), PermissionIdentifier varchar(128), Expiration varchar(256), Context varchar(1536))";
    }

    /**
     * Gets statement to create Subject Data table
     *
     * @return String containing the statement
     */
    protected String getSubjectDataTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_SUBJECTDATA + " (Identifier varchar(512), Type varchar(64), Data varchar(10240))";
    }

    /**
     * Gets statement to create Inheritance table
     *
     * @return String containing the statement
     */
    protected String getInheritanceTableCreationUpdate() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_INHERITANCE + " (Child varchar(512), Parent varchar(512), ChildType varchar(64), ParentType varchar(64), Context varchar(1536))";
    }

    /**
     * Gets statement to query subject data from a specified identifier
     *
     * @return
     */
    protected String getSubjectDataFromIdentifierQuery() {
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " WHERE Identifier=?";
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
     * Gets the statement to delete subject data by some identifier
     *
     * @return
     */
    protected String getDeleteSubjectDataByIdentifierUpdate() {
        return "DELETE FROM " + TABLE_SUBJECTDATA + " WHERE Identifier=?";
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
        return "INSERT INTO " + TABLE_SUBJECTDATA + " (Identifier, Type, Data) VALUES (?, ?, ?)";
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
     * Gets the statement to get permissions from an owner identifier
     *
     * @return
     */
    protected String getPermissionsFromOwnerIdentifierQuery() {
        return "SELECT * FROM " + TABLE_PERMISSIONS + " WHERE OwnerIdentifier=?";
    }

    /**
     * Gets the statement to delete permissions by an owner identifier and a permission string
     *
     * @return
     */
    protected String getDeletePermissionByOwnerIdentifierAndPermissionUpdate() {
        return "DELETE FROM " + TABLE_PERMISSIONS + " WHERE OwnerIdentifier=? AND Permission=?";
    }

    protected String getDeletePermissionByPermissionIdentifierUpdate(){
        return "DELETE FROM " + TABLE_PERMISSIONS + " WHERE PermissionIdentifier=?";
    }

    /**
     * Gets the statement to delete permissions by an owner identifier
     *
     * @return
     */
    protected String getDeletePermissionByOwnerIdentifierUpdate() {
        return "DELETE FROM " + TABLE_PERMISSIONS + " WHERE OwnerIdentifier=?";
    }

    protected String getInsertPermissionUpdate() {
        return "INSERT INTO " + TABLE_PERMISSIONS + " (OwnerIdentifier, Permission, PermissionIdentifier, Expiration, Context) VALUES (?, ?, ?, ?, ?)";
    }

    protected String getPermissionsFromTypeJoinSubjectData(){
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " LEFT JOIN " + TABLE_PERMISSIONS + " ON Identifier=OwnerIdentifier WHERE Type=?";
    }

    protected String getInheritancesFromTypeJoinSubjectData(){
        return "SELECT * FROM " + TABLE_SUBJECTDATA + " LEFT JOIN " + TABLE_INHERITANCE + " ON Identifier=OwnerIdentifier WHERE Type=?";
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
                prepareStatement(this.getClearInheritanceTableUpdate()).executeUpdate();
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

    public CachedSubject getSubject(String identifier) throws SQLException {
        GenericSubjectData data = this.getSubjectData(identifier);
        if(data == null){
            return null;
        }
        String type = data.getType();
        return new CachedSubject(identifier, type, data, this.getPermissions(identifier), this.getInheritances(identifier));
    }

    public void addSubject(Subject subject) throws SQLException {
        if(subjectExists(subject.getIdentifier())){
            this.removeSubject(subject.getIdentifier());
        }

        this.setSubjectData(subject.getIdentifier(), subject.getData(), subject.getType());
        this.addPermissions(subject.getIdentifier(), Subject.getPermissions(subject));
        ArrayList<CachedInheritance> inheritances = new ArrayList<>();
        Subject.getInheritances(subject).forEach(i -> inheritances.add(new CachedInheritance(i.getChild().getIdentifier(), i.getParent().getIdentifier(), i.getChild().getType(), i.getParent().getType(), i.getContext())));
        this.addInheritances(inheritances);
    }

    public boolean subjectExists(String identifier) throws SQLException {
        PreparedStatement s = prepareStatement(getSubjectDataFromIdentifierQuery());
        s.setString(1, identifier);
        ResultSet r = s.executeQuery();
        return r.next();
    }

    public void removeSubject(String identifier) throws SQLException {
        this.removeAllInheritances(identifier);

        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerIdentifierUpdate());
        s.setString(1, identifier);
        s.executeUpdate();

        s = prepareStatement(this.getDeleteSubjectDataByIdentifierUpdate());
        s.setString(1, identifier);
        s.executeUpdate();
    }

    public void removePermission(UUID permissionIdentifier) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByPermissionIdentifierUpdate());

        s.setString(1, permissionIdentifier.toString());

        s.executeUpdate();
    }

    public void removePermissionsExact(ArrayList<UUID> permissionIdentifiers) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByPermissionIdentifierUpdate());

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

        Map<String, CachedSubject> subjectMap = new HashMap<>();

        while(results.next()){
            String identifier = results.getString("Identifier");
            CachedSubject sub = subjectMap.get(identifier);
            if(sub == null){
                sub = new CachedSubject(identifier, type, SubjectData.fromString(results.getString("Data")), new ArrayList<>(), new ArrayList<>());
                subjectMap.put(identifier, sub);
            }
            sub.getPermissions().add(new PPermission(results.getString("Permission"), Context.fromString(results.getString("Context")), Long.parseLong(results.getString("Expiration")), UUID.fromString(results.getString("PermissionIdentifier"))));
        }

        s = prepareStatement(this.getInheritancesFromTypeJoinSubjectData());
        s.setString(1, type);
        results = s.executeQuery();
        
        while(results.next()){
            String identifier = results.getString("Identifier");
            CachedSubject sub = subjectMap.get(identifier);
            if(sub == null){
                sub = new CachedSubject(identifier, type, SubjectData.fromString(results.getString("Data")), new ArrayList<>(), new ArrayList<>());
                subjectMap.put(identifier, sub);
            }
            sub.getInheritances().add(new CachedInheritance(results.getString("Child"), results.getString("Parent"), results.getString("ChildType"), results.getString("ParentType"), Context.fromString(results.getString("Context"))));
        }

        return Lists.newArrayList(subjectMap.values());
    }

    public void addSubjects(ArrayList<Subject> subjects) throws SQLException {
        SQLException e = executeInTransaction(() -> {
            try{
                PreparedStatement sData = prepareStatement(this.getInsertSubjectDataUpdate());
                PreparedStatement sInheritances = prepareStatement(this.getInsertInheritanceUpdate());
                PreparedStatement sPermissions = prepareStatement(this.getInsertPermissionUpdate());

                for(Subject subject : subjects){
                    sData.setString(1, subject.getIdentifier());
                    sData.setString(2, subject.getType());
                    sData.setString(3, subject.getData().toString());
                    sData.addBatch();

                    for(Inheritance inheritances : Subject.getInheritances(subject)){
                        sInheritances.setString(1, subject.getIdentifier());
                        sInheritances.setString(2, inheritances.getParent().getIdentifier());
                        sInheritances.setString(3, inheritances.getChild().getType());
                        sInheritances.setString(4, inheritances.getParent().getType());
                        sInheritances.setString(5, inheritances.getContext().toString());
                        sInheritances.addBatch();
                    }

                    for(PPermission permissions : Subject.getPermissions(subject)){
                        sPermissions.setString(1, subject.getIdentifier());
                        sPermissions.setString(2, permissions.getPermission());
                        sPermissions.setString(3, permissions.getPermissionIdentifier().toString());
                        sPermissions.setString(4, Long.toString(permissions.getExpiry()));
                        sPermissions.setString(5, permissions.getContext().toString());
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

    public void removeSubjects(ArrayList<String> subjects) throws SQLException {
        SQLException e = executeInTransaction(() -> {
            try {
                PreparedStatement sData = prepareStatement(this.getDeleteSubjectDataByIdentifierUpdate());
                PreparedStatement sInheritances = prepareStatement(this.getDeleteInheritanceByChildOrParentUpdate());
                PreparedStatement sPermissions = prepareStatement(this.getDeletePermissionByOwnerIdentifierUpdate());

                for (String subject : subjects) {
                    sData.setString(1, subject);
                    sData.addBatch();

                    sInheritances.setString(1, subject);
                    sInheritances.addBatch();

                    sPermissions.setString(1, subject);
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
            s.setString(1, inheritance.getChild());
            s.setString(2, inheritance.getParent());

            s.addBatch();
        }

        s.executeBatch();
    }

    public void addInheritances(ArrayList<CachedInheritance> inheritances) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertInheritanceUpdate());

        for(CachedInheritance inheritance : inheritances){
            s.setString(1, inheritance.getChild());
            s.setString(2, inheritance.getParent());
            s.setString(3, inheritance.getChildType());
            s.setString(4, inheritance.getParentType());
            s.setString(5, inheritance.getContext().toString());

            s.addBatch();
        }

        s.executeBatch();
    }

    public void addPermissions(ArrayList<OwnerPermissionPair> permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertPermissionUpdate());

        for(OwnerPermissionPair pair : permissions){
            s.setString(1, pair.getOwnerIdentifier());
            s.setString(2, pair.getPermission().getPermission());
            s.setString(3, pair.getPermissionIdentifier().toString());
            s.setString(4, Long.toString(pair.getPermission().getExpiry()));
            s.setString(5, pair.getPermission().getContext().toString());

            s.addBatch();
        }

        s.executeBatch();
    }

    public void removeAllPermissions(String identifier) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerIdentifierUpdate());
        s.setString(1, identifier);
        s.executeUpdate();
    }

    public void removePermissions(ArrayList<OwnerPermissionPair> permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerIdentifierAndPermissionUpdate());

        for(OwnerPermissionPair pair : permissions){
            s.setString(1, pair.getOwnerIdentifier());
            s.setString(2, pair.getPermissionString());

            s.addBatch();
        }

        s.executeBatch();
    }

        //Subject data

    public void setSubjectData(String identifier, SubjectData data, String type) throws SQLException {
        this.removeSubjectData(identifier);

        PreparedStatement s = prepareStatement(this.getInsertSubjectDataUpdate());

        s.setString(1, identifier);
        s.setString(2, type);
        s.setString(3, data.toString());

        s.executeUpdate();
    }

    public void removeSubjectData(String identifier) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteSubjectDataByIdentifierUpdate());

        s.setString(1, identifier);

        s.executeUpdate();
    }

    public GenericSubjectData getSubjectData(String identifier) throws SQLException {
        PreparedStatement s = prepareStatement(this.getSubjectDataFromIdentifierQuery());

        s.setString(1, identifier);

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

    public void removeAllInheritances (String childOrParent) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteInheritanceByChildOrParentUpdate());

        s.setString(1, childOrParent);
        s.setString(2, childOrParent);

        s.executeUpdate();
    }

    public void removeInheritance(String child, String parent) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeleteInheritanceByChildAndParentUpdate());

        s.setString(1, child);
        s.setString(2, parent);

        s.executeUpdate();
    }

    public void addInheritance(String child, String parent, String childType, String parentType, Context context) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertInheritanceUpdate());

        s.setString(1, child);
        s.setString(2, parent);
        s.setString(3, childType);
        s.setString(4, parentType);
        s.setString(5, context.toString());

        s.executeUpdate();
    }

    public ArrayList<CachedInheritance> getInheritances(String child) throws SQLException {
        ArrayList<CachedInheritance> out = new ArrayList<>();

        PreparedStatement s = prepareStatement(this.getInheritancesFromChildQuery());

        s.setString(1, child);

        ResultSet results = s.executeQuery();

        while(results.next()){
            out.add(new CachedInheritance(child, results.getString("Parent"), results.getString("ChildType"), results.getString("ParentType"), Context.fromString(results.getString("Context"))));
        }

        return out;
    }


        //Permissions

    public ArrayList<PPermission> getPermissions(String ownerIdentifier) throws SQLException {
        ArrayList<PPermission> perms = new ArrayList<>();

        PreparedStatement s = prepareStatement(this.getPermissionsFromOwnerIdentifierQuery());

        s.setString(1, ownerIdentifier);

        ResultSet r = s.executeQuery();

        while(r.next()){
            PPermission perm = new PPermission(r.getString("Permission"), Context.fromString(r.getString("Context")), Long.parseLong(r.getString("Expiration")), UUID.fromString(r.getString("PermissionIdentifier")));
            perms.add(perm);
        }

        return perms;
    }

    public void addPermissions(String ownerIdentifier, ImmutablePermissionList permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertPermissionUpdate());

        for (PPermission perm : permissions) {
            s.setString(1, ownerIdentifier);
            s.setString(2, perm.getPermission());
            s.setString(3, perm.getPermissionIdentifier().toString());
            s.setString(4, Long.toString(perm.getExpiry()));
            s.setString(5, perm.getContext().toString());

            s.addBatch();
        }

        s.executeBatch();
    }

    public void addPermission(String ownerIdentifier, PPermission permission) throws SQLException {
        PreparedStatement s = prepareStatement(this.getInsertPermissionUpdate());

        s.setString(1, ownerIdentifier);
        s.setString(2, permission.getPermission());
        s.setString(3, permission.getPermissionIdentifier().toString());
        s.setString(4, Long.toString(permission.getExpiry()));
        s.setString(5, permission.getContext().toString());

        s.executeUpdate();
    }

    public void removePermissions(String ownerIdentifier, ArrayList<String> permissions) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerIdentifierAndPermissionUpdate());

        for (String perms : permissions) {
            s.setString(1, ownerIdentifier);
            s.setString(2, perms);

            s.addBatch();
        }

        s.executeBatch();
    }

    public void removePermission(String ownerIdentifier, String permission) throws SQLException {
        PreparedStatement s = prepareStatement(this.getDeletePermissionByOwnerIdentifierAndPermissionUpdate());

        s.setString(1, ownerIdentifier);
        s.setString(2, permission);

        s.executeUpdate();
    }

    public void removeSubjectsOftype(String type) throws SQLException{
        PreparedStatement s = prepareStatement(this.getSubjectDataFromTypeQuery());
        s.setString(1, type);
        ResultSet set = s.executeQuery();

        s = prepareStatement(this.getDeleteSubjectDataByTypeUpdate());
        s.setString(1, type);
        s.executeUpdate();

        s = prepareStatement(this.getDeletePermissionByOwnerIdentifierUpdate());
        while(set.next()){
            s.setString(1, set.getString("Identifier"));
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

}
