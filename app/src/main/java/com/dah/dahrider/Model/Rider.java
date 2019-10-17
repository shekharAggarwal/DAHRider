package com.dah.dahrider.Model;

public class Rider {

    private String Phone;
    private String Name;
    private String avatarUrl;
    private String rates;
    private String carType;


    public Rider() {

    }

    public Rider(String phone, String name, String avatarUrl, String rates, String carType) {
        Phone = phone;
        Name = name;
        this.avatarUrl = avatarUrl;
        this.rates = rates;
        this.carType = carType;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRates() {
        return rates;
    }

    public void setRates(String rates) {
        this.rates = rates;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }
}
