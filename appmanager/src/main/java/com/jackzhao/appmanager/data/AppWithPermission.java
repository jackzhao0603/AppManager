package com.jackzhao.appmanager.data;


import java.util.ArrayList;
import java.util.List;


public class AppWithPermission {
    private String pkgName;
    private List<String> normalPermissions = new ArrayList();
    private List<String> specialPermissions = new ArrayList();

    public AppWithPermission(String pkgName) {
        this.pkgName = pkgName;
    }

    public List<String> getNormalPermissions() {
        return normalPermissions;
    }

    public void setNormalPermissions(List<String> normalPermissions) {
        this.normalPermissions = normalPermissions;
    }

    public List<String> getSpecialPermissions() {
        return specialPermissions;
    }


    public void setSpecialPermissions(List<String> specialPermissions) {
        this.specialPermissions = specialPermissions;
    }


    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }
}