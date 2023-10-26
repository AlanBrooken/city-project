package org.cityProject.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "countrylanguage", schema = "world")
public class CountryLanguage {
    @Id
    @Column(name = "CountryCode")
    private String countryCode;

    @Column(name = "Language")
    private String language;

    @Column(name = "IsOfficial")
    private IsOfficial isOfficial;

    @Column(name = "Percentage")
    private BigDecimal percentage;

    @ManyToOne()
    @JoinColumn(name = "countryCode", referencedColumnName = "code")
    private Country country;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public IsOfficial getIsOfficial() {
        return isOfficial;
    }

    public void setIsOfficial(IsOfficial isOfficial) {
        this.isOfficial = isOfficial;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }
}
