package com.mobilia.stickerPriceCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by allen on 7/31/17.
 */
public class Key4Chart{
    private String name;
    private List<Double>data;

    public Key4Chart(){
        data = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Double> getData() {
        return data;
    }

    public void setData(List<Double> data) {
        this.data = data;
    }
}
