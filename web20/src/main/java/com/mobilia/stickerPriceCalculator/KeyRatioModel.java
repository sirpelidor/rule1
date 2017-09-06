package com.mobilia.stickerPriceCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by allen on 7/30/17.
 */
public class KeyRatioModel {
    private List<String> big4Year, roicYear, historicalBig4Year;
    private List<Key4Chart> big4Data, roicData;
    private List<String> cashflowTbl, epsTbl, salesTbl, equityTbl;
    private String currentEps;
    private String guess_eps_growthRate;

    public KeyRatioModel() {
        big4Year = new ArrayList<>();
        roicYear = new ArrayList<>();
        big4Data = new ArrayList<>();
        roicData = new ArrayList<>();
        historicalBig4Year = new ArrayList<>();
        cashflowTbl = new ArrayList<>();
        epsTbl = new ArrayList<>();
        salesTbl = new ArrayList<>();
        equityTbl = new ArrayList<>();
    }

    public List<String> getBig4Year() {
        return big4Year;
    }

    public void setBig4Year(List<String> big4Year) {
        this.big4Year = big4Year;
    }

    public List<String> getRoicYear() {
        return roicYear;
    }

    public void setRoicYear(List<String> roicYear) {
        this.roicYear = roicYear;
    }

    public List<Key4Chart> getBig4Data() {
        return big4Data;
    }

    public void setBig4Data(List<Key4Chart> big4Data) {
        this.big4Data = big4Data;
    }

    public List<Key4Chart> getRoicData() {
        return roicData;
    }

    public void setRoicData(List<Key4Chart> roicData) {
        this.roicData = roicData;
    }

    public List<String> getHistoricalBig4Year() {
        return historicalBig4Year;
    }

    public void setHistoricalBig4Year(List<String> historicalBig4Year) {
        this.historicalBig4Year = historicalBig4Year;
    }

    public List<String> getCashflowTbl() {
        return cashflowTbl;
    }

    public void setCashflowTbl(List<String> cashflowTbl) {
        this.cashflowTbl = cashflowTbl;
    }

    public List<String> getEpsTbl() {
        return epsTbl;
    }

    public void setEpsTbl(List<String> epsTbl) {
        this.epsTbl = epsTbl;
    }

    public List<String> getSalesTbl() {
        return salesTbl;
    }

    public void setSalesTbl(List<String> salesTbl) {
        this.salesTbl = salesTbl;
    }

    public List<String> getEquityTbl() {
        return equityTbl;
    }

    public void setEquityTbl(List<String> equityTbl) {
        this.equityTbl = equityTbl;
    }

    public String getCurrentEps() {
        return currentEps;
    }

    public void setCurrentEps(String currentEps) {
        this.currentEps = currentEps;
    }

    public String getGuess_eps_growthRate() {
        return guess_eps_growthRate;
    }

    public void setGuess_eps_growthRate(String guess_eps_growthRate) {
        this.guess_eps_growthRate = guess_eps_growthRate;
    }
}