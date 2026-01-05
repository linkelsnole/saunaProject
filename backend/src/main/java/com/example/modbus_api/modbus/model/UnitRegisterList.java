package com.example.modbus_api.modbus.model;

import java.util.ArrayList;
import java.util.List;

public class UnitRegisterList {
    private int unitIdx;
    private List<Integer> data = new ArrayList<>();
    
    public int getUnitIdx() {
        return unitIdx;
    }
    
    public void setUnitIdx(int unitIdx) {
        this.unitIdx = unitIdx;
    }
    

    public List<Integer> getdata() {
        return data;
    }
    
    public void setdata(List<Integer> data) { 
        this.data = data;
    }
    
    public void add(int register) {
        if (this.data == null) {
            this.data = new ArrayList<>();
        }
        this.data.add(register);
    }
}