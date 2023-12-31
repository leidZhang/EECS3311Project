package main.backend.food.impl;

import main.backend.common.ConnectionUtil;
import main.backend.food.IFoodMapper;
import main.backend.food.entity.Food;
import main.backend.food.entity.Nutrient;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodMapper implements IFoodMapper {
    @Override
    public List<Food> getList() throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet res = null;
        List<Food> list = new ArrayList<>();

        try {
            connection = ConnectionUtil.getConnection();

            String query = "select fn.FoodID, fn.FoodDescription, fg.FoodGroupName ";
            query += "from `food name` fn join `food group` fg ";
            query += "where fn.FoodGroupID = fg.FoodGroupID";
            ps = connection.prepareStatement(query);
            // execute the query
            res = ps.executeQuery();
            while (res.next()) {
                int id = res.getInt("FoodID");
                String foodDescription = res.getString("FoodDescription");
                String foodGroup = res.getString("FoodGroupName");

                Food food = new Food(id, foodDescription, foodGroup);
                food.setNutrientFloatMap(getUnitNutrientValue(id));

                list.add(food);
            }
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, res);
        }

        return list;
    }

    @Override
    public Map<Nutrient, Float> getUnitNutrientValue(int id) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet res = null;
        Map<Nutrient, Float> nutrientFloatMap = new HashMap<>();

        try {
            connection = ConnectionUtil.getConnection();

            // get nutrient values from Nutrient amount
            String query = "select nt.NutrientSymbol, nt.NutrientName, nt.NutrientValue, nt.NutrientUnit ";
            query += "from `food name` as fn inner join (";
            query += "select na.FoodID, nn.NutrientID, nn.NutrientSymbol, nn.NutrientName, na.NutrientValue, nn.NutrientUnit ";
            query += "from `nutrient name` as nn inner join `nutrient amount` as na ";
            query += "on nn.NutrientID = na.NutrientNameID and nn.NutrientName not regexp '^^FATTY ACID(?!.*TOTAL$).*') as nt ";
            query += "on fn.FoodID = nt.FoodID and fn.FoodID = ? and nt.NutrientName <> 'ENERGY (KILOCALORIES)' ";
            query += "and nt.NutrientName <> 'ENERGY (KILOJOULES)' and nt.NutrientName <> 'TOTAL NIACIN EQUIVALENT'";
            query += "and nt.NutrientName <> 'VITAMIN D (INTERNATIONAL UNITS)' and nt.NutrientValue <> 0 ";
            query += "and nt.NutrientName <> 'ASH, TOTAL' and nt.NutrientName <> 'SUGARS, TOTAL' ";
            query += "and nt.NutrientName <> 'FAT (TOTAL LIPIDS)' and nt.NutrientName <> 'FIBRE, TOTAL DIETARY'";
            query += "and nt.NutrientName <> 'TOTAL FOLACIN'";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setInt(1, id);
            // execute the query
            res = ps.executeQuery();
            while (res.next()) {
                String nutrientSymbol = res.getString("NutrientSymbol");
                String nutrientName = res.getString("NutrientName");
                float unitValue = res.getFloat("NutrientValue");
                String nutrientUnit = res.getString("NutrientUnit");
                Nutrient nutrient = new Nutrient(nutrientSymbol, nutrientUnit, nutrientName);
                nutrientFloatMap.put(nutrient, unitValue);
            }
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, res);
        }

        return nutrientFloatMap;
    }

    @Override
    public long getUnitCalories(int id) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet res = null;

        long unitCalories = 0;
        try {
            connection = ConnectionUtil.getConnection();

            // get nutrient values from Nutrient amount
            String query = "select nt.NutrientValue ";
            query += "from `food name` as fn inner join (";
            query += "select na.FoodID, nn.NutrientID, nn.NutrientName, na.NutrientValue, nn.NutrientUnit ";
            query += "from `nutrient name` as nn inner join `nutrient amount` as na ";
            query += "on nn.NutrientID = na.NutrientNameID) as nt ";
            query += "on fn.FoodID = nt.FoodID and fn.FoodID = ? and nt.NutrientName = 'ENERGY (KILOCALORIES)' ";
            ps = connection.prepareStatement(query);
            // set parameters with corresponding methods
            ps.setInt(1, id);
            // execute the query
            res = ps.executeQuery();
            if (res.next()) {
                unitCalories = (long) res.getFloat("NutrientValue");
            }
        } catch (SQLException e) {
            throw new SQLException(e.getMessage());
        } finally {
            ConnectionUtil.close(connection, ps, res);
        }

        return unitCalories; // kCal
    }
}


