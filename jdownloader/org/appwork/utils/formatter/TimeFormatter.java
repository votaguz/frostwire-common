/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.formatter
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.formatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TimeFormatter {

    private static final java.util.List<SimpleDateFormat> dateformats  = new ArrayList<SimpleDateFormat>();
    static {
        try {
            SimpleDateFormat sdf;
            TimeFormatter.dateformats.add(sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy z", Locale.UK));
            sdf.setLenient(false);
            TimeFormatter.dateformats.add(sdf = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss z", Locale.UK));
            sdf.setLenient(false);
            TimeFormatter.dateformats.add(sdf = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.UK));
            sdf.setLenient(false);
            TimeFormatter.dateformats.add(sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.UK));
            sdf.setLenient(false);
            TimeFormatter.dateformats.add(sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.UK));
            sdf.setLenient(false);
            TimeFormatter.dateformats.add(sdf = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.UK));
            sdf.setLenient(false);
            TimeFormatter.dateformats.add(sdf = new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", Locale.UK));
            sdf.setLenient(false);
            TimeFormatter.dateformats.add(sdf = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.UK));
            sdf.setLenient(true);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    public static Date parseDateString(final String date) {
        if (date == null) { return null; }
        Date expireDate = null;
        for (final SimpleDateFormat format : TimeFormatter.dateformats) {
            try {
                expireDate = format.parse(date);
                break;
            } catch (final Throwable e2) {
            }
        }
        if (expireDate == null) { return null; }
        return expireDate;
    }
}
