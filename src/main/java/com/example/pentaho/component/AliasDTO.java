package com.example.pentaho.component;

public class AliasDTO {

    private String alias;

    private String typeName;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return "AliasDTO{" +
                "alias='" + alias + '\'' +
                ", typeName='" + typeName + '\'' +
                '}';
    }
}
