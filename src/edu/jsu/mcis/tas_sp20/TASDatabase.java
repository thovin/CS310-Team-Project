package edu.jsu.mcis.tas_sp20;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TASDatabase {
    private Connection conn;

    public final int DAY_IN_MILLIS = 86400000;

    public static void main(String[] args) {    //TODO: remove
    }

    public TASDatabase(){
        try {
            String server = "jdbc:mysql://localhost/TAS";
            String user = "admin";
            String pass = "password";
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            conn = DriverManager.getConnection(server, user, pass);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TASDatabase(String server, String user, String pass) {
        try {
            server = "jdbc:mysql://localhost/TAS";
            user = "admin";
            pass = "password";
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            conn = DriverManager.getConnection(server, user, pass);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Punch getPunch(int ID) {
        Punch punch = null;
        try {
            String query;
            PreparedStatement pstPunch, pstBadge;
            ResultSet resultSet;

            query = "SELECT * FROM punch WHERE id=?";
            pstPunch = conn.prepareStatement(query);
            pstPunch.setInt(1, ID);

            pstPunch.execute();
            resultSet = pstPunch.getResultSet();
            resultSet.first();

            int terminalID = resultSet.getInt("terminalid");
            String badgeID = resultSet.getString("badgeid");
            long origTimeStamp = resultSet.getTimestamp("originaltimestamp").getTime();
            int punchTypeID = resultSet.getInt("punchtypeid");

            query = "SELECT * FROM badge WHERE id=?";
            pstBadge = conn.prepareStatement(query);
            pstBadge.setString(1, badgeID);
            pstBadge.execute();
            resultSet = pstBadge.getResultSet();
            resultSet.first();

            Badge badge = new Badge(resultSet.getString("id"), resultSet.getString("description"));
            //TODO: remove empty space?
            punch = new Punch(ID, terminalID, badge, origTimeStamp, punchTypeID);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return punch;
    }

    public Badge getBadge(String ID) {
        Badge badge = null;
        String query;
        PreparedStatement pst;
        ResultSet resultSet;

        try {
            query = "SELECT * FROM badge WHERE id=?";
            pst = conn.prepareStatement(query);
            pst.setString(1, ID);
            pst.execute();

            resultSet = pst.getResultSet();
            resultSet.first();  //TODO: new line after?
            badge = new Badge(ID, resultSet.getString("description"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return badge;
    }

    public Shift getShift(int ID) {
        Shift shift = null;
        String query;
        PreparedStatement pst;
        ResultSet resultSet;

        try {
            query = "SELECT * FROM dailyschedule WHERE id=?";
            pst = conn.prepareStatement(query);
            pst.setInt(1, ID);

            pst.execute();
            resultSet = pst.getResultSet();
            resultSet.first();
            java.sql.Time temp;

            int ShiftID = resultSet.getInt("id");
            temp = resultSet.getTime("start");  //TODO: can these lines be combined?
            LocalTime start = temp.toLocalTime();
            temp = resultSet.getTime("stop");
            LocalTime stop = temp.toLocalTime();
            int interval = resultSet.getInt("interval");
            int gracePeriod = resultSet.getInt("graceperiod");
            int dock = resultSet.getInt("dock");
            temp = resultSet.getTime("lunchstart");
            LocalTime lunchStart = temp.toLocalTime();
            temp = resultSet.getTime("lunchstop");
            LocalTime lunchStop = temp.toLocalTime();
            int lunchDeduct = resultSet.getInt("lunchdeduct");

            query = "SELECT * FROM shift WHERE id=?";
            pst = conn.prepareStatement(query);
            pst.setInt(1, ID);

            pst.execute();
            resultSet = pst.getResultSet();
            resultSet.first();

            String description = resultSet.getString("description");

            DailySchedule schedule = new DailySchedule(ShiftID, start, stop, interval, gracePeriod, dock, lunchStart, lunchStop, lunchDeduct);
            shift = new Shift(description, schedule);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return shift;
    }

    public Shift getShift(Badge badge) {
        Shift shift = null;
        String query;
        PreparedStatement pst;
        ResultSet resultSet;

        try {
            query = "SELECT shiftid FROM employee WHERE badgeid = ?";
            pst = conn.prepareStatement(query);
            pst.setString(1, badge.getId());

            pst.execute();
            resultSet = pst.getResultSet();
            resultSet.first();

            int shiftID = resultSet.getInt("shiftid");
            //TODO: remove space?
            shift = getShift(shiftID);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return shift;
    }

    public Shift getShift(Badge badge, Long timestamp){
        Shift shift = null;
        String query;
        PreparedStatement pst;
        ResultSet resultSet;

        try {
            query = "SELECT * FROM scheduleoverride";
            pst = conn.prepareStatement(query);

            pst.execute();
            resultSet = pst.getResultSet();
            Long startTimestamp;
            Long endTimestamp;
            String badgeid;
            
            //Get the default value to return if no override is found
            shift = getShift(badge);
            
            //Loop through scheduleoverride and check for any applicable overrides
            while (resultSet.next()){
                startTimestamp = resultSet.getTimestamp("start").getTime();
                if (resultSet.getTimestamp("end") != null){
                    endTimestamp = resultSet.getTimestamp("end").getTime();
                } else {
                    endTimestamp = null;
                }
                badgeid = resultSet.getString("badgeid");

                if (timestamp >= startTimestamp && ((endTimestamp == null) || (timestamp <= endTimestamp))){
                    if ((badgeid == null) || (badgeid.equals( badge.getId() ))){
                        DailySchedule schedule = null;  //TODO: can these 2 lines be combined?
                        schedule = getDailySchedule(resultSet.getInt("dailyscheduleid"));
                        shift.setSchedule(schedule, resultSet.getInt("day"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shift;
    }
    
    public DailySchedule getDailySchedule(int ID) { //TODO: make private?
        DailySchedule schedule = null;
        String query;
        PreparedStatement pst;
        ResultSet resultSet;

        try {
            query = "SELECT * FROM dailyschedule WHERE id=?";
            pst = conn.prepareStatement(query);
            pst.setInt(1, ID);

            pst.execute();
            resultSet = pst.getResultSet();
            resultSet.first();
            java.sql.Time temp;

            int ShiftID = resultSet.getInt("id");
            temp = resultSet.getTime("start");  //TODO: see above - can combine?
            LocalTime start = temp.toLocalTime();
            temp = resultSet.getTime("stop");
            LocalTime stop = temp.toLocalTime();
            int interval = resultSet.getInt("interval");
            int gracePeriod = resultSet.getInt("graceperiod");
            int dock = resultSet.getInt("dock");
            temp = resultSet.getTime("lunchstart");
            LocalTime lunchStart = temp.toLocalTime();
            temp = resultSet.getTime("lunchstop");
            LocalTime lunchStop = temp.toLocalTime();
            int lunchDeduct = resultSet.getInt("lunchdeduct");

            schedule = new DailySchedule(ShiftID, start, stop, interval, gracePeriod, dock, lunchStart, lunchStop, lunchDeduct);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return schedule;
    }

    public int insertPunch(Punch p) {
        GregorianCalendar ots = new GregorianCalendar();
        ots.setTimeInMillis(p.getOriginaltimestamp());
        String badgeID = p.getBadge().getId();
        int terminalID = p.getTerminalid(), punchTypeID = p.getPunchtypeid();   //TODO: split?

        try {
            PreparedStatement pst;
            ResultSet resultSet;
            String query;


            if (conn.isValid(0)){
                query = "INSERT INTO punch (terminalid, badgeid, originaltimestamp, punchtypeid) VALUES (?, ?, ?, ?)";
                pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                pst.setInt(1, terminalID);
                pst.setString(2, badgeID.toString());   //TODO: remove toString - is already a String
                pst.setString(3, (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(ots.getTime()));
                pst.setInt(4, punchTypeID);

                pst.execute();
                resultSet = pst.getGeneratedKeys();
                resultSet.first();
                if (resultSet.getInt(1) > 0) {  //TODO: change constants to collumn headers
                    return resultSet.getInt(1);
                } else {
                    return -1;  //TODO: duplicate return value?
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;

    }
    
    public ArrayList<Punch> getDailyPunchList(Badge badge, long ts){
        Timestamp timestamp = new Timestamp(ts);
        String timeLike = timestamp.toString().substring(0, 11);
        timeLike += "%";
        ArrayList<Punch> dailyPunchList = new ArrayList<>();
        ArrayList<Punch> sortedDailyPunchList = new ArrayList<>();


        try {
            PreparedStatement pst;
            ResultSet resultSet;
            String query;
            boolean isPaired = true;


            if (conn.isValid(0)){
                query = "SELECT * FROM punch WHERE badgeid = ? AND originaltimestamp LIKE ?";   //TODO: can you add AS in order to skip the sorting?
                pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                pst.setString(1, badge.getId());
                pst.setString(2, timeLike);

                pst.execute();
                resultSet = pst.getResultSet();
                
                while(resultSet.next()){
                    int punchId = resultSet.getInt("id");
                    //TODO: remove extra space?
                    Punch temp = this.getPunch(punchId);
                    dailyPunchList.add(temp);
                    
                    isPaired = !isPaired;
                }
                
                if(!isPaired){
                    timestamp = new Timestamp(timestamp.getTime() +  this.DAY_IN_MILLIS);
                    timeLike = timestamp.toString().substring(0, 11);
                    timeLike += "%";

                    query = "SELECT * FROM punch WHERE badgeid = ? AND originaltimestamp LIKE ?";
                    pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    pst.setString(1, badge.getId());
                    pst.setString(2, timeLike);

                    pst.execute();
                    resultSet = pst.getResultSet();
                    resultSet.first();
                    
                    int punchId = resultSet.getInt("id");

                    Punch temp = this.getPunch(punchId);
                    dailyPunchList.add(temp);
                }
                
                //Sort dailyPunchList if necessary
                if (dailyPunchList.size() > 0){
                    if(dailyPunchList.get(0).getPunchtypeid() == 0){
                        sortPunchList(dailyPunchList, sortedDailyPunchList);
                    } else {
                        sortedDailyPunchList = dailyPunchList;
                    }
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sortedDailyPunchList;
    }
    
    public void sortPunchList(ArrayList<Punch> punchlist, ArrayList<Punch> sortedpunchlist) {   //TODO: make private    //TODO: return a sorted punch list instead of passing by reference? or pass a single arrayList and sort it?
        int count = 1;  //TODO: rename
        while(punchlist.size() > 0){
            for(int i = 0; i < punchlist.size(); i++){
                if(punchlist.get(i).getPunchtypeid() == count){
                    sortedpunchlist.add(punchlist.get(i));
                    punchlist.remove(i);
                }
            }

            count = (count + 1) % 2;
        }
    }

    public ArrayList<Punch> getPayPeriodPunchList(Badge badge, long ts){
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(ts);
        gc.add(Calendar.DAY_OF_WEEK, -(gc.get(Calendar.DAY_OF_WEEK) - 1));
        gc.set(Calendar.HOUR, 0);
        gc.set(Calendar.MINUTE, 0);
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        long tsNew = gc.getTimeInMillis();
        ArrayList<Punch> returnArray = new ArrayList<>();

        for(int i = 0; i < 7; i++){
            ArrayList<Punch> temp = this.getDailyPunchList(badge, tsNew + (this.DAY_IN_MILLIS * i));    //TODO: Can't we just return this arrayList instead of copying it?

            for(Punch p: temp){
                returnArray.add(p);
            }
        }

        return returnArray;
    }

    public Absenteeism getAbsenteeism(String badgeId, long ts){
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(ts);
        gc.add(Calendar.DAY_OF_WEEK, -(gc.get(Calendar.DAY_OF_WEEK) - 1));
        gc.set(Calendar.HOUR, 0);
        gc.set(Calendar.MINUTE, 0);
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        long tsNew = gc.getTimeInMillis();  //TODO: is this a different number than ts?

        Timestamp timestamp = new Timestamp(tsNew);
        Absenteeism returnAbsenteeism = null;

        try {
            PreparedStatement pst;
            ResultSet resultSet;
            String query;

            if (conn.isValid(0)){
                query = "SELECT * FROM absenteeism WHERE badgeid = ? AND payperiod = ?";
                pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                pst.setString(1, badgeId);
                pst.setTimestamp(2, timestamp);

                pst.execute();
                resultSet = pst.getResultSet();
                resultSet.first();

                double percent = resultSet.getDouble("percentage");

                returnAbsenteeism = new Absenteeism(badgeId, ts, percent);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return returnAbsenteeism;

    }

    public void insertAbsenteeism(Absenteeism abs){
        Absenteeism ab = this.getAbsenteeism(abs.getBadgeId(), abs.getTimestampLong());

        try {
            if(ab == null){
                PreparedStatement pst;
                String query;

                //Try to request abs, if null, insert

                if (conn.isValid(0)){
                    //Insert
                    query = "INSERT INTO absenteeism (badgeid, payperiod, percentage) VALUES (?, ?, ?)";
                    pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    pst.setString(1, abs.getBadgeId());
                    pst.setTimestamp(2, abs.getTimestamp());
                    pst.setDouble(3, abs.getPercentage());

                    pst.execute();
                }
            }else{
                PreparedStatement pst;
                String query;

                //Try to request abs, if not null, update

                if (conn.isValid(0)){
                    //Update
                    query = "UPDATE absenteeism SET percentage = ?, payperiod = ? WHERE payperiod = ? AND badgeid = ?";
                    pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    pst.setDouble(1, abs.getPercentage());
                    pst.setString(4, abs.getBadgeId());
                    pst.setTimestamp(2, abs.getTimestamp());
                    pst.setTimestamp(3, abs.getTimestamp());

                    pst.execute();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
