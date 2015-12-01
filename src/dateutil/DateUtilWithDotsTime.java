/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dateutil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Ben
 */
public class DateUtilWithDotsTime {
 
    // List of all date formats that we want to parse.
    // Add your own format here.
    private static List<SimpleDateFormat>
            dateFormats = new ArrayList<SimpleDateFormat>() {{
                
            add(new SimpleDateFormat("dd.MM.yyyyHHmm"));    
            add(new SimpleDateFormat("dd.MM.yyHHmm"));
            add(new SimpleDateFormat("yyyy.MM.ddHHmm"));
            add(new SimpleDateFormat("yy.MM.ddHHmm"));
        }
    };
 
    /**
     * Convert String with various formats into java.util.Date
     * 
     * @param input
     *            Date as a string
     * @return java.util.Date object if input string is parsed 
     *          successfully else returns null
     */
    public static Date convertToDate(String input) {
        Date date = null;
        if(null == input) {
            return null;
        }
        for (SimpleDateFormat format : dateFormats) {
            try {
                format.setLenient(false);
                date = format.parse(input);
            } catch (ParseException e) {
                //Shhh.. try other formats
              //  JOptionPane.showMessageDialog(null, "Unknown Date Format\r\n"+input);
                
               
            }
            if (date != null) {
                break;
            }
        }
 
        return date;
    }
}
