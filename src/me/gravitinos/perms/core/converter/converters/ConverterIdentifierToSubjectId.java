package me.gravitinos.perms.core.converter.converters;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLDao;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.converter.Converter;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.file.SpigotFileDataManager;

import java.sql.SQLException;

public class ConverterIdentifierToSubjectId extends Converter {
    @Override
    public String getName() {
        return "IDENTIFIER_TO_SUBJECT_ID";
    }

    @Override
    public boolean test() {
        //Backend check
        DataManager dataManager = GroupManager.instance.getDataManager();
        if(dataManager instanceof SQLHandler){
            //Using SQL
            SQLHandler sqlHandler = (SQLHandler) dataManager;
            try(SQLDao dao = sqlHandler.getDao()) {
                return dao.checkConverterIdentifierToSubjectId();
            } catch (SQLException e) {
                return false;
            }
        } else if(dataManager instanceof SpigotFileDataManager){
            //Using files
            SpigotFileDataManager fileHandler = (SpigotFileDataManager) dataManager;
            return fileHandler.checkConverterIdentifierToSubjectId();
        }
        return false;
    }

    @Override
    public boolean convert() {
        DataManager dataManager = GroupManager.instance.getDataManager();
        if(dataManager instanceof SQLHandler){
            //Using SQL
            SQLHandler sqlHandler = (SQLHandler) dataManager;
            try(SQLDao dao = sqlHandler.getDao()) {
                return dao.convertIdentifierToSubjectId();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        } else if(dataManager instanceof SpigotFileDataManager){
            //Using files
            SpigotFileDataManager fileHandler = (SpigotFileDataManager) dataManager;
            return fileHandler.convertIdentifierToSubjectId();
        }
        return false;
    }
}
