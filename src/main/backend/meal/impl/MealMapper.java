package main.backend.meal.impl;

import main.backend.common.ConnectionUtil;
import main.backend.meal.IMealMapper;
import main.backend.food.entity.Food;
import main.backend.meal.entity.Meal;
import main.backend.user.entity.User;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class MealMapper implements IMealMapper { // not tested yet
    @Override
    public void save(Meal meal, User user) throws SQLException {
        Connection connection = null;

        try {
            connection = ConnectionUtil.getConnection();
            connection.setAutoCommit(false); // disable auto commit
            // get exercise attributes
            int userID = user.getId();
            Map<Food, Float> foodMap = meal.getFoodMap();

            // insert meal into database and get the new id
            int mealID = insertMeal(meal, userID, connection);
            // insert food used into database
            insertFoodUsed(connection, foodMap, mealID);

            connection.commit(); // commit update
        } catch (SQLException e) {
            connection.rollback(); // roll back if any exception happened
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, null, null);
        }
    }

    // a helper method to insert meal into database and return the new id
    private int insertMeal(Meal meal, int userID, Connection connection) throws SQLException {
        int mealID = -1;
        Date date = meal.getDate();
        String type = meal.getType();
        int totalCalories = meal.getTotalCalories();
        float totalProtein = meal.getTotalProtein();
        float totalCarbs = meal.getTotalCarbs();
        float totalVitamin = meal.getTotalVitamins();
        float totalOthers = meal.getTotalOthers();
        // use PreparedStatement with placeholders
        String query = """
            insert into meal(date, type, total_calories, total_vitamins,
            total_proteins, total_carbs, total_others, user_id)
            values (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            // set parameters with corresponding methods
            ps.setDate(1, date);
            ps.setString(2, type);
            ps.setInt(3, totalCalories);
            ps.setFloat(4, totalVitamin);
            ps.setFloat(5, totalProtein);
            ps.setFloat(6, totalCarbs);
            ps.setFloat(7, totalOthers);
            ps.setInt(8, userID);
            // insert row
            ps.execute();
            try (ResultSet res = ps.getGeneratedKeys()) {
                if (res.next()) mealID = res.getInt(1);
            }
        }

        return mealID;
    }

    // a helper method to insert food used into database
    private void insertFoodUsed(Connection connection, Map<Food, Float> foodMap, int mealID) throws SQLException {
        for (Map.Entry<Food, Float> entry : foodMap.entrySet()) {
            Food food = entry.getKey();
            int foodID = food.getId();
            float quantity = entry.getValue();
            // use PreparedStatement with placeholders
            String query = """
                insert into `food used`(meal_id, food_id, quantity)
                values (?, ?, ?)
            """;
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                // set parameters with corresponding methods
                ps.setInt(1, mealID);
                ps.setInt(2, foodID);
                ps.setFloat(3, quantity);
                // insert row
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = ConnectionUtil.getConnection();
            // use PreparedStatement with placeholders
            String query = "delete from meal where meal_id = ?";
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

    @Override
    public Meal getByDateAndType(Date date, User user, String type) throws SQLException {
        Meal meal = null;
        ResultSet res = null;
        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = ConnectionUtil.getConnection();
            int userId = user.getId();

            String query = "select * from meal where user_id = ? and date = ? and type = ?";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setInt(1, userId);
            ps.setDate(2, date);
            ps.setString(3, type);
            res = ps.executeQuery();
            if (res.next()) {
                meal = setMeal(res);
            }
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, null);
        }

        return meal;
    }

    private void setMealFoodMap(List<Meal> mealList, Connection conn) throws SQLException {
        for (Meal meal : mealList) {
            // language=SQL
            String query = """
                select ct.food_id, ct.FoodDescription, fg.FoodGroupName, ct.quantity from (
                    select fu.food_id, fn.FoodDescription,  fn.FoodGroupID, fu.quantity
                    from `food used` fu join `food name` fn
                    on fn.FoodID = fu.food_id and fu.meal_id = ?
                ) as ct
                join `food group` as fg
                on ct.FoodGroupID = fg.FoodGroupID
            """;

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, meal.getId());
                try (ResultSet res = ps.executeQuery()) {
                    while (res.next()) {
                        int foodID = res.getInt("food_id");
                        String description = res.getString("FoodDescription");
                        float quantity = res.getFloat("quantity");
                        String group = res.getString("FoodGroupName");

                        Map<Food, Float> foodMap = meal.getFoodMap();
                        Food food = new Food(foodID, description, group);
                        foodMap.put(food, quantity);
                        meal.setFoodMap(foodMap);
                    }
                }
            }
        }
    }

    private Meal setMeal(ResultSet res) throws SQLException {
        // get the data from each column
        int mealId = res.getInt("meal_id");
        Date date = res.getDate("date");
        String type = res.getString("type");
        int totalCalories = res.getInt("total_calories");
        float totalVitamin = res.getFloat("total_vitamins");
        float totalProtein = res.getFloat("total_proteins");
        float totalCarbs = res.getFloat("total_carbs");
        float totalOthers = res.getFloat("total_others");
        // create a new Exercise object with the data
        Meal meal = new Meal(mealId, date, type);
        meal.setTotalCalories(totalCalories);
        meal.setTotalCarbs(totalCarbs);
        meal.setTotalOthers(totalOthers);
        meal.setTotalProtein(totalProtein);
        meal.setTotalVitamins(totalVitamin);
        System.out.println("pass");
        meal.setFoodMap(new HashMap<>());

        return meal;
    }

    @Override
    public Meal getByID(int id) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet res = null;
        Meal meal = null;

        try {
            connection = ConnectionUtil.getConnection();
            // use PreparedStatement with placeholders
            String query = "select meal_id, date, type, total_calories, total_vitamins, ";
            query += "total_proteins, total_carbs, total_others ";
            query += "from meal where meal_id = ?";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setInt(1, id);
            res = ps.executeQuery();
            if (res.next()) {
                // set meal
                meal = setMeal(res);
            }

            setMealFoodMap(Collections.singletonList(meal), connection);
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, res);
        }

        return meal;
    }

    @Override
    public List<Meal> getByUser(User user) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet res = null;

        List<Meal> mealList = new ArrayList<>();
        int userId = user.getId();

        try {
            connection = ConnectionUtil.getConnection();
            // use PreparedStatement with placeholders
            String query = "select meal_id, date, type, total_calories, total_vitamins, ";
            query += "total_proteins, total_carbs, total_others ";
            query += "from meal where user_id = ? order by date desc, meal_id desc";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setInt(1, userId);
            // execute the query
            res = ps.executeQuery();
            while (res.next()) {
                // set meal
                Meal meal = setMeal(res);
                // add the object to the list
                mealList.add(meal);
            }

            setMealFoodMap(mealList, connection);
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, res);
        }

        return mealList;
    }

    @Override
    public List<Meal> getByPeriod(User user, Date startDate, Date endDate) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet res = null;

        List<Meal> mealList = new ArrayList<>();
        int userId = user.getId();

        try {
            connection = ConnectionUtil.getConnection();
            // use PreparedStatement with placeholders
            String query = "select meal_id, date, type, total_calories, total_vitamins, ";
            query += "total_proteins, total_carbs, total_others ";
            query += "from meal where user_id = ? and Date between ? and ? order by date desc, meal_id desc";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setInt(1, userId);
            ps.setDate(2, startDate);
            ps.setDate(3, endDate);
            // execute the query
            res = ps.executeQuery();
            while (res.next()) {
                // set meal
                Meal meal = setMeal(res);
                // add the object to the list
                mealList.add(meal);
            }

            setMealFoodMap(mealList, connection);
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, res);
        }

        return mealList;
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
            String query = "select date, sum(total_calories) as total_calories ";
            query += "from meal where user_id = ? and date between ? and ? ";
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
}
