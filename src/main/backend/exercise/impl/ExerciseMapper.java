package main.backend.exercise.impl;

import main.backend.common.ConnectionUtil;
import main.backend.exercise.IExerciseMapper;
import main.backend.exercise.entity.Exercise;
import main.backend.user.entity.User;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class ExerciseMapper implements IExerciseMapper {
    @Override
    public void save(Exercise exercise, User user) throws SQLException {
        String username = user.getUsername();
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = ConnectionUtil.getConnection();
            // get exercise attributes
            Date date = exercise.getDate();
            String type = exercise.getType();
            String intensity = exercise.getIntensity();
            int duration = exercise.getDuration();
            int burnCalories = exercise.getBurnCalories();

            // use PreparedStatement with placeholders
            String query = "insert into exercise(date, type, intensity, duration, burn_calories, user_id) ";
            query += "values (?, ?, ?, ?, ?, (select user_id from user where username = ?))";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setDate(1, date);
            ps.setString(2, type);
            ps.setString(3, intensity);
            ps.setInt(4, duration);
            ps.setInt(5, burnCalories);
            ps.setString(6, username);
            // insert row
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, null);
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = ConnectionUtil.getConnection();
            // use PreparedStatement with placeholders
            String query = "delete from exercise where exercise_id = ?";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setInt(1, id);
            // delete row
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, null);
        }
    }

    private List<Exercise> setExercise(PreparedStatement ps) throws SQLException {
        List<Exercise> exerciseList = new ArrayList<>();

        try (ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                // get the data from each column
                int id = res.getInt("exercise_id");
                Date date = res.getDate("date");
                String type = res.getString("type");
                String intensity = res.getString("intensity");
                int duration = res.getInt("duration");
                int burnCalories = res.getInt("burn_calories");
                // create a new Exercise object with the data
                Exercise exercise = new Exercise(id, date, type, intensity);
                exercise.setDuration(duration);
                exercise.setBurnCalories(burnCalories);
                // add the object to the list
                exerciseList.add(exercise);
            }
        }

        return exerciseList;
    }

    @Override
    public List<Exercise> getByUsername(String username) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        List<Exercise> exerciseList;

        try {
            connection = ConnectionUtil.getConnection();
            // use PreparedStatement with placeholders
            String query = "select exercise_id, date, type, intensity, duration, burn_calories ";
            query += "from exercise where user_id = (select user_id from user where username = ?) ";
            query += "order by date desc";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setString(1, username);
            // execute the query
            exerciseList = setExercise(ps);
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, null);
        }

        return exerciseList;
    }

    @Override
    public List<Exercise> getByPeriod(String username, Date startDate, Date endDate) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        List<Exercise> exerciseList;

        try {
            connection = ConnectionUtil.getConnection();
            // use PreparedStatement with placeholders
            String query = "select exercise_id, date, type, intensity, duration, burn_calories ";
            query += "from exercise where user_id = (select user_id from user where username = ?) ";
            query += "and Date between ? and ? order by date desc";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setString(1, username);
            ps.setDate(2, startDate);
            ps.setDate(3, endDate);
            // execute the query
            exerciseList = setExercise(ps);
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, null);
        }

        return exerciseList;
    }

    @Override
    public Map<Date, Float> getCaloriesByDate(User user, Date startDate, Date endDate) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet res = null;

        Map<Date, Float> calories = new LinkedHashMap<>();
        int userId = user.getId();

        try {
            connection = ConnectionUtil.getConnection();
            // use PreparedStatement with placeholders
            String query = "select date, sum(burn_calories) as total_calories ";
            query += "from exercise where user_id = ? and date between ? and ? ";
            query += "group by date order by date asc";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setInt(1, userId);
            ps.setDate(2, startDate);
            ps.setDate(3, endDate);
            // execute the query
            res = ps.executeQuery();
            while (res.next()) {
                // set meal
                Date date = res.getDate("date");
                float value = res.getFloat("total_calories");
                // add the object to the list
                calories.put(date, value);
            }
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, res);
        }

        return calories;
    }

    @Override
    public Map<Date, Integer> getDailyExerciseMinutesByDate(User user, Date startDate, Date endDate) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet res = null;

        Map<Date, Integer> exerciseMinutes = new LinkedHashMap<>();
        int userId = user.getId();

        try {
            connection = ConnectionUtil.getConnection();
            String query = "select date, sum(duration) as total_minutes ";
            query += "from exercise where user_id = ? and date between ? and ? ";
            query += "group by date order by date asc";
            ps = connection.prepareStatement(query);

            ps.setInt(1, userId);
            ps.setDate(2, startDate);
            ps.setDate(3, endDate);

            res = ps.executeQuery();
            while (res.next()) {
                Date date = res.getDate("date");
                int value = res.getInt("total_minutes");
                exerciseMinutes.put(date, value);
            }
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, res);
        }

        return exerciseMinutes;
    }

}
