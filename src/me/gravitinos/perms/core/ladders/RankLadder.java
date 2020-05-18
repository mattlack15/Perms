package me.gravitinos.perms.core.ladders;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.user.User;

import java.util.List;

@Getter
@AllArgsConstructor
public class RankLadder {
    private String name;
    private List<Group> groups;

    public void setRank(User user, int index) {

    }

    public void promote(User user) {

    }
}