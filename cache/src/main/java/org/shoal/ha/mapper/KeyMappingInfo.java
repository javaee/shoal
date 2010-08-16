package org.shoal.ha.mapper;

/**
 * @author Mahesh Kannan
 *
 */
public class KeyMappingInfo {

    private int index;

    private String[] members;

    public KeyMappingInfo(int index, String[] members) {
        this.index = index;
        this.members = members;
    }

    public int getIndex() {
        return index;
    }

    public String[] getMembers() {
        return members;
    }
}
