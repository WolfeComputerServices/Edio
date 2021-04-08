/*
 * Copyright (C) 2020 -  Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.licensing;

import com.wolfecomputerservices.musse.exception.InvalidLicenseException;
import java.time.ZonedDateTime;
import java.util.Calendar;

public class LicenseData {
    public enum Type {
        PerminateUnlimited (1),
        PerminateLimited (2),
        Expiration (3);
        
        private final int value;
        Type(int v) {  this.value = v; }
        
        public int value() { return value; }
        public static Type valueOf(int d) { 
            return Type.values()[d-1];
        }        
    }
    public LicenseData(String licenseCode) 
            throws InvalidLicenseException {
        LicenseDecode ld = new LicenseDecode();

        Object[] lData = ld.decode(licenseCode);

        long maxUsersToBe = Long.MAX_VALUE;
        Calendar expirationDateToBe = null;
        String descriptionToBe = "invalid";

        if (lData == null)
            throw new InvalidLicenseException();
        
        this.type = (Type)lData[0];
        switch (type) {
            case PerminateUnlimited -> descriptionToBe = "Perminant License";
            case PerminateLimited -> {
                maxUsersToBe = (long)lData[1];
                descriptionToBe = 
                        String.format("Perminant License for up to %d users.", maxUsersToBe);
            }
            case Expiration -> {
                expirationDateToBe =(Calendar)lData[2];
                if (expirationDateToBe.after(ZonedDateTime.now()))  {
                    descriptionToBe = "Expired";
                    break;
                }
                descriptionToBe = "License will expire " +
                        String.format("%04d %s %d",
                                expirationDateToBe.get(Calendar.YEAR),
                                toMonthName(expirationDateToBe.get(Calendar.MONTH)),
                                expirationDateToBe.get(Calendar.DATE));
            }
        }

        this.flags = (int)lData[3];
        this.maxUsers = maxUsersToBe;
        this.description = descriptionToBe;
        this.expirationDate = expirationDateToBe;
        
//        if (description.compareTo("Expired") == 0)
//            throw new CMInvalidLicenseException();
    }
    private final Type type;
    private final int flags;
    private final long maxUsers;
    private final Calendar expirationDate;
    private final String description;
    
    public String getLicenseStatus() { return description; }
    public long getMaxUsers() { return maxUsers; }

    public static String toMonthName(int monthOrdinal) {
        return switch (monthOrdinal) {
            case 0 -> "January";
            case 1 -> "February";
            case 2 -> "March";
            case 3 -> "April";
            case 4 -> "May";
            case 5 -> "June";
            case 6 -> "July";
            case 7 -> "August";
            case 8 -> "September";
            case 9 -> "October";
            case 10 -> "November";
            case 11 -> "December";
            default -> "";
        };
    }
}
