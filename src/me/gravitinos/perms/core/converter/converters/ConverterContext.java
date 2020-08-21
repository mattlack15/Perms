package me.gravitinos.perms.core.converter.converters;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLDao;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.converter.Converter;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.file.SpigotFileDataManager;

import java.sql.SQLException;

public class ConverterContext extends Converter {

    @Override
    public String getName() {
        return "Context Update";
    }

    @Override
    public boolean test() {
        //Backend check
        DataManager dataManager = GroupManager.instance.getDataManager();
        if(dataManager instanceof SQLHandler){
            //Using SQL
            SQLHandler sqlHandler = (SQLHandler) dataManager;
            try(SQLDao dao = sqlHandler.getDao()) {
                return dao.checkConverterContext();
            } catch (SQLException e) {
                return false;
            }
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
                dao.convertContext();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
