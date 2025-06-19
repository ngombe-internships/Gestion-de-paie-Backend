package com.hades.paie1.utils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MathUtils {


    //Multiplication securise
    public  BigDecimal safeMultiply(BigDecimal a, BigDecimal b){
        if(a == null || b == null || a.compareTo(BigDecimal.ZERO) < 0 || b.compareTo(BigDecimal.ZERO ) < 0) {
            return BigDecimal.ZERO;
        }

        return a.multiply(b).setScale(2, RoundingMode.HALF_UP);
    }
    //Addition Securise
    public  BigDecimal safeAdd (BigDecimal... values){
        BigDecimal result = BigDecimal.ZERO;
        for(BigDecimal value: values){
            if (value != null && value.compareTo(BigDecimal.ZERO) >=0) {
                result = result.add(value);
            }
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    //Recherche le mininum
    public BigDecimal safeMin (BigDecimal a, BigDecimal b){
        if(a == null) return b != null ? b : BigDecimal.ZERO;
        if(b == null) return a;
        return a.min(b);
    }
}
