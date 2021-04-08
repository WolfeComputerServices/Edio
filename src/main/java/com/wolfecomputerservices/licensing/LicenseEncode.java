/*
 * Copyright (C) 2020 -  Wolfe Computer Services
 * ALL RIGHTS RESERVED
 */
package com.wolfecomputerservices.licensing;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

public class LicenseEncode extends LicenseCode {
    public String create(LicenseData.Type type, int maxUsers, 
            ZonedDateTime expiration, int flags) {
        final int min = 7;
        Random rand = new Random();
        sections.forEach((section) -> {
            int max = sizeLicense - 1 - section.length;
            int randOffset = min + rand.nextInt(max - min + 1);
            int newOffset = randOffset;
            boolean wrapped = false;
            while (collides(newOffset, section.length)) {
                newOffset++;
                if (newOffset == randOffset) {
                    if (wrapped) {
                        wrapped = false;
                        newOffset = sizeLicense;
                    } else {
                        newOffset = min;
                        wrapped = true;
                    }
                }
            }
            section.setOffset(newOffset);
            if (newOffset + section.length > sizeLicense) {
                sizeLicense = newOffset + section.length;
            }
        });
        String license = ""; 
        license = sections.stream().map((section) -> 
                String.format("%02d", section.offset))
                .reduce(license, String::concat);
        return encode(license, LicenseData.Type.PerminateUnlimited, 0, null, 0);
    }
    private boolean willFit(int lenOfString, int startIndex, int length) {
        int sEndIndex = startIndex+length;
        return !(startIndex > lenOfString || sEndIndex > lenOfString);
    }
    private void replace(StringBuilder s, int index, char c) {
        if (willFit(s.length(), index, 1))
            s.setCharAt(index, c);
    }
    private void replace(StringBuilder s, int startIndex, int length, String value) {
        if (willFit(s.length(), startIndex, length))
            s.replace(startIndex, startIndex+length, 
                String.format("%-" + length + "s", value));
    }
    private void replace(StringBuilder s, int startIndex, int length, int value) {
        if (willFit(s.length(), startIndex, length))
            s.replace(startIndex, startIndex+length, 
                String.format("%0" + length + "d", value));
    }
    private String encode(String preLicense, 
            LicenseData.Type type, int maxUsers, ZonedDateTime expiration, int flags) {
        Random rand = new Random();
        StringBuilder postLicense = new StringBuilder();
        postLicense.append(
                String.format("%-" + sizeLicense + "s", preLicense)
               .toCharArray());
        
        replace(postLicense, offDashes[0], '-');
        replace(postLicense, offDashes[1], '-');
        replace(postLicense, offDashes[2], '-');
        replace(postLicense, offDashes[3], '-');
        
        int value;
        int users = 0;
        Calendar dtExpiration = toUTCCalendar(ZonedDateTime.now());
        value = type.value();
        switch (type) {
            case PerminateUnlimited:
                break;
            case PerminateLimited:
                users = maxUsers;
                break;
            case Expiration:
                dtExpiration = GregorianCalendar.from(expiration);
                dtExpiration.set(Calendar.HOUR_OF_DAY, 23);
                dtExpiration.set(Calendar.MINUTE, 59);
                break;
        }
        // Type
        SectionDefinition section = sections.get(TYPE);
        replace(postLicense, section.offset, section.length, value);
        
        // users
        if (users == 0)
            users = rand.nextInt(9999);
        section = sections.get(MAX_USERS);
        replace(postLicense, section.offset, section.length, users);
        
        // Expiration Date
        section = sections.get(EXPIRATION_DATE);
        replace(postLicense, section.offset, section.length,  
                    dtExpiration.get(Calendar.DAY_OF_YEAR) * 10000 + 
                            (dtExpiration.get(Calendar.YEAR) * 2) + 50);
        
        // Flags
        int newFlags = flags;
        section = sections.get(FLAGS);
        if (newFlags == 0)
            newFlags = 0x8000 + rand.nextInt(0xFFFF - 0x8000 + 1);
        
        replace(postLicense, section.offset, section.length, newFlags);
        
        
        // Fill in white space
        for(int i = 0; i < postLicense.length(); i++){
            if(Character.isWhitespace(postLicense.charAt(i))){
                replace(postLicense, i, (char) (rand.nextInt(26) + 'A'));
            }
        }
        
        return postLicense.toString().trim();
    }
    private boolean collides(int newOffset, int newLength) {
        int x1 = newOffset, x2 = newOffset + newLength - 1;
        // Do ranges [x1,x2] overlapp [y1,y2]
        for (SectionDefinition section : sections) {
            int y1 = section.offset, y2 = section.offset + section.length - 1;
            if (isOverlapped(x1,x2,y1,y2))
                return true;            
        }
        
        for (int offset : offDashes) {
            if (isOverlapped(x1,x2, offset))
                return true;
        }

        return false;
    }

    private boolean isOverlapped(int x1, int x2, int y) {
        return x1 <= y && x2 >= y;
    }
    private boolean isOverlapped(int x1, int x2, int y1, int y2) {
        // Do ranges [x1,x2] and [y1,y2] overlapp
        return (y2 - x1) * (x2 - y1) >= 0;
    }
}
